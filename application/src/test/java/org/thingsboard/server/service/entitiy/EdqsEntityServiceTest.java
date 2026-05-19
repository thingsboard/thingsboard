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
package org.thingsboard.server.service.entitiy;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.kv.TimeseriesSaveResult;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.DeviceTypeFilter;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.RelationsQueryFilter;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.msg.edqs.EdqsService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.edqs.util.EdqsRocksDb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.thingsboard.server.common.data.query.EntityKeyType.ENTITY_FIELD;

@DaoSqlTest
@TestPropertySource(properties = {
        "queue.edqs.sync.enabled=true",
        "queue.edqs.api.supported=true",
        "queue.edqs.api.auto_enable=true",
        "queue.edqs.mode=local",
        "queue.edqs.readiness_check_interval=1000"
})
public class EdqsEntityServiceTest extends EntityServiceTest {

    @Autowired
    private EdqsService edqsService;

    @MockBean
    private EdqsRocksDb edqsRocksDb;

    @Before
    public void beforeEach() {
        await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> edqsService.isApiEnabled());
    }

    // sql implementation has a bug with data duplication, edqs implementation returns correct value
    @Override
    @Test
    public void testCountHierarchicalEntitiesByMultiRootQuery() throws InterruptedException {
        List<Asset> buildings = new ArrayList<>();
        List<Asset> apartments = new ArrayList<>();
        Map<String, Map<UUID, String>> entityNameByTypeMap = new HashMap<>();
        Map<UUID, UUID> childParentRelationMap = new HashMap<>();
        createMultiRootHierarchy(buildings, apartments, entityNameByTypeMap, childParentRelationMap);

        RelationsQueryFilter filter = new RelationsQueryFilter();
        filter.setMultiRoot(true);
        filter.setMultiRootEntitiesType(EntityType.ASSET);
        filter.setMultiRootEntityIds(buildings.stream().map(IdBased::getId).map(d -> d.getId().toString()).collect(Collectors.toSet()));
        filter.setDirection(EntitySearchDirection.FROM);

        EntityCountQuery countQuery = new EntityCountQuery(filter);
        countByQueryAndCheck(countQuery, 63);

        filter.setFilters(Collections.singletonList(new RelationEntityTypeFilter("AptToHeat", Collections.singletonList(EntityType.DEVICE))));
        countByQueryAndCheck(countQuery, 27);

        filter.setMultiRootEntitiesType(EntityType.ASSET);
        filter.setMultiRootEntityIds(apartments.stream().map(IdBased::getId).map(d -> d.getId().toString()).collect(Collectors.toSet()));
        filter.setDirection(EntitySearchDirection.TO);
        filter.setFilters(Lists.newArrayList(
                new RelationEntityTypeFilter("buildingToApt", Collections.singletonList(EntityType.ASSET)),
                new RelationEntityTypeFilter("AptToEnergy", Collections.singletonList(EntityType.DEVICE))));
        countByQueryAndCheck(countQuery, 3);

        deviceService.deleteDevicesByTenantId(tenantId);
        assetService.deleteAssetsByTenantId(tenantId);
    }

    // edqs has no nulls order strategies, always returns NULLs first for ASC and NULLs last for DESC
    @Override
    @Test
    public void testSortByNumericTelemetryKeyWithDifferentNullsOrderStrategy() throws ExecutionException, InterruptedException {
        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device" + i);
            device.setType("default");
            devices.add(deviceService.saveDevice(device));
            Thread.sleep(1);
        }

        List<Long> values = List.of(1L, 0L, 0L);
        List<ListenableFuture<TimeseriesSaveResult>> timeseriesFutures = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            timeseriesFutures.add(saveTimeseries(devices.get(i).getId(), "test", values.get(i)));
        }
        Futures.allAsList(timeseriesFutures).get();

        DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceTypes(List.of("default"));
        filter.setDeviceNameFilter("");

        List<EntityKey> entityFields = Collections.singletonList(new EntityKey(ENTITY_FIELD, "name"));
        List<EntityKey> latestValues = Collections.singletonList(new EntityKey(EntityKeyType.TIME_SERIES, "test"));

        EntityDataSortOrder ascSortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.TIME_SERIES, "test"), EntityDataSortOrder.Direction.ASC);
        EntityDataQuery ascQuery = new EntityDataQuery(filter,
                new EntityDataPageLink(10, 0, null, ascSortOrder), entityFields, latestValues, null);
        List<String> ascTelemetry = loadAllData(ascQuery, devices.size()).stream()
                .map(ed -> ed.getLatest().get(EntityKeyType.TIME_SERIES).get("test").getValue())
                .toList();
        assertThat(ascTelemetry).containsExactlyElementsOf(List.of("", "", "0", "0", "1"));

        EntityDataSortOrder descSortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.TIME_SERIES, "test"), EntityDataSortOrder.Direction.DESC);
        EntityDataQuery descQuery = new EntityDataQuery(filter,
                new EntityDataPageLink(10, 0, null, descSortOrder), entityFields, latestValues, null);
        List<String> descTelemetry = loadAllData(descQuery, devices.size()).stream()
                .map(ed -> ed.getLatest().get(EntityKeyType.TIME_SERIES).get("test").getValue())
                .toList();
        assertThat(descTelemetry).containsExactlyElementsOf(List.of("1", "0", "0", "", ""));
    }

    // edqs has no nulls order strategies, always returns NULLs first for ASC and NULLs last for DESC
    @Override
    @Test
    public void testSortByBooleanKeyWithDifferentNullsOrderStrategy() throws ExecutionException, InterruptedException {
        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device" + i);
            device.setType("default");
            devices.add(deviceService.saveDevice(device));
            Thread.sleep(1);
        }

        List<Boolean> values = List.of(true, false, false);
        List<ListenableFuture<TimeseriesSaveResult>> timeseriesFutures = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            timeseriesFutures.add(saveTimeseries(devices.get(i).getId(), "test", values.get(i)));
        }
        Futures.allAsList(timeseriesFutures).get();

        DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceTypes(List.of("default"));
        filter.setDeviceNameFilter("");

        List<EntityKey> entityFields = Collections.singletonList(new EntityKey(ENTITY_FIELD, "name"));
        List<EntityKey> latestValues = Collections.singletonList(new EntityKey(EntityKeyType.TIME_SERIES, "test"));

        EntityDataSortOrder ascSortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.TIME_SERIES, "test"), EntityDataSortOrder.Direction.ASC);
        EntityDataQuery ascQuery = new EntityDataQuery(filter,
                new EntityDataPageLink(10, 0, null, ascSortOrder), entityFields, latestValues, null);
        List<String> ascTelemetry = loadAllData(ascQuery, devices.size()).stream()
                .map(ed -> ed.getLatest().get(EntityKeyType.TIME_SERIES).get("test").getValue())
                .toList();
        assertThat(ascTelemetry).containsExactlyElementsOf(List.of("", "", "false", "false", "true"));

        EntityDataSortOrder descSortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.TIME_SERIES, "test"), EntityDataSortOrder.Direction.DESC);
        EntityDataQuery descQuery = new EntityDataQuery(filter,
                new EntityDataPageLink(10, 0, null, descSortOrder), entityFields, latestValues, null);
        List<String> descTelemetry = loadAllData(descQuery, devices.size()).stream()
                .map(ed -> ed.getLatest().get(EntityKeyType.TIME_SERIES).get("test").getValue())
                .toList();
        assertThat(descTelemetry).containsExactlyElementsOf(List.of("true", "false", "false", "", ""));
    }

    @Override
    protected PageData<EntityData> findByQueryAndCheck(CustomerId customerId, EntityDataQuery query, long expectedResultSize) {
        return await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> findByQuery(customerId, query),
                result -> result.getTotalElements() == expectedResultSize);
    }

    @Override
    protected List<EntityData> findByQueryAndCheckTelemetry(EntityDataQuery query, EntityKeyType entityKeyType, String key, List<String> expectedTelemetries) {
        return await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> loadAllData(query, expectedTelemetries.size()),
                loadedEntities -> loadedEntities.stream().map(entityData -> entityData.getLatest().get(entityKeyType).get(key).getValue()).toList().containsAll(expectedTelemetries));
    }

    @Override
    protected long countByQueryAndCheck(EntityCountQuery countQuery, int expectedResult) {
        return countByQueryAndCheck(new CustomerId(CustomerId.NULL_UUID), countQuery, expectedResult);
    }

    @Override
    protected long countByQueryAndCheck(CustomerId customerId, EntityCountQuery query, int expectedResult) {
        return await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> countByQuery(customerId, query),
                result -> result == expectedResult);
    }

}
