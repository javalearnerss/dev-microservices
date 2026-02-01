package com.learn.kube.deployment.booking.exception;

public class PaymentUnavailableException extends RuntimeException {
    public PaymentUnavailableException(String message, Throwable cause) { super(message, cause); }
    public PaymentUnavailableException(String message) { super(message); }
}
