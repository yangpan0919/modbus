package com.study.modbus.exception;

public class OfflineException extends Exception {
    public OfflineException() {
    }

    public OfflineException(String message) {
        super(message);
    }
}
