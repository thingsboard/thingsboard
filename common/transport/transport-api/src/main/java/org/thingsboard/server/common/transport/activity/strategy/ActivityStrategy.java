package org.thingsboard.server.common.transport.activity.strategy;

import org.thingsboard.server.common.transport.activity.ActivityState;

public interface ActivityStrategy {

    boolean onActivity(ActivityState state);

    boolean onReportingPeriodEnd(ActivityState state);

}
