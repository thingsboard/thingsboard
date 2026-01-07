/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
