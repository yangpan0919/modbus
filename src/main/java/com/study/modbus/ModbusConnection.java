package com.study.modbus;

import com.serotonin.modbus4j.BatchRead;
import com.serotonin.modbus4j.BatchResults;
import com.serotonin.modbus4j.code.DataType;
import com.serotonin.modbus4j.exception.ErrorResponseException;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.ip.tcp.TcpMaster;
import com.serotonin.modbus4j.locator.BaseLocator;
import com.study.modbus.constant.Constants;
import com.study.modbus.entity.DataInfo;
import com.study.modbus.entity.Response;
import com.study.modbus.util.ModbusUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.time.LocalDateTime;
import java.util.Arrays;


/**
 * modbusMaster类,封装
 * modbus中 master 相当于client端，slave相当于server端
 */
public class ModbusConnection {

    private static final Logger logger = Logger.getLogger(ModbusConnection.class);

    /**
     * 基本信息
     */
    private DataInfo dataInfo;

    /**
     * 事件触发回调接口
     */
    private ModbusEventDealer modbusEventDealer;
    /**
     * slaveId
     */
    private int slaveId;

    private TcpMaster master;

    public BatchRead<String> batchRead = new BatchRead();

    public TcpMaster getMaster() {
        return master;
    }

    public void setMaster(TcpMaster master) {
        this.master = master;
    }

    /**
     * @param deviceCode
     * @param ip
     * @param port
     * @param modbusEventDealer
     * @param initSleep         初始化才有效
     * @param initLinkCount     初始化才有效
     */
    public ModbusConnection(int slaveId, String deviceCode, String ip, int port, ModbusEventDealer modbusEventDealer, long initSleep, int initLinkCount) {
        this.slaveId = slaveId;
        dataInfo = new DataInfo(ip, port, initSleep, initLinkCount, deviceCode);
        this.modbusEventDealer = modbusEventDealer;
        if (!doConnect()) {
            linkTask();
        }

    }

    /**
     * 重连任务 , 根据 dataInfo.getLinkNum() 进行n次重连，
     * dataInfo.getSleepTime() 设置间隔时间
     */
    public void linkTask() {
        boolean flag = dataInfo.getLinkNum() < 0;
        new Thread(() -> {
            MDC.put(Constants.LOG_EQPCODE, dataInfo.getDeviceCode());
            int tempNum = flag ? 1 : dataInfo.getLinkNum();
            for (int i = 0; i < tempNum; i++) {
                if (flag) i--;
                if (doConnect()) {
                    break;
                }
                try {
                    Thread.sleep(dataInfo.getSleepTime());
                } catch (InterruptedException ex) {
                    logger.error("休眠线程被中断", ex);
                }
            }

        }).start();
    }

    /**
     * 进行连接
     *
     * @return
     */
    private boolean doConnect() {
        boolean result = false;
        try {
            this.master = (TcpMaster) ModbusUtils.getMaster(dataInfo.getIp(), dataInfo.getPort());
//            //表示初始连接上了
//            modbusEventDealer.gotoComplete();
            result = true;
        } catch (ModbusInitException e) {
        }
        return result;
    }

    /**
     * 使用前需要对 batchRead 进行初始化
     * 批量获取点位结果
     * 方便用于定时获取点位的结果
     *
     * @return
     * @throws ErrorResponseException
     * @throws ModbusTransportException
     */
    public BatchResults<String> batchRead() throws ErrorResponseException, ModbusTransportException {
        return batchRead(batchRead);
    }


    /**
     * 批量获取点位结果
     *
     * @return
     * @throws ErrorResponseException
     * @throws ModbusTransportException
     */
    public BatchResults<String> batchRead(BatchRead<String> batchRead) throws ErrorResponseException, ModbusTransportException {
        return master.send(batchRead);
    }

