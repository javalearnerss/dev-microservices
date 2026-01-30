package com.learn.kube.deployment.inventory.controller;

import jakarta.annotation.PostConstruct;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/inventory")
public class TicketInventoryController {

    // Stores remaining ticket count per show (showId -> remaining tickets)
    private final ConcurrentHashMap<String, Integer> remaining = new ConcurrentHashMap<>();

    // Stores temporary ticket holds until payment is confirmed or cancelled
    private final ConcurrentHashMap<String, Hold> holds = new ConcurrentHashMap<>();

    /**
     * Load dummy shows into inventory at application startup.
     * Useful for local testing, UI demos, and interviews.
     */
    @PostConstruct
    void loadDummyShows() {
        remaining.put("SHOW-101", 100);
        remaining.put("SHOW-102", 50);
        remaining.put("SHOW-103", 20);
    }

    @GetMapping("/shows")
    public ResponseEntity<?> getAllShows() {
        return ResponseEntity.ok(
                remaining.entrySet().stream()
                        .map(e -> Map.of(
                                "showId", e.getKey(),
                                "remaining", e.getValue()
                        ))
                        .toList()
        );
    }

    /**
     * Create or update ticket inventory for a show.
     * <p>
     * POST /inventory/shows/SHOW-101?totalTickets=100
     */
    @PostMapping("/shows/{showId}")
    public ResponseEntity<?> createOrUpdateShow(
            @PathVariable String showId,
            @RequestParam int totalTickets) {

        if (totalTickets < 0)
            return bad("totalTickets must be >= 0");

        remaining.put(showId, totalTickets);

        return ResponseEntity.ok(
                Map.of("showId", showId, "remaining", remaining.get(showId))
        );
    }

    /**
     * Fetch remaining tickets for a given show.
     * <p>
     * GET /inventory/shows/SHOW-101
     */
    @GetMapping("/shows/{showId}")
    public ResponseEntity<?> getShow(@PathVariable String showId) {
        Integer qty = remaining.get(showId);
        if (qty == null)
            return ResponseEntity.notFound().build();

        return ResponseEntity.ok(
                Map.of("showId", showId, "remaining", qty)
        );
    }

    /**
     * Hold (reserve) tickets temporarily.
     * <p>
     * POST /inventory/shows/SHOW-101/hold?qty=2
     */
    @PostMapping("/shows/{showId}/hold")
    public ResponseEntity<?> hold(
            @PathVariable String showId,
            @RequestParam int qty) {

        if (qty <= 0)
            return bad("qty must be > 0");

        try {
            int after = remaining.compute(showId, (k, v) -> {
                if (v == null) throw new IllegalArgumentException("showId not found");
                if (v < qty) throw new IllegalStateException("insufficient tickets");
                return v - qty;
            });

            String holdId = UUID.randomUUID().toString();
            holds.put(holdId, new Hold(holdId, showId, qty, Instant.now().toString()));

            return ResponseEntity.ok(Map.of(
                    "holdId", holdId,
                    "showId", showId,
                    "heldQty", qty,
                    "remaining", after
            ));
        } catch (RuntimeException ex) {
            return bad(ex.getMessage());
        }
    }

    /**
     * Confirm ticket purchase after payment success.
     * <p>
     * POST /inventory/holds/{holdId}/confirm
     */
    @PostMapping("/holds/{holdId}/confirm")
    public ResponseEntity<?> confirm(@PathVariable String holdId) {
        Hold hold = holds.remove(holdId);
        if (hold == null)
            return bad("invalid holdId");

        return ResponseEntity.ok(Map.of(
                "status", "CONFIRMED",
                "holdId", holdId,
                "showId", hold.showId,
                "qty", hold.qty
        ));
    }

    /**
     * Cancel ticket hold and release inventory.
     * <p>
     * POST /inventory/holds/{holdId}/cancel
     */
    @PostMapping("/holds/{holdId}/cancel")
    public ResponseEntity<?> cancel(@PathVariable String holdId) {
        Hold hold = holds.remove(holdId);
        if (hold == null)
            return bad("invalid holdId");

        int after = remaining.compute(
                hold.showId,
                (k, v) -> (v == null ? 0 : v) + hold.qty
        );

        return ResponseEntity.ok(Map.of(
                "status", "CANCELLED",
                "holdId", holdId,
                "showId", hold.showId,
                "releasedQty", hold.qty,
                "remaining", after
        ));
    }

    private ResponseEntity<Map<String, String>> bad(String msg) {
        return ResponseEntity.badRequest().body(Map.of("error", msg));
    }

    private record Hold(String holdId, String showId, int qty, String createdAt) {
    }
}
