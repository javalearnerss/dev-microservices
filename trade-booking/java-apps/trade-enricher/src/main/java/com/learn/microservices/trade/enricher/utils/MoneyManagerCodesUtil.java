package com.learn.microservices.trade.enricher.utils;

import java.util.Map;

public final class MoneyManagerCodesUtil {

    private MoneyManagerCodesUtil() {
    }

    public static final Map<String, String> CODE_TO_NAME = Map.ofEntries(
            Map.entry("MMC001", "Crypto Alpha Fund"),
            Map.entry("MMC002", "BTC Momentum Desk"),
            Map.entry("MMC003", "ETH Strategy Pool"),
            Map.entry("MMC004", "Breakout Strategy Capital"),
            Map.entry("MMC005", "Mean Reversion Pool"),
            Map.entry("MMC006", "High Frequency Desk"),
            Map.entry("MMC007", "Options Hedging Book"),
            Map.entry("MMC008", "Arbitrage Capital Pool"),
            Map.entry("MMC009", "Low Risk Treasury Book"),
            Map.entry("MMC010", "Quant Research Allocation"),
            Map.entry("MMC011", "External Investor Pool A"),
            Map.entry("MMC012", "Prop Trading Core Book"),
            Map.entry("MMC013", "Asia Session Trading Book"),
            Map.entry("MMC014", "High Leverage Tactical Book"),
            Map.entry("MMC015", "Long Term Holdings Fund")
    );

    public static String getName(String code) {
        return CODE_TO_NAME.get(code);
    }
}