    /**
     * 执行命令
     * write/read 1 0
     *
     * @return
     */
    public Response executeCommand(boolean type, int functionType, int offset, Object object, int dateType) {

        Response response = new Response();
        try {
            if (master == null) {
                response.setStatus(Constants.ERROR_RESPONSE);
                response.setErrorDesc("master还没有与slave连接上");
                return response;
            }

            if (type) {
                //读
                Number result = null;
                switch (functionType) {
                    case 1:
                        result = ModbusUtils.readCoilStatus(master, slaveId, offset) ? 1 : 0;
                        break;
                    case 2:
                        result = ModbusUtils.readInputStatus(master, slaveId, offset) ? 1 : 0;
                        break;
                    case 3:
                        result = ModbusUtils.readHoldingRegister(master, slaveId, offset, dateType);
                        break;
                    case 4:
                        result = ModbusUtils.readInputRegisters(master, slaveId, offset, dateType);
                        break;
                    default:
                        response.setStatus(Constants.ERROR_RESPONSE);
                        String desc = "没有该读类型的functionId:" + functionType;
                        response.setErrorDesc(desc);
                        logger.error(desc);
                }
                response.setResult(result);
                return response;
            }
            //写
            switch (functionType) {
                case 1:
                    ModbusUtils.writeCoils(master, slaveId, offset, (Boolean) object);
                    break;
                case 3:
                    if (dateType == DataType.FOUR_BYTE_FLOAT) {
                        short[] shorts = ModbusUtils.valueToShorts((Number) object);
                        ModbusUtils.writeRegisters(master, slaveId, offset, shorts);
                        break;
                    } else if (dateType == DataType.FOUR_BYTE_INT_SIGNED) {
                        ModbusUtils.writeRegister(master, slaveId, offset, (Integer) object);
                        break;
                    }
                default:
                    logger.error("没有该写类型的functionId:" + functionType + ":" + dateType);
            }
        } catch (ModbusTransportException e) {
            //通信错误
            response.setStatus(Constants.ERROR_RESPONSE);
            response.setErrorDesc("通信错误");
            logger.error("通信错误", e);
        } catch (ErrorResponseException e) {
            //返回结果失败
            response.setStatus(Constants.ERROR_RESPONSE);
            response.setErrorDesc("返回结果失败");
            logger.error("返回结果失败", e);
        } catch (Exception e) {
            //没有预知的错误
            response.setStatus(Constants.ERROR_RESPONSE);
            response.setErrorDesc("没有预知的错误");
            logger.error("没有预知的错误", e);
        }

        return response;
    }

