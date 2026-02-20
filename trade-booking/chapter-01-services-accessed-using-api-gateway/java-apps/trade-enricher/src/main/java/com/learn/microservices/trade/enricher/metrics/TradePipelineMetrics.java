package com.learn.microservices.trade.enricher.metrics;


import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.LongAdder;

@Component
public class TradePipelineMetrics {

    private final LongAdder received = new LongAdder();
    private final LongAdder processed = new LongAdder();
    private final LongAdder sent = new LongAdder();
    private final LongAdder failed = new LongAdder();

    // Optional: track last timestamps (volatile is enough)
    private volatile Instant lastReceivedAt;
    private volatile Instant lastProcessedAt;
    private volatile Instant lastSentAt;
    private volatile Instant lastFailedAt;

    public void incReceived() {
        received.increment();
        lastReceivedAt = Instant.now();
    }

    public void incProcessed() {
        processed.increment();
        lastProcessedAt = Instant.now();
    }

    public void incSent() {
        sent.increment();
        lastSentAt = Instant.now();
    }

    public void incFailed() {
        failed.increment();
        lastFailedAt = Instant.now();
    }

    public TradeEnricherStats snapshot() {
        long received = this.received.sum();
        long processed = this.processed.sum();
        long sent = this.sent.sum();
        long failed = this.failed.sum();
        return new TradeEnricherStats(
                received, processed, sent, failed,
                Math.max(0, received - processed),   // received but not processed
                Math.max(0, processed - sent),   // processed but not sent
                lastReceivedAt, lastProcessedAt, lastSentAt, lastFailedAt
        );
    }

    public record TradeEnricherStats(
            long received,
            long processed,
            long sent,
            long failed,
            long pendingProcessing,
            long pendingSend,
            Instant lastReceivedAt,
            Instant lastProcessedAt,
            Instant lastSentAt,
            Instant lastFailedAt
    ) {}
}

