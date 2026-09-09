// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.activity.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FirstEventActivityStrategyTest {

    private FirstEventActivityStrategy strategy;

    @BeforeEach
    public void setUp() {
        strategy = new FirstEventActivityStrategy();
    }

    @Test
    public void testOnActivity_FirstCall() {
        assertTrue(strategy.onActivity(), "First call of onActivity() should return true.");
    }

    @Test
    public void testOnActivity_SubsequentCalls() {
        assertTrue(strategy.onActivity(), "First call of onActivity() should return true.");
        assertFalse(strategy.onActivity(), "Subsequent calls of onActivity() should return false.");
    }

    @Test
    public void testOnReportingPeriodEnd() {
        assertTrue(strategy.onActivity(), "First call of onActivity() should return true.");
        assertFalse(strategy.onReportingPeriodEnd(), "onReportingPeriodEnd() should always return false.");
        assertTrue(strategy.onActivity(), "onActivity() should return true after onReportingPeriodEnd().");
        assertFalse(strategy.onReportingPeriodEnd(), "onReportingPeriodEnd() should always return false.");
    }

}
