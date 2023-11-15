package org.thingsboard.server.service.executors;

import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ExecutorProvider;
import org.thingsboard.common.util.ThingsBoardThreadFactory;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Component
public class PubSubRuleNodeExecutorProvider implements ExecutorProvider {

    /**
    * Refers to com.google.cloud.pubsub.v1.Publisher default executor configuration
    */
    private static final int THREADS_PER_CPU = 5;
    private ScheduledExecutorService executor;

    @PostConstruct
    public void init() {
        executor = Executors.newScheduledThreadPool(THREADS_PER_CPU * Runtime.getRuntime().availableProcessors(), ThingsBoardThreadFactory.forName("pubsub-rule-nodes"));;
    }

    @Override
    public ScheduledExecutorService getExecutor() {
        return executor;
    }
}
