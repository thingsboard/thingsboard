package org.thingsboard.server.service.stats;

import org.thingsboard.server.queue.stats.QueueStats;

// TODO do we need to have a scheduler that'll reset atomic integer?
public class DefaultQueueStats implements QueueStats {
    private final StatsCounter totalCounter;
    private final StatsCounter successfulCounter;
    private final StatsCounter failedCounter;

    public DefaultQueueStats(StatsCounter totalCounter, StatsCounter successfulCounter, StatsCounter failedCounter) {
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
}
