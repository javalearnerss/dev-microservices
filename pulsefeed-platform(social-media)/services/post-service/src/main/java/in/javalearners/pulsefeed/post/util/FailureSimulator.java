package in.javalearners.pulsefeed.post.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Utility service for simulating failures and delays in the Post Service.
 * Used for testing resilience patterns (timeout, retry, circuit breaker, bulkhead).
 */
@Component
public class FailureSimulator {

    private static final Logger logger = LoggerFactory.getLogger(FailureSimulator.class);

    private volatile boolean simulateSlowness = false;
    private volatile boolean simulateFailure = false;
    private volatile long delayMillis = 500;
    private volatile int failureCount = 0;
    private volatile int totalRequestCount = 0;

    /**
     * Simulates slowness by adding a delay.
     */
    public void simulateCall() throws InterruptedException {
        totalRequestCount++;
        
        if (simulateSlowness) {
            logger.warn("Simulating slowness: delaying {} ms (request #{})", delayMillis, totalRequestCount);
            Thread.sleep(delayMillis);
        }
        
        if (simulateFailure && failureCount > 0) {
            failureCount--;
            logger.warn("Simulating failure (remaining failures: {})", failureCount);
            throw new RuntimeException("Simulated service failure");
        }
    }

    public void setSimulateSlowness(boolean simulate) {
        this.simulateSlowness = simulate;
        logger.info("Slowness simulation set to: {}", simulate);
    }

    public void setSimulateFailure(boolean simulate, int failureCount) {
        this.simulateFailure = simulate;
        this.failureCount = failureCount;
        logger.info("Failure simulation set to: {}, failure count: {}", simulate, failureCount);
    }

    public void setDelayMillis(long delayMillis) {
        this.delayMillis = delayMillis;
        logger.info("Delay set to: {} ms", delayMillis);
    }

    public boolean isSimulatingSlowness() {
        return simulateSlowness;
    }

    public boolean isSimulatingFailure() {
        return simulateFailure;
    }

    public long getDelayMillis() {
        return delayMillis;
    }

    public int getTotalRequestCount() {
        return totalRequestCount;
    }

    public void reset() {
        simulateSlowness = false;
        simulateFailure = false;
        delayMillis = 500;
        failureCount = 0;
        totalRequestCount = 0;
        logger.info("Failure simulator reset");
    }
}
