package com.study.modbus.entity;

public class WriteBean {

    /**
     * 消息类型
     */
    private int dataType;
    /**
     * 需要修改的值
     */
    private Object data;
    /**
     * 点位地址
     */
    private String addr;

    public int getDataType() {
        return dataType;
    }

    public void setDataType(int dataType) {
        this.dataType = dataType;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    @Override
    public String toString() {
        return "WriteBean{" +
                "dataType=" + dataType +
                ", data=" + data +
                ", addr='" + addr + '\'' +
                '}';
    }
}
