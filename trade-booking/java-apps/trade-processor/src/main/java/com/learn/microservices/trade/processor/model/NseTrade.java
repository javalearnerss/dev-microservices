package com.learn.microservices.trade.processor.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "broker_trades")
public class NseTrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String exchange;
    private LocalDate tradeDate;
    private LocalTime tradeTime;
    private String symbol;

    private String tradeId;
    private String orderId;
    private String exchangeOrderId;

    private String buySell;
    private int quantity;
    private BigDecimal price;
    private BigDecimal tradeValue;

    private String clientCode;
    private String brokerCode;
    private String moneyManagerCode;
    private String moneyManagerName;
    private String isin;

    private LocalDateTime timestampExchange;

    // Constructors
    public NseTrade() {
    }

    public NseTrade(String exchange, LocalDate tradeDate, LocalTime tradeTime, String symbol, String tradeId, String orderId, String exchangeOrderId, String buySell, int quantity, BigDecimal price, BigDecimal tradeValue, String clientCode, String brokerCode, String moneyManagerCode, String moneyManagerName, String isin, LocalDateTime timestampExchange) {
        this.exchange = exchange;
        this.tradeDate = tradeDate;
        this.tradeTime = tradeTime;
        this.symbol = symbol;
        this.tradeId = tradeId;
        this.orderId = orderId;
        this.exchangeOrderId = exchangeOrderId;
        this.buySell = buySell;
        this.quantity = quantity;
        this.price = price;
        this.tradeValue = tradeValue;
        this.clientCode = clientCode;
        this.brokerCode = brokerCode;
        this.moneyManagerCode = moneyManagerCode;
        this.moneyManagerName = moneyManagerName;
        this.isin = isin;
        this.timestampExchange = timestampExchange;
    }

    // Getters and Setters
    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public LocalDate getTradeDate() {
        return tradeDate;
    }

    public void setTradeDate(LocalDate tradeDate) {
        this.tradeDate = tradeDate;
    }

    public LocalTime getTradeTime() {
        return tradeTime;
    }

    public void setTradeTime(LocalTime tradeTime) {
        this.tradeTime = tradeTime;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getTradeId() {
        return tradeId;
    }

    public void setTradeId(String tradeId) {
        this.tradeId = tradeId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getExchangeOrderId() {
        return exchangeOrderId;
    }

    public void setExchangeOrderId(String exchangeOrderId) {
        this.exchangeOrderId = exchangeOrderId;
    }

    public String getBuySell() {
        return buySell;
    }

    public void setBuySell(String buySell) {
        this.buySell = buySell;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getTradeValue() {
        return tradeValue;
    }

    public void setTradeValue(BigDecimal tradeValue) {
        this.tradeValue = tradeValue;
    }

    public String getClientCode() {
        return clientCode;
    }

    public void setClientCode(String clientCode) {
        this.clientCode = clientCode;
    }

    public String getBrokerCode() {
        return brokerCode;
    }

    public void setBrokerCode(String brokerCode) {
        this.brokerCode = brokerCode;
    }

    public LocalDateTime getTimestampExchange() {
        return timestampExchange;
    }

    public void setTimestampExchange(LocalDateTime timestampExchange) {
        this.timestampExchange = timestampExchange;
    }

    public String getMoneyManagerCode() {
        return moneyManagerCode;
    }

    public void setMoneyManagerCode(String moneyManagerCode) {
        this.moneyManagerCode = moneyManagerCode;
    }

    public void setMoneyManagerName(String moneyManagerName) {
        this.moneyManagerName = moneyManagerName;
    }

    public String getMoneyManagerName() {
        return moneyManagerName;
    }

    public String getIsin() {
        return isin;
    }

    public void setIsin(String isin) {
        this.isin = isin;
    }
}
