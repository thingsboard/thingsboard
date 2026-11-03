// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.activity.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AllEventsActivityStrategyTest {

    private AllEventsActivityStrategy strategy;

    @BeforeEach
    public void setUp() {
        strategy = AllEventsActivityStrategy.getInstance();
    }

    @Test
    public void testOnActivity() {
        assertTrue(strategy.onActivity(), "onActivity() should always return true.");
    }

    @Test
    public void testOnReportingPeriodEnd() {
        assertTrue(strategy.onReportingPeriodEnd(), "onReportingPeriodEnd() should always return true.");
    }

}
