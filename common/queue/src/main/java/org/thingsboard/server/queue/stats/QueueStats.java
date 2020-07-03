package org.thingsboard.server.queue.stats;

public interface QueueStats {
    default void incrementTotal() {
        incrementTotal(1);
    }

    void incrementTotal(int amount);

    default void incrementSuccessful() {
        incrementSuccessful(1);
    }

    void incrementSuccessful(int amount);


    default void incrementFailed() {
        incrementFailed(1);
    }

    void incrementFailed(int amount);
}
