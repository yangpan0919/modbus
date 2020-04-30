package com.study.modbus;

import com.serotonin.modbus4j.code.DataType;
import com.serotonin.modbus4j.exception.ErrorResponseException;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.ip.tcp.TcpMaster;
import com.study.modbus.constant.Constants;
import com.study.modbus.entity.DataInfo;
import com.study.modbus.util.ModbusUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;


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
    public ModbusConnection(String deviceCode, String ip, int port, ModbusEventDealer modbusEventDealer, long initSleep, int initLinkCount) {
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
     * @param command
     * @return
     */
    public synchronized void executeCommand(String command) {


    }

    public static void main(String[] args) throws ErrorResponseException, ModbusTransportException {
        ModbusConnection modbusConnection = new ModbusConnection("test", "localhost", 502, null, 1000, -1);
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

        boolean b1 = ModbusUtils.writeRegisters(master, 1, 3, (short) 2, (short) 2, (short) 2);
        System.out.println(b1);

        boolean b2 = ModbusUtils.writeMaskRegister(master, 1, 0,2, 2);
        System.out.println(b2);

    }


}
