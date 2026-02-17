package com.learn.microservices.trade.enricher.config.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "consumer.props")
public class ConsumerProperties {

    private String consumerGroup;
    private boolean autoCommit;
    private String keyDeserializer;
    private String valueDeserializer;
    private String ackMode;

    public ConsumerProperties() {
    }

    public ConsumerProperties(String consumerGroup, boolean autoCommit, String keyDeserializer, String valueDeserializer, String ackMode) {
        this.consumerGroup = consumerGroup;
        this.autoCommit = autoCommit;
        this.keyDeserializer = keyDeserializer;
        this.valueDeserializer = valueDeserializer;
        this.ackMode = ackMode;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public boolean isAutoCommit() {
        return autoCommit;
    }

    public void setAutoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
    }

    public String getKeyDeserializer() {
        return keyDeserializer;
    }

    public void setKeyDeserializer(String keyDeserializer) {
        this.keyDeserializer = keyDeserializer;
    }


    public String getValueDeserializer() {
        return valueDeserializer;
    }

    public void setValueDeserializer(String valueDeserializer) {
        this.valueDeserializer = valueDeserializer;
    }


    public String getAckMode() {
        return ackMode;
    }

    public void setAckMode(String ackMode) {
        this.ackMode = ackMode;
    }
}
