package com.learn.microservices.trade.processor.controller;

import com.learn.microservices.trade.processor.metrics.TradePipelineMetrics;
import com.learn.microservices.trade.processor.model.NseTrade;
import com.learn.microservices.trade.processor.service.NseTradeStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("trade-processor")
public class TradeProcessorController {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(TradeProcessorController.class);

    @Autowired
    private NseTradeStoreService tradeStoreService;

    @Autowired
    private TradePipelineMetrics tradePipelineMetrics;


    @GetMapping("metrics")
    public ResponseEntity<TradePipelineMetrics.TradeEnricherStats> get() {

        LOGGER.info("Received request for trade processor metrics");

        TradePipelineMetrics.TradeEnricherStats stats = tradePipelineMetrics.snapshot();

        LOGGER.info("Trade processor metrics snapshot: {}", stats);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("get/trade/{tradeId}")
    public ResponseEntity<NseTrade> getTrade(@PathVariable("tradeId") String tradeId) {
        NseTrade trade = tradeStoreService.getTrade(tradeId);
        if (Objects.isNull(trade)) {
            return ResponseEntity.status(404).body(new NseTrade());
        }
        return ResponseEntity.ok(trade);
    }
}
