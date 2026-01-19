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
package org.thingsboard.server.common.stats;

public class DefaultMessagesStats implements MessagesStats {
    private final StatsCounter totalCounter;
    private final StatsCounter successfulCounter;
    private final StatsCounter failedCounter;

    public DefaultMessagesStats(StatsCounter totalCounter, StatsCounter successfulCounter, StatsCounter failedCounter) {
        this.totalCounter = totalCounter;
        this.successfulCounter = successfulCounter;
        this.failedCounter = failedCounter;
    }

    @Override
    public void incrementTotal(int amount) {
        totalCounter.add(amount);
    }

    @Override
    public void incrementSuccessful(int amount) {
        successfulCounter.add(amount);
    }

    @Override
    public void incrementFailed(int amount) {
        failedCounter.add(amount);
    }

    @Override
    public int getTotal() {
        return totalCounter.get();
    }

    @Override
    public int getSuccessful() {
        return successfulCounter.get();
    }

    @Override
    public int getFailed() {
        return failedCounter.get();
    }

    @Override
    public void reset() {
        totalCounter.clear();
        successfulCounter.clear();
        failedCounter.clear();
    }
}
