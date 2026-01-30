package com.learn.kube.deployment.booking.external.service.feign;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(
        name = "inventoryClient",
        url = "${inventory.base-url}"
)
public interface InventoryClient {

    /**
     * Places a temporary HOLD on tickets for a show.
     * Inventory returns holdId used later for confirm/cancel.
     *
     * POST /inventory/shows/{showId}/hold?qty=2
     */
    @PostMapping("/inventory/shows/{showId}/hold")
    HoldResponse hold(@PathVariable("showId") String showId, @RequestParam("qty") int qty);

    /**
     * Confirms the hold after payment success.
     *
     * POST /inventory/holds/{holdId}/confirm
     */
    @PostMapping("/inventory/holds/{holdId}/confirm")
    Map<String, Object> confirm(@PathVariable("holdId") String holdId);

    /**
     * Cancels the hold (payment failed/user cancelled) and releases tickets.
     *
     * POST /inventory/holds/{holdId}/cancel
     */
    @PostMapping("/inventory/holds/{holdId}/cancel")
    Map<String, Object> cancel(@PathVariable("holdId") String holdId);

    // Minimal response expected from inventory hold API
    record HoldResponse(String holdId, String showId, int heldQty, int remaining) {}
}

