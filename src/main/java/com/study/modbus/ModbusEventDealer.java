package com.study.modbus;

import java.io.IOException;

public interface ModbusEventDealer {
    /**
     * 连接成功
     */
    void gotoComplete();

    /**
     * 断开连接
     */
    void doNetBroken();

    void getIoWrong(IOException var1, int var2);

    void t6Timeout(int var1);

    void sendError(Exception var1, int var2);

    void getSeparate(int var1);

    void T3tIMEOUT(int var1, long var2, String var4);

    void hasSend(int var1, long var2, String var4);

    void hasResponed(int var1, long var2, String var4);

    void gotoDivide(int var1);

    void sendFail(int var1, long var2, String var4);

    void responFail(int var1, long var2, String var4);

    void checkNet(int var1);
}
