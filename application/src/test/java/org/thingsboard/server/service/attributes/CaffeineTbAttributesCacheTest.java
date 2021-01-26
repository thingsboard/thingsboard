/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.service.attributes;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Ticker;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class CaffeineTbAttributesCacheTest {

    private static TbCacheStatsService<Cache<AttributesKey, AttributeCacheEntry>> mockedStatsService = mock(TbCacheStatsService.class);

    @BeforeClass
    public static void initStatsMock(){
        when(mockedStatsService.areCacheStatsEnabled()).thenReturn(false);
    }

    @Test
    public void testCacheExpiration() {
        AttributesCacheConfiguration cacheConfiguration = new AttributesCacheConfiguration();
        cacheConfiguration.setExpireAfterAccessInMinutes(1);
        cacheConfiguration.setMaxSizePerTenant(10);
        CaffeineTbAttributesCache attributesCache = new CaffeineTbAttributesCache(cacheConfiguration, mockedStatsService);
        CustomTicker customTicker = new CustomTicker();
        attributesCache.setCustomTicker(customTicker);
        TenantId tenantId = new TenantId(UUID.randomUUID());
        DeviceId entityId = new DeviceId(UUID.randomUUID());
        String scope = "scope";
        String key = "test";
        BaseAttributeKvEntry entry = new BaseAttributeKvEntry(0, null);
        attributesCache.put(tenantId, entityId, scope, key, entry);
        long insertTime = System.nanoTime();
        Assert.assertNotNull(attributesCache.find(tenantId, entityId, scope, key));
        Assert.assertEquals(entry, attributesCache.find(tenantId, entityId, scope, key).getAttributeKvEntry());
        customTicker.supplierRef.getAndSet(() -> insertTime + TimeUnit.SECONDS.toNanos(61));
        Assert.assertNull(attributesCache.find(tenantId, entityId, scope, key));
    }

    // caffeine can exceed the size limit so cannot properly test this case
    public void testCacheCleanupBySize() {
    }

    private static class CustomTicker implements Ticker {
        AtomicReference<Supplier<Long>> supplierRef = new AtomicReference<>(System::nanoTime);

        @Override
        public long read() {
            return supplierRef.get().get();
        }
    }
}