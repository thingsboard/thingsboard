/**
 * Copyright © 2016-2017 The Thingsboard Authors
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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Vitaliy Paromskiy
 * @version 1.0
 */
public class HostRequestIntervalRegistryTest {

    private HostRequestIntervalRegistry registry;

    @Before
    public void init() {
        registry = new HostRequestIntervalRegistry(10000L, 100L);
    }

    @Test
    public void newHostCreateNewInterval() {
        assertEquals(1L, registry.tick("host1"));
    }

    @Test
    public void existingHostUpdated() {
        registry.tick("aaa");
        assertEquals(1L, registry.tick("bbb"));
        assertEquals(2L, registry.tick("aaa"));
    }

    @Test
    public void expiredIntervalsCleaned() throws InterruptedException {
        registry.tick("aaa");
        Thread.sleep(150L);
        registry.tick("bbb");
        registry.clean();
        assertEquals(1L, registry.tick("aaa"));
        assertEquals(2L, registry.tick("bbb"));
    }
}