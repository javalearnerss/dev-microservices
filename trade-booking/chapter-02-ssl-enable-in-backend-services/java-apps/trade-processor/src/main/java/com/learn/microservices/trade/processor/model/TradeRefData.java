package com.learn.microservices.trade.processor.model;

public class TradeRefData {
    private String symbol;
    private String isin;
    private String status;


    public TradeRefData() {
    }

    public TradeRefData(String symbol, String isin, String status) {
        this.symbol = symbol;
        this.isin = isin;
        this.status = status;
    }


    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getIsin() {
        return isin;
    }

    public void setIsin(String isin) {
        this.isin = isin;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
