// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.edqs;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.common.util.ThingsBoardThreadFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Lazy
@Component
@Getter
@RequiredArgsConstructor
public class EdqsExecutors {

    private final EdqsConfig edqsConfig;

    private ExecutorService consumersExecutor;
    private ExecutorService consumerTaskExecutor;
    private ScheduledExecutorService scheduler;
    private ListeningExecutorService requestExecutor;

    @PostConstruct
    private void init() {
        consumersExecutor = Executors.newCachedThreadPool(ThingsBoardThreadFactory.forName("edqs-consumer"));
        consumerTaskExecutor = ThingsBoardExecutors.newWorkStealingPool(4, "edqs-consumer-task-executor");
        scheduler = ThingsBoardExecutors.newSingleThreadScheduledExecutor("edqs-scheduler");
        requestExecutor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(edqsConfig.getRequestExecutorSize(), "edqs-requests"));
    }

    @PreDestroy
    private void destroy() {
        if (consumersExecutor != null) {
            consumersExecutor.shutdownNow();
        }
        if (consumerTaskExecutor != null) {
            consumerTaskExecutor.shutdownNow();
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        if (requestExecutor != null) {
            requestExecutor.shutdownNow();
        }
    }

}
