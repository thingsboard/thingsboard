/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import java.util.concurrent.TimeUnit;

@Getter
@Slf4j
public class ReconnectStrategyExponential implements ReconnectStrategy {

    public static final int DEFAULT_RECONNECT_INTERVAL = 10;
    final long reconnectIntervalMinSeconds;
    final long reconnectIntervalMaxSeconds = 30;
    long lastDisconnectNanoTime = 0; //isotonic time
    int retryCount = 0;

    public ReconnectStrategyExponential(long reconnectIntervalMin) {
        this.reconnectIntervalMinSeconds = calculateIntervalMin(reconnectIntervalMin);
    }

    private long calculateIntervalMin(long reconnectIntervalMin) {
        return Math.min((reconnectIntervalMin > 0 ? reconnectIntervalMin : DEFAULT_RECONNECT_INTERVAL), this.reconnectIntervalMaxSeconds);
    }

    @Override
    synchronized public long getNextReconnectDelay() {
        final long currentNanoTime = getNanoTime();
        final long lastDisconnectIntervalNanos = currentNanoTime - lastDisconnectNanoTime;
        lastDisconnectNanoTime = currentNanoTime;
        if (TimeUnit.NANOSECONDS.toSeconds(lastDisconnectIntervalNanos) > reconnectIntervalMaxSeconds + reconnectIntervalMinSeconds) {
            log.debug("Reset retry counter");
            retryCount = 0;
            return reconnectIntervalMinSeconds;
        }
        return Math.min(reconnectIntervalMaxSeconds, reconnectIntervalMinSeconds + (1L << retryCount++));
    }

    long getNanoTime() {
        return System.nanoTime();
    }

}
