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
package org.thingsboard.server.common.transport.quota;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Vitaliy Paromskiy
 * @version 1.0
 */
public class ClockTest {

    @Before
    public void init() {
        Clock.reset();
    }

    @After
    public void clear() {
        Clock.reset();
    }

    @Test
    public void defaultClockUseSystemTime() {
        assertFalse(Clock.millis() > System.currentTimeMillis());
    }

    @Test
    public void timeCanBeSet() {
        Clock.setMillis(100L);
        assertEquals(100L, Clock.millis());
    }

    @Test
    public void clockCanBeReseted() {
        Clock.setMillis(100L);
        assertEquals(100L, Clock.millis());
        Clock.reset();
        assertFalse(Clock.millis() > System.currentTimeMillis());
    }

    @Test
    public void timeIsShifted() {
        Clock.setMillis(100L);
        Clock.shift(50L);
        assertEquals(150L, Clock.millis());
    }

}