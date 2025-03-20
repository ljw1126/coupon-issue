package com.example.couponcore.exception;

public class LockAcquisitionException extends RuntimeException {
    public LockAcquisitionException(String lockName, Throwable cause) {
        super("Failed to acquire lock: " + lockName, cause);
    }
}
