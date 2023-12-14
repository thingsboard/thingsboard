package org.thingsboard.server.common.transport.activity.strategy;

import org.thingsboard.server.common.transport.activity.ActivityState;

public class AllEventsActivityStrategy implements ActivityStrategy {

    @Override
    public boolean onActivity(ActivityState state) {
        return state.getLastReportedTime() < state.getLastReportedTime();
    }

    @Override
    public boolean onReportingPeriodEnd(ActivityState state) {
        return state.getLastReportedTime() < state.getLastReportedTime();
    }

}
