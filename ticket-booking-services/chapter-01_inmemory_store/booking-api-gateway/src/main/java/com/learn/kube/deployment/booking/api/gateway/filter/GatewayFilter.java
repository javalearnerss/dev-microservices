package com.learn.kube.deployment.booking.api.gateway.filter;


import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayFilter {

    @Bean
    GlobalFilter simpleLogFilter() {
        return (exchange, chain) -> {
            var req = exchange.getRequest();
            System.out.println("[GATEWAY] " + req.getMethod() + " " + req.getURI());
            return chain.filter(exchange).then(Mono.fromRunnable(() ->
                    System.out.println("[GATEWAY] status=" + exchange.getResponse().getStatusCode())
            ));
        };
    }
}