    public static void main(String[] args) throws ErrorResponseException, ModbusTransportException, InterruptedException {
        ModbusConnection modbusConnection = new ModbusConnection(1, "test", "192.168.2.250", 502, null, 1000, -1);
        TcpMaster master = modbusConnection.getMaster();


        //batch
        BatchRead<String> batchRead = new BatchRead();
        batchRead.addLocator("00001", BaseLocator.coilStatus(1, 0));
        batchRead.addLocator("00002", BaseLocator.coilStatus(1, 1));
        batchRead.addLocator("00003", BaseLocator.coilStatus(1, 2));
        batchRead.addLocator("00004", BaseLocator.coilStatus(1, 3));

        batchRead.addLocator("10001", BaseLocator.inputStatus(1, 0));
        batchRead.addLocator("10002", BaseLocator.inputStatus(1, 1));
        batchRead.addLocator("10003", BaseLocator.inputStatus(1, 2));
        batchRead.addLocator("10004", BaseLocator.inputStatus(1, 3));

        batchRead.addLocator("30001", BaseLocator.inputRegister(1, 0, DataType.FOUR_BYTE_FLOAT));


        batchRead.addLocator("40001", BaseLocator.holdingRegister(1, 0, DataType.FOUR_BYTE_FLOAT));
        batchRead.addLocator("40003", BaseLocator.holdingRegister(1, 1, DataType.FOUR_BYTE_FLOAT));
        batchRead.addLocator("40005", BaseLocator.holdingRegister(1, 2, DataType.FOUR_BYTE_FLOAT));
//        int slaveId = 31;
//        batchRead.addLocator("00011 sb true", BaseLocator.coilStatus(slaveId, 10));
//        batchRead.addLocator("00012 sb false", BaseLocator.coilStatus(slaveId, 11));
//        batchRead.addLocator("00013 sb true", BaseLocator.coilStatus(slaveId, 12));
//        batchRead.addLocator("00014 sb true", BaseLocator.coilStatus(slaveId, 13));
//
//        batchRead.addLocator("10011 sb false", BaseLocator.inputStatus(slaveId, 10));
//        batchRead.addLocator("10012 sb false", BaseLocator.inputStatus(slaveId, 11));
//        batchRead.addLocator("10013 sb true", BaseLocator.inputStatus(slaveId, 12));
//        batchRead.addLocator("10014 sb false", BaseLocator.inputStatus(slaveId, 13));
//
//        batchRead.addLocator("40016-0 sb true", BaseLocator.holdingRegisterBit(slaveId, 40016, 0));
//        batchRead.addLocator("40016-1 sb false", BaseLocator.holdingRegisterBit(slaveId, 40016, 1));
//        batchRead.addLocator("40016-2 sb false", BaseLocator.holdingRegisterBit(slaveId, 40016, 2));
//        batchRead.addLocator("40016-3 sb true", BaseLocator.holdingRegisterBit(slaveId, 40016, 3));
//        batchRead.addLocator("40016-4 sb false", BaseLocator.holdingRegisterBit(slaveId, 40016, 4));
//        batchRead.addLocator("40016-5 sb false", BaseLocator.holdingRegisterBit(slaveId, 40016, 5));
//        batchRead.addLocator("40016-6 sb false", BaseLocator.holdingRegisterBit(slaveId, 40016, 6));
//        batchRead.addLocator("40016-7 sb true", BaseLocator.holdingRegisterBit(slaveId, 40016, 7));
//        batchRead.addLocator("40016-8 sb true", BaseLocator.holdingRegisterBit(slaveId, 40016, 8));
//        batchRead.addLocator("40016-9 sb false", BaseLocator.holdingRegisterBit(slaveId, 40016, 9));
//        batchRead.addLocator("40016-a sb false", BaseLocator.holdingRegisterBit(slaveId, 40016, 10));
//        batchRead.addLocator("40016-b sb false", BaseLocator.holdingRegisterBit(slaveId, 40016, 11));
//        batchRead.addLocator("40016-c sb false", BaseLocator.holdingRegisterBit(slaveId, 40016, 12));
//        batchRead.addLocator("40016-d sb false", BaseLocator.holdingRegisterBit(slaveId, 40016, 13));
//        batchRead.addLocator("40016-e sb true", BaseLocator.holdingRegisterBit(slaveId, 40016, 14));
//        batchRead.addLocator("40016-f sb false", BaseLocator.holdingRegisterBit(slaveId, 40016, 15));
//
//        batchRead.addLocator("30016-0 sb true", BaseLocator.inputRegisterBit(slaveId, 30016, 0));
//        batchRead.addLocator("30016-1 sb false", BaseLocator.inputRegisterBit(slaveId, 30016, 1));
//        batchRead.addLocator("30016-2 sb false", BaseLocator.inputRegisterBit(slaveId, 30016, 2));
//        batchRead.addLocator("30016-3 sb false", BaseLocator.inputRegisterBit(slaveId, 30016, 3));
//        batchRead.addLocator("30016-4 sb false", BaseLocator.inputRegisterBit(slaveId, 30016, 4));
//        batchRead.addLocator("30016-5 sb false", BaseLocator.inputRegisterBit(slaveId, 30016, 5));
//        batchRead.addLocator("30016-6 sb false", BaseLocator.inputRegisterBit(slaveId, 30016, 6));
//        batchRead.addLocator("30016-7 sb true", BaseLocator.inputRegisterBit(slaveId, 30016, 7));
//        batchRead.addLocator("30016-8 sb true", BaseLocator.inputRegisterBit(slaveId, 30016, 8));
//        batchRead.addLocator("30016-9 sb false", BaseLocator.inputRegisterBit(slaveId, 30016, 9));
//        batchRead.addLocator("30016-a sb false", BaseLocator.inputRegisterBit(slaveId, 30016, 10));
//        batchRead.addLocator("30016-b sb false", BaseLocator.inputRegisterBit(slaveId, 30016, 11));
//        batchRead.addLocator("30016-c sb false", BaseLocator.inputRegisterBit(slaveId, 30016, 12));
//        batchRead.addLocator("30016-d sb false", BaseLocator.inputRegisterBit(slaveId, 30016, 13));
//        batchRead.addLocator("30016-e sb false", BaseLocator.inputRegisterBit(slaveId, 30016, 14));
//        batchRead.addLocator("30016-f sb true", BaseLocator.inputRegisterBit(slaveId, 30016, 15));
//
//        batchRead.addLocator("40017 sb -1968",
//                BaseLocator.holdingRegister(slaveId, 40017, DataType.TWO_BYTE_INT_SIGNED));
//        batchRead.addLocator("40018 sb -123456789",
//                BaseLocator.holdingRegister(slaveId, 40018, DataType.FOUR_BYTE_INT_SIGNED));
//        batchRead.addLocator("40020 sb -123456789",
//                BaseLocator.holdingRegister(slaveId, 40020, DataType.FOUR_BYTE_INT_SIGNED_SWAPPED));
//        batchRead.addLocator("40022 sb 1968.1968",
//                BaseLocator.holdingRegister(slaveId, 40022, DataType.FOUR_BYTE_FLOAT));
//        batchRead.addLocator("40024 sb -123456789",
//                BaseLocator.holdingRegister(slaveId, 40024, DataType.EIGHT_BYTE_INT_SIGNED));
//        batchRead.addLocator("40028 sb -123456789",
//                BaseLocator.holdingRegister(slaveId, 40028, DataType.EIGHT_BYTE_INT_SIGNED_SWAPPED));
//        batchRead.addLocator("40032 sb 1968.1968",
//                BaseLocator.holdingRegister(slaveId, 40032, DataType.EIGHT_BYTE_FLOAT));
//
//        batchRead.addLocator("30017 sb -1968 tc",
//                BaseLocator.inputRegister(slaveId, 30017, DataType.TWO_BYTE_INT_UNSIGNED));
//        batchRead.addLocator("30018 sb -123456789 tc",
//                BaseLocator.inputRegister(slaveId, 30018, DataType.FOUR_BYTE_INT_UNSIGNED));
//        batchRead.addLocator("30020 sb -123456789 tc",
//                BaseLocator.inputRegister(slaveId, 30020, DataType.FOUR_BYTE_INT_UNSIGNED_SWAPPED));
//        batchRead.addLocator("30022 sb 1968.1968",
//                BaseLocator.inputRegister(slaveId, 30022, DataType.FOUR_BYTE_FLOAT_SWAPPED));
//        batchRead.addLocator("30024 sb -123456789 tc",
//                BaseLocator.inputRegister(slaveId, 30024, DataType.EIGHT_BYTE_INT_UNSIGNED));
//        batchRead.addLocator("30028 sb -123456789 tc",
//                BaseLocator.inputRegister(slaveId, 30028, DataType.EIGHT_BYTE_INT_UNSIGNED_SWAPPED));
//        batchRead.addLocator("30032 sb 1968.1968",
//                BaseLocator.inputRegister(slaveId, 30032, DataType.EIGHT_BYTE_FLOAT_SWAPPED));

        BatchResults<String> results = master.send(batchRead);

        System.out.println(results);


        boolean[] booleans1 = ModbusUtils.readDiscreteInputs(master, 1, 0, 4);
        boolean[] booleans = ModbusUtils.readCoils(master, 1, 0, 4);
        float[] floats1 = ModbusUtils.readInputRegistersRange(master, 1, 0, 4);
        ModbusConnection modbusConnection2 = new ModbusConnection(1, "test", "192.168.2.250", 502, null, 1000, -1);
        TcpMaster master2 = modbusConnection2.getMaster();
        float[] floats = ModbusUtils.readHoldingRegisters(master2, 1, 0, 10);

//        Boolean aBoolean = ModbusUtils.readCoilStatus(master, 1, 1);
//        System.out.println(aBoolean);
        Number number = ModbusUtils.readHoldingRegister(master, 1, 0, DataType.FOUR_BYTE_FLOAT);
        System.out.println(number);
//
//        aBoolean = ModbusUtils.readInputStatus(master, 1, 1);
//        System.out.println(aBoolean);

        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                Number number1 = null;
                try {
                    number1 = ModbusUtils.readInputRegisters(master, 1, 0, DataType.TWO_BYTE_INT_UNSIGNED);
                } catch (ModbusTransportException e) {
                    e.printStackTrace();
                } catch (ErrorResponseException e) {
                    e.printStackTrace();
                }
                System.out.print(LocalDateTime.now());
                System.out.println("---" + number);
            }).start();

        }
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                float[] floats2 = null;
                try {
                    floats2 = ModbusUtils.readHoldingRegisters(master2, 1, 0, 10);
                } catch (ModbusTransportException e) {
                    e.printStackTrace();
                } catch (ErrorResponseException e) {
                    e.printStackTrace();
                }
                System.out.print(LocalDateTime.now());
                System.out.println("+++" + Arrays.toString(floats2));
            }).start();

        }

