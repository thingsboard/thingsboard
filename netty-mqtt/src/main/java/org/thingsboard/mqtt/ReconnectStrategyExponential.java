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
package org.thingsboard.mqtt;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Getter
@Slf4j
public class ReconnectStrategyExponential implements ReconnectStrategy {

    public static final int DEFAULT_RECONNECT_INTERVAL_SEC = 10;
    public static final int MAX_RECONNECT_INTERVAL_SEC = 60;
    public static final int EXP_MAX = 8;
    public static final long JITTER_MAX = 1;
    private final long reconnectIntervalMinSeconds;
    private final long reconnectIntervalMaxSeconds;
    private long lastDisconnectNanoTime = 0; //isotonic time
    private long retryCount = 0;

    public ReconnectStrategyExponential(long reconnectIntervalMinSeconds) {
        this.reconnectIntervalMaxSeconds = calculateIntervalMax(reconnectIntervalMinSeconds);
        this.reconnectIntervalMinSeconds = calculateIntervalMin(reconnectIntervalMinSeconds);
    }

    long calculateIntervalMax(long reconnectIntervalMinSeconds) {
        return reconnectIntervalMinSeconds > MAX_RECONNECT_INTERVAL_SEC ? reconnectIntervalMinSeconds : MAX_RECONNECT_INTERVAL_SEC;
    }

    long calculateIntervalMin(long reconnectIntervalMinSeconds) {
        return Math.min((reconnectIntervalMinSeconds > 0 ? reconnectIntervalMinSeconds : DEFAULT_RECONNECT_INTERVAL_SEC), this.reconnectIntervalMaxSeconds);
    }

    @Override
    synchronized public long getNextReconnectDelay() {
        final long currentNanoTime = getNanoTime();
        final long coolDownSpentNanos = currentNanoTime - lastDisconnectNanoTime;
        lastDisconnectNanoTime = currentNanoTime;
        if (isCooledDown(coolDownSpentNanos)) {
            retryCount = 0;
            return reconnectIntervalMinSeconds;
        }
        return calculateNextReconnectDelay() + calculateJitter();
    }

    long calculateJitter() {
        return ThreadLocalRandom.current().nextInt() >= 0 ? JITTER_MAX : 0;
    }

    long calculateNextReconnectDelay() {
        return Math.min(reconnectIntervalMaxSeconds, reconnectIntervalMinSeconds + calculateExp(retryCount++));
    }

    long calculateExp(long e) {
        return 1L << Math.min(e, EXP_MAX);
    }

    boolean isCooledDown(long coolDownSpentNanos) {
        return TimeUnit.NANOSECONDS.toSeconds(coolDownSpentNanos) > reconnectIntervalMaxSeconds + reconnectIntervalMinSeconds;
    }

    long getNanoTime() {
        return System.nanoTime();
    }

}
