package com.learn.microservices.refdata.provider.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SymbolIsinCacheService {

    // Thread-safe cache
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {

       // cache.put("RELIANCE", "INE002A01018");
        cache.put("TCS", "INE467B01029");
        cache.put("INFY", "INE009A01021");
        cache.put("HDFCBANK", "INE040A01034");
        cache.put("ICICIBANK", "INE090A01021");
        cache.put("SBIN", "INE062A01020");
        cache.put("LT", "INE018A01030");

        cache.put("HINDUNILVR", "INE030A01027");
        cache.put("ITC", "INE154A01025");
        cache.put("BAJFINANCE", "INE296A01024");
        cache.put("KOTAKBANK", "INE237A01028");
        cache.put("BHARTIARTL", "INE397D01024");
        cache.put("ASIANPAINT", "INE021A01026");
        cache.put("MARUTI", "INE585B01010");
        cache.put("AXISBANK", "INE238A01034");
        cache.put("SUNPHARMA", "INE044A01036");
        cache.put("TITAN", "INE280A01028");
    }

    public String getIsin(String symbol) {
        if (symbol == null) return null;
        return cache.get(symbol.trim().toUpperCase());
    }

}
