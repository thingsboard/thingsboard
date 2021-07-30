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