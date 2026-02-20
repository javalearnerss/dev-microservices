package com.learn.microservices.trade.enricher.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.microservices.trade.enricher.exception.DelayException;
import com.learn.microservices.trade.enricher.metrics.TradePipelineMetrics;
import com.learn.microservices.trade.enricher.model.NseTrade;
import com.learn.microservices.trade.enricher.service.TradeEnricherProcessor;
import com.learn.microservices.trade.enricher.utils.JsonUtil;
import com.learn.microservices.trade.enricher.utils.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class TradeEnricherConsumer {

    private static Logger LOGGER = LoggerFactory.getLogger(TradeEnricherConsumer.class);

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private TradeEnricherProcessor processor;

    @Autowired
    private GenericKafkaProducer<String, String> producer;

    @Value("${producer.trade.enricher.topic}")
    private String enrichedTradesTopic;

    @Autowired
    private TradePipelineMetrics metrics;

    @RetryableTopic(include = DelayException.class,
            attempts = "4",
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            autoCreateTopics = "false",
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            dltTopicSuffix = "-dlt"
    )
    @KafkaListener(topics = "${consumer.trade.enricher.topic}", groupId = "trade-enricher")
    public void consume(@Payload String trade,
                        @Header(value = KafkaHeaders.OFFSET) Long offset,
                        @Header(value = KafkaHeaders.RECEIVED_TOPIC) String topic,
                        Acknowledgment ack) throws Exception {

        LOGGER.info("Trade is received, topic={}, offset={}, trade={}", topic, offset, trade);

        try {
            metrics.incReceived();
            NseTrade tradeObject = JsonUtil.MAPPER.readValue(trade, NseTrade.class);
            NseTrade enrichedTrade = processor.enrich(tradeObject);
            metrics.incProcessed();
            TimeUtil.sleep(100);
            producer.send(enrichedTradesTopic, JsonUtil.MAPPER.writeValueAsString(enrichedTrade));
            metrics.incSent();
        } catch (DelayException delayExp) {
            LOGGER.error("Retrying message due to DelayException: {}", delayExp.getMessage());
            throw delayExp;
        } catch (Exception e) {
            metrics.incFailed();
            LOGGER.error("Moving message to DLT due to exception: {}", e.getMessage());
            throw new Exception(e);
        }
        ack.acknowledge();
    }
}
