// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
