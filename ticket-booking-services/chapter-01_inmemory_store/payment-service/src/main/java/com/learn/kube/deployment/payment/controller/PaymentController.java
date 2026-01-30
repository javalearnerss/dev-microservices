package com.learn.kube.deployment.payment.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    // paymentId -> payment record (in-memory)
    private final ConcurrentHashMap<String, Payment> payments = new ConcurrentHashMap<>();

    /**
     * Creates a payment for a booking.
     * This is a demo "fake gateway": it marks payment SUCCESS unless you pass fail=true.
     *
     * Example:
     * POST /payments/pay?bookingId=...&amount=500
     * POST /payments/pay?bookingId=...&amount=500&fail=true
     */
    @PostMapping("/pay")
    public ResponseEntity<?> pay(@RequestParam String bookingId,
                                 @RequestParam long amount,
                                 @RequestParam(defaultValue = "false") boolean fail) {

        if (amount <= 0) return bad("amount must be > 0");

        // Idempotency: one payment per bookingId (simple demo)
        Payment existing = payments.get(bookingId);
        if (existing != null) {
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
        Payment p = payments.get(bookingId);
        if (p == null) return ResponseEntity.notFound().build();

        return ResponseEntity.ok(Map.of(
                "paymentId", p.paymentId,
                "bookingId", p.bookingId,
                "status", p.status,
                "amount", p.amount
        ));
    }

    /**
     * Refunds a successful payment for a booking.
     * In real life: gateway refund API call + async webhook.
     *
     * Example:
     * POST /payments/refund/{bookingId}
     */
    @PostMapping("/refund/{bookingId}")
    public ResponseEntity<?> refund(@PathVariable String bookingId) {
        Payment p = payments.get(bookingId);
        if (p == null) return bad("payment not found");
        if (!"SUCCESS".equals(p.status)) return bad("only SUCCESS payments can be refunded");

        Payment refunded = p.withStatus("REFUNDED");
        payments.put(bookingId, refunded);

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

