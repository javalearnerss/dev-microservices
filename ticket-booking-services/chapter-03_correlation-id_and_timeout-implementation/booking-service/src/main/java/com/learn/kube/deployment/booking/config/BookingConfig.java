package com.learn.kube.deployment.booking.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

@Configuration
public class BookingConfig {

    @Bean
    public feign.RequestInterceptor correlationIdInterceptor() {
        return template -> {
            String cid = org.slf4j.MDC.get("correlationId");
            if (cid != null && !cid.isBlank()) {
                template.header("X-Correlation-Id", cid);
            }
        };
    }

    @Bean
    public java.util.concurrent.Executor appExecutor(org.springframework.core.task.TaskDecorator taskDecorator) {
        var exec = new org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor();
        exec.setCorePoolSize(20);
        exec.setMaxPoolSize(50);
        exec.setQueueCapacity(1000);
        exec.setTaskDecorator(taskDecorator);
        exec.initialize();
        return exec;
    }

    @Bean
    public TaskDecorator taskDecorator() {
        return runnable -> {
            // Capture MDC from the calling thread (request thread)
            Map<String, String> contextMap = MDC.getCopyOfContextMap();

            return () -> {
                try {
                    if (contextMap != null) {
                        MDC.setContextMap(contextMap);
                    }
                    runnable.run();
                } finally {
                    MDC.clear();
                }
            };
        };
    }

}
