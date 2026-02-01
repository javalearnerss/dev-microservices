package com.learn.kube.deployment.booking.exception;

import com.learn.kube.deployment.booking.model.BookingResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {


    @ExceptionHandler(InventoryUnavailableException.class)
    public ResponseEntity<BookingResponse> inventoryDown(InventoryUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new BookingResponse(null, "FAILED", null, null, ex.getMessage()));
    }

    @ExceptionHandler(PaymentUnavailableException.class)
    public ResponseEntity<BookingResponse> paymentDown(PaymentUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new BookingResponse(null, "FAILED", null, null, ex.getMessage()));
    }

    @ExceptionHandler(java.util.concurrent.TimeoutException.class)
    public ResponseEntity<BookingResponse> timeout(java.util.concurrent.TimeoutException ex) {
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .body(new BookingResponse(null, "FAILED", null, null, "Downstream timeout."));
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<BookingResponse> passthrough(org.springframework.web.server.ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(new BookingResponse(null, "FAILED", null, null, ex.getReason()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BookingResponse> generic(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new BookingResponse(null, "FAILED", null, null, "Unexpected error."));
    }

    @ExceptionHandler(java.util.concurrent.CompletionException.class)
    public ResponseEntity<BookingResponse> completion(java.util.concurrent.CompletionException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof PaymentUnavailableException pue) return paymentDown(pue);
        if (cause instanceof InventoryUnavailableException iue) return inventoryDown(iue);
        if (cause instanceof java.util.concurrent.TimeoutException te) return timeout(te);
        return generic(new Exception(cause));
    }
}

