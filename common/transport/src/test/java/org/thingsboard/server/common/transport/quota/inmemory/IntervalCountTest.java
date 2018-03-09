/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.common.transport.quota.inmemory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.transport.quota.Clock;

import static org.junit.Assert.assertEquals;

/**
 * @author Vitaliy Paromskiy
 * @version 1.0
 */
public class IntervalCountTest {

    @Before
    public void init() {
        Clock.setMillis(1000L);
    }

    @After
    public void clear() {
        Clock.reset();
    }

    @Test
    public void ticksInSameIntervalAreSummed() {
        IntervalCount intervalCount = new IntervalCount(100L);
        assertEquals(1L, intervalCount.resetIfExpiredAndTick());
        Clock.shift(100);
        assertEquals(2L, intervalCount.resetIfExpiredAndTick());
    }

    @Test
    public void oldDataCleanedWhenIntervalExpired() {
        IntervalCount intervalCount = new IntervalCount(100L);
        assertEquals(1L, intervalCount.resetIfExpiredAndTick());
        Clock.shift(101);
        assertEquals(1L, intervalCount.resetIfExpiredAndTick());
    }

    @Test
    public void silenceDurationCalculatedFromLastTick() {
        IntervalCount intervalCount = new IntervalCount(100L);
        assertEquals(1L, intervalCount.resetIfExpiredAndTick());
        Clock.shift(10L);
        assertEquals(10L, intervalCount.silenceDuration());
    }

}