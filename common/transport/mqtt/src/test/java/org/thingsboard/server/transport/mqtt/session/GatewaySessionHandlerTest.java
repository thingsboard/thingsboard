/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.transport.mqtt.session;

import org.junit.Test;

import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.Mockito.mock;

public class GatewaySessionHandlerTest {

    @Test
    public void givenWeakHashMap_WhenGC_thenMapIsEmpty() {
        WeakHashMap<String, Lock> map = new WeakHashMap<>();

        String deviceName = new String("device"); //constants are static and doesn't affected by GC, so use new instead
        map.put(deviceName, new ReentrantLock());
        assertTrue(map.containsKey(deviceName));

        deviceName = null;
        System.gc();

        await().atMost(10, TimeUnit.SECONDS).until(() -> !map.containsKey("device"));
    }

    @Test
    public void givenConcurrentReferenceHashMap_WhenGC_thenMapIsEmpty() {
        GatewaySessionHandler gsh = mock(GatewaySessionHandler.class);
        willCallRealMethod().given(gsh).createWeakMap();

        ConcurrentMap<String, Lock> map = gsh.createWeakMap();
        map.put("device", new ReentrantLock());
        assertTrue(map.containsKey("device"));

        System.gc();

        await().atMost(10, TimeUnit.SECONDS).until(() -> !map.containsKey("device"));
    }

}