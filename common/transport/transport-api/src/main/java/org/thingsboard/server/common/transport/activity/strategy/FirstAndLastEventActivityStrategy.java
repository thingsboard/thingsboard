package org.thingsboard.server.common.transport.activity.strategy;

import org.thingsboard.server.common.transport.activity.ActivityState;

public class FirstAndLastEventActivityStrategy implements ActivityStrategy {

    private volatile long firstEventTs;

    @Override
    public boolean onActivity(ActivityState state) {
        long lastRecordedTime = state.getLastRecordedTime();
        if (firstEventTs == 0L) {
            firstEventTs = lastRecordedTime;
            return state.getLastReportedTime() < firstEventTs;
        }
        return false;
    }

    @Override
    public boolean onReportingPeriodEnd(ActivityState state) {
        firstEventTs = 0L;
        return state.getLastReportedTime() < state.getLastRecordedTime();
    }

}
