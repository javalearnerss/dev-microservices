package com.learn.microservices.trade.processor.repository;

import com.learn.microservices.trade.processor.model.NseTrade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NseTradeRepository extends JpaRepository<NseTrade, Long> {

    public boolean existsByTradeId(String tradeId);

    public Optional<NseTrade> findByTradeId(String tradeId);
}
