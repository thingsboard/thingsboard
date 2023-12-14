package org.thingsboard.server.common.transport.activity.strategy;

public enum ActivityStrategyType {

    FIRST(new FirstEventActivityStrategy()),
    LAST(new LastEventActivityStrategy()),
    FIRST_AND_LAST(new FirstAndLastEventActivityStrategy()),
    ALL(new AllEventsActivityStrategy());

    private final ActivityStrategy strategy;

    ActivityStrategyType(ActivityStrategy strategy) {
        this.strategy = strategy;
    }

    public ActivityStrategy getStrategy() {
        return strategy;
    }

}
