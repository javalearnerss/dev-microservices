package com.learn.kube.deployment.booking.external.service.feign;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "paymentClient", url = "${payment.base-url}")
public interface PaymentClient {

    /**
     * Pay for a booking.
     * POST /payments/pay?bookingId=...&amount=500&fail=false
     */
    @PostMapping("/payments/pay")
    PaymentResponse pay(@RequestParam("bookingId") String bookingId,
                        @RequestParam("amount") long amount,
                        @RequestParam(value = "fail", defaultValue = "false") boolean fail);

    /**
     * Refund a payment (used if inventory confirm fails after payment).
     * POST /payments/refund/{bookingId}
     */
    @PostMapping("/payments/refund/{bookingId}")
    Map<String, Object> refund(@PathVariable("bookingId") String bookingId);

    record PaymentResponse(String paymentId, String bookingId, String status, long amount) {}
}

