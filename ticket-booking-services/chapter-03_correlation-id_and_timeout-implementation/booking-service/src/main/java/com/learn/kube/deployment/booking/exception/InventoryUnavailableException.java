package com.learn.kube.deployment.booking.exception;

public class InventoryUnavailableException extends RuntimeException {
    public InventoryUnavailableException(String message, Throwable cause) { super(message, cause); }
    public InventoryUnavailableException(String message) { super(message); }
}

