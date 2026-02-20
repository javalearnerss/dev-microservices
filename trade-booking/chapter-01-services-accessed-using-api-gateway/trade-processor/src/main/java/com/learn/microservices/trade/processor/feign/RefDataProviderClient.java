package com.learn.microservices.trade.processor.feign;


import com.learn.microservices.trade.processor.model.TradeRefData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "refdata-provider-client", url = "${refdata-provider.url}")
public interface RefDataProviderClient {

    @GetMapping("/reference/symbol/{symbol}")
    public TradeRefData getIsin(@PathVariable("symbol") String symbol);

    @GetMapping("/reference/symbol")
    public Map<String, List<TradeRefData>> getAllIsins(@RequestParam("symbols") String symbols);
}
