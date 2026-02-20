package com.learn.microservices.trade.enricher.utils;

public class TimeUtil {

    public static void sleep(long duration){
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
