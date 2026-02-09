/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
