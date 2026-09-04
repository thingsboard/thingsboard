// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.snmp.session;

import com.google.common.util.concurrent.AsyncCallable;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Data
@Slf4j
public class ScheduledTask {
    private ListenableFuture<?> scheduledFuture;
    private boolean stopped = false;

    public void init(AsyncCallable<Void> task, long delayMs, ScheduledExecutorService scheduler) {
        schedule(task, delayMs, scheduler);
    }

    private void schedule(AsyncCallable<Void> task, long delayMs, ScheduledExecutorService scheduler) {
        scheduledFuture = Futures.scheduleAsync(() -> {
            if (stopped) {
                return Futures.immediateCancelledFuture();
            }
            try {
                return task.call();
            } catch (Throwable t) {
                log.error("Unhandled error in scheduled task", t);
                return Futures.immediateFailedFuture(t);
            }
        }, delayMs, TimeUnit.MILLISECONDS, scheduler);
        if (!stopped) {
            scheduledFuture.addListener(() -> schedule(task, delayMs, scheduler), MoreExecutors.directExecutor());
        }
    }

    public void cancel() {
        stopped = true;
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }
    }

}
