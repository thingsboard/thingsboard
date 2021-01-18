/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.kv.*;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.*;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.EntityTypeFilter;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.model.sqlts.ts.TsKvEntity;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.util.DaoTestUtil;
import org.thingsboard.server.dao.util.SqlDbType;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public abstract class BaseEntityServiceTest extends AbstractServiceTest {

    @Autowired
    private AttributesService attributesService;

    @Autowired
    private TimeseriesService timeseriesService;

    private TenantId tenantId;

    @Autowired
    private JdbcTemplate template;

    @Before
    public void before() {
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = tenantService.saveTenant(tenant);
        Assert.assertNotNull(savedTenant);
        tenantId = savedTenant.getId();
    }

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
    }

    
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
        filter.setDeviceType("default");
        filter.setDeviceNameFilter("");

        EntityCountQuery countQuery = new EntityCountQuery(filter);

        long count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(97, count);

        filter.setDeviceType("unknown");
        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(0, count);

        filter.setDeviceType("default");
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
        createTestHierarchy(assets, devices, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

        RelationsQueryFilter filter = new RelationsQueryFilter();
        filter.setRootEntity(tenantId);
        filter.setDirection(EntitySearchDirection.FROM);

        EntityCountQuery countQuery = new EntityCountQuery(filter);

        long count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(30, count);

        filter.setFilters(Collections.singletonList(new EntityTypeFilter("Contains", Collections.singletonList(EntityType.DEVICE))));
        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(25, count);

        filter.setRootEntity(devices.get(0).getId());
        filter.setDirection(EntitySearchDirection.TO);
        filter.setFilters(Collections.singletonList(new EntityTypeFilter("Manages", Collections.singletonList(EntityType.TENANT))));
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
        filter.setEdgeType("default");
        filter.setEdgeNameFilter("");

        EntityCountQuery countQuery = new EntityCountQuery(filter);

        long count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(97, count);

        filter.setEdgeType("unknown");
        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(0, count);

        filter.setEdgeType("default");
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

    @NotNull
    private Edge createEdge(int i, String type) {
        Edge edge = new Edge();
        edge.setTenantId(tenantId);
        edge.setName("Edge" + i);
        edge.setType(type);
        edge.setLabel("EdgeLabel" + i);
        edge.setSecret(RandomStringUtils.randomAlphanumeric(20));
        edge.setRoutingKey(RandomStringUtils.randomAlphanumeric(20));
        edge.setEdgeLicenseKey(RandomStringUtils.randomAlphanumeric(20));
        edge.setCloudEndpoint("http://localhost:8080");
        return edge;
    }

    @Test
    public void testHierarchicalFindEntityDataWithAttributesByQuery() throws ExecutionException, InterruptedException {
        List<Asset> assets = new ArrayList<>();
        List<Device> devices = new ArrayList<>();
        List<Long> temperatures = new ArrayList<>();
        List<Long> highTemperatures = new ArrayList<>();
        createTestHierarchy(assets, devices, new ArrayList<>(), new ArrayList<>(), temperatures, highTemperatures);

        List<ListenableFuture<List<Void>>> attributeFutures = new ArrayList<>();
        for (int i = 0; i < devices.size(); i++) {
            Device device = devices.get(i);
            attributeFutures.add(saveLongAttribute(device.getId(), "temperature", temperatures.get(i), DataConstants.CLIENT_SCOPE));
        }
        Futures.successfulAsList(attributeFutures).get();

        RelationsQueryFilter filter = new RelationsQueryFilter();
        filter.setRootEntity(tenantId);
        filter.setDirection(EntitySearchDirection.FROM);
        filter.setFilters(Collections.singletonList(new EntityTypeFilter("Contains", Collections.singletonList(EntityType.DEVICE))));

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
    public void testHierarchicalFindDevicesWithAttributesByQuery() throws ExecutionException, InterruptedException {
        List<Asset> assets = new ArrayList<>();
        List<Device> devices = new ArrayList<>();
        List<Long> temperatures = new ArrayList<>();
        List<Long> highTemperatures = new ArrayList<>();
        createTestHierarchy(assets, devices, new ArrayList<>(), new ArrayList<>(), temperatures, highTemperatures);

        List<ListenableFuture<List<Void>>> attributeFutures = new ArrayList<>();
        for (int i = 0; i < devices.size(); i++) {
            Device device = devices.get(i);
            attributeFutures.add(saveLongAttribute(device.getId(), "temperature", temperatures.get(i), DataConstants.CLIENT_SCOPE));
        }
        Futures.successfulAsList(attributeFutures).get();

        DeviceSearchQueryFilter filter = new DeviceSearchQueryFilter();
        filter.setRootEntity(tenantId);
        filter.setDirection(EntitySearchDirection.FROM);
        filter.setRelationType("Contains");

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
        createTestHierarchy(assets, devices, consumptions, highConsumptions, new ArrayList<>(), new ArrayList<>());

        List<ListenableFuture<List<Void>>> attributeFutures = new ArrayList<>();
        for (int i = 0; i < assets.size(); i++) {
            Asset asset = assets.get(i);
            attributeFutures.add(saveLongAttribute(asset.getId(), "consumption", consumptions.get(i), DataConstants.SERVER_SCOPE));
        }
        Futures.successfulAsList(attributeFutures).get();

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

    private void createTestHierarchy(List<Asset> assets, List<Device> devices, List<Long> consumptions, List<Long> highConsumptions, List<Long> temperatures, List<Long> highTemperatures) throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            asset.setName("Asset" + i);
            asset.setType("type" + i);
            asset.setLabel("AssetLabel" + i);
            asset = assetService.saveAsset(asset);
            //TO make sure devices have different created time
            Thread.sleep(1);
            assets.add(asset);
            EntityRelation er = new EntityRelation();
            er.setFrom(tenantId);
            er.setTo(asset.getId());
            er.setType("Manages");
            er.setTypeGroup(RelationTypeGroup.COMMON);
            relationService.saveRelation(tenantId, er);
            long consumption = (long) (Math.random() * 100);
            consumptions.add(consumption);
            if (consumption > 50) {
                highConsumptions.add(consumption);
            }
            for (int j = 0; j < 5; j++) {
                Device device = new Device();
                device.setTenantId(tenantId);
                device.setName("A" + i + "Device" + j);
                device.setType("default" + j);
                device.setLabel("testLabel" + (int) (Math.random() * 1000));
                device = deviceService.saveDevice(device);
                //TO make sure devices have different created time
                Thread.sleep(1);
                devices.add(device);
                er = new EntityRelation();
                er.setFrom(asset.getId());
                er.setTo(device.getId());
                er.setType("Contains");
                er.setTypeGroup(RelationTypeGroup.COMMON);
                relationService.saveRelation(tenantId, er);
                long temperature = (long) (Math.random() * 100);
                temperatures.add(temperature);
                if (temperature > 45) {
                    highTemperatures.add(temperature);
                }
            }
        }
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
        filter.setDeviceType("default");
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

        List<ListenableFuture<List<Void>>> attributeFutures = new ArrayList<>();
        for (int i = 0; i < devices.size(); i++) {
            Device device = devices.get(i);
            for (String currentScope : DataConstants.allScopes()) {
                attributeFutures.add(saveLongAttribute(device.getId(), "temperature", temperatures.get(i), currentScope));
            }
        }
        Futures.successfulAsList(attributeFutures).get();

        DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceType("default");
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
    public void testBuildNumericPredicateQueryOperations() throws ExecutionException, InterruptedException{

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

        List<ListenableFuture<List<Void>>> attributeFutures = new ArrayList<>();
        for (int i = 0; i < devices.size(); i++) {
            Device device = devices.get(i);
            attributeFutures.add(saveLongAttribute(device.getId(), "temperature", temperatures.get(i), DataConstants.CLIENT_SCOPE));
        }
        Futures.successfulAsList(attributeFutures).get();

        DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceType("default");
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
        Futures.successfulAsList(timeseriesFutures).get();

        DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceType("default");
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
        if (DaoTestUtil.getSqlDbType(template) == SqlDbType.H2) {
            // in H2 double values are stored with E0 in the end of the string
            loadedTemperatures = loadedTemperatures.stream().map(s -> s.substring(0, s.length() - 2)).collect(Collectors.toList());
        }
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

        if (DaoTestUtil.getSqlDbType(template) == SqlDbType.H2) {
            // in H2 double values are stored with E0 in the end of the string
            loadedHighTemperatures = loadedHighTemperatures.stream().map(s -> s.substring(0, s.length() - 2)).collect(Collectors.toList());
        }
        Assert.assertEquals(deviceHighTemperatures, loadedHighTemperatures);

        deviceService.deleteDevicesByTenantId(tenantId);
    }

    @Test
    public void testBuildStringPredicateQueryOperations() throws ExecutionException, InterruptedException{

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
            List<StringFilterPredicate.StringOperation> operationValues= Arrays.asList(StringFilterPredicate.StringOperation.values());
            StringFilterPredicate.StringOperation operation = operationValues.get(new Random().nextInt(operationValues.size()));
            String operationName = operation.name();
            attributeStrings.add(operationName);
            switch(operation){
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
            }
        }

        List<ListenableFuture<List<Void>>> attributeFutures = new ArrayList<>();
        for (int i = 0; i < devices.size(); i++) {
            Device device = devices.get(i);
            attributeFutures.add(saveStringAttribute(device.getId(), "attributeString", attributeStrings.get(i), DataConstants.CLIENT_SCOPE));
        }
        Futures.successfulAsList(attributeFutures).get();

        DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceType("default");
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
    public void testBuildStringPredicateQueryOperationsForEntityType() throws ExecutionException, InterruptedException{

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
        filter.setDeviceType("default");
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
    public void testBuildSimplePredicateQueryOperations() throws InterruptedException{

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
        filter.setDeviceType("default");
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
        PageData data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
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

    private List<EntityData> getLoadedEntities(PageData data, EntityDataQuery query) {
        List<EntityData> loadedEntities = new ArrayList<>(data.getData());

        while (data.hasNext()) {
            query = query.next();
            data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
            loadedEntities.addAll(data.getData());
        }
        return loadedEntities;
    }

    private List<KeyFilter> createStringKeyFilters(String key, EntityKeyType keyType, StringFilterPredicate.StringOperation operation, String value){
        KeyFilter filter = new KeyFilter();
        filter.setKey(new EntityKey(keyType, key));
        StringFilterPredicate predicate = new StringFilterPredicate();
        predicate.setValue(FilterPredicateValue.fromString(value));
        predicate.setOperation(operation);
        predicate.setIgnoreCase(true);
        filter.setPredicate(predicate);
        return Collections.singletonList(filter);
    }

    private KeyFilter createNumericKeyFilter(String key, EntityKeyType keyType, NumericFilterPredicate.NumericOperation operation, double value){
        KeyFilter filter = new KeyFilter();
        filter.setKey(new EntityKey(keyType, key));
        NumericFilterPredicate predicate = new NumericFilterPredicate();
        predicate.setValue(FilterPredicateValue.fromDouble(value));
        predicate.setOperation(operation);
        filter.setPredicate(predicate);

        return filter;
    }

    private ListenableFuture<List<Void>> saveLongAttribute(EntityId entityId, String key, long value, String scope) {
        KvEntry attrValue = new LongDataEntry(key, value);
        AttributeKvEntry attr = new BaseAttributeKvEntry(attrValue, 42L);
        return attributesService.save(SYSTEM_TENANT_ID, entityId, scope, Collections.singletonList(attr));
    }

    private ListenableFuture<List<Void>> saveStringAttribute(EntityId entityId, String key, String value, String scope) {
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
}
