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
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.EntityTypeFilter;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.NumericFilterPredicate;
import org.thingsboard.server.common.data.query.StringFilterPredicate;
import org.thingsboard.server.edqs.util.RepositoryUtils;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class EntityTypeFilterTest extends AbstractEDQTest {

    private Device device;
    private Device device2;
    private Device device3;

    @Before
    public void setUp() {
        device = buildDevice("LoRa-1");
        device2 = buildDevice("LoRa-2");
        device3 = buildDevice("Parking-Sensor-1");
        addOrUpdate(EntityType.DEVICE, device);
        addOrUpdate(EntityType.DEVICE, device2);
        addOrUpdate(EntityType.DEVICE, device3);
        addOrUpdate(new LatestTsKv(device.getId(), new BasicTsKvEntry(43, new StringDataEntry("state", "enabled")), 0L));
        addOrUpdate(new LatestTsKv(device2.getId(), new BasicTsKvEntry(43, new StringDataEntry("state", "disabled")), 0L));
        addOrUpdate(new LatestTsKv(device3.getId(), new BasicTsKvEntry(43, new BooleanDataEntry("free", true)), 0L));

        addOrUpdate(new LatestTsKv(device.getId(), new BasicTsKvEntry(43, new StringDataEntry("temperature", "26.0")), 0L));
        addOrUpdate(new LatestTsKv(device2.getId(), new BasicTsKvEntry(43, new DoubleDataEntry("temperature", 25.0)), 0L));
        addOrUpdate(new LatestTsKv(device3.getId(), new BasicTsKvEntry(43, new DoubleDataEntry("temperature", 19.0)), 0L));
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testFindTenantDeviceEntities() {
        // find all tenant devices
        var result = repository.findEntityDataByQuery(tenantId, null, getEntityTypeQuery(EntityType.DEVICE,  null), false);

        Assert.assertEquals(3, result.getTotalElements());
        var first = result.getData().stream().filter(queryResult -> queryResult.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue().equals("LoRa-1")).findAny();
        assertThat(first).isPresent();
        assertThat(first.get().getEntityId()).isEqualTo(device.getId());
        assertThat(first.get().getLatest().get(EntityKeyType.ENTITY_FIELD).get("createdTime").getValue()).isEqualTo(String.valueOf(device.getCreatedTime()));
        assertThat(first.get().getLatest().get(EntityKeyType.TIME_SERIES).get("state").getValue()).isEqualTo("enabled");

        // find all tenant devices with filter by name
        KeyFilter keyFilter = getDeviceNameKeyFilter(StringFilterPredicate.StringOperation.CONTAINS, "Lora", true);
        result = repository.findEntityDataByQuery(tenantId, null, getEntityTypeQuery(EntityType.DEVICE,  List.of(keyFilter)), false);
        Assert.assertEquals(2, result.getTotalElements());

        // find asset entities
        result = repository.findEntityDataByQuery(tenantId, null, getEntityTypeQuery(EntityType.ASSET,  null),  false);
        Assert.assertEquals(0, result.getTotalElements());

        // find all tenant devices with filter by temperature
        KeyFilter tempFilter = getTemperatureFilter(NumericFilterPredicate.NumericOperation.GREATER_OR_EQUAL, 20.0);
        result = repository.findEntityDataByQuery(tenantId, null, getEntityTypeQuery(EntityType.DEVICE,  List.of(tempFilter)), false);
        Assert.assertEquals(2, result.getTotalElements());
    }

    @Test
    public void testFindCustomerDeviceEntities() {
        var result = repository.findEntityDataByQuery(tenantId, customerId, getEntityTypeQuery(EntityType.DEVICE,  null),  false);
        Assert.assertEquals(0, result.getTotalElements());

        device.setCustomerId(customerId);
        addOrUpdate(EntityType.DEVICE, device);

        result = repository.findEntityDataByQuery(tenantId, customerId, getEntityTypeQuery(EntityType.DEVICE,  null), false);

        Assert.assertEquals(1, result.getTotalElements());
        var first = result.getData().get(0);
        Assert.assertEquals(device.getId(), first.getEntityId());
        Assert.assertEquals("LoRa-1", first.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue());
        Assert.assertEquals("42", first.getLatest().get(EntityKeyType.ENTITY_FIELD).get("createdTime").getValue());
        Assert.assertEquals("enabled", first.getLatest().get(EntityKeyType.TIME_SERIES).get("state").getValue());

        result = repository.findEntityDataByQuery(tenantId, customerId, getEntityTypeQuery(EntityType.ASSET,  null), false);
        Assert.assertEquals(0, result.getTotalElements());
    }

    private Device buildDevice(String name) {
        Device device = new Device();
        device.setId(new DeviceId(UUID.randomUUID()));
        device.setTenantId(tenantId);
        device.setName(name);
        device.setCreatedTime(42L);
        device.setDeviceProfileId(new DeviceProfileId(defaultDeviceProfileId));
        return device;
    }

    private static EntityDataQuery getEntityTypeQuery(EntityType entityType, List<KeyFilter> keyFilters) {
        EntityTypeFilter filter = new EntityTypeFilter();
        filter.setEntityType(entityType);
        var pageLink = new EntityDataPageLink(20, 0, null, new EntityDataSortOrder(new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.DESC), false);

        var entityFields = Arrays.asList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"), new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"));
        var latestValues = Arrays.asList(new EntityKey(EntityKeyType.TIME_SERIES, "state"));

        return new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFilters);
    }

    private static KeyFilter getDeviceNameKeyFilter(StringFilterPredicate.StringOperation operation, String predicateValue, boolean ignoreCase) {
        KeyFilter nameFilter = new KeyFilter();
        nameFilter.setKey(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));
        var predicate = new StringFilterPredicate();
        predicate.setIgnoreCase(ignoreCase);
        predicate.setOperation(operation);
        predicate.setValue(new FilterPredicateValue<>(predicateValue));
        nameFilter.setPredicate(predicate);
        nameFilter.setValueType(EntityKeyValueType.STRING);
        return nameFilter;
    }

    private static KeyFilter getTemperatureFilter(NumericFilterPredicate.NumericOperation operation, Double predicateValue) {
        KeyFilter tempFilter = new KeyFilter();
        tempFilter.setKey(new EntityKey(EntityKeyType.TIME_SERIES, "temperature"));
        var predicate = new NumericFilterPredicate();
        predicate.setOperation(operation);
        predicate.setValue(new FilterPredicateValue<>(predicateValue));
        tempFilter.setPredicate(predicate);
        tempFilter.setValueType(EntityKeyValueType.NUMERIC);
        return tempFilter;
    }

}
