package com.learn.kube.deployment.booking.service;

import com.learn.kube.deployment.booking.external.service.feign.InventoryClient;
import com.learn.kube.deployment.booking.external.service.feign.PaymentClient;
import com.learn.kube.deployment.booking.external.service.feign.UserClient;
import com.learn.kube.deployment.booking.model.Booking;
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
public class BookingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookingService.class);

    @Autowired
    private InventoryClient inventoryClient;
    @Autowired
    private UserClient userClient;
    @Autowired
    private PaymentClient paymentClient;

    @Autowired
    private TimeoutClients timeoutClients;

    public Booking createBooking(Booking booking, boolean failPayment) {

        LOGGER.info("TimeoutClients bean class = {}", timeoutClients.getClass());

        timeoutClients.getUserAsync(booking.getUserId())
                .whenComplete((user, e) -> {
                    if (e != null) {
                        LOGGER.info("STEP1 user-validate <- failed userID={}, bookingId={}", booking.getUserId(), booking.getBookingId(), e);
                    } else {
                        LOGGER.info("STEP1 user-validate <- success userID={}, bookingId={}", booking.getUserId(), booking.getBookingId());
                    }
                })
                .thenCompose(user -> timeoutClients.holdAsync(booking.getShowId(), booking.getQty()))
                .whenComplete((hold, e) -> {
                    if (e != null) {
                        LOGGER.warn("STEP2 inventory-hold <- failed bookingId={} null hold/holdId", booking.getBookingId(), e);
                    } else {
                        LOGGER.info("STEP2 inventory-hold <- success bookingId={} holdId={}", booking.getBookingId(), hold.holdId());
                        booking.setHoldId(hold.holdId());
                    }
                })
                .thenCompose(holdResponse -> timeoutClients.paymentAsync(booking.getBookingId(), booking.getAmount(), failPayment))
                .whenComplete((paymentResponse, e) -> {
                    if (e != null) {
                        booking.withStatus("PAYMENT_FAILED");
                        LOGGER.error("createBooking failed, error occurred in payment", e);
                        safeCancelHold(booking.getHoldId());
                    } else {
                        if ("SUCCESS".equalsIgnoreCase(paymentResponse.status())) {
                            timeoutClients.confirmAsync(booking.getHoldId()).whenComplete((result, ex) -> {
                                if (ex != null) {
                                    safeCancelHold(booking.getHoldId());
                                    safeRefund(booking.getBookingId());
                                    booking.withStatus("CONFIRM_FAILED_REFUNDED");
                                    LOGGER.error("createBooking failed, payment confirmation failed", e);
                                } else {
                                    booking.setStatus("CONFIRMED");
                                    booking.setPaymentStatus(paymentResponse.status());
                                    booking.setPaymentId(paymentResponse.paymentId());
                                    LOGGER.info("STEP3 inventory-confirm <- success bookingId={}", booking.getBookingId());
                                    LOGGER.info("STATE booking saved bookingId={} status={}", booking.getBookingId(), booking.getStatus());
                                }
                            }).join();
                        }
                    }
                }).join();

        return booking;
    }

    private void safeRefund(String bookingId) {
        timeoutClients.refundAsync(bookingId).handle((result, ex) -> {
            if (ex != null) {
                LOGGER.warn("PAYMENT refund failed bookingId={} cause={}", bookingId, ex);
                // Publish an even to kafka topic for refund
            } else {
                LOGGER.info("PAYMENT refund success bookingId={}", bookingId);
            }
            return null;
        }).join();
    }

    private void safeCancelHold(String holdId) {
        timeoutClients.cancelAsync(holdId).handle((result, ex) -> {
            if (ex != null) {
                LOGGER.warn("INVENTORY cancel hold failed holdId={} cause={}", holdId, ex);
                // Publish an even to kafka topic for cancellation
            } else {
                LOGGER.info("INVENTORY cancel hold success holdId={}", holdId);
            }
            return null;
        }).join();
    }


}
