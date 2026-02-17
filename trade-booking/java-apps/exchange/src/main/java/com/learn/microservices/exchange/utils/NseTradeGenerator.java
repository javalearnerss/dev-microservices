package com.learn.microservices.exchange.utils;

import com.learn.microservices.exchange.model.NseTrade;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Random;
import java.util.UUID;

public class NseTradeGenerator {

    private static final String[] SYMBOLS = {
            "RELIANCE", "TCS", "INFY", "HDFCBANK", "ICICIBANK", "SBIN", "LT",
            "HINDUNILVR", "ITC", "BAJFINANCE", "KOTAKBANK", "BHARTIARTL",
            "ASIANPAINT", "MARUTI", "AXISBANK", "SUNPHARMA", "TITAN"
    };

    private static final String[] MMC_CODES = {
            "MMC001", "MMC002", "MMC003", "MMC004", "MMC005",
            "MMC006", "MMC007", "MMC008", "MMC009", "MMC010",
            "MMC011", "MMC012", "MMC013", "MMC014", "MMC015"
    };
    private static final Random RANDOM = new Random();

    public static NseTrade generateTrade() {

        String symbol = SYMBOLS[RANDOM.nextInt(SYMBOLS.length)];

        int quantity = (RANDOM.nextInt(10) + 1) * 50;

        BigDecimal price = BigDecimal.valueOf(1000 + RANDOM.nextDouble() * 3000)
                .setScale(2, BigDecimal.ROUND_HALF_UP);

        BigDecimal tradeValue = price.multiply(BigDecimal.valueOf(quantity));

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));

        NseTrade trade = new NseTrade();
        trade.setExchange("NSE");
        trade.setTradeDate(now.toLocalDate());
        trade.setTradeTime(now.toLocalTime());
        trade.setSymbol(symbol);

        trade.setMoneyManagerCode(
                MMC_CODES[RANDOM.nextInt(MMC_CODES.length)]
        );
        trade.setTradeId("NSE" + Math.abs(UUID.randomUUID().getMostSignificantBits()));
        trade.setOrderId(String.valueOf(System.nanoTime()));
        trade.setExchangeOrderId(String.valueOf(System.currentTimeMillis()));

        trade.setBuySell(RANDOM.nextBoolean() ? "BUY" : "SELL");
        trade.setQuantity(quantity);
        trade.setPrice(price);
        trade.setTradeValue(tradeValue);

        trade.setClientCode("UCC" + (10000 + RANDOM.nextInt(90000)));
        trade.setBrokerCode("INZ000123456");

        trade.setTimestampExchange(now);

        return trade;
    }


}
