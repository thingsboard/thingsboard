// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.activity.strategy;

public final class AllEventsActivityStrategy implements ActivityStrategy {

    private static final AllEventsActivityStrategy INSTANCE = new AllEventsActivityStrategy();

    private AllEventsActivityStrategy() {
    }

    public static AllEventsActivityStrategy getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean onActivity() {
        return true;
    }

    @Override
    public boolean onReportingPeriodEnd() {
        return true;
    }

}
