package com.study.modbus;

import com.serotonin.modbus4j.code.DataType;
import com.serotonin.modbus4j.exception.ErrorResponseException;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.ip.tcp.TcpMaster;
import com.study.modbus.constant.Constants;
import com.study.modbus.entity.DataInfo;
import com.study.modbus.entity.Response;
import com.study.modbus.util.ModbusUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import sun.security.util.BitArray;

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
     * 执行命令
     *
     * @return
     */
    public synchronized Response executeCommand(boolean type, int functionType, int offset, Object object, int dateType) {

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
        ModbusConnection modbusConnection = new ModbusConnection(1, "test", "localhost", 502, null, 1000, -1);
        TcpMaster master = modbusConnection.getMaster();


        Boolean aBoolean = ModbusUtils.readCoilStatus(master, 1, 1);
        System.out.println(aBoolean);

        aBoolean = ModbusUtils.readInputStatus(master, 1, 1);
        System.out.println(aBoolean);

        Number number = ModbusUtils.readHoldingRegister(master, 1, 0, DataType.FOUR_BYTE_FLOAT);
        System.out.println(number);

        number = ModbusUtils.readInputRegisters(master, 1, 0, DataType.TWO_BYTE_INT_SIGNED);
        System.out.println(number);

        boolean b = ModbusUtils.writeCoils(master, 1, 0, false, false, false);
        System.out.println(b);

        short[] shorts = ModbusUtils.valueToShorts(2.85f);
        boolean b1 = ModbusUtils.writeRegisters(master, 1, 0, shorts);
        System.out.println(b1);

        boolean b2 = ModbusUtils.writeRegister(master, 1, 2, 100);
        System.out.println(b2);

        boolean b3 = ModbusUtils.writeMaskRegister(master, 1, 4, 1, 111);
        System.out.println(b3);
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
