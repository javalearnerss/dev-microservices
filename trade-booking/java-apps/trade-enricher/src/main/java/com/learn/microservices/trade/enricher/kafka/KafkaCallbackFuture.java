package com.learn.microservices.trade.enricher.kafka;

import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class KafkaCallbackFuture<K, V> {

    private CompletableFuture<SendResult<K, V>> future;

    public KafkaCallbackFuture(CompletableFuture<SendResult<K, V>> future) {
        this.future = future;
    }

    public KafkaCallbackFuture<K, V> onSuccess(Consumer<SendResult<K, V>> consumer) {
        future.thenAccept(result -> consumer.accept(result));
        return this;
    }


    public KafkaCallbackFuture<K, V> onFailure(Consumer<Throwable> failure) {
        future.exceptionally(ex -> {
            failure.accept(ex);
            return null;
        });
        return this;
    }
}
