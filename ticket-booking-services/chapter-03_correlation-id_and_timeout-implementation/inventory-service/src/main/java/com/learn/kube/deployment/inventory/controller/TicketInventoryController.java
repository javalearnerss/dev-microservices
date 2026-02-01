package com.learn.kube.deployment.inventory.controller;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/inventory")
public class TicketInventoryController {

    private static final Logger logger = LoggerFactory.getLogger(TicketInventoryController.class);
    
    // showId -> remaining tickets
    private final ConcurrentHashMap<String, Integer> remaining = new ConcurrentHashMap<>();

    // holdId -> hold details
    private final ConcurrentHashMap<String, Hold> holds = new ConcurrentHashMap<>();

    @PostConstruct
    void loadDummyShows() {
        logger.info("Loading dummy shows into inventory");

        remaining.put("SHOW-101", 100);
        remaining.put("SHOW-102", 50);
        remaining.put("SHOW-103", 20);

        logger.info("Inventory initialized. showsLoaded={}", remaining.size());
    }

    @GetMapping("/shows")
    public ResponseEntity<?> getAllShows() {
        logger.info("GET /inventory/shows called");

        var result = remaining.entrySet().stream()
                .map(e -> Map.of(
                        "showId", e.getKey(),
                        "remaining", e.getValue()
                ))
                .toList();

        logger.info("Returning all shows. count={}", result.size());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/shows/{showId}")
    public ResponseEntity<?> createOrUpdateShow(
            @PathVariable String showId,
            @RequestParam int totalTickets) {

        logger.info("POST /inventory/shows/{} called. totalTickets={}", showId, totalTickets);

        if (totalTickets < 0) {
            logger.warn("Invalid totalTickets for showId={}. totalTickets={}", showId, totalTickets);
            return bad("totalTickets must be >= 0");
        }

        remaining.put(showId, totalTickets);

        logger.info("Show inventory updated. showId={}, remaining={}", showId, totalTickets);

        return ResponseEntity.ok(
                Map.of("showId", showId, "remaining", remaining.get(showId))
        );
    }

    @GetMapping("/shows/{showId}")
    public ResponseEntity<?> getShow(@PathVariable String showId) {
        logger.info("GET /inventory/shows/{} called", showId);

        Integer qty = remaining.get(showId);
        if (qty == null) {
            logger.warn("Show not found. showId={}", showId);
            return ResponseEntity.notFound().build();
        }

        logger.info("Show found. showId={}, remaining={}", showId, qty);

        return ResponseEntity.ok(
                Map.of("showId", showId, "remaining", qty)
        );
    }

    @PostMapping("/shows/{showId}/hold")
    public ResponseEntity<?> hold(
            @PathVariable String showId,
            @RequestParam int qty) {

        logger.info("POST /inventory/shows/{}/hold called. qty={}", showId, qty);

        if (qty <= 0) {
            logger.warn("Invalid hold qty. showId={}, qty={}", showId, qty);
            return bad("qty must be > 0");
        }

        try {
            int after = remaining.compute(showId, (k, v) -> {
                if (v == null) throw new IllegalArgumentException("showId not found");
                if (v < qty) throw new IllegalStateException("insufficient tickets");
                return v - qty;
            });

            String holdId = UUID.randomUUID().toString();
            holds.put(holdId, new Hold(holdId, showId, qty, Instant.now().toString()));

            logger.info("Hold created. holdId={}, showId={}, heldQty={}, remaining={}",
                    holdId, showId, qty, after);

            return ResponseEntity.ok(Map.of(
                    "holdId", holdId,
                    "showId", showId,
                    "heldQty", qty,
                    "remaining", after
            ));

        } catch (Exception ex) {
            logger.warn("Hold failed. showId={}, qty={}, reason={}", showId, qty, ex.getMessage());
            return bad(ex.getMessage());
        }
    }

    @PostMapping("/holds/{holdId}/confirm")
    public ResponseEntity<?> confirm(@PathVariable String holdId) {

        logger.info("POST /inventory/holds/{}/confirm called", holdId);

        Hold hold = holds.remove(holdId);
        if (hold == null) {
            logger.warn("Confirm failed. Invalid holdId={}", holdId);
            return bad("invalid holdId");
        }

        logger.info("Hold confirmed. holdId={}, showId={}, qty={}",
                holdId, hold.showId(), hold.qty());

        return ResponseEntity.ok(Map.of(
                "status", "CONFIRMED",
                "holdId", holdId,
                "showId", hold.showId(),
                "qty", hold.qty()
        ));
    }

    @PostMapping("/holds/{holdId}/cancel")
    public ResponseEntity<?> cancel(@PathVariable String holdId) {

        logger.info("POST /inventory/holds/{}/cancel called", holdId);

        Hold hold = holds.remove(holdId);
        if (hold == null) {
            logger.warn("Cancel failed. Invalid holdId={}", holdId);
            return bad("invalid holdId");
        }

        int after = remaining.compute(
                hold.showId(),
                (k, v) -> (v == null ? 0 : v) + hold.qty()
        );

        logger.info("Hold cancelled. holdId={}, showId={}, releasedQty={}, remaining={}",
                holdId, hold.showId(), hold.qty(), after);

        return ResponseEntity.ok(Map.of(
                "status", "CANCELLED",
                "holdId", holdId,
                "showId", hold.showId(),
                "releasedQty", hold.qty(),
                "remaining", after
        ));
    }

    private ResponseEntity<Map<String, String>> bad(String msg) {
        logger.warn("Bad request: {}", msg);
        return ResponseEntity.badRequest().body(Map.of("error", msg));
    }

    private record Hold(String holdId, String showId, int qty, String createdAt) {}
}
