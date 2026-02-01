package com.learn.kube.deployment.booking.api.gateway.filter;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.rmi.server.UID;
import java.util.UUID;

@Configuration
public class GatewayFilter {

    Logger logger = LoggerFactory.getLogger(GatewayFilter.class);

    private final static String CORRELATION_TOKEN_NAME = "X-Correlation-Id";

    @Bean
    public GlobalFilter internalCorrelationFilter(){
        return (exchange, chain)-> {
            String correlationId = UUID.randomUUID().toString();
            logger.info("{} = {}, Request URI = {}", CORRELATION_TOKEN_NAME, correlationId, exchange.getRequest().getURI());
            ServerHttpRequest req = exchange.getRequest().mutate().headers(h -> {
                h.remove(CORRELATION_TOKEN_NAME);
                h.add(CORRELATION_TOKEN_NAME, correlationId);
            }).build();

            ServerWebExchange mutatedExchange = exchange.mutate().request(req).build();
            return chain.filter(mutatedExchange);
        };
    }

}
