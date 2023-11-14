package org.thingsboard.server.service.executors;

import com.google.api.gax.core.FixedExecutorProvider;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.PubSubExecutor;
import org.thingsboard.common.util.ThingsBoardThreadFactory;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Component
public class PubSubExecutorService implements PubSubExecutor {
    private static final int THREADS_PER_CPU = 5;
    private FixedExecutorProvider fixedExecutorProvider;

    @PostConstruct
    public void init() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(THREADS_PER_CPU * Runtime.getRuntime().availableProcessors(), ThingsBoardThreadFactory.forName("tb-pubsub-producer-scheduler"));;
        this.fixedExecutorProvider = FixedExecutorProvider.create(scheduler);
    }

    public FixedExecutorProvider getExecutorProvider() {
        return fixedExecutorProvider;
    }
}
