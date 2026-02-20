package com.learn.microservices.trade.processor.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.microservices.trade.processor.exception.InvalidTradeException;
import com.learn.microservices.trade.processor.metrics.TradePipelineMetrics;
import com.learn.microservices.trade.processor.model.NseTrade;
import com.learn.microservices.trade.processor.service.TradeProcessorService;
import com.learn.microservices.trade.processor.utils.JsonUtil;
import com.learn.microservices.trade.processor.utils.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
public class TradeProcessorConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TradeProcessorConsumer.class);

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private TradeProcessorService tradeProcessorService;

    @Autowired
    private TradePipelineMetrics metrics;

    @RetryableTopic(
            include = InvalidTradeException.class,
            backoff = @Backoff(value = 3000L, multiplierExpression = "2"),
            attempts = "4", // N-1 Retry attempts
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            autoCreateTopics = "false",
            dltTopicSuffix = "-dlt",
            dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(
            topics = "${consumer.trade.processor.topic}",
            groupId = "trade-enricher"
    )
    public void consume(@Payload String trade,
                        @Header(KafkaHeaders.OFFSET) Long offset,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
                        @Header(KafkaHeaders.RECEIVED_TIMESTAMP) Long ts,
                        Acknowledgment ack) throws Exception {

        LOGGER.info("TRADE RECEIVED | topic={} partition={} offset={} ts={} payload={}", topic, partition, offset, ts, trade);
        NseTrade tradeObject = null;
        try {
            // ================= DESERIALIZATION =================
            LOGGER.debug("Deserializing trade message...");
            tradeObject = JsonUtil.MAPPER.readValue(trade, NseTrade.class);

            LOGGER.info("Trade Parsed | tradeId={} symbol={} qty={}", tradeObject.getTradeId(), tradeObject.getSymbol(), tradeObject.getQuantity());

            // ================= METRICS =================
            if (!metrics.isTradePresent(tradeObject.getTradeId())) {
                metrics.incReceived();
                LOGGER.debug("Metrics Updated | received incremented");
            }

            // ================= SIMULATE PROCESS DELAY =================
            TimeUtil.sleep(100);

            // ================= BUSINESS PROCESS =================
            LOGGER.info("Processing Trade | tradeId={}", tradeObject.getTradeId());
            tradeProcessorService.process(tradeObject);
            metrics.incProcessed();
            LOGGER.info("Trade Processed Successfully | tradeId={}", tradeObject.getTradeId());
        }
        catch (InvalidTradeException invalidTradeException) {
            // ================= RETRYABLE ERROR =================
            if (tradeObject != null && !metrics.isTradePresent(tradeObject.getTradeId())) {
                metrics.getFailedTrades().put(tradeObject.getTradeId(), 1);
                metrics.incFailed();
            }
            LOGGER.error("RETRY TRIGGERED | tradeId={} error={}", tradeObject != null ? tradeObject.getTradeId() : "UNKNOWN", invalidTradeException.getMessage(), invalidTradeException);
            throw invalidTradeException;
        }
        catch (Exception e) {
            // ================= DLT ERROR =================
            if (tradeObject != null && !metrics.isTradePresent(tradeObject.getTradeId())) {
                metrics.getFailedTrades().put(tradeObject.getTradeId(), 1);
                metrics.incFailed();
            }
            LOGGER.error("DLT MOVE | tradeId={} error={}", tradeObject != null ? tradeObject.getTradeId() : "UNKNOWN", e.getMessage());
            throw new Exception(e);
        }
        // ================= ACK =================
        ack.acknowledge();
    }
}
