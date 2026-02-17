package com.learn.microservices.trade.enricher.service;

import com.learn.microservices.trade.enricher.exception.DelayException;
import com.learn.microservices.trade.enricher.model.NseTrade;
import com.learn.microservices.trade.enricher.utils.MoneyManagerCodesUtil;
import com.learn.microservices.trade.enricher.utils.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class TradeEnricherProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TradeEnricherProcessor.class);

    public NseTrade enrich(NseTrade trade) throws DelayException {
        String mmName = MoneyManagerCodesUtil.getName(trade.getMoneyManagerCode());
        if(Objects.nonNull(mmName)){
            LOGGER.info("Trade enriched, money manager code={} name={}", trade.getMoneyManagerCode(), mmName);
            trade.setMoneyManagerName(mmName);
            TimeUtil.sleep(100);
        }
        else{
            LOGGER.info("Trade cannot be enriched, money manager code not found - code={}", trade.getMoneyManagerCode());
            throw new DelayException("Money manager code is missing in cache");
        }
        return trade;
    }
}
