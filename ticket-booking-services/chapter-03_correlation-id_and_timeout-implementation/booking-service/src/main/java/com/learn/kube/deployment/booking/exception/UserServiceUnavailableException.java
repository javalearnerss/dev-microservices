package com.learn.kube.deployment.booking.exception;

public class UserServiceUnavailableException extends RuntimeException {
    public UserServiceUnavailableException(String message, Throwable cause) { super(message, cause); }
    public UserServiceUnavailableException(String message) { super(message); }
}