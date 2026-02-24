package com.learn.microservices.exchange.utils;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class KafkaCallbackFuture<T> {
    private CompletableFuture<T> completableFuture;

    public KafkaCallbackFuture(CompletableFuture<T> completableFuture) {
        this.completableFuture = completableFuture;
    }

    public KafkaCallbackFuture<T> onSuccess(Consumer<T> success) {
        completableFuture.thenAccept(success);
        return this;
    }

    public KafkaCallbackFuture<T> onFailure(Consumer<Throwable> failure) {
        completableFuture.exceptionally(ex -> {
            failure.accept(ex);
            return null;
        });
        return this;
    }
}
