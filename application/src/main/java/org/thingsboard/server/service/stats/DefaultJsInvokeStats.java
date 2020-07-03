package org.thingsboard.server.service.stats;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.actors.JsInvokeStats;

import javax.annotation.PostConstruct;

@Service
public class DefaultJsInvokeStats implements JsInvokeStats {
    private static final String REQUESTS = "requests";
    private static final String RESPONSES = "responses";
    private static final String FAILURES = "failures";

    private StatsCounter requestsCounter;
    private StatsCounter responsesCounter;
    private StatsCounter failuresCounter;

    @Autowired
    private StatsCounterFactory counterFactory;

    @PostConstruct
    public void init() {
        String key = StatsType.JS_INVOKE.getName();
        this.requestsCounter = counterFactory.createStatsCounter(key, REQUESTS);
        this.responsesCounter = counterFactory.createStatsCounter(key, RESPONSES);
        this.failuresCounter = counterFactory.createStatsCounter(key, FAILURES);
    }

    @Override
    public void incrementRequests(int amount) {
        requestsCounter.add(amount);
    }

    @Override
    public void incrementResponses(int amount) {
        responsesCounter.add(amount);
    }

    @Override
    public void incrementFailures(int amount) {
        failuresCounter.add(amount);
    }

    @Override
    public int getRequests() {
        return requestsCounter.get();
    }

    @Override
    public int getResponses() {
        return responsesCounter.get();
    }

    @Override
    public int getFailures() {
        return failuresCounter.get();
    }

    @Override
    public void reset() {
        requestsCounter.clear();
        responsesCounter.clear();
        failuresCounter.clear();
    }
}
