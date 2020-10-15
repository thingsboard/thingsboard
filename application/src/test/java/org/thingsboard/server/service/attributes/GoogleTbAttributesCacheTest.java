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
        Assert.assertEquals(entry, attributesCache.find(tenantId, entityId, scope, key).get());
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