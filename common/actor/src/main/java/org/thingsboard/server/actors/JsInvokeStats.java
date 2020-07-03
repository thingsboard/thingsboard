package org.thingsboard.server.actors;

public interface JsInvokeStats {
    default void incrementRequests() {
        incrementRequests(1);
    }

    void incrementRequests(int amount);

    default void incrementResponses() {
        incrementResponses(1);
    }

    void incrementResponses(int amount);

    default void incrementFailures() {
        incrementFailures(1);
    }

    void incrementFailures(int amount);

    int getRequests();

    int getResponses();

    int getFailures();

    void reset();
}
