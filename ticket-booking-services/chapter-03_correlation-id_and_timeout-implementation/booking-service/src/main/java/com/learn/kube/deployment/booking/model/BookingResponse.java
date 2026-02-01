package com.learn.kube.deployment.booking.model;

public record BookingResponse(
        String bookingId,
        String status,
        String holdId,
        String paymentId,
        String message
) {}
