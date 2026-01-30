package com.learn.kube.deployment.booking.controller;

import com.learn.kube.deployment.booking.external.service.feign.InventoryClient;
import com.learn.kube.deployment.booking.external.service.feign.UserClient;
import com.learn.kube.deployment.booking.external.service.feign.PaymentClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private static final Logger log = LoggerFactory.getLogger(BookingController.class);

    private final ConcurrentHashMap<String, Booking> bookings = new ConcurrentHashMap<>();

    private final InventoryClient inventoryClient;
    private final UserClient userClient;
    private final PaymentClient paymentClient;

    public BookingController(InventoryClient inventoryClient, UserClient userClient, PaymentClient paymentClient) {
        this.inventoryClient = inventoryClient;
        this.userClient = userClient;
        this.paymentClient = paymentClient;
    }

    @PostMapping
    public ResponseEntity<?> createBooking(@RequestParam String userId,
                                           @RequestParam String showId,
                                           @RequestParam int qty,
                                           @RequestParam long amount,
                                           @RequestParam(defaultValue = "false") boolean failPayment) {

        final long startNs = System.nanoTime();
        final String bookingId = UUID.randomUUID().toString();
        final String traceId = bookingId; // simplest: bookingId as traceId (works great)

        String holdId = null;
        String paymentId = null;

        MDC.put("traceId", traceId);
        MDC.put("bookingId", bookingId);

        log.info("CREATE_BOOKING start userId={} showId={} qty={} amount={} failPayment={}",
                userId, showId, qty, amount, failPayment);

        try {
            if (qty <= 0) {
                log.warn("CREATE_BOOKING validation failed: qty must be > 0 (qty={})", qty);
                return bad("qty must be > 0");
            }
            if (amount <= 0) {
                log.warn("CREATE_BOOKING validation failed: amount must be > 0 (amount={})", amount);
                return bad("amount must be > 0");
            }

            // 1) validate user exists
            long t1 = System.nanoTime();
            log.info("STEP1 user-validate -> calling UserService.getUser userId={}", userId);
            userClient.getUser(userId);
            log.info("STEP1 user-validate <- success in {}ms", msSince(t1));

            // 2) hold tickets
            long t2 = System.nanoTime();
            log.info("STEP2 inventory-hold -> calling InventoryService.hold showId={} qty={}", showId, qty);
            InventoryClient.HoldResponse hold = inventoryClient.hold(showId, qty);

            if (hold == null || hold.holdId() == null) {
                log.warn("STEP2 inventory-hold <- failed: null hold/holdId in {}ms", msSince(t2));
                return bad("inventory hold failed");
            }

            holdId = hold.holdId();
            MDC.put("holdId", holdId);
            log.info("STEP2 inventory-hold <- success holdId={} in {}ms", holdId, msSince(t2));

            // Create initial booking as PENDING_PAYMENT
            Booking pending = new Booking(
                    bookingId, userId, showId, qty, holdId,
                    "PENDING_PAYMENT", Instant.now().toString(),
                    null, amount, "NOT_STARTED"
            );
            bookings.put(bookingId, pending);
            log.info("STATE booking saved status={} holdId={}", pending.status, pending.holdId);

            // 3) pay
            long t3 = System.nanoTime();
            log.info("STEP3 payment-pay -> calling PaymentService.pay bookingId={} amount={} failPayment={}",
                    bookingId, amount, failPayment);
            PaymentClient.PaymentResponse pay = paymentClient.pay(bookingId, amount, failPayment);

            if (pay == null || pay.status() == null) {
                log.error("STEP3 payment-pay <- invalid response (pay/status null) in {}ms. Cancelling holdId={}",
                        msSince(t3), holdId);

                safeCancelHold(holdId);

                Booking failed = pending.withStatus("PAYMENT_FAILED").withPayment(null, "FAILED");
                bookings.put(bookingId, failed);

                log.info("STATE booking saved status={} paymentStatus={}", failed.status, failed.paymentStatus);
                log.info("CREATE_BOOKING end status={} totalTimeMs={}", failed.status, msSince(startNs));
                return ResponseEntity.ok(toResponse(failed));
            }

            paymentId = pay.paymentId();
            MDC.put("paymentId", paymentId == null ? "" : paymentId);

            log.info("STEP3 payment-pay <- response paymentId={} status={} in {}ms",
                    paymentId, pay.status(), msSince(t3));

            // Update payment info
            Booking paid = pending.withPayment(paymentId, pay.status());
            bookings.put(bookingId, paid);
            log.info("STATE booking saved status={} paymentStatus={} paymentId={}",
                    paid.status, paid.paymentStatus, paid.paymentId);

            if (!"SUCCESS".equalsIgnoreCase(pay.status())) {
                log.warn("STEP3 payment-pay status != SUCCESS (status={}) -> cancelling holdId={}", pay.status(), holdId);
                safeCancelHold(holdId);

                Booking failed = paid.withStatus("PAYMENT_FAILED");
                bookings.put(bookingId, failed);

                log.info("STATE booking saved status={}", failed.status);
                log.info("CREATE_BOOKING end status={} totalTimeMs={}", failed.status, msSince(startNs));
                return ResponseEntity.ok(toResponse(failed));
            }

            // 4) payment success -> confirm inventory
            long t4 = System.nanoTime();
            try {
                log.info("STEP4 inventory-confirm -> calling InventoryService.confirm holdId={}", holdId);
                inventoryClient.confirm(holdId);

                Booking confirmed = paid.withStatus("CONFIRMED");
                bookings.put(bookingId, confirmed);

                log.info("STEP4 inventory-confirm <- success in {}ms", msSince(t4));
                log.info("STATE booking saved status={}", confirmed.status);
                log.info("CREATE_BOOKING end status={} totalTimeMs={}", confirmed.status, msSince(startNs));
                return ResponseEntity.ok(toResponse(confirmed));

            } catch (Exception confirmEx) {
                log.error("STEP4 inventory-confirm <- failed in {}ms. Will cancel hold + refund. cause={}",
                        msSince(t4), rootMsg(confirmEx));

                safeCancelHold(holdId);
                safeRefund(bookingId);

                Booking failed = paid.withStatus("CONFIRM_FAILED_REFUNDED");
                bookings.put(bookingId, failed);

                log.info("STATE booking saved status={}", failed.status);
                log.info("CREATE_BOOKING end status={} totalTimeMs={}", failed.status, msSince(startNs));
                return ResponseEntity.ok(toResponse(failed));
            }

        } catch (Exception ex) {
            log.error("CREATE_BOOKING failed: {}. bookingId={} holdId={} paymentId={}",
                    rootMsg(ex), bookingId, holdId, paymentId, ex);

            if (holdId != null) {
                log.warn("CREATE_BOOKING rollback: cancelling holdId={} best-effort", holdId);
                safeCancelHold(holdId);
            }

            log.info("CREATE_BOOKING end status=ERROR totalTimeMs={}", msSince(startNs));
            return bad("createBooking failed: " + rootMsg(ex));

        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<?> getBooking(@PathVariable String bookingId) {
        MDC.put("traceId", bookingId);
        MDC.put("bookingId", bookingId);
        try {
            log.info("GET_BOOKING bookingId={}", bookingId);
            Booking b = bookings.get(bookingId);
            if (b == null) {
                log.warn("GET_BOOKING not found bookingId={}", bookingId);
                return ResponseEntity.notFound().build();
            }
            log.info("GET_BOOKING found status={} userId={} showId={} qty={}", b.status, b.userId, b.showId, b.qty);
            return ResponseEntity.ok(toResponse(b));
        } finally {
            MDC.clear();
        }
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<?> cancelBooking(@PathVariable String bookingId) {
        MDC.put("traceId", bookingId);
        MDC.put("bookingId", bookingId);
        try {
            log.info("CANCEL_BOOKING start bookingId={}", bookingId);

            Booking b = bookings.get(bookingId);
            if (b == null) {
                log.warn("CANCEL_BOOKING booking not found bookingId={}", bookingId);
                return bad("booking not found");
            }

            if ("CANCELLED".equals(b.status)) {
                log.info("CANCEL_BOOKING already cancelled bookingId={}", bookingId);
                return ResponseEntity.ok(toResponse(b));
            }

            if ("CONFIRMED".equals(b.status)) {
                log.warn("CANCEL_BOOKING not supported for CONFIRMED bookingId={}", bookingId);
                return bad("confirmed booking cancel not supported in this demo");
            }

            if (b.holdId != null && !b.holdId.isBlank()) {
                log.info("CANCEL_BOOKING cancelling holdId={}", b.holdId);
                safeCancelHold(b.holdId);
            }

            Booking updated = b.withStatus("CANCELLED");
            bookings.put(bookingId, updated);

            log.info("CANCEL_BOOKING end status={}", updated.status);
            return ResponseEntity.ok(toResponse(updated));
        } finally {
            MDC.clear();
        }
    }

    private void safeCancelHold(String holdId) {
        try {
            log.info("INVENTORY cancel hold best-effort holdId={}", holdId);
            inventoryClient.cancel(holdId);
            log.info("INVENTORY cancel hold success holdId={}", holdId);
        } catch (Exception e) {
            log.warn("INVENTORY cancel hold failed holdId={} cause={}", holdId, rootMsg(e));
        }
    }

    private void safeRefund(String bookingId) {
        try {
            log.info("PAYMENT refund best-effort bookingId={}", bookingId);
            paymentClient.refund(bookingId);
            log.info("PAYMENT refund success bookingId={}", bookingId);
        } catch (Exception e) {
            log.warn("PAYMENT refund failed bookingId={} cause={}", bookingId, rootMsg(e));
        }
    }

    private Map<String, Object> toResponse(Booking b) {
        return Map.of(
                "bookingId", b.bookingId,
                "userId", b.userId,
                "showId", b.showId,
                "qty", b.qty,
                "status", b.status,
                "holdId", b.holdId,
                "createdAt", b.createdAt,
                "paymentId", b.paymentId == null ? "" : b.paymentId,
                "amount", b.amount,
                "paymentStatus", b.paymentStatus
        );
    }

    private ResponseEntity<Map<String, String>> bad(String msg) {
        return ResponseEntity.badRequest().body(Map.of("error", msg));
    }

    private String rootMsg(Throwable t) {
        return (t.getCause() != null && t.getCause().getMessage() != null)
                ? t.getCause().getMessage()
                : String.valueOf(t.getMessage());
    }

    private static long msSince(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000;
    }

    private static class Booking {
        final String bookingId;
        final String userId;
        final String showId;
        final int qty;
        final String holdId;
        final String status;
        final String createdAt;

        final String paymentId;
        final long amount;
        final String paymentStatus;

        Booking(String bookingId, String userId, String showId, int qty, String holdId,
                String status, String createdAt, String paymentId, long amount, String paymentStatus) {
            this.bookingId = bookingId;
            this.userId = userId;
            this.showId = showId;
            this.qty = qty;
            this.holdId = holdId;
            this.status = status;
            this.createdAt = createdAt;
            this.paymentId = paymentId;
            this.amount = amount;
            this.paymentStatus = paymentStatus;
        }

        Booking withStatus(String newStatus) {
            return new Booking(this.bookingId, this.userId, this.showId, this.qty, this.holdId,
                    newStatus, this.createdAt, this.paymentId, this.amount, this.paymentStatus);
        }

        Booking withPayment(String newPaymentId, String newPaymentStatus) {
            return new Booking(this.bookingId, this.userId, this.showId, this.qty, this.holdId,
                    this.status, this.createdAt, newPaymentId, this.amount, newPaymentStatus);
        }
    }
}
