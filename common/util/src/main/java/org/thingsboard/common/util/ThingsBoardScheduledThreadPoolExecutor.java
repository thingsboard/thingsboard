// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.common.util;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@Slf4j
final class ThingsBoardScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor {

    public ThingsBoardScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory) {
        super(corePoolSize, threadFactory);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        if (command == null) { // preserve the original NPE behavior of ScheduledThreadPoolExecutor with a more helpful message
            throw new NullPointerException("Command is null");
        }
        return super.scheduleAtFixedRate(new PeriodicRunnable(command), initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        if (command == null) { // preserve the original NPE behavior of ScheduledThreadPoolExecutor with a more helpful message
            throw new NullPointerException("Command is null");
        }
        return super.scheduleWithFixedDelay(new PeriodicRunnable(command), initialDelay, delay, unit);
    }

    private record PeriodicRunnable(Runnable runnable) implements Runnable {

        public void run() {
            try {
                runnable.run();
            } catch (Exception e) {
                // Log exceptions but do not propagate it. This ensures that subsequent scheduled tasks will still run.
                log.error("Uncaught exception occurred during periodic task execution!", e);
            } catch (Throwable th) {
                // Log and rethrow other serious issues that are not regular Exceptions.
                log.error("Critical exception occurred during periodic task execution!", th);
                throw th;
            }
        }

    }

}
