package com.learn.microservices.trade.enricher.config.kafka;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Autowired
    private KafkaProducerProperties producerProps;

    @Bean
    public DefaultKafkaProducerFactory<String, String> kafkaProducerFactory() {

        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, producerProps.getBootstrapServers());
        properties.put(ProducerConfig.BATCH_SIZE_CONFIG, producerProps.getBatchSize());
        properties.put(ProducerConfig.LINGER_MS_CONFIG, producerProps.getLingerMs());
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, producerProps.getKeySerializer());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, producerProps.getValueSerializer());
        properties.put(ProducerConfig.ACKS_CONFIG, producerProps.getAckStrategy());
        properties.put(ProducerConfig.RETRIES_CONFIG, producerProps.getRetries());
        properties.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, producerProps.getRetryBackoffMs());
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        DefaultKafkaProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(properties);
      //  producerFactory.setTransactionIdPrefix(producerProps.getTransIdPrefix());
        return producerFactory;
    }

    @Bean
    public KafkaTemplate<String, String> kafkaProducer() {
        return new KafkaTemplate<>(kafkaProducerFactory());
    }
}
