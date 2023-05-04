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
package org.thingsboard.server.dao.service;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AssetSearchQueryFilter;
import org.thingsboard.server.common.data.query.AssetTypeFilter;
import org.thingsboard.server.common.data.query.DeviceSearchQueryFilter;
import org.thingsboard.server.common.data.query.DeviceTypeFilter;
import org.thingsboard.server.common.data.query.EdgeSearchQueryFilter;
import org.thingsboard.server.common.data.query.EdgeTypeFilter;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityListFilter;
import org.thingsboard.server.common.data.query.EntityNameFilter;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.NumericFilterPredicate;
import org.thingsboard.server.common.data.query.RelationsQueryFilter;
import org.thingsboard.server.common.data.query.StringFilterPredicate;
import org.thingsboard.server.common.data.query.StringFilterPredicate.StringOperation;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.model.sqlts.ts.TsKvEntity;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.sql.relation.RelationRepository;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

@Slf4j
@DaoSqlTest
public class BaseEntityServiceTest extends AbstractServiceTest {

    static final int ENTITY_COUNT = 5;

    @Autowired
    AssetService assetService;
    @Autowired
    AttributesService attributesService;
    @Autowired
    DeviceService deviceService;
    @Autowired
    EdgeService edgeService;
    @Autowired
    EntityService entityService;
    @Autowired
    RelationRepository relationRepository;
    @Autowired
    RelationService relationService;
    @Autowired
    TimeseriesService timeseriesService;

    @Test
    public void testCountEntitiesByQuery() throws InterruptedException {
        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 97; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            devices.add(deviceService.saveDevice(device));
        }

        DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceTypes(List.of("default"));
        filter.setDeviceNameFilter("");

        EntityCountQuery countQuery = new EntityCountQuery(filter);

        long count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(97, count);

        filter.setDeviceTypes(List.of("unknown"));
        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(0, count);

        filter.setDeviceTypes(List.of("default"));
        filter.setDeviceNameFilter("Device1");
        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(11, count);

        EntityListFilter entityListFilter = new EntityListFilter();
        entityListFilter.setEntityType(EntityType.DEVICE);
        entityListFilter.setEntityList(devices.stream().map(Device::getId).map(DeviceId::toString).collect(Collectors.toList()));

        countQuery = new EntityCountQuery(entityListFilter);
        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(97, count);

