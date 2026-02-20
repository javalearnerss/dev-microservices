package com.learn.microservices.trade.processor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
@EnableFeignClients
@EnableConfigurationProperties
public class TradeProcessorApplication {


    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(TradeProcessorApplication.class, args);
    }

}
