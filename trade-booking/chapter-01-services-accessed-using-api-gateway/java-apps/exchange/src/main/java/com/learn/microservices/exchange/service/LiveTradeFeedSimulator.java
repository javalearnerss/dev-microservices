package com.learn.microservices.exchange.service;

import com.learn.microservices.exchange.kafka.GenericKafkaProducer;
import com.learn.microservices.exchange.model.NseTrade;
import com.learn.microservices.exchange.utils.NseTradeGenerator;
import com.learn.microservices.exchange.utils.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class LiveTradeFeedSimulator {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiveTradeFeedSimulator.class);


    private final AtomicBoolean liveFeedOn = new AtomicBoolean(false);

    // feedName -> trade count
    private final Map<String, Integer> tradeFeedInfo = new ConcurrentHashMap<>();


    @Value("${exchange.msg.producer.topic}")
    private String exchangeTopic;

    @Autowired
    private GenericKafkaProducer kafkaProducer;

    private final ObjectMapper mapper = new ObjectMapper();

    private CompletableFuture<Void> runningTask;

    public boolean isLiveFeedOn() {
        return liveFeedOn.get();
    }

    public Map<String, Integer> getTradeFeedInfo() {
        return tradeFeedInfo;
    }

    public synchronized String start(long intervalMs) {
        if (liveFeedOn.get())
            return "Live trade feed is already ON";

        liveFeedOn.set(true);
        String feedName = LocalDate.now().toString();
        tradeFeedInfo.putIfAbsent(LocalDate.now().toString(), 0);

        runningTask = CompletableFuture.runAsync(() -> {
            LOGGER.info("Feed started : {}", feedName);
            while (liveFeedOn.get()) {
                try {
                    NseTrade trade = NseTradeGenerator.generateTrade();
                    kafkaProducer.send(exchangeTopic, mapper.writeValueAsString(trade));
                    tradeFeedInfo.compute(feedName, (k, v) -> v == null ? 1 : v + 1);
                    TimeUtil.sleep(intervalMs);
                } catch (Exception e) {
                    LOGGER.error("Error generating or sending trade", e);
                }
            }
            LOGGER.info("Feed stopped : {}", feedName);
        });
        return "Live trade feed is ON";
    }

    public synchronized String stop() {
        if (!liveFeedOn.get())
            return "Live trade feed is already OFF";

        liveFeedOn.set(false);
        if (runningTask != null) {
            runningTask.cancel(true);
        }
        return "Live trade feed is OFF";
    }
}
