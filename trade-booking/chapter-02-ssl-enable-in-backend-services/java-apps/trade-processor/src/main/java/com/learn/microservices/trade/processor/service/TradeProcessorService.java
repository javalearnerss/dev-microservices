package com.learn.microservices.trade.processor.service;


import com.learn.microservices.trade.processor.exception.InvalidTradeException;
import com.learn.microservices.trade.processor.feign.RefDataProviderClient;
import com.learn.microservices.trade.processor.model.NseTrade;
import com.learn.microservices.trade.processor.model.TradeRefData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class TradeProcessorService {

    @Autowired
    private RefDataProviderClient refDataClient;

    @Autowired
    private NseTradeStoreService tradeStoreService;

    public void process(NseTrade trade) throws InvalidTradeException {
        TradeRefData refData = refDataClient.getIsin(trade.getSymbol());
        if (Objects.nonNull(refData)) {
            if (Status.FOUND.toString().equals(refData.getStatus())) {
                trade.setIsin(refData.getIsin());
            } else{
                throw  new InvalidTradeException("Reference data is missing, symbol="+trade.getSymbol());
            }
        }
        tradeStoreService.save(trade);
    }


    private enum Status {
        FOUND,
        NOT_FOUND
    }
}
