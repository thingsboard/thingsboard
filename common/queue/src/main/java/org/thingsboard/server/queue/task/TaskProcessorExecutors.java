// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.task;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.common.util.ThingsBoardThreadFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Getter
@Lazy
@Component
public class TaskProcessorExecutors {

    private ExecutorService consumersExecutor;
    private ExecutorService mgmtExecutor;
    private ScheduledExecutorService scheduler;

    @PostConstruct
    private void init() {
        consumersExecutor = Executors.newCachedThreadPool(ThingsBoardThreadFactory.forName("task-consumer"));
        mgmtExecutor = ThingsBoardExecutors.newWorkStealingPool(4, "task-consumer-mgmt");
        scheduler = ThingsBoardExecutors.newSingleThreadScheduledExecutor("task-consumer-scheduler");
    }

    @PreDestroy
    private void destroy() {
        if (consumersExecutor != null) {
            consumersExecutor.shutdownNow();
        }
        if (mgmtExecutor != null) {
            mgmtExecutor.shutdownNow();
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

}
