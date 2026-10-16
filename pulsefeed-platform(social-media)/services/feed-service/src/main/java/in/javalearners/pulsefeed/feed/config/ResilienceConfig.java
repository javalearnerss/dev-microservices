package in.javalearners.pulsefeed.feed.config;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

/**
 * Configuration for resilience4j metrics and observability.
 * 
 * Resilience policies (Retry, CircuitBreaker, Bulkhead, TimeLimiter) are
 * defined in application.yml and auto-configured by Spring Boot.
 * This class adds event listeners for operational monitoring.
 */
@Configuration
public class ResilienceConfig {

    private static final Logger logger = LoggerFactory.getLogger(ResilienceConfig.class);

    @Bean
    public CircuitBreakerRegistry.EventPublisher circuitBreakerEventPublisher(CircuitBreakerRegistry registry) {
        return registry.getEventPublisher()
                .onEntryAdded(this::logCircuitBreakerAdded)
                .onEntryRemoved(this::logCircuitBreakerRemoved);
    }

    @Bean
    public RetryRegistry.EventPublisher retryEventPublisher(RetryRegistry registry) {
        return registry.getEventPublisher()
                .onEntryAdded(event -> logger.info("Retry policy configured: {}", event.getAddedEntry().getName()))
                .onEntryRemoved(event -> logger.info("Retry policy removed: {}", event.getRemovedEntry().getName()));
    }

    @Bean
    public BulkheadRegistry.EventPublisher bulkheadEventPublisher(BulkheadRegistry registry) {
        return registry.getEventPublisher()
                .onEntryAdded(event -> logger.info("Bulkhead policy configured: {}", event.getAddedEntry().getName()))
                .onEntryRemoved(event -> logger.info("Bulkhead policy removed: {}", event.getRemovedEntry().getName()));
    }

    @Bean
    public TimeLimiterRegistry.EventPublisher timeLimiterEventPublisher(TimeLimiterRegistry registry) {
        return registry.getEventPublisher()
                .onEntryAdded(event -> logger.info("TimeLimiter policy configured: {}", event.getAddedEntry().getName()))
                .onEntryRemoved(event -> logger.info("TimeLimiter policy removed: {}", event.getRemovedEntry().getName()));
    }

    private void logCircuitBreakerAdded(EntryAddedEvent<CircuitBreaker> event) {
        logger.info("Circuit breaker policy configured: {}", event.getAddedEntry().getName());
    }

    private void logCircuitBreakerRemoved(EntryRemovedEvent<CircuitBreaker> event) {
        logger.info("Circuit breaker policy removed: {}", event.getRemovedEntry().getName());
    }
}
