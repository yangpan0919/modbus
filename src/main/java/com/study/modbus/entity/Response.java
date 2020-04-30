package com.study.modbus.entity;

import com.study.modbus.constant.Constants;

/**
 * modBus 消息返回
 */
public class Response {
    /**
     * 状态码
     */
    private int status = Constants.SUCCESS_RESPONSE;
    /**
     * 返回数值
     */
    private Number result;
    /**
     * 错误信息描述
     */
    private String errorDesc;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Number getResult() {
        return result;
    }

    public void setResult(Number result) {
        this.result = result;
    }

    public String getErrorDesc() {
        return errorDesc;
    }

    public void setErrorDesc(String errorDesc) {
        this.errorDesc = errorDesc;
    }
}
