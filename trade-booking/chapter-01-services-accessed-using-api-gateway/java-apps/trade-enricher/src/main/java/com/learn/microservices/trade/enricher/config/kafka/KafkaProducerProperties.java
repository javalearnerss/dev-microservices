package com.learn.microservices.trade.enricher.config.kafka;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("kafka.producer")
public class KafkaProducerProperties {

    private String bootstrapServers;
    private Integer batchSize;
    private Long lingerMs;
    private String compressionType;
    private String ackStrategy;
    private Integer retries;
    private Long requestTimeout;
    private Long deliveryTimeout;
    private Boolean idempotence;
    private String keySerializer;
    private String valueSerializer;
    private Integer retryBackoffMs;
    private String transIdPrefix;

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public Long getLingerMs() {
        return lingerMs;
    }

    public void setLingerMs(Long lingerMs) {
        this.lingerMs = lingerMs;
    }

    public String getCompressionType() {
        return compressionType;
    }

    public void setCompressionType(String compressionType) {
        this.compressionType = compressionType;
    }

    public String getAckStrategy() {
        return ackStrategy;
    }

    public void setAckStrategy(String ackStrategy) {
        this.ackStrategy = ackStrategy;
    }

    public Integer getRetries() {
        return retries;
    }

    public void setRetries(Integer retries) {
        this.retries = retries;
    }

    public Long getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Long requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public Long getDeliveryTimeout() {
        return deliveryTimeout;
    }

    public void setDeliveryTimeout(Long deliveryTimeout) {
        this.deliveryTimeout = deliveryTimeout;
    }

    public Boolean getIdempotence() {
        return idempotence;
    }

    public void setIdempotence(Boolean idempotence) {
        this.idempotence = idempotence;
    }

    public String getKeySerializer() {
        return keySerializer;
    }

    public void setKeySerializer(String keySerializer) {
        this.keySerializer = keySerializer;
    }

    public String getValueSerializer() {
        return valueSerializer;
    }

    public void setValueSerializer(String valueSerializer) {
        this.valueSerializer = valueSerializer;
    }

    public Integer getRetryBackoffMs() {
        return retryBackoffMs;
    }

    public void setRetryBackoffMs(Integer retryBackoffMs) {
        this.retryBackoffMs = retryBackoffMs;
    }

    public String getTransIdPrefix() {
        return transIdPrefix;
    }

    public void setTransIdPrefix(String transIdPrefix) {
        this.transIdPrefix = transIdPrefix;
    }
}
