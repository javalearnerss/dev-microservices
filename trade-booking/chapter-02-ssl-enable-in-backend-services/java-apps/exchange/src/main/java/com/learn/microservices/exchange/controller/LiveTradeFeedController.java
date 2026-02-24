package com.learn.microservices.exchange.controller;

import com.learn.microservices.exchange.service.LiveTradeFeedSimulator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("exchange")
public class LiveTradeFeedController {

    @Autowired
    private LiveTradeFeedSimulator liveTradeFeedSimulator;

    /**
     * Starts the live trade feed in the background (non-blocking).
     * interval must be between 50 and 60000 ms.
     */
    @PostMapping("feed/on")
    public ResponseEntity<String> start(@RequestParam long interval) {

        if (interval < 50 || interval > 60000) {
            return ResponseEntity.badRequest().body("Interval must be between 50 ms and 60000 ms");
        }

        // Start is safe to call multiple times; service will handle "already ON"
        String result = liveTradeFeedSimulator.start(interval);
        return ResponseEntity.ok(result);
    }

    /**
     * Stops the live trade feed.
     * Returns immediately; service flips flag and cancels the running future.
     */
    @PostMapping("feed/off")
    public ResponseEntity<String> stop() {
        String result = liveTradeFeedSimulator.stop();
        return ResponseEntity.ok(result);
    }

    /**
     * Returns simple ON/OFF status.
     */
    @GetMapping("feed/status")
    public ResponseEntity<String> feedStatus() {
        return ResponseEntity.ok(liveTradeFeedSimulator.isLiveFeedOn() ? "ON" : "OFF");
    }

    /**
     * Returns dynamic feed counters (Map<String, Integer>).
     * Example: { "Feed=1": 120, "Feed=2": 45 }
     */
    @GetMapping("feed/details")
    public ResponseEntity<Map<String, Integer>> feedDetails() {
        return ResponseEntity.ok(liveTradeFeedSimulator.getTradeFeedInfo());
    }
}
