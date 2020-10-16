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

import com.google.common.base.Supplier;
import com.google.common.base.Ticker;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class GoogleTbAttributesCacheTest {

    @Test
    public void testCacheExpiration() {
        AttributesCacheConfiguration cacheConfiguration = new AttributesCacheConfiguration();
        cacheConfiguration.setExpireAfterAccessInMinutes(1);
        cacheConfiguration.setMaxSizePerTenant(10);
        GoogleTbAttributesCache attributesCache = new GoogleTbAttributesCache(cacheConfiguration);
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

    @Test
    public void testCacheCleanupBySize() {
        AttributesCacheConfiguration cacheConfiguration = new AttributesCacheConfiguration();
        cacheConfiguration.setExpireAfterAccessInMinutes(10);
        cacheConfiguration.setMaxSizePerTenant(5);
        GoogleTbAttributesCache attributesCache = new GoogleTbAttributesCache(cacheConfiguration);
        TenantId tenantId = new TenantId(UUID.randomUUID());
        DeviceId entityId = new DeviceId(UUID.randomUUID());
        String scope = "scope";
        BaseAttributeKvEntry entry = new BaseAttributeKvEntry(0, null);
        for (int i = 0; i < 10; i++) {
            attributesCache.put(tenantId, entityId, scope, Integer.toString(i), entry);
        }
        for (int i = 0; i < 10; i++) {
            if (i < 5) {
                Assert.assertNull(attributesCache.find(tenantId, entityId, scope, Integer.toString(i)));
            } else {
                Assert.assertNotNull(attributesCache.find(tenantId, entityId, scope, Integer.toString(i)));
            }
        }
    }

    private static class CustomTicker extends Ticker {
        AtomicReference<Supplier<Long>> supplierRef = new AtomicReference<>(System::nanoTime);

        @Override
        public long read() {
            return supplierRef.get().get();
        }
    }
}