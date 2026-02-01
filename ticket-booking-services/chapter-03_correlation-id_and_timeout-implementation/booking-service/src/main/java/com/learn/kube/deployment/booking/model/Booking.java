package com.learn.kube.deployment.booking.model;


public class Booking {
    private String bookingId;
    private String userId;
    private String showId;
    private int qty;
    private String holdId;
    private String status;
    private String createdAt;

    private String paymentId;
    private long amount;
    private String paymentStatus;

    public Booking(String bookingId, String userId, String showId, int qty, String holdId,
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

    public Booking withStatus(String newStatus) {
        return new Booking(this.bookingId, this.userId, this.showId, this.qty, this.holdId,
                newStatus, this.createdAt, this.paymentId, this.amount, this.paymentStatus);
    }

    public Booking withPayment(String newPaymentId, String newPaymentStatus) {
        return new Booking(this.bookingId, this.userId, this.showId, this.qty, this.holdId,
                this.status, this.createdAt, newPaymentId, this.amount, newPaymentStatus);
    }

    public String getBookingId() {
        return bookingId;
    }

    public String getUserId() {
        return userId;
    }

    public String getShowId() {
        return showId;
    }

    public int getQty() {
        return qty;
    }

    public String getHoldId() {
        return holdId;
    }

    public String getStatus() {
        return status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public long getAmount() {
        return amount;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setShowId(String showId) {
        this.showId = showId;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    public void setHoldId(String holdId) {
        this.holdId = holdId;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
}