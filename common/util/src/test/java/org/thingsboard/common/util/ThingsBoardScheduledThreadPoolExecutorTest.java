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
package org.thingsboard.common.util;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ThingsBoardScheduledThreadPoolExecutorTest {

    ThingsBoardScheduledThreadPoolExecutor scheduler;

    @BeforeEach
    void setup() {
        scheduler = new ThingsBoardScheduledThreadPoolExecutor(1, Executors.defaultThreadFactory());
    }

    @AfterEach
    void cleanup() {
        scheduler.shutdownNow();
    }

    @Test
    @DisplayName("scheduleAtFixedRate() should continue periodic execution even if command throws exception")
    void scheduleAtFixedRateShouldNotStopPeriodicExecutionWhenCommandThrowsException() {
        // GIVEN
        AtomicInteger executionCounter = new AtomicInteger(0);

        Runnable exceptionThrowingCommand = () -> {
            try {
                throw new RuntimeException("Unexpected exception");
            } finally {
                executionCounter.incrementAndGet();
            }
        };

        // WHEN
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(exceptionThrowingCommand, 0, 100, TimeUnit.MILLISECONDS);

        // THEN
        Awaitility.await().alias("Wait until command is executed at least twice")
                .atMost(10, TimeUnit.SECONDS)
                .failFast("Future should not be done or cancelled; task should continue running", () -> future.isDone() || future.isCancelled())
                .untilAsserted(() -> assertThat(executionCounter.get())
                        .as("Task should be executed at least twice")
                        .isGreaterThan(2));
    }

    @Test
    @DisplayName("scheduleAtFixedRate() should stop periodic execution if command throws an error")
    void scheduleAtFixedRateShouldStopPeriodicExecutionWhenCommandThrowsException() {
        // GIVEN
        AtomicInteger executionCounter = new AtomicInteger(0);

        Runnable exceptionThrowingCommand = () -> {
            try {
                throw new Error("Unexpected error");
            } finally {
                executionCounter.incrementAndGet();
            }
        };

        // WHEN
        scheduler.scheduleAtFixedRate(exceptionThrowingCommand, 0, 100, TimeUnit.MILLISECONDS);

        // THEN
        Awaitility.await().alias("Command that throws an error should execute exactly once")
                .pollDelay(5, TimeUnit.SECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .failFast("Command that throws an error should not execute more than once", () -> executionCounter.get() > 1)
                .until(() -> executionCounter.get() == 1);
    }

    @Test
    @DisplayName("scheduleWithFixedDelay() should continue periodic execution even if command throws exception")
    void scheduleWithFixedDelayShouldNotStopPeriodicExecutionWhenCommandThrowsException() {
        // GIVEN
        AtomicInteger executionCounter = new AtomicInteger(0);

        Runnable exceptionThrowingCommand = () -> {
            try {
                throw new RuntimeException("Unexpected exception");
            } finally {
                executionCounter.incrementAndGet();
            }
        };

        // WHEN
        ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(exceptionThrowingCommand, 0, 100, TimeUnit.MILLISECONDS);

        // THEN
        Awaitility.await().alias("Wait until command is executed at least twice")
                .atMost(10, TimeUnit.SECONDS)
                .failFast("Future should not be done or cancelled; task should continue running", () -> future.isDone() || future.isCancelled())
                .untilAsserted(() -> assertThat(executionCounter.get())
                        .as("Task should be executed at least twice")
                        .isGreaterThan(2));
    }

    @Test
    @DisplayName("scheduleWithFixedDelay() should stop periodic execution if command throws an error")
    void scheduleWithFixedDelayShouldStopPeriodicExecutionWhenCommandThrowsException() {
        // GIVEN
        AtomicInteger executionCounter = new AtomicInteger(0);

        Runnable exceptionThrowingCommand = () -> {
            try {
                throw new Error("Unexpected error");
            } finally {
                executionCounter.incrementAndGet();
            }
        };

        // WHEN
        scheduler.scheduleWithFixedDelay(exceptionThrowingCommand, 0, 100, TimeUnit.MILLISECONDS);

        // THEN
        Awaitility.await().alias("Command that throws an error should execute exactly once")
                .pollDelay(5, TimeUnit.SECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .failFast("Command that throws an error should not execute more than once", () -> executionCounter.get() > 1)
                .until(() -> executionCounter.get() == 1);
    }

}
