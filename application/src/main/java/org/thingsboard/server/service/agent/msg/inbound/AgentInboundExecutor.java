// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.msg.inbound;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Runs inbound agent messages off the gRPC callback thread while preserving per-agent ordering.
 * <p>
 * Work is striped over single-threaded executors keyed by agent id, so every message of one agent runs on the
 * same thread in arrival order - the guarantee the event state machine previously inherited from gRPC's
 * per-stream callback serialization. Each stripe has a bounded queue and rejects when full: the caller is a
 * transport thread that must not block, and a refused message is recoverable (the watchdog re-drives an event,
 * metrics and compose sync self-heal on the next tick).
 */
@Slf4j
@TbCoreComponent
@Component
public class AgentInboundExecutor {

    @Value("${agents.inbound.stripes:8}")
    private int stripes;
    @Value("${agents.inbound.queueSizePerStripe:200}")
    private int queueSizePerStripe;

    private List<ExecutorService> executors;

    @PostConstruct
    public void init() {
        executors = new ArrayList<>(stripes);
        for (int i = 0; i < stripes; i++) {
            ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 60L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(queueSizePerStripe),
                    ThingsBoardThreadFactory.forName("agent-inbound-" + i));
            executor.allowCoreThreadTimeOut(true);
            executors.add(executor);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (executors != null) {
            executors.forEach(ExecutorService::shutdownNow);
        }
    }

    /**
     * @return true when the task was accepted; false when the agent's stripe is saturated. A false return must
     * not fail the gRPC stream - closing it would turn transient overload into a cluster-wide reconnect storm.
     */
    public boolean submit(AgentId agentId, Runnable task) {
        try {
            executorFor(agentId).execute(task);
            return true;
        } catch (RejectedExecutionException e) {
            log.warn("[{}] Inbound queue is full, dropping message", agentId);
            return false;
        }
    }

    private ExecutorService executorFor(AgentId agentId) {
        int index = Math.floorMod(agentId.getId().hashCode(), executors.size());
        return executors.get(index);
    }
}
