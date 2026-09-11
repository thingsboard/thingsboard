// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.scheduler;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardExecutors;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class DefaultSchedulerComponent implements SchedulerComponent {

    private ScheduledExecutorService schedulerExecutor;

    @PostConstruct
    public void init() {
        schedulerExecutor = ThingsBoardExecutors.newSingleThreadScheduledExecutor("queue-scheduler");
    }

    @PreDestroy
    public void destroy() {
        if (schedulerExecutor != null) {
            schedulerExecutor.shutdownNow();
        }
    }

    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return schedulerExecutor.schedule(command, delay, unit);
    }

    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return schedulerExecutor.schedule(callable, delay, unit);
    }

    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return schedulerExecutor.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return schedulerExecutor.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

}
