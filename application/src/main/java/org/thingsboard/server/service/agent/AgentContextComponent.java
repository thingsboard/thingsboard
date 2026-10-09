// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.agent.event.AgentEventProcessor;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Lazy
@Getter
@Component
@TbCoreComponent
public class AgentContextComponent {

    @Autowired
    private AgentEventProcessor agentEventProcessor;


    @Value("${agents.event.executor_pool_size:8}")
    private int executorPoolSize;

    @Value("${agents.event.queue_capacity:10000}")
    private int queueCapacity;

    @Value("${agents.event.reconnect_resume_max_delay_ms:5000}")
    private long reconnectResumeMaxDelayMs;

    @Autowired
    private ObjectProvider<AgentRpcService> agentRpcServiceProvider;
    private ListeningExecutorService agentEventExecutor;
    private ScheduledExecutorService reconnectResumeScheduler;

    /** Null when {@code agents.enabled} is false, so callers must tolerate its absence. */
    public AgentRpcService getAgentRpcService() {
        return agentRpcServiceProvider.getIfAvailable();
    }

    @PostConstruct
    public void init() {
        this.agentEventExecutor = MoreExecutors.listeningDecorator(
                new ThreadPoolExecutor(executorPoolSize, executorPoolSize,
                        0L, TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<>(queueCapacity),
                        ThingsBoardThreadFactory.forName("agent-event-processor"),
                        new ThreadPoolExecutor.AbortPolicy()));
        this.reconnectResumeScheduler = ThingsBoardExecutors.newSingleThreadScheduledExecutor("agent-reconnect-resume");
    }

    @PreDestroy
    public void destroy() {
        if (agentEventExecutor != null) {
            MoreExecutors.shutdownAndAwaitTermination(agentEventExecutor, 30, TimeUnit.SECONDS);
        }
        if (reconnectResumeScheduler != null) {
            reconnectResumeScheduler.shutdownNow();
        }
    }
}
