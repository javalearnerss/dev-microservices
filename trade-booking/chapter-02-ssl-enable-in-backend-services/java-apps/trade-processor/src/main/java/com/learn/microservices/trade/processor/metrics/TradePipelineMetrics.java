package com.learn.microservices.trade.processor.metrics;


import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

@Component
public class TradePipelineMetrics {

    private final LongAdder received = new LongAdder();
    private final LongAdder processed = new LongAdder();
    private final LongAdder failed = new LongAdder();

    // Optional: track last timestamps (volatile is enough)
    private volatile Instant lastReceivedAt;
    private volatile Instant lastProcessedAt;
    private volatile Instant lastFailedAt;


    private Map<String, Integer> failedTrades = new ConcurrentHashMap<>();


    public boolean isTradePresent(String tradeId) {
        return failedTrades.get(tradeId) != null;
    }

    public Map<String, Integer> getFailedTrades() {
        return failedTrades;
    }


    public void incReceived() {
        received.increment();
        lastReceivedAt = Instant.now();
    }

    public void incProcessed() {
        processed.increment();
        lastProcessedAt = Instant.now();
    }

    public void incFailed() {
        failed.increment();
        lastFailedAt = Instant.now();
    }

    public TradeEnricherStats snapshot() {
        long received = this.received.sum();
        long processed = this.processed.sum();
        long failed = this.failed.sum();
        return new TradeEnricherStats(
                received, processed, failed,
                Math.max(0, received - processed),   // received but not processed
                lastReceivedAt, lastProcessedAt, lastFailedAt
        );
    }

    public record TradeEnricherStats(
            long received,
            long processed,
            long failed,
            long pendingProcessing,
            Instant lastReceivedAt,
            Instant lastProcessedAt,
            Instant lastFailedAt
    ) {}
}

