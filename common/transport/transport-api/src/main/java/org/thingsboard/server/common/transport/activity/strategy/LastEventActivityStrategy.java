package org.thingsboard.server.common.transport.activity.strategy;

import org.thingsboard.server.common.transport.activity.ActivityState;

public class LastEventActivityStrategy implements ActivityStrategy {

    @Override
    public boolean onActivity(ActivityState state) {
        return false;
    }

    @Override
    public boolean onReportingPeriodEnd(ActivityState state) {
        return state.getLastReportedTime() < state.getLastRecordedTime();
    }

}
