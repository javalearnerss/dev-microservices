package com.learn.kube.deployment.booking.service;

import com.learn.kube.deployment.booking.exception.InventoryUnavailableException;
import com.learn.kube.deployment.booking.exception.PaymentUnavailableException;
import com.learn.kube.deployment.booking.exception.UserServiceUnavailableException;
import com.learn.kube.deployment.booking.external.service.feign.InventoryClient;
import com.learn.kube.deployment.booking.external.service.feign.PaymentClient;
import com.learn.kube.deployment.booking.external.service.feign.UserClient;
import com.learn.kube.deployment.booking.model.HoldResponse;
import com.learn.kube.deployment.booking.model.PaymentResponse;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class TimeoutClients {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeoutClients.class);

    @Autowired
    private InventoryClient inventoryClient;
    @Autowired
    private UserClient userClient;
    @Autowired
    private PaymentClient paymentClient;

    @Autowired private java.util.concurrent.Executor appExecutor;


    @TimeLimiter(name = "paymentTL", fallbackMethod = "paymentRefundFallback")
    public CompletableFuture<Map<String, Object>> refundAsync(String bookingId) {
        return CompletableFuture.supplyAsync(() -> paymentClient.refund(bookingId), appExecutor);
    }



    @TimeLimiter(name = "userTL", fallbackMethod = "userFallback")
    public CompletableFuture<Map<String, Object>> getUserAsync(String userId) {
        return CompletableFuture.supplyAsync(() -> userClient.getUser(userId), appExecutor);
    }

    @TimeLimiter(name = "paymentTL", fallbackMethod = "paymentPayFallback")
    public CompletableFuture<PaymentResponse> paymentAsync(String bookingId, long amount, boolean failPayment) {
        return CompletableFuture.supplyAsync(() -> paymentClient.pay(bookingId, amount, failPayment), appExecutor);
    }

    @TimeLimiter(name = "inventoryTL", fallbackMethod = "inventoryCancelFallback")
    public CompletableFuture<Map<String, Object>> cancelAsync(String holdId) {
        return CompletableFuture.supplyAsync(() -> inventoryClient.cancel(holdId), appExecutor);
    }

    @TimeLimiter(name = "inventoryTL", fallbackMethod = "inventoryHoldFallback")
    public CompletableFuture<HoldResponse> holdAsync(String showId, int qty) {
        return CompletableFuture.supplyAsync(() -> inventoryClient.hold(showId, qty), appExecutor);
    }

    @TimeLimiter(name = "inventoryTL", fallbackMethod = "inventoryConfirmFallback")
    public CompletableFuture<Map<String, Object>> confirmAsync(String holdId) {
        return CompletableFuture.supplyAsync(() -> inventoryClient.confirm(holdId), appExecutor);
    }


    // -------------------- TIME LIMITER FALLBACKS --------------------

    // USER
    public CompletableFuture<Map<String, Object>> userFallback(String userId, Throwable ex) {
        LOGGER.error("TL_FALLBACK userTL triggered userId={} cause={}", userId, rootMsg(ex), ex);
        // Option 1: fail fast (recommended for user validation)
        return CompletableFuture.failedFuture(new UserServiceUnavailableException("User service timed out/unavailable", ex));

        // Option 2 (not recommended for user validation): return an empty/default map
        // return CompletableFuture.completedFuture(Map.of("status", "UNKNOWN_USER"));
    }

    // INVENTORY HOLD
    public CompletableFuture<HoldResponse> inventoryHoldFallback(String showId, int qty, Throwable ex) {
        LOGGER.error("TL_FALLBACK inventoryTL(hold) triggered showId={} qty={} cause={}",
                showId, qty, rootMsg(ex), ex);

        // Better: fail fast so your chain returns inventory failure response via @RestControllerAdvice
        return CompletableFuture.failedFuture(new InventoryUnavailableException("Inventory hold timed out/unavailable", ex));

        // Alternative: return a "null holdId" and let your existing null-check fail
        // return CompletableFuture.completedFuture(new HoldResponse(null));
    }

    // INVENTORY CONFIRM
    public CompletableFuture<Map<String, Object>> inventoryConfirmFallback(String holdId, Throwable ex) {
        LOGGER.error("TL_FALLBACK inventoryTL(confirm) triggered holdId={} cause={}", holdId, rootMsg(ex), ex);

        // confirm timeout should be treated as failure -> compensation path
        return CompletableFuture.failedFuture(new InventoryUnavailableException("Inventory confirm timed out/unavailable", ex));
    }

    // INVENTORY CANCEL (COMPENSATION)
    public CompletableFuture<Map<String, Object>> inventoryCancelFallback(String holdId, Throwable ex) {
        LOGGER.warn("TL_FALLBACK inventoryTL(cancel) triggered holdId={} cause={}", holdId, rootMsg(ex), ex);

        // For compensation, often best-effort: return "accepted" and later Kafka retry
        return CompletableFuture.completedFuture(Map.of(
                "status", "CANCEL_DEFERRED",
                "holdId", holdId
        ));
    }


    // PAYMENT PAY
    public CompletableFuture<PaymentResponse> paymentPayFallback(String bookingId, long amount, boolean failPayment, Throwable ex) {
        LOGGER.error("TL_FALLBACK paymentTL(pay) triggered bookingId={} amount={} cause={}",
                bookingId, amount, rootMsg(ex), ex);

        // Fail fast (so you mark PAYMENT_FAILED and cancel hold)
        return CompletableFuture.failedFuture(new PaymentUnavailableException("Payment timed out/unavailable", ex));

        // Or return a synthetic failure response:
        // return CompletableFuture.completedFuture(new PaymentResponse(null, "FAILED"));
    }

    // PAYMENT REFUND (COMPENSATION)
    public CompletableFuture<Map<String, Object>> paymentRefundFallback(String bookingId, Throwable ex) {
        LOGGER.warn("TL_FALLBACK paymentTL(refund) triggered bookingId={} cause={}", bookingId, rootMsg(ex), ex);

        // Best-effort compensation: return deferred so caller can publish Kafka retry
        return CompletableFuture.completedFuture(Map.of(
                "status", "REFUND_DEFERRED",
                "bookingId", bookingId
        ));
    }


    private String rootMsg(Throwable t) {
        return (t.getCause() != null && t.getCause().getMessage() != null)
                ? t.getCause().getMessage()
                : String.valueOf(t.getMessage());
    }
}
