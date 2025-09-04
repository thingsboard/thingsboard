/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
