package com.learn.microservices.trade.enricher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class TradeEnricherServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TradeEnricherServiceApplication.class, args);
	}

}
