package com.learn.kube.deployment.booking.model;

public record PaymentResponse(String paymentId, String bookingId, String status, long amount) {}
