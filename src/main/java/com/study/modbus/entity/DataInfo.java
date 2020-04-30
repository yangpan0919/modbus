package com.study.modbus.entity;

/**
 * 用户存储所有数据
 */
public class DataInfo {

    public DataInfo(String ip, int port, long sleepTime, int linkNum, String deviceCode) {
        this.ip = ip;
        this.port = port;
        this.sleepTime = sleepTime;
        this.linkNum = linkNum;
        this.deviceCode = deviceCode;
    }

    /**
     * 所要连接的IP地址
     */
    private String ip;
    /**
     * 所要连接的端口
     */
    private int port;

    /**
     * 重连间隔时间
     */
    private long sleepTime;

    /**
     * 重连允许次数多少次
     */
    private int linkNum;
    /**
     * 设备编号，日志标志位
     */
    private String deviceCode;


    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public long getSleepTime() {
        return sleepTime;
    }

    public void setSleepTime(long sleepTime) {
        this.sleepTime = sleepTime;
    }

    public int getLinkNum() {
        return linkNum;
    }

    public void setLinkNum(int linkNum) {
        this.linkNum = linkNum;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }
}
