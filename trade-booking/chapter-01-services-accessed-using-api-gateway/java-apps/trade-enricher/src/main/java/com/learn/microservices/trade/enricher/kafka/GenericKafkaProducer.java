package com.learn.microservices.trade.enricher.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class GenericKafkaProducer<K, V> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericKafkaProducer.class);

    private KafkaTemplate<K, V> kafkaTemplate;

    public GenericKafkaProducer(KafkaTemplate<K, V> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(String topic, V data) {
        CompletableFuture<SendResult<K, V>> future = kafkaTemplate.send(topic, data);
        new KafkaCallbackFuture<>(future)
                .onSuccess(result ->
                        LOGGER.info("Message sent topic={} partition={} offset={} payload={}",
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset(),
                                data))
                .onFailure(ex ->
                        LOGGER.error("Unable to send message topic={} payload={} due to {}",
                                topic, data, ex.toString(), ex));
    }

    public void send(String topic, K key, V data) {
        CompletableFuture<SendResult<K, V>> future = kafkaTemplate.send(topic, key, data);
        new KafkaCallbackFuture<>(future)
                .onSuccess(result ->
                        LOGGER.info("Message sent topic={} partition={} offset={} key={} payload={}",
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset(),
                                key,
                                data))
                .onFailure(ex ->
                        LOGGER.error("Unable to send message topic={} key={} payload={} due to {}",
                                topic, key, data, ex.toString(), ex));
    }
}
