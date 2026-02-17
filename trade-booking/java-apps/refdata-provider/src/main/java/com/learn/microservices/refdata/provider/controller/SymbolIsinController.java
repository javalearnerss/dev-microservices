package com.learn.microservices.refdata.provider.controller;

import com.learn.microservices.refdata.provider.metrics.AppMetrics;
import com.learn.microservices.refdata.provider.model.TradeRefData;
import com.learn.microservices.refdata.provider.service.SymbolIsinCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST Controller responsible for providing Symbol → ISIN reference data.
 *
 * Supported APIs:
 * 1. Single symbol lookup
 *      GET /reference/isin/{symbol}
 *
 * 2. Bulk symbol lookup (comma separated)
 *      GET /reference/isin?symbols=AAA,BBB,CCC
 *
 * Data Source:
 * Uses SymbolIsinCacheService which fetches data from in-memory cache.
 *
 * Response Model:
 * Uses TradeRefData DTO for standardized response structure.
 */
@RestController
@RequestMapping("/reference/isin")
public class SymbolIsinController {

    private static final Logger log = LoggerFactory.getLogger(SymbolIsinController.class);

    @Autowired
    private AppMetrics metrics;

    /**
     * Cache service used to fetch ISIN using symbol.
     * Typically backed by in-memory map / distributed cache.
     */
    private final SymbolIsinCacheService cacheService;

    /**
     * Constructor Injection (Recommended Spring Practice)
     */
    public SymbolIsinController(SymbolIsinCacheService cacheService) {
        this.cacheService = cacheService;
    }

    // =========================================================
    // SINGLE SYMBOL LOOKUP
    // =========================================================

    /**
     * Fetch ISIN for a single symbol.
     *
     * Example:
     * GET /reference/isin/TCS
     *
     * @param symbol Raw symbol from request path
     * @return TradeRefData containing symbol, isin and lookup status
     */
    @GetMapping("/{symbol}")
    public ResponseEntity<TradeRefData> getByPath(@PathVariable String symbol) {

        metrics.incReceived();
        long startNs = System.nanoTime();
        log.info("ISIN lookup requested (single). rawSymbol='{}'", symbol);

        // Normalize input → trim + uppercase
        String normalized = normalize(symbol);
        log.debug("ISIN lookup (single). normalizedSymbol='{}'", normalized);

        // Fetch ISIN from cache
        String isin = cacheService.getIsin(normalized);

        // If symbol not found → return NOT_FOUND status
        if (isin == null) {
            metrics.incMissing();
            long tookMs = (System.nanoTime() - startNs) / 1_000_000;
            log.warn("ISIN lookup result (single). symbol='{}' status=NOT_FOUND tookMs={}",
                    normalized, tookMs);
            metrics.incProcessed();
            return ResponseEntity.ok(new TradeRefData(normalized, null, "NOT_FOUND"));
        }

        metrics.incPresent();
        // If found → return FOUND status with ISIN
        long tookMs = (System.nanoTime() - startNs) / 1_000_000;
        log.info("ISIN lookup result (single). symbol='{}' status=FOUND isin='{}' tookMs={}",
                normalized, isin, tookMs);

        metrics.incProcessed();
        return ResponseEntity.ok(new TradeRefData(normalized, isin, "FOUND"));
    }

    // =========================================================
    // BULK SYMBOL LOOKUP
    // =========================================================

    /**
     * Fetch ISIN for multiple symbols (comma separated).
     *
     * Example:
     * GET /reference/isin?symbols=TCS,INFY,RELIANCE
     *
     * Features:
     * - Input normalization
     * - Duplicate symbol removal
     * - Order preservation
     * - Partial success handling
     *
     * @param symbolsInput Comma separated symbol string
     * @return JSON response containing count and list of TradeRefData
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getBulk(@RequestParam("symbols") String symbolsInput) {
        metrics.incReceived();
        long startNs = System.nanoTime();
        int rawLen = (symbolsInput == null) ? 0 : symbolsInput.length();
        log.info("ISIN lookup requested (bulk). rawLength={} rawInput='{}'", rawLen, symbolsInput);

        // Validate input
        if (symbolsInput == null || symbolsInput.isBlank()) {
            log.warn("ISIN lookup rejected (bulk). reason='symbols parameter is required'");
            return ResponseEntity.badRequest().body(Map.of("error", "symbols parameter is required"));
        }

        /**
         * LinkedHashSet used because:
         * 1. Removes duplicates
         * 2. Preserves original request order
         */
        Set<String> normalizedSymbols = new LinkedHashSet<>();

        // Split comma separated symbols and normalize each
        for (String sym : symbolsInput.split(",")) {
            String normalized = normalize(sym);
            if (!normalized.isEmpty()) {
                normalizedSymbols.add(normalized);
            }
        }

        log.debug("ISIN lookup (bulk). normalizedCount={} normalizedSymbols={}",
                normalizedSymbols.size(), normalizedSymbols);

        // Prepare response list
        List<TradeRefData> data = new ArrayList<>(normalizedSymbols.size());

        int found = 0, notFound = 0;

        // Fetch ISIN for each symbol
        for (String sym : normalizedSymbols) {
            String isin = cacheService.getIsin(sym);

            if (isin == null) {
                metrics.incMissing();
                notFound++;
                data.add(new TradeRefData(sym, null, "NOT_FOUND"));
                log.debug("ISIN lookup (bulk item). symbol='{}' status=NOT_FOUND", sym);
            } else {
                metrics.incPresent();
                found++;
                data.add(new TradeRefData(sym, isin, "FOUND"));
                log.debug("ISIN lookup (bulk item). symbol='{}' status=FOUND isin='{}'", sym, isin);
            }
        }

        long tookMs = (System.nanoTime() - startNs) / 1_000_000;
        log.info("ISIN lookup completed (bulk). uniqueSymbols={} found={} notFound={} tookMs={}",
                normalizedSymbols.size(), found, notFound, tookMs);

        metrics.incProcessed();;
        // Final response wrapper
        return ResponseEntity.ok(Map.of("count", data.size(), "data", data));
    }

    // =========================================================
    // INTERNAL UTIL METHOD
    // =========================================================

    /**
     * Normalizes symbol input:
     * - Null safe
     * - Trim spaces
     * - Convert to uppercase (locale safe)
     */
    private String normalize(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
    }

    @GetMapping("stats")
    public ResponseEntity<AppMetrics.Stats> getStats(){
        return ResponseEntity.ok(metrics.getStats());
    }
}
