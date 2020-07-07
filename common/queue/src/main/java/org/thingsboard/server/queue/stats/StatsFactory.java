package org.thingsboard.server.queue.stats;

public interface StatsFactory {
    StatsCounter createStatsCounter(String key, String statsName);

    MessagesStats createMessagesStats(String key);
}