        deviceService.deleteDevicesByTenantId(tenantId);
        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(0, count);
    }


    @Test
    public void testCountHierarchicalEntitiesByQuery() throws InterruptedException {
        List<Asset> assets = new ArrayList<>();
        List<Device> devices = new ArrayList<>();
        createTestHierarchy(tenantId, assets, devices, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

        RelationsQueryFilter filter = new RelationsQueryFilter();
        filter.setRootEntity(tenantId);
        filter.setDirection(EntitySearchDirection.FROM);

        EntityCountQuery countQuery = new EntityCountQuery(filter);

        long count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(31, count); //due to the loop relations in hierarchy, the TenantId included in total count (1*Tenant + 5*Asset + 5*5*Devices = 31)

        filter.setFilters(Collections.singletonList(new RelationEntityTypeFilter("Contains", Collections.singletonList(EntityType.DEVICE))));
        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(25, count);

        filter.setRootEntity(devices.get(0).getId());
        filter.setDirection(EntitySearchDirection.TO);
        filter.setFilters(Collections.singletonList(new RelationEntityTypeFilter("Manages", Collections.singletonList(EntityType.TENANT))));
        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(1, count);

        DeviceSearchQueryFilter filter2 = new DeviceSearchQueryFilter();
        filter2.setRootEntity(tenantId);
        filter2.setDirection(EntitySearchDirection.FROM);
        filter2.setRelationType("Contains");

        countQuery = new EntityCountQuery(filter2);

        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(25, count);

        filter2.setDeviceTypes(Arrays.asList("default0", "default1"));
        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(10, count);

        filter2.setRootEntity(devices.get(0).getId());
        filter2.setDirection(EntitySearchDirection.TO);
        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(0, count);

        AssetSearchQueryFilter filter3 = new AssetSearchQueryFilter();
        filter3.setRootEntity(tenantId);
        filter3.setDirection(EntitySearchDirection.FROM);
        filter3.setRelationType("Manages");

        countQuery = new EntityCountQuery(filter3);

        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(5, count);

        filter3.setAssetTypes(Arrays.asList("type0", "type1"));
        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(2, count);

        filter3.setRootEntity(devices.get(0).getId());
        filter3.setDirection(EntitySearchDirection.TO);
        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(0, count);
    }

    @Test
    public void testCountEdgeEntitiesByQuery() throws InterruptedException {
        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < 97; i++) {
            Edge edge = createEdge(i, "default");
            edges.add(edgeService.saveEdge(edge));
        }

        EdgeTypeFilter filter = new EdgeTypeFilter();
        filter.setEdgeTypes(List.of("default"));
        filter.setEdgeNameFilter("");

        EntityCountQuery countQuery = new EntityCountQuery(filter);

        long count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(97, count);

        filter.setEdgeTypes(List.of("unknown"));
        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(0, count);

        filter.setEdgeTypes(List.of("default"));
        filter.setEdgeNameFilter("Edge1");
        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(11, count);

        EntityListFilter entityListFilter = new EntityListFilter();
        entityListFilter.setEntityType(EntityType.EDGE);
        entityListFilter.setEntityList(edges.stream().map(Edge::getId).map(EdgeId::toString).collect(Collectors.toList()));

        countQuery = new EntityCountQuery(entityListFilter);
        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(97, count);

        edgeService.deleteEdgesByTenantId(tenantId);
        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(0, count);
    }

    @Test
    public void testCountHierarchicalEntitiesByEdgeSearchQuery() throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            Edge edge = createEdge(i, "type" + i);
            edge = edgeService.saveEdge(edge);
            //TO make sure devices have different created time
            Thread.sleep(1);

            EntityRelation er = new EntityRelation();
            er.setFrom(tenantId);
            er.setTo(edge.getId());
            er.setType("Manages");
            er.setTypeGroup(RelationTypeGroup.COMMON);
            relationService.saveRelation(tenantId, er);
        }

        EdgeSearchQueryFilter filter = new EdgeSearchQueryFilter();
        filter.setRootEntity(tenantId);
        filter.setDirection(EntitySearchDirection.FROM);
        filter.setRelationType("Manages");

        EntityCountQuery countQuery = new EntityCountQuery(filter);

        long count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(5, count);

        filter.setEdgeTypes(Arrays.asList("type0", "type1"));
        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(2, count);
    }

    private Edge createEdge(int i, String type) {
        Edge edge = new Edge();
        edge.setTenantId(tenantId);
        edge.setName("Edge" + i);
        edge.setType(type);
        edge.setLabel("EdgeLabel" + i);
        edge.setSecret(StringUtils.randomAlphanumeric(20));
        edge.setRoutingKey(StringUtils.randomAlphanumeric(20));
        return edge;
    }

    @Test
    public void testHierarchicalFindEntityDataWithAttributesByQuery() throws ExecutionException, InterruptedException {
        doTestHierarchicalFindEntityDataWithAttributesByQuery(0, false);
    }

    @Test
    public void testHierarchicalFindEntityDataWithAttributesByQueryWithLevel() throws ExecutionException, InterruptedException {
        doTestHierarchicalFindEntityDataWithAttributesByQuery(2, false);
    }

    @Test
    public void testHierarchicalFindEntityDataWithAttributesByQueryWithLastLevelOnly() throws ExecutionException, InterruptedException {
        doTestHierarchicalFindEntityDataWithAttributesByQuery(2, true);
    }

    private void doTestHierarchicalFindEntityDataWithAttributesByQuery(final int maxLevel, final boolean fetchLastLevelOnly) throws ExecutionException, InterruptedException {
        List<Asset> assets = new ArrayList<>();
        List<Device> devices = new ArrayList<>();
        List<Long> temperatures = new ArrayList<>();
        List<Long> highTemperatures = new ArrayList<>();
        createTestHierarchy(tenantId, assets, devices, new ArrayList<>(), new ArrayList<>(), temperatures, highTemperatures);

        List<ListenableFuture<List<String>>> attributeFutures = new ArrayList<>();
        for (int i = 0; i < devices.size(); i++) {
            Device device = devices.get(i);
            attributeFutures.add(saveLongAttribute(device.getId(), "temperature", temperatures.get(i), DataConstants.CLIENT_SCOPE));
        }
        Futures.allAsList(attributeFutures).get();

        RelationsQueryFilter filter = new RelationsQueryFilter();
        filter.setRootEntity(tenantId);
        filter.setDirection(EntitySearchDirection.FROM);
        filter.setFilters(Collections.singletonList(new RelationEntityTypeFilter("Contains", Collections.singletonList(EntityType.DEVICE))));
        filter.setMaxLevel(maxLevel);
        filter.setFetchLastLevelOnly(fetchLastLevelOnly);

        EntityDataSortOrder sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.ASC
        );
        EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        List<EntityKey> entityFields = Collections.singletonList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));
        List<EntityKey> latestValues = Collections.singletonList(new EntityKey(EntityKeyType.ATTRIBUTE, "temperature"));

        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, null);
        PageData<EntityData> data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        List<EntityData> loadedEntities = new ArrayList<>(data.getData());
        while (data.hasNext()) {
            query = query.next();
            data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
            loadedEntities.addAll(data.getData());
        }
        Assert.assertEquals(25, loadedEntities.size());
        List<String> loadedTemperatures = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ATTRIBUTE).get("temperature").getValue()).collect(Collectors.toList());
        List<String> deviceTemperatures = temperatures.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());
        Assert.assertEquals(deviceTemperatures, loadedTemperatures);

        pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        KeyFilter highTemperatureFilter = new KeyFilter();
        highTemperatureFilter.setKey(new EntityKey(EntityKeyType.ATTRIBUTE, "temperature"));
        NumericFilterPredicate predicate = new NumericFilterPredicate();
        predicate.setValue(FilterPredicateValue.fromDouble(45));
        predicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperatureFilter.setPredicate(predicate);
        List<KeyFilter> keyFilters = Collections.singletonList(highTemperatureFilter);

        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFilters);

        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);

        loadedEntities = new ArrayList<>(data.getData());
        while (data.hasNext()) {
            query = query.next();
            data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
            loadedEntities.addAll(data.getData());
        }
        Assert.assertEquals(highTemperatures.size(), loadedEntities.size());

        List<String> loadedHighTemperatures = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ATTRIBUTE).get("temperature").getValue()).collect(Collectors.toList());
        List<String> deviceHighTemperatures = highTemperatures.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());

        Assert.assertEquals(deviceHighTemperatures, loadedHighTemperatures);

        deviceService.deleteDevicesByTenantId(tenantId);
    }

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

        long count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(63, count);

        filter.setFilters(Collections.singletonList(new RelationEntityTypeFilter("AptToHeat", Collections.singletonList(EntityType.DEVICE))));
        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(27, count);

        filter.setMultiRootEntitiesType(EntityType.ASSET);
        filter.setMultiRootEntityIds(apartments.stream().map(IdBased::getId).map(d -> d.getId().toString()).collect(Collectors.toSet()));
        filter.setDirection(EntitySearchDirection.TO);
        filter.setFilters(Lists.newArrayList(
                new RelationEntityTypeFilter("buildingToApt", Collections.singletonList(EntityType.ASSET)),
                new RelationEntityTypeFilter("AptToEnergy", Collections.singletonList(EntityType.DEVICE))));

        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(9, count);

        deviceService.deleteDevicesByTenantId(tenantId);
        assetService.deleteAssetsByTenantId(tenantId);

    }

    @Test
    public void testMultiRootHierarchicalFindEntityDataWithAttributesByQuery() throws ExecutionException, InterruptedException {
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

        EntityDataSortOrder sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.ASC
        );
        EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        List<EntityKey> entityFields = Lists.newArrayList(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "name"),
                new EntityKey(EntityKeyType.ENTITY_FIELD, "parentId"),
                new EntityKey(EntityKeyType.ENTITY_FIELD, "type")
        );
        List<EntityKey> latestValues = Collections.singletonList(new EntityKey(EntityKeyType.ATTRIBUTE, "status"));

        KeyFilter onlineStatusFilter = new KeyFilter();
        onlineStatusFilter.setKey(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));
        StringFilterPredicate predicate = new StringFilterPredicate();
        predicate.setOperation(StringOperation.ENDS_WITH);
        predicate.setValue(FilterPredicateValue.fromString("_1"));
        onlineStatusFilter.setPredicate(predicate);
        List<KeyFilter> keyFilters = Collections.singletonList(onlineStatusFilter);

        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFilters);
        PageData<EntityData> data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        List<EntityData> loadedEntities = new ArrayList<>(data.getData());
        while (data.hasNext()) {
            query = query.next();
            data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
            loadedEntities.addAll(data.getData());
        }

        long expectedEntitiesCnt = entityNameByTypeMap.entrySet()
                .stream()
                .filter(e -> !e.getKey().equals("building"))
                .flatMap(e -> e.getValue().entrySet().stream())
                .map(Map.Entry::getValue)
                .filter(e -> StringUtils.endsWith(e, "_1"))
                .count();
        Assert.assertEquals(expectedEntitiesCnt, loadedEntities.size());

        Map<UUID, UUID> actualRelations = new HashMap<>();
        loadedEntities.forEach(ed -> {
            UUID parentId = UUID.fromString(ed.getLatest().get(EntityKeyType.ENTITY_FIELD).get("parentId").getValue());
            UUID entityId = ed.getEntityId().getId();
            Assert.assertEquals(childParentRelationMap.get(entityId), parentId);
            actualRelations.put(entityId, parentId);

            String entityType = ed.getLatest().get(EntityKeyType.ENTITY_FIELD).get("type").getValue();
            String actualEntityName = ed.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue();
            String expectedEntityName = entityNameByTypeMap.get(entityType).get(entityId);
            Assert.assertEquals(expectedEntityName, actualEntityName);
        });

        deviceService.deleteDevicesByTenantId(tenantId);
        assetService.deleteAssetsByTenantId(tenantId);
    }

    @Test
    public void testHierarchicalFindDevicesWithAttributesByQuery() throws ExecutionException, InterruptedException {
        List<Asset> assets = new ArrayList<>();
        List<Device> devices = new ArrayList<>();
        List<Long> temperatures = new ArrayList<>();
        List<Long> highTemperatures = new ArrayList<>();
        createTestHierarchy(tenantId, assets, devices, new ArrayList<>(), new ArrayList<>(), temperatures, highTemperatures);

        List<ListenableFuture<List<String>>> attributeFutures = new ArrayList<>();
        for (int i = 0; i < devices.size(); i++) {
            Device device = devices.get(i);
            attributeFutures.add(saveLongAttribute(device.getId(), "temperature", temperatures.get(i), DataConstants.CLIENT_SCOPE));
        }
        Futures.allAsList(attributeFutures).get();

        DeviceSearchQueryFilter filter = new DeviceSearchQueryFilter();
        filter.setRootEntity(tenantId);
        filter.setDirection(EntitySearchDirection.FROM);
        filter.setRelationType("Contains");
        filter.setMaxLevel(2);
        filter.setFetchLastLevelOnly(true);

        EntityDataSortOrder sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.ASC
        );
        EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        List<EntityKey> entityFields = Collections.singletonList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));
        List<EntityKey> latestValues = Collections.singletonList(new EntityKey(EntityKeyType.ATTRIBUTE, "temperature"));

        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, null);
        PageData<EntityData> data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        List<EntityData> loadedEntities = new ArrayList<>(data.getData());
        while (data.hasNext()) {
            query = query.next();
            data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
            loadedEntities.addAll(data.getData());
        }
        Assert.assertEquals(25, loadedEntities.size());
        loadedEntities.forEach(entity -> Assert.assertTrue(devices.stream().map(Device::getId).collect(Collectors.toSet()).contains(entity.getEntityId())));
        List<String> loadedTemperatures = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ATTRIBUTE).get("temperature").getValue()).collect(Collectors.toList());
        List<String> deviceTemperatures = temperatures.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());
        Assert.assertEquals(deviceTemperatures, loadedTemperatures);

        pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        KeyFilter highTemperatureFilter = new KeyFilter();
        highTemperatureFilter.setKey(new EntityKey(EntityKeyType.ATTRIBUTE, "temperature"));
        NumericFilterPredicate predicate = new NumericFilterPredicate();
        predicate.setValue(FilterPredicateValue.fromDouble(45));
        predicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperatureFilter.setPredicate(predicate);
        List<KeyFilter> keyFilters = Collections.singletonList(highTemperatureFilter);

        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFilters);

        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);

        loadedEntities = new ArrayList<>(data.getData());
        while (data.hasNext()) {
            query = query.next();
            data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
            loadedEntities.addAll(data.getData());
        }
        Assert.assertEquals(highTemperatures.size(), loadedEntities.size());

        List<String> loadedHighTemperatures = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ATTRIBUTE).get("temperature").getValue()).collect(Collectors.toList());
        List<String> deviceHighTemperatures = highTemperatures.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());

        Assert.assertEquals(deviceHighTemperatures, loadedHighTemperatures);

        deviceService.deleteDevicesByTenantId(tenantId);
    }


    @Test
    public void testHierarchicalFindAssetsWithAttributesByQuery() throws ExecutionException, InterruptedException {
        List<Asset> assets = new ArrayList<>();
        List<Device> devices = new ArrayList<>();
        List<Long> consumptions = new ArrayList<>();
        List<Long> highConsumptions = new ArrayList<>();
        createTestHierarchy(tenantId, assets, devices, consumptions, highConsumptions, new ArrayList<>(), new ArrayList<>());

        List<ListenableFuture<List<String>>> attributeFutures = new ArrayList<>();
        for (int i = 0; i < assets.size(); i++) {
            Asset asset = assets.get(i);
            attributeFutures.add(saveLongAttribute(asset.getId(), "consumption", consumptions.get(i), DataConstants.SERVER_SCOPE));
        }
        Futures.allAsList(attributeFutures).get();

        AssetSearchQueryFilter filter = new AssetSearchQueryFilter();
        filter.setRootEntity(tenantId);
        filter.setDirection(EntitySearchDirection.FROM);
        filter.setRelationType("Manages");

        EntityDataSortOrder sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.ASC
        );
        EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        List<EntityKey> entityFields = Collections.singletonList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));
        List<EntityKey> latestValues = Collections.singletonList(new EntityKey(EntityKeyType.ATTRIBUTE, "consumption"));

        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, null);
        PageData<EntityData> data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        List<EntityData> loadedEntities = new ArrayList<>(data.getData());
        while (data.hasNext()) {
            query = query.next();
            data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
            loadedEntities.addAll(data.getData());
        }
        Assert.assertEquals(5, loadedEntities.size());
        List<String> loadedTemperatures = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ATTRIBUTE).get("consumption").getValue()).collect(Collectors.toList());
        List<String> deviceTemperatures = consumptions.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());
        Assert.assertEquals(deviceTemperatures, loadedTemperatures);

        pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        KeyFilter highTemperatureFilter = new KeyFilter();
        highTemperatureFilter.setKey(new EntityKey(EntityKeyType.ATTRIBUTE, "consumption"));
        NumericFilterPredicate predicate = new NumericFilterPredicate();
        predicate.setValue(FilterPredicateValue.fromDouble(50));
        predicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperatureFilter.setPredicate(predicate);
        List<KeyFilter> keyFilters = Collections.singletonList(highTemperatureFilter);

        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFilters);

        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);

        loadedEntities = new ArrayList<>(data.getData());
        while (data.hasNext()) {
            query = query.next();
            data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
            loadedEntities.addAll(data.getData());
        }
        Assert.assertEquals(highConsumptions.size(), loadedEntities.size());

        List<String> loadedHighTemperatures = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ATTRIBUTE).get("consumption").getValue()).collect(Collectors.toList());
        List<String> deviceHighTemperatures = highConsumptions.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());

        Assert.assertEquals(deviceHighTemperatures, loadedHighTemperatures);

        deviceService.deleteDevicesByTenantId(tenantId);
    }

    private void createTestHierarchy(TenantId tenantId, List<Asset> assets, List<Device> devices, List<Long> consumptions, List<Long> highConsumptions, List<Long> temperatures, List<Long> highTemperatures) throws InterruptedException {
        for (int i = 0; i < ENTITY_COUNT; i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            asset.setName("Asset" + i);
            asset.setType("type" + i);
            asset.setLabel("AssetLabel" + i);
            asset = assetService.saveAsset(asset);
            //TO make sure devices have different created time
            Thread.sleep(1);
            assets.add(asset);
            createRelation(tenantId, "Manages", tenantId, asset.getId());
            long consumption = (long) (Math.random() * 100);
            consumptions.add(consumption);
            if (consumption > 50) {
                highConsumptions.add(consumption);
            }

            //tenant -> asset : one-to-one but many edges
            for (int n = 0; n < ENTITY_COUNT; n++) {
                createRelation(tenantId, "UseCase-" + n, tenantId, asset.getId());
            }

            for (int j = 0; j < ENTITY_COUNT; j++) {
                Device device = new Device();
                device.setTenantId(tenantId);
                device.setName("A" + i + "Device" + j);
                device.setType("default" + j);
                device.setLabel("testLabel" + (int) (Math.random() * 1000));
                device = deviceService.saveDevice(device);
                //TO make sure devices have different created time
                Thread.sleep(1);
                devices.add(device);
                createRelation(tenantId, "Contains", asset.getId(), device.getId());
                long temperature = (long) (Math.random() * 100);
                temperatures.add(temperature);
                if (temperature > 45) {
                    highTemperatures.add(temperature);
                }

                //asset -> device : one-to-one but many edges
                for (int n = 0; n < ENTITY_COUNT; n++) {
                    createRelation(tenantId, "UseCase-" + n, asset.getId(), device.getId());
                }
            }
        }

        //asset -> device one-to-many shared with other assets
        for (int n = 0; n < devices.size(); n = n + ENTITY_COUNT) {
            createRelation(tenantId, "SharedWithAsset0", assets.get(0).getId(), devices.get(n).getId());
        }

        createManyCustomRelationsBetweenTwoNodes(tenantId, "UseCase", assets, devices);
        createHorizontalRingRelations(tenantId, "Ring(Loop)-Ast", assets);
        createLoopRelations(tenantId, "Loop-Tnt-Ast-Dev", tenantId, assets.get(0).getId(), devices.get(0).getId());
        createLoopRelations(tenantId, "Loop-Tnt-Ast", tenantId, assets.get(1).getId());
        createLoopRelations(tenantId, "Loop-Ast-Tnt-Ast", assets.get(2).getId(), tenantId, assets.get(3).getId());

        //printAllRelations();
    }

    private ResultSetExtractor<List<List<String>>> getListResultSetExtractor() {
        return rs -> {
            List<List<String>> list = new ArrayList<>();
            final int columnCount = rs.getMetaData().getColumnCount();
            List<String> columns = new ArrayList<>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                columns.add(rs.getMetaData().getColumnName(i));
            }
            list.add(columns);
            while (rs.next()) {
                List<String> data = new ArrayList<>(columnCount);
                for (int i = 1; i <= columnCount; i++) {
                    data.add(rs.getString(i));
                }
                list.add(data);
            }
            return list;
        };
    }

    /*
     * This useful to reproduce exact data in the PostgreSQL and play around with pgadmin query and analyze tool
     * */
    private void printAllRelations() {
        System.out.println("" +
                "DO\n" +
                "$$\n" +
                "    DECLARE\n" +
                "        someint integer;\n" +
                "    BEGIN\n" +
                "        DROP TABLE IF EXISTS relation_test;\n" +
                "        CREATE TABLE IF NOT EXISTS relation_test\n" +
                "        (\n" +
                "            from_id             uuid,\n" +
                "            from_type           varchar(255),\n" +
                "            to_id               uuid,\n" +
                "            to_type             varchar(255),\n" +
                "            relation_type_group varchar(255),\n" +
                "            relation_type       varchar(255),\n" +
                "            additional_info     varchar,\n" +
                "            CONSTRAINT relation_test_pkey PRIMARY KEY (from_id, from_type, relation_type_group, relation_type, to_id, to_type)\n" +
                "        );");

        relationRepository.findAll().forEach(r ->
                System.out.printf("INSERT INTO relation_test (from_id, from_type, to_id, to_type, relation_type_group, relation_type, additional_info)" +
                                " VALUES (%s, %s, %s, %s, %s, %s, %s);\n",
                        quote(r.getFromId()), quote(r.getFromType()), quote(r.getToId()), quote(r.getToType()),
                        quote(r.getRelationTypeGroup()), quote(r.getRelationType()), quote(r.getAdditionalInfo()))
        );

        System.out.println("" +
                "    END\n" +
                "$$;");
    }

    private String quote(Object s) {
        return s == null ? null : "'" + s + "'";
    }

    void createLoopRelations(TenantId tenantId, String type, EntityId... ids) {
        assertThat("ids lenght", ids.length, Matchers.greaterThanOrEqualTo(1));
        //chain all from the head to the tail
        for (int i = 1; i < ids.length; i++) {
            relationService.saveRelation(tenantId, new EntityRelation(ids[i - 1], ids[i], type, RelationTypeGroup.COMMON));
        }
        //chain tail -> head
        relationService.saveRelation(tenantId, new EntityRelation(ids[ids.length - 1], ids[0], type, RelationTypeGroup.COMMON));
    }

    void createHorizontalRingRelations(TenantId tenantId, String type, List<Asset> assets) {
        createLoopRelations(tenantId, type, assets.stream().map(Asset::getId).toArray(EntityId[]::new));
    }

    void createManyCustomRelationsBetweenTwoNodes(TenantId tenantId, String type, List<Asset> assets, List<Device> devices) {
        for (int i = 1; i <= 5; i++) {
            final String typeI = type + i;
            createOneToManyRelations(tenantId, typeI, tenantId, assets.stream().map(Asset::getId).collect(Collectors.toList()));
            assets.forEach(asset ->
                    createOneToManyRelations(tenantId, typeI, asset.getId(), devices.stream().map(Device::getId).collect(Collectors.toList())));
        }
    }

    void createOneToManyRelations(TenantId tenantId, String type, EntityId from, List<EntityId> toIds) {
        toIds.forEach(toId -> createRelation(tenantId, type, from, toId));
    }

    void createRelation(TenantId tenantId, String type, EntityId from, EntityId toId) {
        relationService.saveRelation(tenantId, new EntityRelation(from, toId, type, RelationTypeGroup.COMMON));
    }


    @Test
    public void testSimpleFindEntityDataByQuery() throws InterruptedException {
        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 97; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            //TO make sure devices have different created time
            Thread.sleep(1);
            devices.add(deviceService.saveDevice(device));
        }

        DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceTypes(List.of("default"));
        filter.setDeviceNameFilter("");

        EntityDataSortOrder sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.ASC
        );
        EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        List<EntityKey> entityFields = Collections.singletonList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));

        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, null, null);
        PageData<EntityData> data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);

        Assert.assertEquals(97, data.getTotalElements());
        Assert.assertEquals(10, data.getTotalPages());
        Assert.assertTrue(data.hasNext());
        Assert.assertEquals(10, data.getData().size());

        List<EntityData> loadedEntities = new ArrayList<>(data.getData());
        while (data.hasNext()) {
            query = query.next();
            data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
            loadedEntities.addAll(data.getData());
        }
        Assert.assertEquals(97, loadedEntities.size());

        List<EntityId> loadedIds = loadedEntities.stream().map(EntityData::getEntityId).collect(Collectors.toList());
        List<EntityId> deviceIds = devices.stream().map(Device::getId).collect(Collectors.toList());
        deviceIds.sort(Comparator.comparing(EntityId::getId));
        loadedIds.sort(Comparator.comparing(EntityId::getId));
        Assert.assertEquals(deviceIds, loadedIds);

        List<String> loadedNames = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue()).collect(Collectors.toList());
        List<String> deviceNames = devices.stream().map(Device::getName).collect(Collectors.toList());

        Collections.sort(loadedNames);
        Collections.sort(deviceNames);
        Assert.assertEquals(deviceNames, loadedNames);

        sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "name"), EntityDataSortOrder.Direction.DESC
        );

        pageLink = new EntityDataPageLink(10, 0, "device1", sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, null, null);
        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        Assert.assertEquals(11, data.getTotalElements());
        Assert.assertEquals("Device19", data.getData().get(0).getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue());

        deviceService.deleteDevicesByTenantId(tenantId);
    }

    @Test
    public void testFindEntityDataByQuery_operationEqual_emptySearchQuery() {
        List<Device> devices = createMockDevices(10);
        devices.get(0).setLabel("");
        devices.get(1).setLabel(null);
        devices.forEach(deviceService::saveDevice);

        String searchQuery = "";
        EntityDataQuery query = createDeviceSearchQuery("label", StringOperation.EQUAL, searchQuery);

        PageData<EntityData> result = searchEntities(query);
        assertEquals(devices.size(), result.getTotalElements());
    }

    @Test
    public void testFindEntityDataByQuery_operationNotEqual() {
        List<Device> devices = createMockDevices(10);
        devices.get(0).setLabel("");
        devices.get(1).setLabel(null);
        devices.forEach(deviceService::saveDevice);

        String searchQuery = devices.get(2).getLabel();
        EntityDataQuery query = createDeviceSearchQuery("label", StringOperation.NOT_EQUAL, searchQuery);

        PageData<EntityData> result = searchEntities(query);
        assertEquals(devices.size() - 1, result.getTotalElements());
    }

    @Test
    public void testFindEntityDataByQuery_operationNotEqual_emptySearchQuery() {
        List<Device> devices = createMockDevices(10);
        devices.get(0).setLabel("");
        devices.get(1).setLabel(null);
        devices.forEach(deviceService::saveDevice);

        String searchQuery = "";
        EntityDataQuery query = createDeviceSearchQuery("label", StringOperation.NOT_EQUAL, searchQuery);

        PageData<EntityData> result = searchEntities(query);
        assertEquals(devices.size(), result.getTotalElements());
    }

    @Test
    public void testFindEntityDataByQuery_operationStartsWith_emptySearchQuery() {
        List<Device> devices = createMockDevices(10);
        devices.get(0).setLabel("");
        devices.get(1).setLabel(null);
        devices.forEach(deviceService::saveDevice);

        String searchQuery = "";
        EntityDataQuery query = createDeviceSearchQuery("label", StringOperation.STARTS_WITH, searchQuery);

        PageData<EntityData> result = searchEntities(query);
        assertEquals(devices.size(), result.getTotalElements());
    }

    @Test
    public void testFindEntityDataByQuery_operationEndsWith_emptySearchQuery() {
        List<Device> devices = createMockDevices(10);
        devices.get(0).setLabel("");
        devices.get(1).setLabel(null);
        devices.forEach(deviceService::saveDevice);

        String searchQuery = "";
        EntityDataQuery query = createDeviceSearchQuery("label", StringOperation.ENDS_WITH, searchQuery);

        PageData<EntityData> result = searchEntities(query);
        assertEquals(devices.size(), result.getTotalElements());
    }

    @Test
    public void testFindEntityDataByQuery_operationContains_emptySearchQuery() {
        List<Device> devices = createMockDevices(10);
        devices.get(0).setLabel("");
        devices.get(1).setLabel(null);
        devices.forEach(deviceService::saveDevice);

        String searchQuery = "";
        EntityDataQuery query = createDeviceSearchQuery("label", StringOperation.CONTAINS, searchQuery);

        PageData<EntityData> result = searchEntities(query);
        assertEquals(devices.size(), result.getTotalElements());
    }

    @Test
    public void testFindEntityDataByQuery_operationNotContains() {
        List<Device> devices = createMockDevices(10);
        devices.get(0).setLabel("");
        devices.get(1).setLabel(null);
        devices.forEach(deviceService::saveDevice);

        String searchQuery = "label-";
        EntityDataQuery query = createDeviceSearchQuery("label", StringOperation.NOT_CONTAINS, searchQuery);

        PageData<EntityData> result = searchEntities(query);
        assertEquals(2, result.getTotalElements());
    }

    @Test
    public void testFindEntityDataByQuery_operationNotContains_emptySearchQuery() {
        List<Device> devices = createMockDevices(10);
        devices.get(0).setLabel("");
        devices.get(1).setLabel(null);
        devices.forEach(deviceService::saveDevice);

        String searchQuery = "";
        EntityDataQuery query = createDeviceSearchQuery("label", StringOperation.NOT_CONTAINS, searchQuery);

        PageData<EntityData> result = searchEntities(query);
        assertEquals(devices.size(), result.getTotalElements());
    }

    @Test
    public void testFindEntityDataByQuery_filter_entity_name_starts_with() {
        List<Device> devices = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device " + i + " test");
            device.setType("default");
            devices.add(device);
        }

        devices.forEach(deviceService::saveDevice);

        EntityNameFilter deviceTypeFilter = new EntityNameFilter();
        deviceTypeFilter.setEntityType(EntityType.DEVICE);
        deviceTypeFilter.setEntityNameFilter("Device");

        EntityDataPageLink pageLink = new EntityDataPageLink(1000, 0, null, null);

        EntityDataQuery query = new EntityDataQuery(deviceTypeFilter, pageLink, null, null, null);

        PageData<EntityData> result = searchEntities(query);
        assertEquals(devices.size(), result.getTotalElements());

        deviceTypeFilter.setEntityNameFilter("Device%");

        result = searchEntities(query);
        assertEquals(devices.size(), result.getTotalElements());

        deviceTypeFilter.setEntityNameFilter("%Device%");

        result = searchEntities(query);
        assertEquals(devices.size(), result.getTotalElements());

        deviceTypeFilter.setEntityNameFilter("%Device");

        result = searchEntities(query);
        assertEquals(0, result.getTotalElements());
    }

    @Test
    public void testFindEntityDataByQuery_filter_entity_name_ends_with() {
        List<Device> devices = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device " + i + " test");
            device.setType("default");
            devices.add(device);
        }

        devices.forEach(deviceService::saveDevice);

        EntityNameFilter deviceTypeFilter = new EntityNameFilter();
        deviceTypeFilter.setEntityType(EntityType.DEVICE);
        deviceTypeFilter.setEntityNameFilter("%test");

        EntityDataPageLink pageLink = new EntityDataPageLink(1000, 0, null, null);

        EntityDataQuery query = new EntityDataQuery(deviceTypeFilter, pageLink, null, null, null);

        PageData<EntityData> result = searchEntities(query);
        assertEquals(devices.size(), result.getTotalElements());

        deviceTypeFilter.setEntityNameFilter("%test%");

        result = searchEntities(query);
        assertEquals(devices.size(), result.getTotalElements());

        deviceTypeFilter.setEntityNameFilter("test%");

        result = searchEntities(query);
        assertEquals(0, result.getTotalElements());

        deviceTypeFilter.setEntityNameFilter("test");

        result = searchEntities(query);
        assertEquals(0, result.getTotalElements());
    }

    @Test
    public void testFindEntityDataByQuery_filter_entity_name_contains() {
        List<Device> devices = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device test" + i);
            device.setType("default");
            devices.add(device);
        }

        devices.forEach(deviceService::saveDevice);

        EntityNameFilter deviceTypeFilter = new EntityNameFilter();
        deviceTypeFilter.setEntityType(EntityType.DEVICE);
        deviceTypeFilter.setEntityNameFilter("%test%");

        EntityDataPageLink pageLink = new EntityDataPageLink(1000, 0, null, null);

        EntityDataQuery query = new EntityDataQuery(deviceTypeFilter, pageLink, null, null, null);

        PageData<EntityData> result = searchEntities(query);
        assertEquals(devices.size(), result.getTotalElements());

        deviceTypeFilter.setEntityNameFilter("test%");

        result = searchEntities(query);
        assertEquals(0, result.getTotalElements());

        deviceTypeFilter.setEntityNameFilter("%test");

        result = searchEntities(query);
        assertEquals(0, result.getTotalElements());
    }

    @Test
    public void testFindEntityDataByQuery_filter_device_type_name_starts_with() {
        List<Device> devices = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device " + i + " test");
            device.setType("default");
            devices.add(device);
        }

        devices.forEach(deviceService::saveDevice);

        DeviceTypeFilter deviceTypeFilter = new DeviceTypeFilter();
        deviceTypeFilter.setDeviceType("default");
        deviceTypeFilter.setDeviceNameFilter("Device");

        EntityDataPageLink pageLink = new EntityDataPageLink(1000, 0, null, null);

        EntityDataQuery query = new EntityDataQuery(deviceTypeFilter, pageLink, null, null, null);

        PageData<EntityData> result = searchEntities(query);
        assertEquals(devices.size(), result.getTotalElements());

        deviceTypeFilter.setDeviceNameFilter("Device%");

        result = searchEntities(query);
        assertEquals(devices.size(), result.getTotalElements());

        deviceTypeFilter.setDeviceNameFilter("%Device%");

        result = searchEntities(query);
        assertEquals(devices.size(), result.getTotalElements());

        deviceTypeFilter.setDeviceNameFilter("%Device");

        result = searchEntities(query);
        assertEquals(0, result.getTotalElements());
    }

    @Test
    public void testFindEntityDataByQuery_filter_device_type_name_ends_with() {
        List<Device> devices = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device " + i + " test");
            device.setType("default");
            devices.add(device);
        }

        devices.forEach(deviceService::saveDevice);

        DeviceTypeFilter deviceTypeFilter = new DeviceTypeFilter();
        deviceTypeFilter.setDeviceType("default");
        deviceTypeFilter.setDeviceNameFilter("%test");

        EntityDataPageLink pageLink = new EntityDataPageLink(1000, 0, null, null);

        EntityDataQuery query = new EntityDataQuery(deviceTypeFilter, pageLink, null, null, null);

        PageData<EntityData> result = searchEntities(query);
        assertEquals(devices.size(), result.getTotalElements());

        deviceTypeFilter.setDeviceNameFilter("%test%");

        result = searchEntities(query);
        assertEquals(devices.size(), result.getTotalElements());

        deviceTypeFilter.setDeviceNameFilter("test%");

        result = searchEntities(query);
        assertEquals(0, result.getTotalElements());

        deviceTypeFilter.setDeviceNameFilter("test");

        result = searchEntities(query);
        assertEquals(0, result.getTotalElements());
    }

    @Test
    public void testFindEntityDataByQuery_filter_device_type_name_contains() {
        List<Device> devices = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device test" + i);
            device.setType("default");
            devices.add(device);
        }

        devices.forEach(deviceService::saveDevice);

        DeviceTypeFilter deviceTypeFilter = new DeviceTypeFilter();
        deviceTypeFilter.setDeviceType("default");
        deviceTypeFilter.setDeviceNameFilter("%test%");

        EntityDataPageLink pageLink = new EntityDataPageLink(1000, 0, null, null);

        EntityDataQuery query = new EntityDataQuery(deviceTypeFilter, pageLink, null, null, null);

        PageData<EntityData> result = searchEntities(query);
        assertEquals(devices.size(), result.getTotalElements());

        deviceTypeFilter.setDeviceNameFilter("test%");

        result = searchEntities(query);
        assertEquals(0, result.getTotalElements());

        deviceTypeFilter.setDeviceNameFilter("%test");

        result = searchEntities(query);
        assertEquals(0, result.getTotalElements());
    }

    @Test
    public void testFindEntityDataByQuery_filter_asset_type_name_starts_with() {
        List<Asset> assets = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            asset.setName("Asset " + i + " test");
            asset.setType("default");
            assets.add(asset);
        }

        assets.forEach(assetService::saveAsset);

        AssetTypeFilter assetTypeFilter = new AssetTypeFilter();
        assetTypeFilter.setAssetType("default");
        assetTypeFilter.setAssetNameFilter("Asset");

        EntityDataPageLink pageLink = new EntityDataPageLink(1000, 0, null, null);

        EntityDataQuery query = new EntityDataQuery(assetTypeFilter, pageLink, null, null, null);

        PageData<EntityData> result = searchEntities(query);
        assertEquals(assets.size(), result.getTotalElements());

        assetTypeFilter.setAssetNameFilter("Asset%");

        result = searchEntities(query);
        assertEquals(assets.size(), result.getTotalElements());

        assetTypeFilter.setAssetNameFilter("%Asset%");

        result = searchEntities(query);
        assertEquals(assets.size(), result.getTotalElements());

        assetTypeFilter.setAssetNameFilter("%Asset");

        result = searchEntities(query);
        assertEquals(0, result.getTotalElements());
    }

    @Test
    public void testFindEntityDataByQuery_filter_asset_type_name_ends_with() {
        List<Asset> assets = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            asset.setName("Asset " + i + " test");
            asset.setType("default");
            assets.add(asset);
        }

        assets.forEach(assetService::saveAsset);

        AssetTypeFilter assetTypeFilter = new AssetTypeFilter();
        assetTypeFilter.setAssetType("default");
        assetTypeFilter.setAssetNameFilter("%test");

        EntityDataPageLink pageLink = new EntityDataPageLink(1000, 0, null, null);

        EntityDataQuery query = new EntityDataQuery(assetTypeFilter, pageLink, null, null, null);

        PageData<EntityData> result = searchEntities(query);
        assertEquals(assets.size(), result.getTotalElements());

        assetTypeFilter.setAssetNameFilter("%test%");

        result = searchEntities(query);
        assertEquals(assets.size(), result.getTotalElements());

        assetTypeFilter.setAssetNameFilter("test%");

        result = searchEntities(query);
        assertEquals(0, result.getTotalElements());

        assetTypeFilter.setAssetNameFilter("test");

        result = searchEntities(query);
        assertEquals(0, result.getTotalElements());
    }

    @Test
    public void testFindEntityDataByQuery_filter_asset_type_name_contains() {
        List<Asset> assets = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            asset.setName("Asset test" + i);
            asset.setType("default");
            assets.add(asset);
        }

        assets.forEach(assetService::saveAsset);

        AssetTypeFilter assetTypeFilter = new AssetTypeFilter();
        assetTypeFilter.setAssetType("default");
        assetTypeFilter.setAssetNameFilter("%test%");

        EntityDataPageLink pageLink = new EntityDataPageLink(1000, 0, null, null);

        EntityDataQuery query = new EntityDataQuery(assetTypeFilter, pageLink, null, null, null);

        PageData<EntityData> result = searchEntities(query);
        assertEquals(assets.size(), result.getTotalElements());

        assetTypeFilter.setAssetNameFilter("test%");

        result = searchEntities(query);
        assertEquals(0, result.getTotalElements());

        assetTypeFilter.setAssetNameFilter("%test");

        result = searchEntities(query);
        assertEquals(0, result.getTotalElements());
    }

    private PageData<EntityData> searchEntities(EntityDataQuery query) {
        return entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
    }

    private EntityDataQuery createDeviceSearchQuery(String deviceField, StringOperation operation, String searchQuery) {
        DeviceTypeFilter deviceTypeFilter = new DeviceTypeFilter();
        deviceTypeFilter.setDeviceTypes(List.of("default"));
        deviceTypeFilter.setDeviceNameFilter("");

        EntityDataSortOrder sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.ASC
        );
        EntityDataPageLink pageLink = new EntityDataPageLink(1000, 0, null, sortOrder);
        List<EntityKey> entityFields = Arrays.asList(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "name"),
                new EntityKey(EntityKeyType.ENTITY_FIELD, "label")
        );

        List<KeyFilter> keyFilters = createStringKeyFilters(deviceField, EntityKeyType.ENTITY_FIELD, operation, searchQuery);

        return new EntityDataQuery(deviceTypeFilter, pageLink, entityFields, null, keyFilters);
    }

    private List<Device> createMockDevices(int count) {
        return Stream.iterate(1, i -> i + 1)
                .map(i -> {
                    Device device = new Device();
                    device.setTenantId(tenantId);
                    device.setName("Device " + i);
                    device.setType("default");
                    device.setLabel("label-" + RandomUtils.nextInt(100, 10000));
                    return device;
                })
                .limit(count)
                .collect(Collectors.toList());
    }

    @Test
    public void testFindEntityDataByQueryWithAttributes() throws ExecutionException, InterruptedException {

        List<EntityKeyType> attributesEntityTypes = new ArrayList<>(Arrays.asList(EntityKeyType.CLIENT_ATTRIBUTE, EntityKeyType.SHARED_ATTRIBUTE, EntityKeyType.SERVER_ATTRIBUTE));

        List<Device> devices = new ArrayList<>();
        List<Long> temperatures = new ArrayList<>();
        List<Long> highTemperatures = new ArrayList<>();
        for (int i = 0; i < 67; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            devices.add(deviceService.saveDevice(device));
            //TO make sure devices have different created time
            Thread.sleep(1);
            long temperature = (long) (Math.random() * 100);
            temperatures.add(temperature);
            if (temperature > 45) {
                highTemperatures.add(temperature);
            }
        }

        List<ListenableFuture<List<String>>> attributeFutures = new ArrayList<>();
        for (int i = 0; i < devices.size(); i++) {
            Device device = devices.get(i);
            for (String currentScope : DataConstants.allScopes()) {
                attributeFutures.add(saveLongAttribute(device.getId(), "temperature", temperatures.get(i), currentScope));
            }
        }
        Futures.allAsList(attributeFutures).get();

        DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceTypes(List.of("default"));
        filter.setDeviceNameFilter("");

        EntityDataSortOrder sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.ASC
        );
        EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        List<EntityKey> entityFields = Collections.singletonList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));
        for (EntityKeyType currentAttributeKeyType : attributesEntityTypes) {
            List<EntityKey> latestValues = Collections.singletonList(new EntityKey(currentAttributeKeyType, "temperature"));
            EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, null);
            PageData<EntityData> data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
            List<EntityData> loadedEntities = new ArrayList<>(data.getData());
            while (data.hasNext()) {
                query = query.next();
                data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
                loadedEntities.addAll(data.getData());
            }
            Assert.assertEquals(67, loadedEntities.size());
            List<String> loadedTemperatures = new ArrayList<>();
            for (Device device : devices) {
                loadedTemperatures.add(loadedEntities.stream().filter(entityData -> entityData.getEntityId().equals(device.getId())).findFirst().orElse(null)
                        .getLatest().get(currentAttributeKeyType).get("temperature").getValue());
            }
            List<String> deviceTemperatures = temperatures.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());
            Assert.assertEquals(deviceTemperatures, loadedTemperatures);

            pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
            KeyFilter highTemperatureFilter = createNumericKeyFilter("temperature", currentAttributeKeyType, NumericFilterPredicate.NumericOperation.GREATER, 45);
            List<KeyFilter> keyFiltersHighTemperature = Collections.singletonList(highTemperatureFilter);

            query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFiltersHighTemperature);

            data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);

            loadedEntities = new ArrayList<>(data.getData());

            while (data.hasNext()) {
                query = query.next();
                data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
                loadedEntities.addAll(data.getData());
            }
            Assert.assertEquals(highTemperatures.size(), loadedEntities.size());

            List<String> loadedHighTemperatures = loadedEntities.stream().map(entityData ->
                    entityData.getLatest().get(currentAttributeKeyType).get("temperature").getValue()).collect(Collectors.toList());
            List<String> deviceHighTemperatures = highTemperatures.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());

            Assert.assertEquals(deviceHighTemperatures, loadedHighTemperatures);

        }
        deviceService.deleteDevicesByTenantId(tenantId);
    }

    @Test
    public void testBuildNumericPredicateQueryOperations() throws ExecutionException, InterruptedException {

        List<Device> devices = new ArrayList<>();
        List<Long> temperatures = new ArrayList<>();
        List<Long> equalTemperatures = new ArrayList<>();
        List<Long> notEqualTemperatures = new ArrayList<>();
        List<Long> greaterTemperatures = new ArrayList<>();
        List<Long> greaterOrEqualTemperatures = new ArrayList<>();
        List<Long> lessTemperatures = new ArrayList<>();
        List<Long> lessOrEqualTemperatures = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            devices.add(deviceService.saveDevice(device));
            //TO make sure devices have different created time
            Thread.sleep(1);
            long temperature = (long) (Math.random() * 100);
            temperatures.add(temperature);
            if (temperature == 45) {
                greaterOrEqualTemperatures.add(temperature);
                lessOrEqualTemperatures.add(temperature);
                equalTemperatures.add(temperature);
            } else if (temperature > 45) {
                greaterTemperatures.add(temperature);
                greaterOrEqualTemperatures.add(temperature);
                notEqualTemperatures.add(temperature);
            } else {
                lessTemperatures.add(temperature);
                lessOrEqualTemperatures.add(temperature);
                notEqualTemperatures.add(temperature);
            }
        }

        List<ListenableFuture<List<String>>> attributeFutures = new ArrayList<>();
        for (int i = 0; i < devices.size(); i++) {
            Device device = devices.get(i);
            attributeFutures.add(saveLongAttribute(device.getId(), "temperature", temperatures.get(i), DataConstants.CLIENT_SCOPE));
        }
        Futures.allAsList(attributeFutures).get();

        DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceTypes(List.of("default"));
        filter.setDeviceNameFilter("");

        EntityDataSortOrder sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.ASC
        );

        List<EntityKey> entityFields = Collections.singletonList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));
        List<EntityKey> latestValues = Collections.singletonList(new EntityKey(EntityKeyType.CLIENT_ATTRIBUTE, "temperature"));

        KeyFilter greaterTemperatureFilter = createNumericKeyFilter("temperature", EntityKeyType.CLIENT_ATTRIBUTE, NumericFilterPredicate.NumericOperation.GREATER, 45);
        List<KeyFilter> keyFiltersGreaterTemperature = Collections.singletonList(greaterTemperatureFilter);

        KeyFilter greaterOrEqualTemperatureFilter = createNumericKeyFilter("temperature", EntityKeyType.CLIENT_ATTRIBUTE, NumericFilterPredicate.NumericOperation.GREATER_OR_EQUAL, 45);
        List<KeyFilter> keyFiltersGreaterOrEqualTemperature = Collections.singletonList(greaterOrEqualTemperatureFilter);

        KeyFilter lessTemperatureFilter = createNumericKeyFilter("temperature", EntityKeyType.CLIENT_ATTRIBUTE, NumericFilterPredicate.NumericOperation.LESS, 45);
        List<KeyFilter> keyFiltersLessTemperature = Collections.singletonList(lessTemperatureFilter);

        KeyFilter lessOrEqualTemperatureFilter = createNumericKeyFilter("temperature", EntityKeyType.CLIENT_ATTRIBUTE, NumericFilterPredicate.NumericOperation.LESS_OR_EQUAL, 45);
        List<KeyFilter> keyFiltersLessOrEqualTemperature = Collections.singletonList(lessOrEqualTemperatureFilter);

        KeyFilter equalTemperatureFilter = createNumericKeyFilter("temperature", EntityKeyType.CLIENT_ATTRIBUTE, NumericFilterPredicate.NumericOperation.EQUAL, 45);
        List<KeyFilter> keyFiltersEqualTemperature = Collections.singletonList(equalTemperatureFilter);

        KeyFilter notEqualTemperatureFilter = createNumericKeyFilter("temperature", EntityKeyType.CLIENT_ATTRIBUTE, NumericFilterPredicate.NumericOperation.NOT_EQUAL, 45);
        List<KeyFilter> keyFiltersNotEqualTemperature = Collections.singletonList(notEqualTemperatureFilter);

        //Greater Operation

        EntityDataPageLink pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFiltersGreaterTemperature);
        PageData<EntityData> data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        List<EntityData> loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(greaterTemperatures.size(), loadedEntities.size());

        List<String> loadedTemperatures = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE).get("temperature").getValue()).collect(Collectors.toList());
        List<String> deviceTemperatures = greaterTemperatures.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());

        Assert.assertEquals(deviceTemperatures, loadedTemperatures);

        //Greater or equal Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFiltersGreaterOrEqualTemperature);
        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(greaterOrEqualTemperatures.size(), loadedEntities.size());

        loadedTemperatures = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE).get("temperature").getValue()).collect(Collectors.toList());
        deviceTemperatures = greaterOrEqualTemperatures.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());

        Assert.assertEquals(deviceTemperatures, loadedTemperatures);

        //Less Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFiltersLessTemperature);
        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(lessTemperatures.size(), loadedEntities.size());

        loadedTemperatures = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE).get("temperature").getValue()).collect(Collectors.toList());
        deviceTemperatures = lessTemperatures.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());

        Assert.assertEquals(deviceTemperatures, loadedTemperatures);

        //Less or equal Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFiltersLessOrEqualTemperature);
        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(lessOrEqualTemperatures.size(), loadedEntities.size());

        loadedTemperatures = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE).get("temperature").getValue()).collect(Collectors.toList());
        deviceTemperatures = lessOrEqualTemperatures.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());

        Assert.assertEquals(deviceTemperatures, loadedTemperatures);

        //Equal Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFiltersEqualTemperature);
        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(equalTemperatures.size(), loadedEntities.size());

        loadedTemperatures = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE).get("temperature").getValue()).collect(Collectors.toList());
        deviceTemperatures = equalTemperatures.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());

        Assert.assertEquals(deviceTemperatures, loadedTemperatures);

        //Not equal Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFiltersNotEqualTemperature);
        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(notEqualTemperatures.size(), loadedEntities.size());

        loadedTemperatures = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE).get("temperature").getValue()).collect(Collectors.toList());
        deviceTemperatures = notEqualTemperatures.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());

        Assert.assertEquals(deviceTemperatures, loadedTemperatures);


        deviceService.deleteDevicesByTenantId(tenantId);
    }

    @Test
    public void testFindEntityDataByQueryWithTimeseries() throws ExecutionException, InterruptedException {

        List<Device> devices = new ArrayList<>();
        List<Double> temperatures = new ArrayList<>();
        List<Double> highTemperatures = new ArrayList<>();
        for (int i = 0; i < 67; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            devices.add(deviceService.saveDevice(device));
            //TO make sure devices have different created time
            Thread.sleep(1);
            double temperature = (double) (Math.random() * 100.0);
            temperatures.add(temperature);
            if (temperature > 45.0) {
                highTemperatures.add(temperature);
            }
        }

        List<ListenableFuture<Integer>> timeseriesFutures = new ArrayList<>();
        for (int i = 0; i < devices.size(); i++) {
            Device device = devices.get(i);
            timeseriesFutures.add(saveLongTimeseries(device.getId(), "temperature", temperatures.get(i)));
        }
        Futures.allAsList(timeseriesFutures).get();

        DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceTypes(List.of("default"));
        filter.setDeviceNameFilter("");

        EntityDataSortOrder sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.ASC
        );
        EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        List<EntityKey> entityFields = Collections.singletonList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));
        List<EntityKey> latestValues = Collections.singletonList(new EntityKey(EntityKeyType.TIME_SERIES, "temperature"));

        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, null);
        PageData<EntityData> data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);

        List<EntityData> loadedEntities = new ArrayList<>(data.getData());
        while (data.hasNext()) {
            query = query.next();
            data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
            loadedEntities.addAll(data.getData());
        }
        Assert.assertEquals(67, loadedEntities.size());
        List<String> loadedTemperatures = new ArrayList<>();
        for (Device device : devices) {
            loadedTemperatures.add(loadedEntities.stream().filter(entityData -> entityData.getEntityId().equals(device.getId())).findFirst().orElse(null)
                    .getLatest().get(EntityKeyType.TIME_SERIES).get("temperature").getValue());
        }
        List<String> deviceTemperatures = temperatures.stream().map(aDouble -> Double.toString(aDouble)).collect(Collectors.toList());

        Assert.assertEquals(deviceTemperatures, loadedTemperatures);

        pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        KeyFilter highTemperatureFilter = new KeyFilter();
        highTemperatureFilter.setKey(new EntityKey(EntityKeyType.TIME_SERIES, "temperature"));
        NumericFilterPredicate predicate = new NumericFilterPredicate();
        predicate.setValue(FilterPredicateValue.fromDouble(45));
        predicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperatureFilter.setPredicate(predicate);
        List<KeyFilter> keyFilters = Collections.singletonList(highTemperatureFilter);

        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFilters);

        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);

        loadedEntities = new ArrayList<>(data.getData());
        while (data.hasNext()) {
            query = query.next();
            data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
            loadedEntities.addAll(data.getData());
        }
        Assert.assertEquals(highTemperatures.size(), loadedEntities.size());

        List<String> loadedHighTemperatures = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.TIME_SERIES).get("temperature").getValue()).collect(Collectors.toList());
        List<String> deviceHighTemperatures = highTemperatures.stream().map(aDouble -> Double.toString(aDouble)).collect(Collectors.toList());

        Assert.assertEquals(deviceHighTemperatures, loadedHighTemperatures);

        deviceService.deleteDevicesByTenantId(tenantId);
    }

    @Test
    public void testBuildStringPredicateQueryOperations() throws ExecutionException, InterruptedException {

        List<Device> devices = new ArrayList<>();
        List<String> attributeStrings = new ArrayList<>();
        List<String> equalStrings = new ArrayList<>();
        List<String> notEqualStrings = new ArrayList<>();
        List<String> startsWithStrings = new ArrayList<>();
        List<String> endsWithStrings = new ArrayList<>();
        List<String> containsStrings = new ArrayList<>();
        List<String> notContainsStrings = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            devices.add(deviceService.saveDevice(device));
            //TO make sure devices have different created time
            Thread.sleep(1);
            List<StringFilterPredicate.StringOperation> operationValues = Arrays.asList(StringFilterPredicate.StringOperation.values());
            StringFilterPredicate.StringOperation operation = operationValues.get(new Random().nextInt(operationValues.size()));
            String operationName = operation.name();
            attributeStrings.add(operationName);
            switch (operation) {
                case EQUAL:
                    equalStrings.add(operationName);
                    notContainsStrings.add(operationName);
                    notEqualStrings.add(operationName);
                    break;
                case NOT_EQUAL:
                    notContainsStrings.add(operationName);
                    break;
                case STARTS_WITH:
                    notEqualStrings.add(operationName);
                    startsWithStrings.add(operationName);
                    endsWithStrings.add(operationName);
                    notContainsStrings.add(operationName);
                    break;
                case ENDS_WITH:
                    notEqualStrings.add(operationName);
                    endsWithStrings.add(operationName);
                    notContainsStrings.add(operationName);
                    break;
                case CONTAINS:
                    notEqualStrings.add(operationName);
                    notContainsStrings.add(operationName);
                    containsStrings.add(operationName);
                    break;
                case NOT_CONTAINS:
                    notEqualStrings.add(operationName);
                    containsStrings.add(operationName);
                    break;
                case IN:
                    notEqualStrings.add(operationName);
                    notContainsStrings.add(operationName);
                    break;
                case NOT_IN:
                    notEqualStrings.add(operationName);
                    notContainsStrings.add(operationName);
                    break;
            }
        }

        List<ListenableFuture<List<String>>> attributeFutures = new ArrayList<>();
        for (int i = 0; i < devices.size(); i++) {
            Device device = devices.get(i);
            attributeFutures.add(saveStringAttribute(device.getId(), "attributeString", attributeStrings.get(i), DataConstants.CLIENT_SCOPE));
        }
        Futures.allAsList(attributeFutures).get();

        DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceTypes(List.of("default"));
        filter.setDeviceNameFilter("");

        EntityDataSortOrder sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.DESC
        );

        List<EntityKey> entityFields = Arrays.asList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"),
                new EntityKey(EntityKeyType.ENTITY_FIELD, "entityType"));

        List<EntityKey> latestValues = Collections.singletonList(new EntityKey(EntityKeyType.CLIENT_ATTRIBUTE, "attributeString"));

        List<KeyFilter> keyFiltersEqualString = createStringKeyFilters("attributeString", EntityKeyType.CLIENT_ATTRIBUTE, StringFilterPredicate.StringOperation.EQUAL, "equal");

        List<KeyFilter> keyFiltersNotEqualString = createStringKeyFilters("attributeString", EntityKeyType.CLIENT_ATTRIBUTE, StringFilterPredicate.StringOperation.NOT_EQUAL, "NOT_EQUAL");

        List<KeyFilter> keyFiltersStartsWithString = createStringKeyFilters("attributeString", EntityKeyType.CLIENT_ATTRIBUTE, StringFilterPredicate.StringOperation.STARTS_WITH, "starts_");

        List<KeyFilter> keyFiltersEndsWithString = createStringKeyFilters("attributeString", EntityKeyType.CLIENT_ATTRIBUTE, StringFilterPredicate.StringOperation.ENDS_WITH, "_WITH");

        List<KeyFilter> keyFiltersContainsString = createStringKeyFilters("attributeString", EntityKeyType.CLIENT_ATTRIBUTE, StringFilterPredicate.StringOperation.CONTAINS, "contains");

        List<KeyFilter> keyFiltersNotContainsString = createStringKeyFilters("attributeString", EntityKeyType.CLIENT_ATTRIBUTE, StringFilterPredicate.StringOperation.NOT_CONTAINS, "NOT_CONTAINS");

        List<KeyFilter> deviceTypeFilters = createStringKeyFilters("entityType", EntityKeyType.ENTITY_FIELD, StringFilterPredicate.StringOperation.NOT_EQUAL, "NOT_EQUAL");

        // Equal Operation

        EntityDataPageLink pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFiltersEqualString);
        PageData<EntityData> data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        List<EntityData> loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(equalStrings.size(), loadedEntities.size());

        List<String> loadedStrings = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE).get("attributeString").getValue()).collect(Collectors.toList());

        Assert.assertTrue(listEqualWithoutOrder(equalStrings, loadedStrings));

        // Not equal Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFiltersNotEqualString);
        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(notEqualStrings.size(), loadedEntities.size());

        loadedStrings = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE).get("attributeString").getValue()).collect(Collectors.toList());

        Assert.assertTrue(listEqualWithoutOrder(notEqualStrings, loadedStrings));

        // Starts with Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFiltersStartsWithString);
        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(startsWithStrings.size(), loadedEntities.size());

        loadedStrings = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE).get("attributeString").getValue()).collect(Collectors.toList());

        Assert.assertTrue(listEqualWithoutOrder(startsWithStrings, loadedStrings));

        // Ends with Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFiltersEndsWithString);
        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(endsWithStrings.size(), loadedEntities.size());

        loadedStrings = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE).get("attributeString").getValue()).collect(Collectors.toList());

        Assert.assertTrue(listEqualWithoutOrder(endsWithStrings, loadedStrings));

        // Contains Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFiltersContainsString);
        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(containsStrings.size(), loadedEntities.size());

        loadedStrings = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE).get("attributeString").getValue()).collect(Collectors.toList());

        Assert.assertTrue(listEqualWithoutOrder(containsStrings, loadedStrings));

        // Not contains Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFiltersNotContainsString);
        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(notContainsStrings.size(), loadedEntities.size());

        loadedStrings = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE).get("attributeString").getValue()).collect(Collectors.toList());

        Assert.assertTrue(listEqualWithoutOrder(notContainsStrings, loadedStrings));

        // Device type filters Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, deviceTypeFilters);
        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(devices.size(), loadedEntities.size());

        deviceService.deleteDevicesByTenantId(tenantId);
    }

    @Test
    public void testBuildStringPredicateQueryOperationsForEntityType() throws ExecutionException, InterruptedException {

        List<Device> devices = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            devices.add(deviceService.saveDevice(device));
            //TO make sure devices have different created time
            Thread.sleep(1);
        }

        DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceTypes(List.of("default"));
        filter.setDeviceNameFilter("");

        EntityDataSortOrder sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.DESC
        );

        List<EntityKey> entityFields = Arrays.asList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"),
                new EntityKey(EntityKeyType.ENTITY_FIELD, "entityType"));

        List<KeyFilter> keyFiltersEqualString = createStringKeyFilters("entityType", EntityKeyType.ENTITY_FIELD, StringFilterPredicate.StringOperation.EQUAL, "device");
        List<KeyFilter> keyFiltersNotEqualString = createStringKeyFilters("entityType", EntityKeyType.ENTITY_FIELD, StringFilterPredicate.StringOperation.NOT_EQUAL, "asset");
        List<KeyFilter> keyFiltersStartsWithString = createStringKeyFilters("entityType", EntityKeyType.ENTITY_FIELD, StringFilterPredicate.StringOperation.STARTS_WITH, "dev");
        List<KeyFilter> keyFiltersEndsWithString = createStringKeyFilters("entityType", EntityKeyType.ENTITY_FIELD, StringFilterPredicate.StringOperation.ENDS_WITH, "ice");
        List<KeyFilter> keyFiltersContainsString = createStringKeyFilters("entityType", EntityKeyType.ENTITY_FIELD, StringFilterPredicate.StringOperation.CONTAINS, "vic");
        List<KeyFilter> keyFiltersNotContainsString = createStringKeyFilters("entityType", EntityKeyType.ENTITY_FIELD, StringFilterPredicate.StringOperation.NOT_CONTAINS, "dolphin");

        // Equal Operation

        EntityDataPageLink pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, null, keyFiltersEqualString);
        PageData<EntityData> data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        List<EntityData> loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(devices.size(), loadedEntities.size());

        List<String> loadedStrings = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue()).collect(Collectors.toList());

        List<String> devicesNames = devices.stream().map(Device::getName).collect(Collectors.toList());

        Assert.assertTrue(listEqualWithoutOrder(devicesNames, loadedStrings));

        // Not equal Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, null, keyFiltersNotEqualString);
        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(devices.size(), loadedEntities.size());

        loadedStrings = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue()).collect(Collectors.toList());

        Assert.assertTrue(listEqualWithoutOrder(devicesNames, loadedStrings));

        // Starts with Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, null, keyFiltersStartsWithString);
        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(devices.size(), loadedEntities.size());

        loadedStrings = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue()).collect(Collectors.toList());

        Assert.assertTrue(listEqualWithoutOrder(devicesNames, loadedStrings));

        // Ends with Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, null, keyFiltersEndsWithString);
        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(devices.size(), loadedEntities.size());

        loadedStrings = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue()).collect(Collectors.toList());

        Assert.assertTrue(listEqualWithoutOrder(devicesNames, loadedStrings));

        // Contains Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, null, keyFiltersContainsString);
        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(devices.size(), loadedEntities.size());

        loadedStrings = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue()).collect(Collectors.toList());

        Assert.assertTrue(listEqualWithoutOrder(devicesNames, loadedStrings));

        // Not contains Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, null, keyFiltersNotContainsString);
        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(devices.size(), loadedEntities.size());

        loadedStrings = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue()).collect(Collectors.toList());

        Assert.assertTrue(listEqualWithoutOrder(devicesNames, loadedStrings));

        deviceService.deleteDevicesByTenantId(tenantId);
    }

    @Test
    public void testBuildSimplePredicateQueryOperations() throws InterruptedException {

        List<Device> devices = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            devices.add(deviceService.saveDevice(device));
            //TO make sure devices have different created time
            Thread.sleep(1);
        }

        DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceTypes(List.of("default"));
        filter.setDeviceNameFilter("");

        EntityDataSortOrder sortOrder = new EntityDataSortOrder(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"), EntityDataSortOrder.Direction.DESC);

        List<KeyFilter> deviceTypeFilters = createStringKeyFilters("type", EntityKeyType.ENTITY_FIELD, StringFilterPredicate.StringOperation.EQUAL, "default");

        KeyFilter createdTimeFilter = createNumericKeyFilter("createdTime", EntityKeyType.ENTITY_FIELD, NumericFilterPredicate.NumericOperation.GREATER, 1L);
        List<KeyFilter> createdTimeFilters = Collections.singletonList(createdTimeFilter);

        List<KeyFilter> nameFilters = createStringKeyFilters("name", EntityKeyType.ENTITY_FIELD, StringFilterPredicate.StringOperation.CONTAINS, "Device");

        List<EntityKey> entityFields = Arrays.asList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"),
                new EntityKey(EntityKeyType.ENTITY_FIELD, "type"));

        // Device type filters

        EntityDataPageLink pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, null, deviceTypeFilters);
        PageData<EntityData> data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        List<EntityData> loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(devices.size(), loadedEntities.size());

        // Device create time filters

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, null, createdTimeFilters);
        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(devices.size(), loadedEntities.size());

        // Device name filters

        pageLink = new EntityDataPageLink(100, 0, null, null);
        query = new EntityDataQuery(filter, pageLink, entityFields, null, nameFilters);
        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(devices.size(), loadedEntities.size());

        deviceService.deleteDevicesByTenantId(tenantId);
    }

    private Boolean listEqualWithoutOrder(List<String> A, List<String> B) {
        return A.containsAll(B) && B.containsAll(A);
    }

    private List<EntityData> getLoadedEntities(PageData<EntityData> data, EntityDataQuery query) {
        List<EntityData> loadedEntities = new ArrayList<>(data.getData());

        while (data.hasNext()) {
            query = query.next();
            data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
            loadedEntities.addAll(data.getData());
        }
        return loadedEntities;
    }

    private List<KeyFilter> createStringKeyFilters(String key, EntityKeyType keyType, StringFilterPredicate.StringOperation operation, String value) {
        KeyFilter filter = new KeyFilter();
        filter.setKey(new EntityKey(keyType, key));
        StringFilterPredicate predicate = new StringFilterPredicate();
        predicate.setValue(FilterPredicateValue.fromString(value));
        predicate.setOperation(operation);
        predicate.setIgnoreCase(true);
        filter.setPredicate(predicate);
        return Collections.singletonList(filter);
    }

    private KeyFilter createNumericKeyFilter(String key, EntityKeyType keyType, NumericFilterPredicate.NumericOperation operation, double value) {
        KeyFilter filter = new KeyFilter();
        filter.setKey(new EntityKey(keyType, key));
        NumericFilterPredicate predicate = new NumericFilterPredicate();
        predicate.setValue(FilterPredicateValue.fromDouble(value));
        predicate.setOperation(operation);
        filter.setPredicate(predicate);

        return filter;
    }

    private ListenableFuture<List<String>> saveLongAttribute(EntityId entityId, String key, long value, String scope) {
        KvEntry attrValue = new LongDataEntry(key, value);
        AttributeKvEntry attr = new BaseAttributeKvEntry(attrValue, 42L);
        return attributesService.save(SYSTEM_TENANT_ID, entityId, scope, Collections.singletonList(attr));
    }

    private ListenableFuture<List<String>> saveStringAttribute(EntityId entityId, String key, String value, String scope) {
        KvEntry attrValue = new StringDataEntry(key, value);
        AttributeKvEntry attr = new BaseAttributeKvEntry(attrValue, 42L);
        return attributesService.save(SYSTEM_TENANT_ID, entityId, scope, Collections.singletonList(attr));
    }

    private ListenableFuture<Integer> saveLongTimeseries(EntityId entityId, String key, Double value) {
        TsKvEntity tsKv = new TsKvEntity();
        tsKv.setStrKey(key);
        tsKv.setDoubleValue(value);
        KvEntry telemetryValue = new DoubleDataEntry(key, value);
        BasicTsKvEntry timeseries = new BasicTsKvEntry(42L, telemetryValue);
        return timeseriesService.save(SYSTEM_TENANT_ID, entityId, timeseries);
    }

    private void createMultiRootHierarchy(List<Asset> buildings, List<Asset> apartments,
                                          Map<String, Map<UUID, String>> entityNameByTypeMap,
                                          Map<UUID, UUID> childParentRelationMap) throws InterruptedException {
        for (int k = 0; k < 3; k++) {
            Asset building = new Asset();
            building.setTenantId(tenantId);
            building.setName("Building _" + k);
            building.setType("building");
            building.setLabel("building label" + k);
            building = assetService.saveAsset(building);
            buildings.add(building);
            entityNameByTypeMap.computeIfAbsent(building.getType(), n -> new HashMap<>()).put(building.getId().getId(), building.getName());

            for (int i = 0; i < 3; i++) {
                Asset asset = new Asset();
                asset.setTenantId(tenantId);
                asset.setName("Apt " + k + "_" + i);
                asset.setType("apartment");
                asset.setLabel("apartment " + i);
                asset = assetService.saveAsset(asset);
                //TO make sure devices have different created time
                Thread.sleep(1);
                entityNameByTypeMap.computeIfAbsent(asset.getType(), n -> new HashMap<>()).put(asset.getId().getId(), asset.getName());
                apartments.add(asset);
                EntityRelation er = new EntityRelation();
                er.setFrom(building.getId());
                er.setTo(asset.getId());
                er.setType("buildingToApt");
                er.setTypeGroup(RelationTypeGroup.COMMON);
                relationService.saveRelation(tenantId, er);
                childParentRelationMap.put(asset.getUuidId(), building.getUuidId());
                for (int j = 0; j < 3; j++) {
                    Device device = new Device();
                    device.setTenantId(tenantId);
                    device.setName("Heat" + k + "_" + i + "_" + j);
                    device.setType("heatmeter");
                    device.setLabel("heatmeter" + (int) (Math.random() * 1000));
                    device = deviceService.saveDevice(device);
                    //TO make sure devices have different created time
                    Thread.sleep(1);
                    entityNameByTypeMap.computeIfAbsent(device.getType(), n -> new HashMap<>()).put(device.getId().getId(), device.getName());
                    er = new EntityRelation();
                    er.setFrom(asset.getId());
                    er.setTo(device.getId());
                    er.setType("AptToHeat");
                    er.setTypeGroup(RelationTypeGroup.COMMON);
                    relationService.saveRelation(tenantId, er);
                    childParentRelationMap.put(device.getUuidId(), asset.getUuidId());
                }

                for (int j = 0; j < 3; j++) {
                    Device device = new Device();
                    device.setTenantId(tenantId);
                    device.setName("Energy" + k + "_" + i + "_" + j);
                    device.setType("energymeter");
                    device.setLabel("energymeter" + (int) (Math.random() * 1000));
                    device = deviceService.saveDevice(device);
                    //TO make sure devices have different created time
                    Thread.sleep(1);
                    entityNameByTypeMap.computeIfAbsent(device.getType(), n -> new HashMap<>()).put(device.getId().getId(), device.getName());
                    er = new EntityRelation();
                    er.setFrom(asset.getId());
                    er.setTo(device.getId());
                    er.setType("AptToEnergy");
                    er.setTypeGroup(RelationTypeGroup.COMMON);
                    relationService.saveRelation(tenantId, er);
                    childParentRelationMap.put(device.getUuidId(), asset.getUuidId());
                }
            }
        }
    }
}
