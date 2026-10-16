package in.javalearners.pulsefeed.post.controller;

import in.javalearners.pulsefeed.post.util.FailureSimulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Management controller for testing resilience patterns.
 * Allows testing timeout, retry, circuit breaker, and bulkhead behaviors.
 */
@RestController
@RequestMapping("/api/management/failure-simulator")
public class FailureSimulatorController {

    private static final Logger log = LoggerFactory.getLogger(FailureSimulatorController.class);

    private final FailureSimulator failureSimulator;

    public FailureSimulatorController(FailureSimulator failureSimulator) {
        this.failureSimulator = failureSimulator;
    }

    /**
     * Starts simulating slowness.
     *
     * @param delayMs delay in milliseconds (default: 500)
     * @return confirmation message
     */
    @PostMapping("/slowness/start")
    public ResponseEntity<String> startSlowness(@RequestParam(value = "delayMs", defaultValue = "500") long delayMs) {
        failureSimulator.setDelayMillis(delayMs);
        failureSimulator.setSimulateSlowness(true);
        log.info("Started simulating slowness with delay: {} ms", delayMs);
        return ResponseEntity.ok("Slowness simulation started with delay: " + delayMs + " ms");
    }

    /**
     * Stops simulating slowness.
     *
     * @return confirmation message
     */
    @PostMapping("/slowness/stop")
    public ResponseEntity<String> stopSlowness() {
        failureSimulator.setSimulateSlowness(false);
        log.info("Stopped simulating slowness");
        return ResponseEntity.ok("Slowness simulation stopped");
    }

    /**
     * Starts simulating failures.
     *
     * @param failureCount number of failures to simulate (default: 5)
     * @return confirmation message
     */
    @PostMapping("/failure/start")
    public ResponseEntity<String> startFailure(@RequestParam(value = "count", defaultValue = "5") int failureCount) {
        failureSimulator.setSimulateFailure(true, failureCount);
        log.info("Started simulating failures - will fail {} times", failureCount);
        return ResponseEntity.ok("Failure simulation started - will fail " + failureCount + " times");
    }

    /**
     * Stops simulating failures.
     *
     * @return confirmation message
     */
    @PostMapping("/failure/stop")
    public ResponseEntity<String> stopFailure() {
        failureSimulator.setSimulateFailure(false, 0);
        log.info("Stopped simulating failures");
        return ResponseEntity.ok("Failure simulation stopped");
    }

    /**
     * Gets the current status of the failure simulator.
     *
     * @return current status
     */
    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Failure Simulator Status:\n");
        status.append("- Slowness Simulation: ").append(failureSimulator.isSimulatingSlowness()).append("\n");
        status.append("- Delay: ").append(failureSimulator.getDelayMillis()).append(" ms\n");
        status.append("- Failure Simulation: ").append(failureSimulator.isSimulatingFailure()).append("\n");
        status.append("- Total Requests Received: ").append(failureSimulator.getTotalRequestCount());
        
        log.info("Status requested: {}", status);
        return ResponseEntity.ok(status.toString());
    }

    /**
     * Resets the failure simulator.
     *
     * @return confirmation message
     */
    @PostMapping("/reset")
    public ResponseEntity<String> reset() {
        failureSimulator.reset();
        log.info("Failure simulator reset");
        return ResponseEntity.ok("Failure simulator has been reset");
    }
}
