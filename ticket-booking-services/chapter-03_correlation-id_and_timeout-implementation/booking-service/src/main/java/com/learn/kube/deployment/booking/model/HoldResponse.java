package com.learn.kube.deployment.booking.model;

public record HoldResponse(String holdId, String showId, int heldQty, int remaining) {}
