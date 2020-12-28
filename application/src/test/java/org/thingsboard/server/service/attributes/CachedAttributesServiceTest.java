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

import com.google.common.cache.Cache;
import com.google.common.util.concurrent.Futures;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.service.queue.TbClusterService;

import java.util.*;

import static org.mockito.Mockito.*;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class CachedAttributesServiceTest {
    private static final String scope = "scope";
    private static final String key = "testKey";
    private static final String value = "testValue";
    private static final String firstKey = "firstTestKey";
    private static final String secondKey = "secondTestKey";
    private static final List<String> keys = Arrays.asList(firstKey, secondKey);

    private static final TenantId tenantId = new TenantId(UUID.randomUUID());
    private static final CustomerId customerId = new CustomerId(UUID.randomUUID());
    private static final DeviceId deviceId = new DeviceId(UUID.randomUUID());

    private GoogleTbAttributesCache attributesCache;
    private TbClusterService clusterService;
    private PartitionService partitionService;
    private AttributesService daoAttributesService;
    private CachedAttributesService cachedAttributesService;

    @Before
    public void before(){
        AttributesCacheConfiguration cacheConfiguration =  AttributesCacheConfiguration.builder()
                .expireAfterAccessInMinutes(1)
                .maxSizePerTenant(10)
                .build();
        TbCacheStatsService<Cache<AttributesKey, AttributeCacheEntry>> mockedStatsService = mock(TbCacheStatsService.class);
        when(mockedStatsService.areCacheStatsEnabled()).thenReturn(false);
        this.attributesCache = new GoogleTbAttributesCache(cacheConfiguration, mockedStatsService);

        this.clusterService = mock(TbClusterService.class);
        this.partitionService = mock(PartitionService.class);
        this.daoAttributesService = mock(AttributesService.class);

        this.cachedAttributesService = new CachedAttributesService(daoAttributesService, clusterService,
                partitionService, attributesCache);
    }

    @Test
    public void testFindCustomerAttributeOnLocalPartition() throws Exception{
        BaseAttributeKvEntry entry = new BaseAttributeKvEntry(0, new StringDataEntry(key, value));
        when(daoAttributesService.find(Mockito.any(TenantId.class), Mockito.any(EntityId.class), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Futures.immediateFuture(Optional.of(entry)));

        for (int i = 0; i < 10; i++) {
            Assert.assertEquals(entry, cachedAttributesService.find(tenantId, customerId, scope, key).get().get());
            verify(daoAttributesService, times(1)).find(tenantId, customerId, scope, key);
        }
    }

    @Test
    public void testFindCustomerAttributeOnRemotePartition() throws Exception{
        BaseAttributeKvEntry entry = new BaseAttributeKvEntry(0, new StringDataEntry(key, value));
        when(daoAttributesService.find(Mockito.any(TenantId.class), Mockito.any(EntityId.class), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Futures.immediateFuture(Optional.of(entry)));

        for (int i = 0; i < 10; i++) {
            Assert.assertEquals(entry, cachedAttributesService.find(tenantId, customerId, scope, key).get().get());
            verify(daoAttributesService, times(1)).find(tenantId, customerId, scope, key);
        }
    }

    @Test
    public void testFindDeviceAttributeOnLocalPartition() throws Exception{
        BaseAttributeKvEntry entry = new BaseAttributeKvEntry(0, new StringDataEntry(key, value));

        when(daoAttributesService.find(Mockito.any(TenantId.class), Mockito.any(EntityId.class), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Futures.immediateFuture(Optional.of(entry)));
        when(partitionService.resolve(Mockito.any(ServiceType.class), Mockito.anyString(), Mockito.any(TenantId.class), Mockito.any(EntityId.class)))
                .thenReturn(new TopicPartitionInfo("", null, 0, true));

        for (int i = 0; i < 10; i++) {
            Assert.assertEquals(entry, cachedAttributesService.find(tenantId, deviceId, scope, key).get().get());
            verify(daoAttributesService, times(1)).find(tenantId, deviceId, scope, key);
        }
    }

    @Test
    public void testFindDeviceAttributeOnRemotePartition() throws Exception{
        BaseAttributeKvEntry entry = new BaseAttributeKvEntry(0, new StringDataEntry(key, value));

        when(daoAttributesService.find(Mockito.any(TenantId.class), Mockito.any(EntityId.class), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Futures.immediateFuture(Optional.of(entry)));
        when(partitionService.resolve(Mockito.any(ServiceType.class), Mockito.anyString(), Mockito.any(TenantId.class), Mockito.any(EntityId.class)))
                .thenReturn(new TopicPartitionInfo("", null, 0, false));

        for (int i = 0; i < 10; i++) {
            Assert.assertEquals(entry, cachedAttributesService.find(tenantId, deviceId, scope, key).get().get());
            verify(daoAttributesService, times(i+1)).find(tenantId, deviceId, scope, key);
        }
    }

    @Test
    public void testFindMultipleDeviceAttributeOnLocalPartition() throws Exception{
        BaseAttributeKvEntry firstEntry = new BaseAttributeKvEntry(0, new StringDataEntry(firstKey, "value1"));
        BaseAttributeKvEntry secondEntry = new BaseAttributeKvEntry(0, new StringDataEntry(secondKey, "value2"));

        when(daoAttributesService.find(Mockito.any(TenantId.class), Mockito.any(EntityId.class), Mockito.anyString(), Mockito.anyCollection()))
                .thenReturn(Futures.immediateFuture(Arrays.asList(firstEntry, secondEntry)));
        when(partitionService.resolve(Mockito.any(ServiceType.class), Mockito.anyString(), Mockito.any(TenantId.class), Mockito.any(EntityId.class)))
                .thenReturn(new TopicPartitionInfo("", null, 0, true));

        Assert.assertEquals(2, cachedAttributesService.find(tenantId, deviceId, scope, keys).get().size());
        verify(daoAttributesService, times(1)).find(Mockito.any(TenantId.class), Mockito.any(EntityId.class), Mockito.anyString(), Mockito.anyCollection());
        Assert.assertEquals(firstEntry, cachedAttributesService.find(tenantId, deviceId, scope, firstKey).get().get());
        Assert.assertEquals(secondEntry, cachedAttributesService.find(tenantId, deviceId, scope, secondKey).get().get());
        verify(daoAttributesService, times(0)).find(tenantId, deviceId, scope, firstKey);
        verify(daoAttributesService, times(0)).find(tenantId, deviceId, scope, secondKey);
    }

    @Test
    public void testFindMultipleDeviceAttributeOnLocalPartition_2() throws Exception{
        BaseAttributeKvEntry firstEntry = new BaseAttributeKvEntry(0, new StringDataEntry(firstKey, "value1"));
        BaseAttributeKvEntry secondEntry = new BaseAttributeKvEntry(0, new StringDataEntry(secondKey, "value2"));

        when(daoAttributesService.find(Mockito.any(TenantId.class), Mockito.any(EntityId.class), Mockito.anyString(), Mockito.eq(firstKey)))
                .thenReturn(Futures.immediateFuture(Optional.of(firstEntry)));
        when(daoAttributesService.find(Mockito.any(TenantId.class), Mockito.any(EntityId.class), Mockito.anyString(), Mockito.eq(secondKey)))
                .thenReturn(Futures.immediateFuture(Optional.of(secondEntry)));
        when(partitionService.resolve(Mockito.any(ServiceType.class), Mockito.anyString(), Mockito.any(TenantId.class), Mockito.any(EntityId.class)))
                .thenReturn(new TopicPartitionInfo("", null, 0, true));

        Assert.assertEquals(firstEntry, cachedAttributesService.find(tenantId, deviceId, scope, firstKey).get().get());
        Assert.assertEquals(secondEntry, cachedAttributesService.find(tenantId, deviceId, scope, secondKey).get().get());
        verify(daoAttributesService, times(1)).find(tenantId, deviceId, scope, firstKey);
        verify(daoAttributesService, times(1)).find(tenantId, deviceId, scope, secondKey);
        Assert.assertEquals(2, cachedAttributesService.find(tenantId, deviceId, scope, keys).get().size());
        verify(daoAttributesService, times(0)).find(tenantId, deviceId, scope, keys);
    }

    @Test
    public void testFindMultipleDeviceAttributeOnLocalPartition_3() throws Exception{
        BaseAttributeKvEntry firstEntry = new BaseAttributeKvEntry(0, new StringDataEntry(firstKey, "value1"));
        BaseAttributeKvEntry secondEntry = new BaseAttributeKvEntry(0, new StringDataEntry(secondKey, "value2"));

        when(daoAttributesService.find(Mockito.any(TenantId.class), Mockito.any(EntityId.class), Mockito.anyString(), Mockito.eq(firstKey)))
                .thenReturn(Futures.immediateFuture(Optional.of(firstEntry)));
        when(partitionService.resolve(Mockito.any(ServiceType.class), Mockito.anyString(), Mockito.any(TenantId.class), Mockito.any(EntityId.class)))
                .thenReturn(new TopicPartitionInfo("", null, 0, true));

        Assert.assertEquals(firstEntry, cachedAttributesService.find(tenantId, deviceId, scope, firstKey).get().get());
        verify(daoAttributesService, times(1)).find(tenantId, deviceId, scope, firstKey);

        Set<String> secondKeyCollection = new HashSet<>();
        secondKeyCollection.add(secondKey);
        when(daoAttributesService.find(Mockito.any(TenantId.class), Mockito.any(EntityId.class), Mockito.anyString(), Mockito.eq(secondKeyCollection)))
                .thenReturn(Futures.immediateFuture(Arrays.asList(secondEntry)));
        Assert.assertEquals(2, cachedAttributesService.find(tenantId, deviceId, scope, keys).get().size());
        Assert.assertEquals(secondEntry, cachedAttributesService.find(tenantId, deviceId, scope, secondKey).get().get());
        verify(daoAttributesService, times(0)).find(tenantId, deviceId, scope, secondKey);
    }

    @Test
    public void testSaveDeviceAttributeOnLocalPartition() throws Exception{
        BaseAttributeKvEntry firstEntry = new BaseAttributeKvEntry(0, new StringDataEntry(firstKey, "value1"));
        BaseAttributeKvEntry secondEntry = new BaseAttributeKvEntry(0, new StringDataEntry(secondKey, "value2"));

        when(daoAttributesService.find(Mockito.any(TenantId.class), Mockito.any(EntityId.class), Mockito.anyString(), Mockito.anyCollection()))
                .thenReturn(Futures.immediateFuture(Arrays.asList(firstEntry, secondEntry)));
        when(partitionService.resolve(Mockito.any(ServiceType.class), Mockito.anyString(), Mockito.any(TenantId.class), Mockito.any(EntityId.class)))
                .thenReturn(new TopicPartitionInfo("", null, 0, true));

        Assert.assertEquals(2, cachedAttributesService.find(tenantId, deviceId, scope, keys).get().size());
        verify(daoAttributesService, times(1)).find(Mockito.any(TenantId.class), Mockito.any(EntityId.class), Mockito.anyString(), Mockito.anyCollection());

        when(daoAttributesService.save(Mockito.any(TenantId.class), Mockito.any(EntityId.class), Mockito.anyString(), Mockito.anyList()))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));
        BaseAttributeKvEntry newSecondEntry = new BaseAttributeKvEntry(1, new StringDataEntry(secondKey, "newValue2"));
        cachedAttributesService.save(tenantId, deviceId, scope, Arrays.asList(newSecondEntry));

        Assert.assertEquals(2, cachedAttributesService.find(tenantId, deviceId, scope, keys).get().size());
        verify(daoAttributesService, times(1)).find(Mockito.any(TenantId.class), Mockito.any(EntityId.class), Mockito.anyString(), Mockito.anyCollection());

        Assert.assertEquals(firstEntry, cachedAttributesService.find(tenantId, deviceId, scope, firstKey).get().get());
        verify(daoAttributesService, times(0)).find(tenantId, deviceId, scope, firstKey);

        Assert.assertEquals(newSecondEntry, cachedAttributesService.find(tenantId, deviceId, scope, secondKey).get().get());
        verify(daoAttributesService, times(0)).find(tenantId, deviceId, scope, firstKey);
    }

    @Test
    public void testRemoveDeviceAttributeOnLocalPartition() throws Exception{
        BaseAttributeKvEntry firstEntry = new BaseAttributeKvEntry(0, new StringDataEntry(firstKey, "value1"));
        BaseAttributeKvEntry secondEntry = new BaseAttributeKvEntry(0, new StringDataEntry(secondKey, "value2"));

        when(daoAttributesService.find(Mockito.any(TenantId.class), Mockito.any(EntityId.class), Mockito.anyString(), Mockito.anyCollection()))
                .thenReturn(Futures.immediateFuture(Arrays.asList(firstEntry, secondEntry)));
        when(partitionService.resolve(Mockito.any(ServiceType.class), Mockito.anyString(), Mockito.any(TenantId.class), Mockito.any(EntityId.class)))
                .thenReturn(new TopicPartitionInfo("", null, 0, true));

        Assert.assertEquals(2, cachedAttributesService.find(tenantId, deviceId, scope, keys).get().size());
        verify(daoAttributesService, times(1)).find(Mockito.any(TenantId.class), Mockito.any(EntityId.class), Mockito.anyString(), Mockito.anyCollection());

        when(daoAttributesService.removeAll(Mockito.any(TenantId.class), Mockito.any(EntityId.class), Mockito.anyString(), Mockito.anyList()))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));
        cachedAttributesService.removeAll(tenantId, deviceId, scope, Arrays.asList(secondKey));

        Assert.assertEquals(firstEntry, cachedAttributesService.find(tenantId, deviceId, scope, firstKey).get().get());
        verify(daoAttributesService, times(0)).find(tenantId, deviceId, scope, firstKey);

        when(daoAttributesService.find(Mockito.any(TenantId.class), Mockito.any(EntityId.class), Mockito.anyString(), Mockito.eq(secondKey)))
                .thenReturn(Futures.immediateFuture(Optional.empty()));
        Assert.assertNull(cachedAttributesService.find(tenantId, deviceId, scope, secondKey).get().orElse(null));
        verify(daoAttributesService, times(1)).find(tenantId, deviceId, scope, secondKey);
    }

    @Test
    public void testRemoveClientAttribute() throws Exception{
        BaseAttributeKvEntry entry = new BaseAttributeKvEntry(1, new StringDataEntry(key, "value1"));

        when(partitionService.resolve(Mockito.any(ServiceType.class), Mockito.anyString(), Mockito.any(TenantId.class), Mockito.any(EntityId.class)))
                .thenReturn(new TopicPartitionInfo("", null, 0, true));
        when(daoAttributesService.save(Mockito.any(TenantId.class), Mockito.any(EntityId.class), Mockito.anyString(), Mockito.anyList()))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));

        cachedAttributesService.save(tenantId, customerId, scope, Arrays.asList(entry));
        verify(clusterService, times(1))
                .onAttributesCacheUpdated(Mockito.any(TenantId.class), Mockito.any(EntityId.class), Mockito.anyString(), Mockito.anyList());
    }

}
