package com.study.modbus.util;

import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.exception.ErrorResponseException;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.ip.IpParameters;
import com.serotonin.modbus4j.locator.BaseLocator;
import com.serotonin.modbus4j.msg.*;

import java.util.Arrays;

/**
 * modbus通讯工具类,采用modbus4j实现
 */
public class ModbusUtils {
    /**
     * 工厂。
     */
    static ModbusFactory modbusFactory = new ModbusFactory();

    /**
     * 获取master
     *
     * @return
     * @throws ModbusInitException
     */
    public static ModbusMaster getMaster(String ip, int port) throws ModbusInitException {
        IpParameters params = new IpParameters();
        params.setHost(ip);
        params.setPort(port);
        //
        // modbusFactory.createRtuMaster(wapper); //RTU 协议
        // modbusFactory.createUdpMaster(params);//UDP 协议
        // modbusFactory.createAsciiMaster(wrapper);//ASCII 协议
        ModbusMaster master = modbusFactory.createTcpMaster(params, false);// TCP 协议
        master.init();

        return master;
    }

    /**
     * 读取[01 Coil Status 0x]类型 开关数据
     *
     * @param slaveId slaveId
     * @param offset  位置
     * @return 读取值
     * @throws ModbusTransportException 异常
     * @throws ErrorResponseException   异常
     */
    public static Boolean readCoilStatus(ModbusMaster master, int slaveId, int offset)
            throws ModbusTransportException, ErrorResponseException {
        // 01 Coil Status
        BaseLocator<Boolean> loc = BaseLocator.coilStatus(slaveId, offset);
        Boolean value = master.getValue(loc);
        return value;
    }

    /**
     * 批量读取开关量  00001
     *
     * @param master
     * @param slaveId
     * @param start
     * @param len
     * @return
     * @throws ModbusTransportException
     * @throws ErrorResponseException
     */
    public static boolean[] readCoils(ModbusMaster master, int slaveId, int start, int len) throws ModbusTransportException, ErrorResponseException {

        ReadCoilsRequest request = new ReadCoilsRequest(slaveId, start, len);
        ReadCoilsResponse response = (ReadCoilsResponse) master.send(request);

        boolean[] result = bytesToBoolean(response.getData(), len);
        if (response.isException())
            throw new ErrorResponseException(request, response);
        return result;
    }

    /**
     * 批量读取开关量  10001
     *
     * @param master
     * @param slaveId
     * @param start
     * @param len
     * @return
     * @throws ModbusTransportException
     * @throws ErrorResponseException
     */
    public static boolean[] readDiscreteInputs(ModbusMaster master, int slaveId, int start, int len) throws ModbusTransportException, ErrorResponseException {

        ReadDiscreteInputsRequest request = new ReadDiscreteInputsRequest(slaveId, start, len);
        ReadDiscreteInputsResponse response = (ReadDiscreteInputsResponse) master.send(request);

        boolean[] result = bytesToBoolean(response.getData(), len);
        if (response.isException())
            throw new ErrorResponseException(request, response);
        return result;
    }

    /**
     * 读取[02 Input Status 1x]类型 开关数据
     *
     * @param slaveId
     * @param offset
     * @return
     * @throws ModbusTransportException
     * @throws ErrorResponseException
     */
    public static Boolean readInputStatus(ModbusMaster master, int slaveId, int offset)
            throws ModbusTransportException, ErrorResponseException {
        // 02 Input Status
        BaseLocator<Boolean> loc = BaseLocator.inputStatus(slaveId, offset);
        Boolean value = master.getValue(loc);
        return value;
    }

    /**
     * 读取[03 Holding Register类型 2x]模拟量数据
     *
     * @param slaveId  slave Id
     * @param offset   位置
     * @param dataType 数据类型,来自com.serotonin.modbus4j.code.DataType
     * @return
     * @throws ModbusTransportException 异常
     * @throws ErrorResponseException   异常
     */
    public static Number readHoldingRegister(ModbusMaster master, int slaveId, int offset, int dataType)
            throws ModbusTransportException, ErrorResponseException {
        // 03 Holding Register类型数据读取
        BaseLocator<Number> loc = BaseLocator.holdingRegister(slaveId, offset, dataType);
        Number value = master.getValue(loc);
        return value;
    }

