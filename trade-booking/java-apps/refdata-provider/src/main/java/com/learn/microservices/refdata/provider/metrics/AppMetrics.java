package com.learn.microservices.refdata.provider.metrics;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.LongAdder;

@Component
public class AppMetrics {

    private LongAdder received = new LongAdder();
    private LongAdder present = new LongAdder();
    private LongAdder missing = new LongAdder();
    private LongAdder processed = new LongAdder();

    public void incReceived() {
        received.increment();
    }

    public void incPresent() {
        present.increment();
    }


    public void incMissing() {
        missing.increment();
    }

    public void incProcessed() {
        processed.increment();
    }

    public Stats getStats() {
        return new Stats(received.sum(), present.sum(), missing.sum(), processed.sum(), received.sum() - processed.sum());
    }

    public record Stats(long received, long present, long missing, long processed, long pendingProcessing) {
    }
}
