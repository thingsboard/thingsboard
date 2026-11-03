// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.activity.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LastEventActivityStrategyTest {

    private LastEventActivityStrategy strategy;

    @BeforeEach
    public void setUp() {
        strategy = LastEventActivityStrategy.getInstance();
    }

    @Test
    public void testOnActivity() {
        assertFalse(strategy.onActivity(), "onActivity() should always return false.");
    }

    @Test
    public void testOnReportingPeriodEnd() {
        assertTrue(strategy.onReportingPeriodEnd(), "onReportingPeriodEnd() should always return true.");
    }

}