    /**
     * 批量读取浮点型数据 40001
     *
     * @param master
     * @param slaveId
     * @param start
     * @param len
     * @return
     * @throws ModbusTransportException
     * @throws ErrorResponseException
     */
    public static float[] readHoldingRegisters(ModbusMaster master, int slaveId, int start, int len) throws ModbusTransportException, ErrorResponseException {

        ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest(slaveId, start, len);
        ReadHoldingRegistersResponse response = (ReadHoldingRegistersResponse) master.send(request);
        float[] floats = bytesToFloat(response.getData());
//        float[] floats = shortsToFloat(response.getShortData());
        if (response.isException())
            throw new ErrorResponseException(request, response);

        return floats;
    }

    /**
     * 获取byte字节 数组
     *
     * @param master
     * @param slaveId
     * @param start
     * @param len
     * @return
     * @throws ModbusTransportException
     * @throws ErrorResponseException
     */
    public static byte[] readHoldingRegistersGetByte(ModbusMaster master, int slaveId, int start, int len) throws ModbusTransportException, ErrorResponseException {

        ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest(slaveId, start, len);
        ReadHoldingRegistersResponse response = (ReadHoldingRegistersResponse) master.send(request);
        if (response.isException())
            throw new ErrorResponseException(request, response);
        return response.getData();

    }

    /**
     * 批量读取浮点型数据  30001
     *
     * @param master
     * @param slaveId
     * @param start
     * @param len
     * @return
     * @throws ModbusTransportException
     * @throws ErrorResponseException
     */
    public static float[] readInputRegistersRange(ModbusMaster master, int slaveId, int start, int len) throws ModbusTransportException, ErrorResponseException {
        ReadInputRegistersRequest request = new ReadInputRegistersRequest(slaveId, start, len);
        ReadInputRegistersResponse response = (ReadInputRegistersResponse) master.send(request);
        float[] floats = bytesToFloat(response.getData());
//        float[] floats = shortsToFloat(response.getShortData());
        if (response.isException())
            throw new ErrorResponseException(request, response);

        return floats;
    }


    /**
     * 读取[04 Input Registers 3x]类型 模拟量数据
     *
     * @param slaveId  slaveId
     * @param offset   位置
     * @param dataType 数据类型,来自com.serotonin.modbus4j.code.DataType
     * @return 返回结果
     * @throws ModbusTransportException 异常
     * @throws ErrorResponseException   异常
     */
    public static Number readInputRegisters(ModbusMaster master, int slaveId, int offset, int dataType)
            throws ModbusTransportException, ErrorResponseException {
        // 04 Input Registers类型数据读取
        BaseLocator<Number> loc = BaseLocator.inputRegister(slaveId, offset, dataType);
        Number value = master.getValue(loc);
        return value;
    }

    /**
     * 写 [5 WRITE  Coil Status]类型 模拟量数据
     *
     * @param slaveId slaveId
     * @return 返回结果
     * @throws ModbusTransportException 异常
     * @throws ErrorResponseException   异常
     */
    public static boolean writeCoil(ModbusMaster master, int slaveId, int start, boolean values) throws ErrorResponseException, ModbusTransportException {

        WriteCoilRequest request = new WriteCoilRequest(slaveId, start, values);
        WriteCoilsResponse response = (WriteCoilsResponse) master.send(request);
        if (response.isException())
            throw new ErrorResponseException(request, response);
        return true;
    }

