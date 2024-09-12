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
package org.thingsboard.server.queue.scheduler;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultSchedulerComponentTest {

    DefaultSchedulerComponent schedulerComponent;

    @BeforeEach
    void setup() {
        schedulerComponent = new DefaultSchedulerComponent();
        schedulerComponent.init();
    }

    @AfterEach
    void cleanup() {
        schedulerComponent.destroy();
    }

    @Test
    @DisplayName("scheduleAtFixedRate() should continue periodic execution even if command throws exception")
    void scheduleAtFixedRateShouldNotStopPeriodicExecutionWhenCommandThrowsException() {
        // GIVEN
        var wasExecutedAtLeastOnce = new AtomicBoolean(false);

        Runnable exceptionThrowingCommand = () -> {
            try {
                throw new RuntimeException("Unexpected exception");
            } finally {
                wasExecutedAtLeastOnce.set(true);
            }
        };

        // WHEN
        ScheduledFuture<?> future = schedulerComponent.scheduleAtFixedRate(exceptionThrowingCommand, 0, 200, TimeUnit.MILLISECONDS);

        // THEN
        Awaitility.await().alias("Wait until command is executed at least once")
                .atMost(5, TimeUnit.SECONDS)
                .until(wasExecutedAtLeastOnce::get);

        assertThat(future.isDone()).as("Periodic execution should not stop after unhandled exception is thrown by the command").isFalse();
    }

    @Test
    @DisplayName("scheduleWithFixedDelay() should continue periodic execution even if command throws exception")
    void scheduleWithFixedDelayShouldNotStopPeriodicExecutionWhenCommandThrowsException() {
        // GIVEN
        var wasExecutedAtLeastOnce = new AtomicBoolean(false);

        Runnable exceptionThrowingCommand = () -> {
            try {
                throw new RuntimeException("Unexpected exception");
            } finally {
                wasExecutedAtLeastOnce.set(true);
            }
        };

        // WHEN
        ScheduledFuture<?> future = schedulerComponent.scheduleWithFixedDelay(exceptionThrowingCommand, 0, 200, TimeUnit.MILLISECONDS);

        // THEN
        Awaitility.await().alias("Wait until command is executed at least once")
                .atMost(5, TimeUnit.SECONDS)
                .until(wasExecutedAtLeastOnce::get);

        assertThat(future.isDone()).as("Periodic execution should not stop after unhandled exception is thrown by the command").isFalse();
    }

}
