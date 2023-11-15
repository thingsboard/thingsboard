package org.thingsboard.server.queue.pubsub;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ExecutorProvider;
import org.thingsboard.common.util.ThingsBoardThreadFactory;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@ConditionalOnExpression("'${queue.type:null}'=='pubsub'")
@Component
public class TbPubSubQueueExecutorProvider implements ExecutorProvider {

    @Value("${queue.pubsub.executor_thread_pool_size}")
    private Integer threadPoolSize;
    /**
     * Refers to com.google.cloud.pubsub.v1.Publisher default executor configuration
     */
    private static final int THREADS_PER_CPU = 5;
    private ScheduledExecutorService executor;

    @PostConstruct
    public void init() {
        if (threadPoolSize == null) {
            threadPoolSize = THREADS_PER_CPU * Runtime.getRuntime().availableProcessors();
        }
        executor = Executors.newScheduledThreadPool(threadPoolSize, ThingsBoardThreadFactory.forName("pubsub-queue-executor"));;
    }

    @Override
    public ScheduledExecutorService getExecutor() {
        return executor;
    }
}
