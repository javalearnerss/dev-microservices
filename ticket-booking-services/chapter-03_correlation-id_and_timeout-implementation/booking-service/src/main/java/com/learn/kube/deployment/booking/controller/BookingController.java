package com.learn.kube.deployment.booking.controller;

import com.learn.kube.deployment.booking.external.service.feign.InventoryClient;
import com.learn.kube.deployment.booking.external.service.feign.PaymentClient;
import com.learn.kube.deployment.booking.external.service.feign.UserClient;
import com.learn.kube.deployment.booking.model.Booking;
import com.learn.kube.deployment.booking.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookingController.class);

    private final ConcurrentHashMap<String, Booking> bookings = new ConcurrentHashMap<>();

    private final InventoryClient inventoryClient;
    private final UserClient userClient;
    private final PaymentClient paymentClient;


    @Autowired
    private BookingService bookingService;

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

        final String bookingId = UUID.randomUUID().toString();
        LOGGER.info("CREATE_BOOKING start bookingId={} userId={} showId={} qty={} amount={} failPayment={}",
                bookingId, userId, showId, qty, amount, failPayment);

        Booking booking = new Booking(
                bookingId, userId, showId, qty, null,
                "PENDING_PAYMENT", Instant.now().toString(),
                null, amount, "NOT_STARTED"
        );
        bookings.put(bookingId, booking);
        try {
            bookingService.createBooking(booking, failPayment);
            LOGGER.info("CREATE_BOOKING end bookingId={} status={}", bookingId, booking.getStatus());
        }catch(Exception e){
            LOGGER.error("CREATE_BOOKING failed bookingId={}", bookingId);
            throw e;
        }
        return ResponseEntity.ok(toResponse(booking));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<?> getBooking(@PathVariable String bookingId) {
        LOGGER.info("GET_BOOKING bookingId={}", bookingId);
        Booking b = bookings.get(bookingId);
        if (b == null) {
            LOGGER.warn("GET_BOOKING not found bookingId={}", bookingId);
            return ResponseEntity.notFound().build();
        }
        LOGGER.info("GET_BOOKING found bookingId={} status={} userId={} showId={} qty={}",
                bookingId, b.getStatus(), b.getUserId(), b.getShowId(), b.getQty());
        return ResponseEntity.ok(toResponse(b));
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<?> cancelBooking(@PathVariable String bookingId) {
        LOGGER.info("CANCEL_BOOKING start bookingId={}", bookingId);

        Booking b = bookings.get(bookingId);
        if (b == null) {
            LOGGER.warn("CANCEL_BOOKING booking not found bookingId={}", bookingId);
            return bad("booking not found");
        }

        if ("CANCELLED".equals(b.getStatus())) {
            LOGGER.info("CANCEL_BOOKING already cancelled bookingId={}", bookingId);
            return ResponseEntity.ok(toResponse(b));
        }

        if ("CONFIRMED".equals(b.getStatus())) {
            LOGGER.warn("CANCEL_BOOKING not supported for CONFIRMED bookingId={}", bookingId);
            return bad("confirmed booking cancel not supported in this demo");
        }

        if (b.getHoldId() != null && !b.getHoldId().isBlank()) {
            LOGGER.info("CANCEL_BOOKING cancelling holdId={} bookingId={}", b.getHoldId(), bookingId);
            safeCancelHold(b.getHoldId());
        }

        Booking updated = b.withStatus("CANCELLED");
        bookings.put(bookingId, updated);

        LOGGER.info("CANCEL_BOOKING end bookingId={} status={}", bookingId, updated.getStatus());
        return ResponseEntity.ok(toResponse(updated));
    }

    private void safeCancelHold(String holdId) {
        try {
            inventoryClient.cancel(holdId);
            LOGGER.info("INVENTORY cancel hold success holdId={}", holdId);
        } catch (Exception e) {
            LOGGER.warn("INVENTORY cancel hold failed holdId={} cause={}", holdId, rootMsg(e));
        }
    }

    private void safeRefund(String bookingId) {
        try {
            LOGGER.info("PAYMENT refund best-effort bookingId={}", bookingId);
            paymentClient.refund(bookingId);
            LOGGER.info("PAYMENT refund success bookingId={}", bookingId);
        } catch (Exception e) {
            LOGGER.error("PAYMENT refund failed bookingId={} cause={}", bookingId, rootMsg(e));
        }
    }

    private Map<String, Object> toResponse(Booking b) {
        return Map.of(
                "bookingId", b.getBookingId(),
                "userId", b.getUserId(),
                "showId", b.getShowId(),
                "qty", b.getQty(),
                "status", b.getStatus(),
                "holdId", b.getHoldId(),
                "createdAt", b.getCreatedAt(),
                "paymentId", b.getPaymentId() == null ? "" : b.getPaymentId(),
                "amount", b.getAmount(),
                "paymentStatus", b.getPaymentStatus()
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


}
