/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.dao.service.attributes;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.service.AbstractServiceTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Andrew Shvayka
 */
@Slf4j
public abstract class BaseAttributesServiceTest extends AbstractServiceTest {

    private static final String OLD_VALUE = "OLD VALUE";
    private static final String NEW_VALUE = "NEW VALUE";

    @Autowired
    private AttributesService attributesService;

    @Before
    public void before() {
    }

    @Test
    public void saveAndFetch() throws Exception {
        DeviceId deviceId = new DeviceId(Uuids.timeBased());
        KvEntry attrValue = new StringDataEntry("attribute1", "value1");
        AttributeKvEntry attr = new BaseAttributeKvEntry(attrValue, 42L);
        attributesService.save(SYSTEM_TENANT_ID, deviceId, AttributeScope.CLIENT_SCOPE, Collections.singletonList(attr)).get();
        Optional<AttributeKvEntry> saved = attributesService.find(SYSTEM_TENANT_ID, deviceId, AttributeScope.CLIENT_SCOPE, attr.getKey()).get();
        Assert.assertTrue(saved.isPresent());
        equalsIgnoreVersion(attr, saved.get());
    }

    @Test
    public void saveMultipleTypeAndFetch() throws Exception {
        DeviceId deviceId = new DeviceId(Uuids.timeBased());
        KvEntry attrOldValue = new StringDataEntry("attribute1", "value1");
        AttributeKvEntry attrOld = new BaseAttributeKvEntry(attrOldValue, 42L);

        attributesService.save(SYSTEM_TENANT_ID, deviceId, AttributeScope.CLIENT_SCOPE, Collections.singletonList(attrOld)).get();
        Optional<AttributeKvEntry> saved = attributesService.find(SYSTEM_TENANT_ID, deviceId, AttributeScope.CLIENT_SCOPE, attrOld.getKey()).get();

        Assert.assertTrue(saved.isPresent());
        equalsIgnoreVersion(attrOld, saved.get());

        KvEntry attrNewValue = new StringDataEntry("attribute1", "value2");
        AttributeKvEntry attrNew = new BaseAttributeKvEntry(attrNewValue, 73L);
        attributesService.save(SYSTEM_TENANT_ID, deviceId, AttributeScope.CLIENT_SCOPE, Collections.singletonList(attrNew)).get();

        saved = attributesService.find(SYSTEM_TENANT_ID, deviceId, AttributeScope.CLIENT_SCOPE, attrOld.getKey()).get();
        Assert.assertTrue(saved.isPresent());
        equalsIgnoreVersion(attrNew, saved.get());
    }

    @Test
    public void findAll() throws Exception {
        DeviceId deviceId = new DeviceId(Uuids.timeBased());

        KvEntry attrAOldValue = new StringDataEntry("A", "value1");
        AttributeKvEntry attrAOld = new BaseAttributeKvEntry(attrAOldValue, 42L);
        KvEntry attrANewValue = new StringDataEntry("A", "value2");
        AttributeKvEntry attrANew = new BaseAttributeKvEntry(attrANewValue, 73L);
        KvEntry attrBNewValue = new StringDataEntry("B", "value3");
        AttributeKvEntry attrBNew = new BaseAttributeKvEntry(attrBNewValue, 73L);

        attributesService.save(SYSTEM_TENANT_ID, deviceId, AttributeScope.CLIENT_SCOPE, Collections.singletonList(attrAOld)).get();
        attributesService.save(SYSTEM_TENANT_ID, deviceId, AttributeScope.CLIENT_SCOPE, Collections.singletonList(attrANew)).get();
        attributesService.save(SYSTEM_TENANT_ID, deviceId, AttributeScope.CLIENT_SCOPE, Collections.singletonList(attrBNew)).get();

        List<AttributeKvEntry> saved = attributesService.findAll(SYSTEM_TENANT_ID, deviceId, AttributeScope.CLIENT_SCOPE).get();

        Assert.assertNotNull(saved);
        Assert.assertEquals(2, saved.size());

        equalsIgnoreVersion(attrANew, saved.get(0));
        equalsIgnoreVersion(attrBNew, saved.get(1));
    }

