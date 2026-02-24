package com.learn.microservices.trade.processor.service;

import com.learn.microservices.trade.processor.model.NseTrade;
import com.learn.microservices.trade.processor.repository.NseTradeRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class NseTradeStoreService {

    private final NseTradeRepository tradeRepository;

    public NseTradeStoreService(NseTradeRepository tradeRepository) {
        this.tradeRepository = tradeRepository;
    }

    @Transactional
    public void save(NseTrade trade) {
        if (tradeRepository.existsByTradeId(trade.getTradeId())) {
            return;
        }
        tradeRepository.save(trade);
    }

    public NseTrade getTrade(String tradeId){
        Optional<NseTrade> trade = tradeRepository.findByTradeId(tradeId);
        if(trade.isPresent())
            return  trade.get();
        return null;
    }
}

