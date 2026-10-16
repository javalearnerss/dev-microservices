package in.javalearners.pulsefeed.feed.config;

import in.javalearners.pulsefeed.feed.client.PostServiceClient;
import in.javalearners.pulsefeed.feed.model.Post;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.function.Supplier;

/**
 * Applies resilience4j patterns (Retry, Circuit Breaker, Bulkhead)
 * to the Feign clients to protect the Feed Service from failures in dependent services.
 * 
 * All resilience policies are configured in application.yml and automatically
 * loaded by Spring Boot's Resilience4j starter. This class only handles decorator composition.
 * 
 * Note: TimeLimiter is configured but not applied to synchronous Feign calls.
 * Use the HTTP client timeout settings in application.yml for read/connection timeouts.
 */
@Configuration
public class ResilienceDecoratorConfig {

    private static final Logger logger = LoggerFactory.getLogger(ResilienceDecoratorConfig.class);

    @Autowired(required = false)
    private PostServiceClient postServiceClientImpl;

    /**
     * Wraps PostServiceClient with resilience4j patterns.
     * Policies are defined in application.yml under resilience4j.*.instances.post-service.
     */
    @Bean("resilientPostServiceClient")
    @Primary
    public PostServiceClient resilientPostServiceClient(
            Retry postServiceRetry,
            CircuitBreaker postServiceCircuitBreaker,
            Bulkhead postServiceBulkhead) {

        logger.info("Creating resilient PostServiceClient decorator with retry, circuit breaker, and bulkhead");
        return new PostServiceClientDecorator(
                postServiceClientImpl,
                postServiceRetry,
                postServiceCircuitBreaker,
                postServiceBulkhead);
    }

    /**
     * Decorator for PostServiceClient with resilience patterns applied.
     * Decoration order: Bulkhead (resource limit) → CircuitBreaker (fail fast) → Retry (recover transient failures)
     * 
     * This ordering ensures:
     * 1. Bulkhead immediately rejects if call limit is exceeded (fail fast)
     * 2. CircuitBreaker breaks fast when failure threshold is exceeded
     * 3. Retry handles transient failures with exponential backoff
     */
    private static class PostServiceClientDecorator implements PostServiceClient {
        private final PostServiceClient delegate;
        private final Retry retry;
        private final CircuitBreaker circuitBreaker;
        private final Bulkhead bulkhead;

        PostServiceClientDecorator(PostServiceClient delegate,
                                   Retry retry, CircuitBreaker circuitBreaker, Bulkhead bulkhead) {
            this.delegate = delegate;
            this.retry = retry;
            this.circuitBreaker = circuitBreaker;
            this.bulkhead = bulkhead;
        }

        @Override
        public List<Post> findByAuthorIds(List<String> authorIds) {
            logger.debug("Calling Post Service findByAuthorIds with resilience patterns for {} authors", authorIds.size());
            return decorateCall(() -> delegate.findByAuthorIds(authorIds));
        }

        @Override
        public List<Post> getPostsByIds(String ids) {
            logger.debug("Calling Post Service getPostsByIds with resilience patterns");
            return decorateCall(() -> delegate.getPostsByIds(ids));
        }

        @Override
        public Long getPostCountByAuthor(String authorId) {
            logger.debug("Calling Post Service getPostCountByAuthor with resilience patterns for author: {}", authorId);
            return decorateCall(() -> delegate.getPostCountByAuthor(authorId));
        }

        @Override
        public List<Post> getPostsByAuthor(String authorId) {
            logger.debug("Calling Post Service getPostsByAuthor with resilience patterns for author: {}", authorId);
            return decorateCall(() -> delegate.getPostsByAuthor(authorId));
        }

        // ==================== Failure Simulator Control Methods ====================

        @Override
        public org.springframework.http.ResponseEntity<String> startPostSlowness(long delayMs) {
            logger.info("Starting Post Service slowness simulation with delay: {} ms", delayMs);
            return delegate.startPostSlowness(delayMs);
        }

        @Override
        public org.springframework.http.ResponseEntity<String> stopPostSlowness() {
            logger.info("Stopping Post Service slowness simulation");
            return delegate.stopPostSlowness();
        }

        @Override
        public org.springframework.http.ResponseEntity<String> startPostFailure(int failureCount) {
            logger.info("Starting Post Service failure simulation for {} calls", failureCount);
            return delegate.startPostFailure(failureCount);
        }

        @Override
        public org.springframework.http.ResponseEntity<String> stopPostFailure() {
            logger.info("Stopping Post Service failure simulation");
            return delegate.stopPostFailure();
        }

        @Override
        public org.springframework.http.ResponseEntity<String> getPostSimulatorStatus() {
            logger.debug("Getting Post Service failure simulator status");
            return delegate.getPostSimulatorStatus();
        }

        @Override
        public org.springframework.http.ResponseEntity<String> resetPostSimulator() {
            logger.info("Resetting Post Service failure simulator");
            return delegate.resetPostSimulator();
        }

        /**
         * Decorates a supplier with resilience patterns.
         * Catches known failure patterns and logs them appropriately.
         */
        private <T> T decorateCall(Supplier<T> supplier) {
            try {
                Supplier<T> decorated = Bulkhead.decorateSupplier(bulkhead, supplier);
                decorated = CircuitBreaker.decorateSupplier(circuitBreaker, decorated);
                decorated = Retry.decorateSupplier(retry, decorated);

                return decorated.get();
            } catch (io.github.resilience4j.bulkhead.BulkheadFullException e) {
                logger.warn("Post Service bulkhead exhausted: max concurrent calls reached");
                throw new RuntimeException("Post Service bulkhead exhausted; try again later", e);
            } catch (io.github.resilience4j.circuitbreaker.CallNotPermittedException e) {
                logger.warn("Post Service circuit breaker is open; dependency may be unhealthy");
                throw new RuntimeException("Post Service circuit breaker is open", e);
            } catch (Exception e) {
                logger.error("Post Service call failed after all retries and fallbacks", e);
                throw new RuntimeException("Failed to call Post Service: " + e.getMessage(), e);
            }
        }
    }
}