//        number = ModbusUtils.readInputRegisters(master, 1, 0, DataType.TWO_BYTE_INT_SIGNED);
//        System.out.println(number);
//
//        boolean b = ModbusUtils.writeCoils(master, 1, 0, false, false, false);
//        System.out.println(b);
//
//        short[] shorts = ModbusUtils.valueToShorts(3.76f);
//        boolean b1 = ModbusUtils.writeRegisters(master, 1, 0, shorts);
//        System.out.println(b1);
//
//        boolean b2 = ModbusUtils.writeRegister(master, 1, 2, 100);
//        System.out.println(b2);
//
//        boolean b3 = ModbusUtils.writeMaskRegister(master, 1, 4, 1, 111);
//        System.out.println(b3);
//        char[] chars = s.toCharArray();
//        StringBuilder sb = new StringBuilder();
//        for (int i = chars.length - 1; i >= 0; i--) {
//            sb.append(chars[i]);
//        }
//        String s1 = sb.toString();
//
//        System.out.println(binToInt(s1));
//
//
//        BitArray bitArray = new BitArray(22);
//        System.out.println(bitArray.toString());
//        boolean b2 = ModbusUtils.writeMaskRegister(master, 1, 0, 0, 1);
//        b2 = ModbusUtils.writeMaskRegister(master, 1, 2, 0, -567775230);
//        b2 = ModbusUtils.writeMaskRegister(master, 1, 4, 0, 1);
//        System.out.println(b2);


    }


}
