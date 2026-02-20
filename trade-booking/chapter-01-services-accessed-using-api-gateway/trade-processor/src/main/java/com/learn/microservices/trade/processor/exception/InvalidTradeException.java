package com.learn.microservices.trade.processor.exception;

public class InvalidTradeException extends  Exception{

    public InvalidTradeException() {
    }


    public InvalidTradeException(String message) {
        super(message);
    }
}
