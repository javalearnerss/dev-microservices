package com.learn.microservices.trade.processor.utils;

public class TimeUtil {

    public static void sleep(long duration){
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