    @Test
    public void testDummyRequestWithEmptyResult() throws Exception {
        var future = attributesService.find(new TenantId(UUID.randomUUID()), new DeviceId(UUID.randomUUID()), AttributeScope.SERVER_SCOPE, "TEST");
        Assert.assertNotNull(future);
        var result = future.get(10, TimeUnit.SECONDS);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testConcurrentFetchAndUpdate() throws Exception {
        var tenantId = new TenantId(UUID.randomUUID());
        ListeningExecutorService pool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(2));
        try {
            for (int i = 0; i < 100; i++) {
                var deviceId = new DeviceId(UUID.randomUUID());
                testConcurrentFetchAndUpdate(tenantId, deviceId, pool);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    public void testConcurrentFetchAndUpdateMulti() throws Exception {
        var tenantId = new TenantId(UUID.randomUUID());
        ListeningExecutorService pool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(2));
        try {
            for (int i = 0; i < 100; i++) {
                var deviceId = new DeviceId(UUID.randomUUID());
                testConcurrentFetchAndUpdateMulti(tenantId, deviceId, pool);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    public void testFetchAndUpdateEmpty() throws Exception {
        var tenantId = new TenantId(UUID.randomUUID());
        var deviceId = new DeviceId(UUID.randomUUID());
        var scope = AttributeScope.SERVER_SCOPE;
        var key = "TEST";

        Optional<AttributeKvEntry> emptyValue = attributesService.find(tenantId, deviceId, scope, key).get(10, TimeUnit.SECONDS);
        Assert.assertTrue(emptyValue.isEmpty());

        saveAttribute(tenantId, deviceId, scope, key, NEW_VALUE);
        Assert.assertEquals(NEW_VALUE, getAttributeValue(tenantId, deviceId, scope, key));
    }

    @Test
    public void testFetchAndUpdateMulti() throws Exception {
        var tenantId = new TenantId(UUID.randomUUID());
        var deviceId = new DeviceId(UUID.randomUUID());
        var scope = AttributeScope.SERVER_SCOPE;
        var key1 = "TEST1";
        var key2 = "TEST2";

        var value = getAttributeValues(tenantId, deviceId, scope, Arrays.asList(key1, key2));
        Assert.assertTrue(value.isEmpty());

        saveAttribute(tenantId, deviceId, scope, key1, OLD_VALUE);

        value = getAttributeValues(tenantId, deviceId, scope, Arrays.asList(key1, key2));
        Assert.assertEquals(1, value.size());
        Assert.assertEquals(OLD_VALUE, value.get(0));

        saveAttribute(tenantId, deviceId, scope, key2, NEW_VALUE);

        value = getAttributeValues(tenantId, deviceId, scope, Arrays.asList(key1, key2));
        Assert.assertEquals(2, value.size());
        Assert.assertTrue(value.contains(OLD_VALUE));
        Assert.assertTrue(value.contains(NEW_VALUE));

        saveAttribute(tenantId, deviceId, scope, key1, NEW_VALUE);

        value = getAttributeValues(tenantId, deviceId, scope, Arrays.asList(key1, key2));
        Assert.assertEquals(2, value.size());
        Assert.assertEquals(NEW_VALUE, value.get(0));
        Assert.assertEquals(NEW_VALUE, value.get(1));
    }

    @Test
    public void testFindAllKeysByEntityId() {
        var tenantId = new TenantId(UUID.randomUUID());
        var deviceId = new DeviceId(UUID.randomUUID());
        saveAttribute(tenantId, deviceId, AttributeScope.SERVER_SCOPE, "key1", "123");
        saveAttribute(tenantId, deviceId, AttributeScope.SERVER_SCOPE, "key2", "123");

        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            List<String> keys = attributesService.findAllKeysByEntityIds(tenantId, List.of(deviceId));
            assertThat(keys).containsOnly("key1", "key2");
        });
    }

    @Test
    public void testFindAllKeysByEntityIdAndAttributeType() {
        var tenantId = new TenantId(UUID.randomUUID());
        var deviceId = new DeviceId(UUID.randomUUID());
        saveAttribute(tenantId, deviceId, AttributeScope.SERVER_SCOPE, "key1", "123");
        saveAttribute(tenantId, deviceId, AttributeScope.SERVER_SCOPE, "key2", "123");

        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            List<String> keys = attributesService.findAllKeysByEntityIds(tenantId, List.of(deviceId), AttributeScope.SERVER_SCOPE);
            assertThat(keys).containsOnly("key1", "key2");
        });
    }

    @Test
    public void testFindAllByEntityIdAndAttributeType() {
        var tenantId = new TenantId(UUID.randomUUID());
        var deviceId = new DeviceId(UUID.randomUUID());
        saveAttribute(tenantId, deviceId, AttributeScope.SERVER_SCOPE, "key1", "123");
        saveAttribute(tenantId, deviceId, AttributeScope.SERVER_SCOPE, "key2", "123");

        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            List<AttributeKvEntry> attributes = attributesService.findAll(tenantId, deviceId, AttributeScope.SERVER_SCOPE).get();
            assertThat(attributes).extracting(KvEntry::getKey).containsOnly("key1", "key2");
        });
    }

    private void testConcurrentFetchAndUpdate(TenantId tenantId, DeviceId deviceId, ListeningExecutorService pool) throws Exception {
        var scope = AttributeScope.SERVER_SCOPE;
        var key = "TEST";
        saveAttribute(tenantId, deviceId, scope, key, OLD_VALUE);
        List<ListenableFuture<?>> futures = new ArrayList<>();
        futures.add(pool.submit(() -> {
            var value = getAttributeValue(tenantId, deviceId, scope, key);
            Assert.assertTrue(value.equals(OLD_VALUE) || value.equals(NEW_VALUE));
        }));
        futures.add(pool.submit(() -> saveAttribute(tenantId, deviceId, scope, key, NEW_VALUE)));
        Futures.allAsList(futures).get(10, TimeUnit.SECONDS);

        String attributeValue = getAttributeValue(tenantId, deviceId, scope, key);
        if (!NEW_VALUE.equals(attributeValue)) {
            System.out.println();
        }
        Assert.assertEquals(NEW_VALUE, getAttributeValue(tenantId, deviceId, scope, key));
    }

    private void testConcurrentFetchAndUpdateMulti(TenantId tenantId, DeviceId deviceId, ListeningExecutorService pool) throws Exception {
        var scope = AttributeScope.SERVER_SCOPE;
        var key1 = "TEST1";
        var key2 = "TEST2";
        saveAttribute(tenantId, deviceId, scope, key1, OLD_VALUE);
        saveAttribute(tenantId, deviceId, scope, key2, OLD_VALUE);
        List<ListenableFuture<?>> futures = new ArrayList<>();
        futures.add(pool.submit(() -> {
            var value = getAttributeValues(tenantId, deviceId, scope, Arrays.asList(key1, key2));
            Assert.assertEquals(2, value.size());
            Assert.assertTrue(value.contains(OLD_VALUE) || value.contains(NEW_VALUE));
        }));
        futures.add(pool.submit(() -> {
            saveAttribute(tenantId, deviceId, scope, key1, NEW_VALUE);
            saveAttribute(tenantId, deviceId, scope, key2, NEW_VALUE);
        }));
        Futures.allAsList(futures).get(10, TimeUnit.SECONDS);
        var newResult = getAttributeValues(tenantId, deviceId, scope, Arrays.asList(key1, key2));
        Assert.assertEquals(2, newResult.size());
        Assert.assertEquals(NEW_VALUE, newResult.get(0));
        Assert.assertEquals(NEW_VALUE, newResult.get(1));
    }

    private String getAttributeValue(TenantId tenantId, DeviceId deviceId, AttributeScope scope, String key) {
        try {
            Optional<AttributeKvEntry> entry = attributesService.find(tenantId, deviceId, scope, key).get(10, TimeUnit.SECONDS);
            return entry.orElseThrow(RuntimeException::new).getStrValue().orElse("Unknown");
        } catch (Exception e) {
            log.warn("Failed to get attribute", e.getCause());
            throw new RuntimeException(e);
        }
    }

    private List<String> getAttributeValues(TenantId tenantId, DeviceId deviceId, AttributeScope scope, List<String> keys) {
        try {
            List<AttributeKvEntry> entry = attributesService.find(tenantId, deviceId, scope, keys).get(10, TimeUnit.SECONDS);
            return entry.stream().map(e -> e.getStrValue().orElse(null)).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to get attributes", e.getCause());
            throw new RuntimeException(e);
        }
    }

    private void saveAttribute(TenantId tenantId, DeviceId deviceId, AttributeScope scope, String key, String s) {
        try {
            AttributeKvEntry newEntry = new BaseAttributeKvEntry(System.currentTimeMillis(), new StringDataEntry(key, s));
            attributesService.save(tenantId, deviceId, scope, Collections.singletonList(newEntry)).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to save attribute", e.getCause());
            Assert.assertNull(e);
        }
    }

    private void equalsIgnoreVersion(AttributeKvEntry expected, AttributeKvEntry actual) {
        Assert.assertEquals(expected.getKey(), actual.getKey());
        Assert.assertEquals(expected.getValue(), actual.getValue());
        Assert.assertEquals(expected.getLastUpdateTs(), actual.getLastUpdateTs());
    }

}
