package com.learn.microservices.trade.enricher.controller;

import com.learn.microservices.trade.enricher.metrics.TradePipelineMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("trade-enricher")
public class TradeEnricherController {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(TradeEnricherController.class);

    private final TradePipelineMetrics tradePipelineMetrics;

    public TradeEnricherController(TradePipelineMetrics tradePipelineMetrics) {
        this.tradePipelineMetrics = tradePipelineMetrics;
    }

    @GetMapping("metrics")
    public ResponseEntity<TradePipelineMetrics.TradeEnricherStats> get() {

        LOGGER.info("Received request for trade enricher metrics");

        TradePipelineMetrics.TradeEnricherStats stats = tradePipelineMetrics.snapshot();

        LOGGER.info("Trade enricher metrics snapshot: {}", stats);

        return ResponseEntity.ok(stats);
    }
}
