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

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.transport.quota.host.HostIntervalRegistryLogger;
import org.thingsboard.server.common.transport.quota.host.HostRequestIntervalRegistry;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * @author Vitaliy Paromskiy
 * @version 1.0
 */
public class IntervalRegistryLoggerTest {

    private IntervalRegistryLogger logger;

    private HostRequestIntervalRegistry requestRegistry = mock(HostRequestIntervalRegistry.class);

    @Before
    public void init() {
        logger = new HostIntervalRegistryLogger(3, 10, requestRegistry);
    }

    @Test
    public void onlyMaxHostsCollected() {
        Map<String, Long> map = ImmutableMap.of("a", 8L, "b", 3L, "c", 1L, "d", 3L);
        Map<String, Long> actual = logger.getTopElements(map);
        Map<String, Long> expected = ImmutableMap.of("a", 8L, "b", 3L, "d", 3L);

        assertEquals(expected, actual);
    }

    @Test
    public void emptyMapProcessedCorrectly() {
        Map<String, Long> map = Collections.emptyMap();
        Map<String, Long> actual = logger.getTopElements(map);
        Map<String, Long> expected = Collections.emptyMap();

        assertEquals(expected, actual);
    }

}