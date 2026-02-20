package com.learn.microservices.exchange.utils;

public class TimeUtil {

    public static void sleep(long duration){
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
