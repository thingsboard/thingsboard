/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.common.util;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        logExceptionsAfterExecute(r, t);
    }

    private static void logExceptionsAfterExecute(Runnable r, Throwable directThrowable) {
        Throwable wrappedThrowable = extractThrowableWrappedInFuture(r);
        if (wrappedThrowable != null) {
            if (wrappedThrowable instanceof CancellationException) {
                log.debug("Task was cancelled.", wrappedThrowable);
            } else {
                log.error("Uncaught error occurred during task execution!", wrappedThrowable);
            }
        }

        if (directThrowable != null) {
            log.error("Uncaught error occurred during task execution!", directThrowable);
        }
    }

    private static Throwable extractThrowableWrappedInFuture(Runnable runnable) {
        if (runnable instanceof Future<?> future && future.isDone()) {
            try {
                future.get();
            } catch (InterruptedException e) { // should not happen due to isDone() check
                throw new AssertionError("InterruptedException caught after isDone() check on a future", e);
            } catch (CancellationException e) {
                return e;
            } catch (ExecutionException e) {
                return e.getCause();
            }
        }
        return null;
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        if (command == null) { // preserve the original NPE behavior of ScheduledThreadPoolExecutor with a more helpful message
            throw new NullPointerException("command is null");
        }
        return super.scheduleAtFixedRate(new SafePeriodicRunnable(command), initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        if (command == null) { // preserve the original NPE behavior of ScheduledThreadPoolExecutor with a more helpful message
            throw new NullPointerException("command is null");
        }
        return super.scheduleWithFixedDelay(new SafePeriodicRunnable(command), initialDelay, delay, unit);
    }

    private record SafePeriodicRunnable(Runnable runnable) implements Runnable {

        public void run() {
            try {
                runnable.run();
            } catch (Exception ex) {
                log.error("Uncaught exception occurred during periodic task execution!", ex);
            }
            // Intentionally, no catch block for Throwable; uncaught Throwables will be handled in afterExecute()
        }

    }

}
