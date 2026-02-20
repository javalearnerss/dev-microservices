package com.learn.microservices.trade.enricher.exception;

public class DelayException extends  Exception{

    public DelayException(){

    }

    public DelayException(String msg){
        super(msg);
    }
}