    /**
     * 写 [15 WRITE  Coils Status]类型 模拟量数据
     *
     * @param slaveId slaveId
     * @return 返回结果
     * @throws ModbusTransportException 异常
     * @throws ErrorResponseException   异常
     */
    public static boolean writeCoils(ModbusMaster master, int slaveId, int start, boolean... values) throws ErrorResponseException, ModbusTransportException {

        WriteCoilsRequest request = new WriteCoilsRequest(slaveId, start, values);
        WriteCoilsResponse response = (WriteCoilsResponse) master.send(request);
        if (response.isException())
            throw new ErrorResponseException(request, response);
        return true;
    }

    /**
     * 写 [6 WRITE  Register]类型 模拟量数据
     * 写入整型数字
     *
     * @param slaveId slaveId
     * @return 返回结果
     * @throws ModbusTransportException 异常
     * @throws ErrorResponseException   异常
     */
    public static boolean writeRegister(ModbusMaster master, int slaveId, int offset, int value) throws ModbusTransportException, ErrorResponseException {

        WriteRegisterRequest request = new WriteRegisterRequest(slaveId, offset, value);
        WriteRegisterResponse response = (WriteRegisterResponse) master.send(request);

        if (response.isException())
            throw new ErrorResponseException(request, response);
        return true;
    }

    /**
     * 写 [16 WRITE  Registers]类型 模拟量数据
     * float类型 传输，符合IEEE 754 标准
     *
     * @param slaveId slaveId
     * @param values  （float类型）占用了两个寄存器，两个表示一个
     * @return 返回结果
     * @throws ModbusTransportException 异常
     * @throws ErrorResponseException   异常
     */
    public static boolean writeRegisters(ModbusMaster master, int slaveId, int start, short[] values) throws ModbusTransportException, ErrorResponseException {
        WriteRegistersRequest request = new WriteRegistersRequest(slaveId, start, values);
        WriteRegistersResponse response = (WriteRegistersResponse) master.send(request);
        if (response.isException())
            throw new ErrorResponseException(request, response);
        return true;

    }

    /**
     * 不知道如何使用
     *
     * @param master
     * @param slaveId
     * @param offset
     * @param and
     * @param or
     * @return
     * @throws ModbusTransportException
     * @throws ErrorResponseException
     */
    public static boolean writeMaskRegister(ModbusMaster master, int slaveId, int offset, int and, int or) throws ModbusTransportException, ErrorResponseException {

        WriteMaskRegisterRequest request = new WriteMaskRegisterRequest(slaveId, offset, and, or);
        WriteMaskRegisterResponse response = (WriteMaskRegisterResponse) master.send(request);
        if (response.isException())
            throw new ErrorResponseException(request, response);
        return true;
    }

    /**
     * 即将四个字节float的装成两个两字节的short
     *
     * @param value
     * @return
     */
    public static short[] valueToShorts(Number value) {
        int i = Float.floatToIntBits(value.floatValue());
        return new short[]{(short) (i >> 16), (short) i};
    }

    /**
     * 将short 装换成float
     *
     * @param shorts
     * @return
     */
    public static float[] shortsToFloat(short[] shorts) {
        float[] floats = new float[shorts.length / 2];
        for (int i = 0; i < shorts.length; i += 2) {
            floats[i / 2] = Float.intBitsToFloat((shorts[i] & 65535) << 16 | (shorts[i + 1] & 65535));
        }
        return floats;
    }

    /**
     * 将byte 装换成float
     *
     * @param data
     * @return
     */
    public static float[] bytesToFloat(byte[] data) {
        float[] floats = new float[data.length / 4];
        for (int i = 0, index = 0; i < data.length; i += 4, index++) {
            floats[index] = Float.intBitsToFloat((data[i] & 255) << 24 | (data[i + 1] & 255) << 16 | (data[i + 2] & 255) << 8 | data[i + 3] & 255);
        }
        return floats;
    }

    /**
     * 将byte 装换成 boolean
     *
     * @param data
     * @param len  需要的长度
     * @return
     */
    public static boolean[] bytesToBoolean(byte[] data, int len) {

        boolean[] result = new boolean[len];
        for (int i = 0; i < len; i++) {
            result[i] = (data[i / 8] >> (i % 8) & 1) == 1;
        }
        return result;
    }


}
