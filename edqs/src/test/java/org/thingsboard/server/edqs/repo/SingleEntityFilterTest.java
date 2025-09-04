/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.edqs.repo;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edqs.LatestTsKv;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.query.AliasEntityId;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.query.StringFilterPredicate;

import java.util.Arrays;
import java.util.UUID;

public class SingleEntityFilterTest extends AbstractEDQTest {

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testFindTenantDevice() {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        Device device = new Device();
        device.setId(deviceId);
        device.setTenantId(tenantId);
        device.setName("LoRa-1");
        device.setCreatedTime(42L);
        device.setDeviceProfileId(new DeviceProfileId(defaultDeviceProfileId));
        addOrUpdate(EntityType.DEVICE, device);
        addOrUpdate(new LatestTsKv(deviceId, new BasicTsKvEntry(43, new StringDataEntry("state", "TEST")), 0L));

        var result = repository.findEntityDataByQuery(tenantId, null, getEntityDataQuery(device.getId()), false);

        Assert.assertEquals(1, result.getTotalElements());
        var first = result.getData().get(0);
        Assert.assertEquals(deviceId, first.getEntityId());
        Assert.assertEquals("LoRa-1", first.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue());
        Assert.assertEquals("42", first.getLatest().get(EntityKeyType.ENTITY_FIELD).get("createdTime").getValue());

        result = repository.findEntityDataByQuery(tenantId, null, getEntityDataQuery(new DeviceId(UUID.randomUUID())), false);
        Assert.assertEquals(0, result.getTotalElements());

        device.setCustomerId(customerId);
        addOrUpdate(EntityType.DEVICE, device);

        result = repository.findEntityDataByQuery(tenantId, null, getEntityDataQuery(device.getId()), false);
        Assert.assertEquals(1, result.getTotalElements());
        first = result.getData().get(0);
        Assert.assertEquals(deviceId, first.getEntityId());
        Assert.assertEquals("LoRa-1", first.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue());
        Assert.assertEquals("42", first.getLatest().get(EntityKeyType.ENTITY_FIELD).get("createdTime").getValue());
    }

    @Test
    public void testFindCustomerDevice() {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        Device device = new Device();
        device.setId(deviceId);
        device.setTenantId(tenantId);
        device.setName("LoRa-1");
        device.setCreatedTime(42L);
        device.setDeviceProfileId(new DeviceProfileId(defaultDeviceProfileId));
        addOrUpdate(EntityType.DEVICE, device);
        addOrUpdate(new LatestTsKv(deviceId, new BasicTsKvEntry(43, new StringDataEntry("state", "TEST")), 0L));

        var result = repository.findEntityDataByQuery(tenantId, customerId, getEntityDataQuery(device.getId()), false);
        Assert.assertEquals(0, result.getTotalElements());

        device.setCustomerId(customerId);
        addOrUpdate(EntityType.DEVICE, device);

        result = repository.findEntityDataByQuery(tenantId, customerId, getEntityDataQuery(device.getId()), false);

        Assert.assertEquals(1, result.getTotalElements());
        var first = result.getData().get(0);
        Assert.assertEquals(deviceId, first.getEntityId());
        Assert.assertEquals("LoRa-1", first.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue());
        Assert.assertEquals("42", first.getLatest().get(EntityKeyType.ENTITY_FIELD).get("createdTime").getValue());
    }

    private static EntityDataQuery getEntityDataQuery(DeviceId deviceId) {
        SingleEntityFilter filter = new SingleEntityFilter();
        filter.setSingleEntity(AliasEntityId.fromEntityId(deviceId));
        var pageLink = new EntityDataPageLink(20, 0, null, new EntityDataSortOrder(new EntityKey(EntityKeyType.TIME_SERIES, "state"), EntityDataSortOrder.Direction.DESC), false);

        var entityFields = Arrays.asList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"), new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"));
        var latestValues = Arrays.asList(new EntityKey(EntityKeyType.TIME_SERIES, "state"));
        KeyFilter nameFilter = new KeyFilter();
        nameFilter.setKey(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));
        var predicate = new StringFilterPredicate();
        predicate.setIgnoreCase(false);
        predicate.setOperation(StringFilterPredicate.StringOperation.CONTAINS);
        predicate.setValue(new FilterPredicateValue<>("LoRa-"));
        nameFilter.setPredicate(predicate);
        nameFilter.setValueType(EntityKeyValueType.STRING);

        return new EntityDataQuery(filter, pageLink, entityFields, latestValues, Arrays.asList(nameFilter));
    }

}
