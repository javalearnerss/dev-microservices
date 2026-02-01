package com.learn.kube.deployment.payment.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    // bookingId -> payment record (in-memory)  (demo idempotency)
    private final ConcurrentHashMap<String, Payment> payments = new ConcurrentHashMap<>();

    /**
     * Creates a payment for a booking.
     * Example:
     * POST /payments/pay?bookingId=...&amount=500
     * POST /payments/pay?bookingId=...&amount=500&fail=true
     */
    @PostMapping("/pay")
    public ResponseEntity<?> pay(@RequestParam String bookingId,
                                 @RequestParam long amount,
                                 @RequestParam(defaultValue = "false") boolean fail) {

        logger.info("POST /payments/pay called. bookingId={}, amount={}, fail={}", bookingId, amount, fail);

        if (amount <= 0) {
            logger.warn("Payment rejected: invalid amount. bookingId={}, amount={}", bookingId, amount);
            return bad("amount must be > 0");
        }

        // Idempotency: one payment per bookingId (simple demo)
        Payment existing = payments.get(bookingId);
        if (existing != null) {
            logger.info("Idempotent payment hit: returning existing payment. bookingId={}, paymentId={}, status={}",
                    bookingId, existing.paymentId, existing.status);

            return ResponseEntity.ok(Map.of(
                    "paymentId", existing.paymentId,
                    "bookingId", existing.bookingId,
                    "status", existing.status,
                    "amount", existing.amount
            ));
        }

        String paymentId = UUID.randomUUID().toString();
        String status = fail ? "FAILED" : "SUCCESS";

        Payment p = new Payment(paymentId, bookingId, amount, status, Instant.now().toString());
        payments.put(bookingId, p);

        if ("SUCCESS".equals(status)) {
            logger.info("Payment success. bookingId={}, paymentId={}, amount={}", bookingId, paymentId, amount);
        } else {
            logger.warn("Payment failed (simulated). bookingId={}, paymentId={}, amount={}", bookingId, paymentId, amount);
        }

        return ResponseEntity.ok(Map.of(
                "paymentId", paymentId,
                "bookingId", bookingId,
                "status", status,
                "amount", amount
        ));
    }

    /**
     * Returns payment status for a booking.
     * Example:
     * GET /payments/status/{bookingId}
     */
    @GetMapping("/status/{bookingId}")
    public ResponseEntity<?> status(@PathVariable String bookingId) {
        logger.info("GET /payments/status/{} called", bookingId);

        Payment p = payments.get(bookingId);
        if (p == null) {
            logger.warn("Payment not found. bookingId={}", bookingId);
            return ResponseEntity.notFound().build();
        }

        logger.info("Payment status returned. bookingId={}, paymentId={}, status={}", bookingId, p.paymentId, p.status);

        return ResponseEntity.ok(Map.of(
                "paymentId", p.paymentId,
                "bookingId", p.bookingId,
                "status", p.status,
                "amount", p.amount
        ));
    }

    /**
     * Refunds a successful payment for a booking.
     * Example:
     * POST /payments/refund/{bookingId}
     */
    @PostMapping("/refund/{bookingId}")
    public ResponseEntity<?> refund(@PathVariable String bookingId) {
        logger.info("POST /payments/refund/{} called", bookingId);

        Payment p = payments.get(bookingId);
        if (p == null) {
            logger.warn("Refund rejected: payment not found. bookingId={}", bookingId);
            return bad("payment not found");
        }

        if (!"SUCCESS".equals(p.status)) {
            logger.warn("Refund rejected: only SUCCESS can be refunded. bookingId={}, currentStatus={}", bookingId, p.status);
            return bad("only SUCCESS payments can be refunded");
        }

        Payment refunded = p.withStatus("REFUNDED");
        payments.put(bookingId, refunded);

        logger.info("Refund successful. bookingId={}, paymentId={}, amount={}", bookingId, refunded.paymentId, refunded.amount);

        return ResponseEntity.ok(Map.of(
                "paymentId", refunded.paymentId,
                "bookingId", refunded.bookingId,
                "status", refunded.status,
                "amount", refunded.amount
        ));
    }

    /**
     * Helper method to return consistent bad request responses.
     */
    private ResponseEntity<Map<String, String>> bad(String msg) {
        logger.warn("Bad request: {}", msg);
        return ResponseEntity.badRequest().body(Map.of("error", msg));
    }

    /**
     * In-memory payment record.
     * Keyed by bookingId for simple idempotency in this demo.
     */
    private static class Payment {
        final String paymentId;
        final String bookingId;
        final long amount;
        final String status; // SUCCESS | FAILED | REFUNDED
        final String createdAt;

        Payment(String paymentId, String bookingId, long amount, String status, String createdAt) {
            this.paymentId = paymentId;
            this.bookingId = bookingId;
            this.amount = amount;
            this.status = status;
            this.createdAt = createdAt;
        }

        Payment withStatus(String newStatus) {
            return new Payment(this.paymentId, this.bookingId, this.amount, newStatus, this.createdAt);
        }
    }
}
