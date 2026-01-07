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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.AttributesSaveResult;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TimeseriesSaveResult;
import org.thingsboard.server.common.data.objects.TelemetryEntityView;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AliasEntityId;
import org.thingsboard.server.common.data.query.ApiUsageStateFilter;
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
import org.thingsboard.server.common.data.query.EntityViewTypeFilter;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.NumericFilterPredicate;
import org.thingsboard.server.common.data.query.RelationsQueryFilter;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.query.StringFilterPredicate;
import org.thingsboard.server.common.data.query.StringFilterPredicate.StringOperation;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardDao;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.entityview.EntityViewDao;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.sql.relation.RelationRepository;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateService;
import org.thingsboard.server.dao.user.UserService;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.AttributeScope.SERVER_SCOPE;
import static org.thingsboard.server.common.data.query.EntityKeyType.ATTRIBUTE;
import static org.thingsboard.server.common.data.query.EntityKeyType.ENTITY_FIELD;
import static org.thingsboard.server.common.data.query.EntityKeyType.SERVER_ATTRIBUTE;

@Slf4j
@DaoSqlTest
public class EntityServiceTest extends AbstractControllerTest {

    static final int ENTITY_COUNT = 5;
    public static final String TEST_CUSTOMER_NAME = "Test";

    @Autowired
    AssetService assetService;
    @Autowired
    AssetProfileService assetProfileService;
    @Autowired
    DashboardService dashboardService;
    @Autowired
    EntityViewService entityViewService;
    @Autowired
    UserService userService;
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
    @Autowired
    ApiUsageStateService apiUsageStateService;
    @Autowired
    CustomerService customerService;
    @Autowired
    DashboardDao dashboardDao;
    @Autowired
    EntityViewDao entityViewDao;
    @Autowired
    AlarmService alarmService;

    private CustomerId customerId;

    @Before
    public void before() {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle(TEST_CUSTOMER_NAME);
        customer = customerService.saveCustomer(customer);
        customerId = customer.getId();
    }

    @Test
    public void testCountEntitiesByQuery() {
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
        countByQueryAndCheck(countQuery, 97);

        filter.setDeviceTypes(List.of("unknown"));
        countByQueryAndCheck(countQuery, 0);

        filter.setDeviceTypes(List.of("default"));
        filter.setDeviceNameFilter("Device1");
        countByQueryAndCheck(countQuery, 11);

        EntityListFilter entityListFilter = new EntityListFilter();
        entityListFilter.setEntityType(EntityType.DEVICE);
        entityListFilter.setEntityList(devices.stream().map(Device::getId).map(DeviceId::toString).collect(Collectors.toList()));

        countQuery = new EntityCountQuery(entityListFilter);
        countByQueryAndCheck(countQuery, 97);

        deviceService.deleteDevicesByTenantId(tenantId);
        countByQueryAndCheck(countQuery, 0);
    }

    @Test
    public void testCountHierarchicalEntitiesByQuery() throws InterruptedException {
        List<Asset> assets = new ArrayList<>();
        List<Device> devices = new ArrayList<>();
        createTestHierarchy(tenantId, assets, devices, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

        RelationsQueryFilter filter = new RelationsQueryFilter();
        filter.setRootEntity(AliasEntityId.fromEntityId(tenantId));
        filter.setDirection(EntitySearchDirection.FROM);

        EntityCountQuery countQuery = new EntityCountQuery(filter);
        countByQueryAndCheck(countQuery, 31); //due to the loop relations in hierarchy, the TenantId included in total count (1*Tenant + 5*Asset + 5*5*Devices = 31)

        filter.setFilters(Collections.singletonList(new RelationEntityTypeFilter("Contains", Collections.singletonList(EntityType.DEVICE))));
        countByQueryAndCheck(countQuery, 25);

        filter.setRootEntity(AliasEntityId.fromEntityId(devices.get(0).getId()));
        filter.setDirection(EntitySearchDirection.TO);
        filter.setFilters(Collections.singletonList(new RelationEntityTypeFilter("Manages", Collections.singletonList(EntityType.TENANT))));
        countByQueryAndCheck(countQuery, 1);

        DeviceSearchQueryFilter filter2 = new DeviceSearchQueryFilter();
        filter2.setRootEntity(AliasEntityId.fromEntityId(tenantId));
        filter2.setDirection(EntitySearchDirection.FROM);
        filter2.setRelationType("Contains");

        countQuery = new EntityCountQuery(filter2);
        countByQueryAndCheck(countQuery, 25);

        filter2.setDeviceTypes(Arrays.asList("default0", "default1"));
        countByQueryAndCheck(countQuery, 10);

        filter2.setRootEntity(AliasEntityId.fromEntityId(devices.get(0).getId()));
        filter2.setDirection(EntitySearchDirection.TO);
        countByQueryAndCheck(countQuery, 0);

        AssetSearchQueryFilter filter3 = new AssetSearchQueryFilter();
        filter3.setRootEntity(AliasEntityId.fromEntityId(tenantId));
        filter3.setDirection(EntitySearchDirection.FROM);
        filter3.setRelationType("Manages");

        countQuery = new EntityCountQuery(filter3);
        countByQueryAndCheck(countQuery, 5);

        filter3.setAssetTypes(Arrays.asList("type0", "type1"));
        countByQueryAndCheck(countQuery, 2);

        filter3.setRootEntity(AliasEntityId.fromEntityId(devices.get(0).getId()));
        filter3.setDirection(EntitySearchDirection.TO);
        countByQueryAndCheck(countQuery, 0);
    }

    @Test
    public void testCountHierarchicalUserEntitiesByQuery() throws InterruptedException {
        List<User> users = new ArrayList<>();
        createTestUserRelations(tenantId, users);

        RelationsQueryFilter filter = new RelationsQueryFilter();
        filter.setRootEntity(AliasEntityId.fromEntityId(tenantId));
        filter.setDirection(EntitySearchDirection.FROM);

        EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, null);
        List<EntityKey> entityFields = Arrays.asList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"), new EntityKey(EntityKeyType.ENTITY_FIELD, "phone"));

        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, null, null);

        PageData<EntityData> entityDataByQuery = findByQueryAndCheck(query, 5);
        List<EntityData> data = entityDataByQuery.getData();
        Assert.assertEquals(data.size(), 5);
        data.forEach(entityData -> Assert.assertNotNull(entityData.getLatest().get(EntityKeyType.ENTITY_FIELD).get("phone")));

        countByQueryAndCheck(query, 5);

        // delete user
        userService.deleteUser(tenantId, users.get(0));
        countByQueryAndCheck(query, 4);
    }

    private void createTestUserRelations(TenantId tenantId, List<User> users) {
        for (int i = 0; i < ENTITY_COUNT; i++) {
            User user = new User();
            user.setTenantId(tenantId);
            user.setAuthority(Authority.TENANT_ADMIN);
            user.setEmail(StringUtils.randomAlphabetic(10) + "@gmail.com");
            user.setPhone(StringUtils.randomNumeric(10));
            user = userService.saveUser(tenantId, user);
            users.add(user);
            createRelation(tenantId, "Contains", tenantId, user.getId());
        }
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
        countByQueryAndCheck(countQuery, 97);

        filter.setEdgeTypes(List.of("unknown"));
        countByQueryAndCheck(countQuery, 0);

        filter.setEdgeTypes(List.of("default"));
        filter.setEdgeNameFilter("Edge1");
        countByQueryAndCheck(countQuery, 11);

        EntityListFilter entityListFilter = new EntityListFilter();
        entityListFilter.setEntityType(EntityType.EDGE);
        entityListFilter.setEntityList(edges.stream().map(Edge::getId).map(EdgeId::toString).collect(Collectors.toList()));

        countQuery = new EntityCountQuery(entityListFilter);
        countByQueryAndCheck(countQuery, 97);

        edgeService.deleteEdgesByTenantId(tenantId);
        countByQueryAndCheck(countQuery, 0);
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
        filter.setRootEntity(AliasEntityId.fromEntityId(tenantId));
        filter.setDirection(EntitySearchDirection.FROM);
        filter.setRelationType("Manages");

        EntityCountQuery countQuery = new EntityCountQuery(filter);
        countByQueryAndCheck(countQuery, 5);

        filter.setEdgeTypes(Arrays.asList("type0", "type1"));
        countByQueryAndCheck(countQuery, 2);
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

        List<ListenableFuture<AttributesSaveResult>> attributeFutures = new ArrayList<>();
        for (int i = 0; i < devices.size(); i++) {
            Device device = devices.get(i);
            attributeFutures.add(saveLongAttribute(device.getId(), "temperature", temperatures.get(i), AttributeScope.CLIENT_SCOPE));
        }
        Futures.allAsList(attributeFutures).get();

        RelationsQueryFilter filter = new RelationsQueryFilter();
        filter.setRootEntity(AliasEntityId.fromEntityId(tenantId));
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

        List<String> deviceTemperatures = temperatures.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());

        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, null);
        findByQueryAndCheckTelemetry(query, EntityKeyType.ATTRIBUTE, "temperature", deviceTemperatures);

        pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        KeyFilter highTemperatureFilter = new KeyFilter();
        highTemperatureFilter.setKey(new EntityKey(EntityKeyType.ATTRIBUTE, "temperature"));
        NumericFilterPredicate predicate = new NumericFilterPredicate();
        predicate.setValue(FilterPredicateValue.fromDouble(45));
        predicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperatureFilter.setPredicate(predicate);
        List<KeyFilter> keyFilters = Collections.singletonList(highTemperatureFilter);

        List<String> deviceHighTemperatures = highTemperatures.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());

        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFilters);
        findByQueryAndCheckTelemetry(query, EntityKeyType.ATTRIBUTE, "temperature", deviceHighTemperatures);

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
        countByQueryAndCheck(countQuery, 63);

        filter.setFilters(Collections.singletonList(new RelationEntityTypeFilter("AptToHeat", Collections.singletonList(EntityType.DEVICE))));
        countByQueryAndCheck(countQuery, 27);

        filter.setMultiRootEntitiesType(EntityType.ASSET);
        filter.setMultiRootEntityIds(apartments.stream().map(IdBased::getId).map(d -> d.getId().toString()).collect(Collectors.toSet()));
        filter.setDirection(EntitySearchDirection.TO);
        filter.setFilters(Lists.newArrayList(
                new RelationEntityTypeFilter("buildingToApt", Collections.singletonList(EntityType.ASSET)),
                new RelationEntityTypeFilter("AptToEnergy", Collections.singletonList(EntityType.DEVICE))));
        countByQueryAndCheck(countQuery, 9);

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

        long expectedEntitiesCnt = entityNameByTypeMap.entrySet()
                .stream()
                .filter(e -> !e.getKey().equals("building"))
                .flatMap(e -> e.getValue().entrySet().stream())
                .map(Map.Entry::getValue)
                .filter(e -> StringUtils.endsWith(e, "_1"))
                .count();
        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFilters);
        PageData<EntityData> data = findByQueryAndCheck(query, expectedEntitiesCnt);
        List<EntityData> loadedEntities = new ArrayList<>(data.getData());
        while (data.hasNext()) {
            query = query.next();
            data = findByQuery(query);
            loadedEntities.addAll(data.getData());
        }
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

        List<ListenableFuture<AttributesSaveResult>> attributeFutures = new ArrayList<>();
        for (int i = 0; i < devices.size(); i++) {
            Device device = devices.get(i);
            attributeFutures.add(saveLongAttribute(device.getId(), "temperature", temperatures.get(i), AttributeScope.CLIENT_SCOPE));
        }
        Futures.allAsList(attributeFutures).get();

        DeviceSearchQueryFilter filter = new DeviceSearchQueryFilter();
        filter.setRootEntity(AliasEntityId.fromEntityId(tenantId));
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

        List<String> deviceTemperatures = temperatures.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());

        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, null);
        List<EntityData> loadedEntities = findByQueryAndCheckTelemetry(query, EntityKeyType.ATTRIBUTE, "temperature", deviceTemperatures);

        loadedEntities.forEach(entity -> Assert.assertTrue(devices.stream().map(Device::getId).collect(Collectors.toSet()).contains(entity.getEntityId())));

        pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        KeyFilter highTemperatureFilter = new KeyFilter();
        highTemperatureFilter.setKey(new EntityKey(EntityKeyType.ATTRIBUTE, "temperature"));
        NumericFilterPredicate predicate = new NumericFilterPredicate();
        predicate.setValue(FilterPredicateValue.fromDouble(45));
        predicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperatureFilter.setPredicate(predicate);
        List<KeyFilter> keyFilters = Collections.singletonList(highTemperatureFilter);

        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFilters);

        List<String> deviceHighTemperatures = highTemperatures.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());
        findByQueryAndCheckTelemetry(query, EntityKeyType.ATTRIBUTE, "temperature", deviceHighTemperatures);

        deviceService.deleteDevicesByTenantId(tenantId);
    }


    @Test
    public void testHierarchicalFindAssetsWithAttributesByQuery() throws ExecutionException, InterruptedException {
        List<Asset> assets = new ArrayList<>();
        List<Device> devices = new ArrayList<>();
        List<Long> consumptions = new ArrayList<>();
        List<Long> highConsumptions = new ArrayList<>();
        createTestHierarchy(tenantId, assets, devices, consumptions, highConsumptions, new ArrayList<>(), new ArrayList<>());

        List<ListenableFuture<AttributesSaveResult>> attributeFutures = new ArrayList<>();
        for (int i = 0; i < assets.size(); i++) {
            Asset asset = assets.get(i);
            attributeFutures.add(saveLongAttribute(asset.getId(), "consumption", consumptions.get(i), SERVER_SCOPE));
        }
        Futures.allAsList(attributeFutures).get();

        AssetSearchQueryFilter filter = new AssetSearchQueryFilter();
        filter.setRootEntity(AliasEntityId.fromEntityId(tenantId));
        filter.setDirection(EntitySearchDirection.FROM);
        filter.setRelationType("Manages");

        EntityDataSortOrder sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.ASC
        );
        EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        List<EntityKey> entityFields = Collections.singletonList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));
        List<EntityKey> latestValues = Collections.singletonList(new EntityKey(EntityKeyType.ATTRIBUTE, "consumption"));

        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, null);

        List<String> deviceTemperatures = consumptions.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());
        findByQueryAndCheckTelemetry(query, EntityKeyType.ATTRIBUTE, "consumption", deviceTemperatures);

        pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        KeyFilter highTemperatureFilter = new KeyFilter();
        highTemperatureFilter.setKey(new EntityKey(EntityKeyType.ATTRIBUTE, "consumption"));
        NumericFilterPredicate predicate = new NumericFilterPredicate();
        predicate.setValue(FilterPredicateValue.fromDouble(50));
        predicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperatureFilter.setPredicate(predicate);
        List<KeyFilter> keyFilters = Collections.singletonList(highTemperatureFilter);

        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFilters);

        List<String> deviceHighTemperatures = highConsumptions.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());
        findByQueryAndCheckTelemetry(query, EntityKeyType.ATTRIBUTE, "consumption", deviceHighTemperatures);

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
        assertThat(ids.length).isGreaterThanOrEqualTo(1);
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
        PageData<EntityData> data = findByQueryAndCheck(query, 97);
        Assert.assertEquals(10, data.getTotalPages());
        Assert.assertTrue(data.hasNext());
        Assert.assertEquals(10, data.getData().size());

        List<EntityData> loadedEntities = new ArrayList<>(data.getData());
        while (data.hasNext()) {
            query = query.next();
            data = findByQuery(query);
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
        data = findByQuery(query);
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

        // FIXME (for Dasha, plz investigate):
        //  this and other tests below submit an empty value to a KEY FILTER, this is not "search text".
        //  why are we supposed to ignore it and return all devices? maybe it's a bug?
        String searchQuery = "";
        EntityDataQuery query = createDeviceSearchQuery("label", StringOperation.EQUAL, searchQuery);
        findByQueryAndCheck(query, devices.size());
    }

    @Test
    public void testFindEntityDataByQuery_operationNotEqual() {
        List<Device> devices = createMockDevices(10);
        devices.get(0).setLabel("");
        devices.get(1).setLabel(null);
        devices.forEach(deviceService::saveDevice);

        String searchQuery = devices.get(2).getLabel();
        EntityDataQuery query = createDeviceSearchQuery("label", StringOperation.NOT_EQUAL, searchQuery);
        findByQueryAndCheck(query, devices.size() - 1);
    }

    @Test
    public void testFindEntityDataByQuery_operationNotEqual_emptySearchQuery() {
        List<Device> devices = createMockDevices(10);
        devices.get(0).setLabel("");
        devices.get(1).setLabel(null);
        devices.forEach(deviceService::saveDevice);

        String searchQuery = "";
        EntityDataQuery query = createDeviceSearchQuery("label", StringOperation.NOT_EQUAL, searchQuery);

        findByQueryAndCheck(query, devices.size());
    }

    @Test
    public void testFindEntityDataByQuery_operationStartsWith_emptySearchQuery() {
        List<Device> devices = createMockDevices(10);
        devices.get(0).setLabel("");
        devices.get(1).setLabel(null);
        devices.forEach(deviceService::saveDevice);

        String searchQuery = "";
        EntityDataQuery query = createDeviceSearchQuery("label", StringOperation.STARTS_WITH, searchQuery);
        findByQueryAndCheck(query, devices.size());
    }

    @Test
    public void testFindEntityDataByQuery_operationEndsWith_emptySearchQuery() {
        List<Device> devices = createMockDevices(10);
        devices.get(0).setLabel("");
        devices.get(1).setLabel(null);
        devices.forEach(deviceService::saveDevice);

        String searchQuery = "";
        EntityDataQuery query = createDeviceSearchQuery("label", StringOperation.ENDS_WITH, searchQuery);
        findByQueryAndCheck(query, devices.size());
    }

    @Test
    public void testFindEntityDataByQuery_operationContains_emptySearchQuery() {
        List<Device> devices = createMockDevices(10);
        devices.get(0).setLabel("");
        devices.get(1).setLabel(null);
        devices.forEach(deviceService::saveDevice);

        String searchQuery = "";
        EntityDataQuery query = createDeviceSearchQuery("label", StringOperation.CONTAINS, searchQuery);
        findByQueryAndCheck(query, devices.size());
    }

    @Test
    public void testFindEntityDataByQuery_operationNotContains() {
        List<Device> devices = createMockDevices(10);
        devices.get(0).setLabel("");
        devices.get(1).setLabel(null);
        devices.forEach(deviceService::saveDevice);

        String searchQuery = "label-";
        EntityDataQuery query = createDeviceSearchQuery("label", StringOperation.NOT_CONTAINS, searchQuery);
        findByQueryAndCheck(query, 2);
    }

    @Test
    public void testFindEntityDataByQuery_operationNotContains_emptySearchQuery() {
        List<Device> devices = createMockDevices(10);
        devices.get(0).setLabel("");
        devices.get(1).setLabel(null);
        devices.forEach(deviceService::saveDevice);

        String searchQuery = "";
        EntityDataQuery query = createDeviceSearchQuery("label", StringOperation.NOT_CONTAINS, searchQuery);
        findByQueryAndCheck(query, devices.size());
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
        findByQueryAndCheck(query, devices.size());

        deviceTypeFilter.setEntityNameFilter("Device%");
        findByQueryAndCheck(query, devices.size());

        deviceTypeFilter.setEntityNameFilter("%Device%");
        findByQueryAndCheck(query, devices.size());

        deviceTypeFilter.setEntityNameFilter("%Device");
        findByQueryAndCheck(query, 0);
    }

    @Test
    public void testFindEntityDataByQuery_filter_entity_name_ends_with() {
        List<Device> devices = new ArrayList<>();

        String suffixes = RandomStringUtils.randomAlphanumeric(5);
        for (int i = 0; i < 10; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device " + i + suffixes);
            device.setType("default");
            devices.add(device);
        }

        devices.forEach(deviceService::saveDevice);

        EntityNameFilter deviceTypeFilter = new EntityNameFilter();
        deviceTypeFilter.setEntityType(EntityType.DEVICE);
        deviceTypeFilter.setEntityNameFilter("%" + suffixes);

        EntityDataPageLink pageLink = new EntityDataPageLink(1000, 0, null, null);

        EntityDataQuery query = new EntityDataQuery(deviceTypeFilter, pageLink, null, null, null);
        findByQueryAndCheck(query, devices.size());

        deviceTypeFilter.setEntityNameFilter("%" + suffixes + "%");
        findByQueryAndCheck(query, devices.size());

        deviceTypeFilter.setEntityNameFilter(suffixes + "%");
        findByQueryAndCheck(query, 0);

        deviceTypeFilter.setEntityNameFilter(suffixes);
        findByQueryAndCheck(query, 0);
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
        findByQueryAndCheck(query, devices.size());

        deviceTypeFilter.setEntityNameFilter("test%");
        findByQueryAndCheck(query, 0);

        deviceTypeFilter.setEntityNameFilter("%test");
        findByQueryAndCheck(query, 0);
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
        findByQueryAndCheck(query, devices.size());

        deviceTypeFilter.setDeviceNameFilter("Device%");
        findByQueryAndCheck(query, devices.size());

        deviceTypeFilter.setDeviceNameFilter("%Device%");
        findByQueryAndCheck(query, devices.size());

        deviceTypeFilter.setDeviceNameFilter("%Device");
        findByQueryAndCheck(query, 0);
    }

    @Test
    public void testFindEntitiesBySingleEntityFilter() {
        List<Device> devices = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device test" + i);
            device.setType("default");
            devices.add(deviceService.saveDevice(device));
        }

        SingleEntityFilter singleEntityFilter = new SingleEntityFilter();
        singleEntityFilter.setSingleEntity(AliasEntityId.fromEntityId(devices.get(0).getId()));

        List<EntityKey> entityFields = List.of(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "name")
        );

        EntityDataPageLink pageLink = new EntityDataPageLink(1000, 0, null, null);

        EntityDataQuery query = new EntityDataQuery(singleEntityFilter, pageLink, entityFields, null, null);

        PageData<EntityData> result = findByQueryAndCheck(query, 1);

        String deviceName = result.getData().get(0).getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue();
        assertThat(deviceName).isEqualTo(devices.get(0).getName());
    }

    @Test
    public void testFindCustomerBySingleEntityFilter() {
        SingleEntityFilter singleEntityFilter = new SingleEntityFilter();
        singleEntityFilter.setSingleEntity(AliasEntityId.fromEntityId(customerId));
        List<EntityKey> entityFields = List.of(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "name")
        );
        EntityDataPageLink pageLink = new EntityDataPageLink(1000, 0, null, null);
        EntityDataQuery query = new EntityDataQuery(singleEntityFilter, pageLink, entityFields, null, null);

        //find by tenant
        PageData<EntityData> result = findByQueryAndCheck(query, 1);
        String customerName = result.getData().get(0).getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue();
        assertThat(customerName).isEqualTo(TEST_CUSTOMER_NAME);

        // find by customer user
        PageData<EntityData> customerResults = findByQueryAndCheck(customerId, query, 1);

        customerName = customerResults.getData().get(0).getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue();
        assertThat(customerName).isEqualTo(TEST_CUSTOMER_NAME);
    }


    @Test
    public void testFindEntitiesByRelationEntityTypeFilter() {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle("Customer Relation Query");
        customer = customerService.saveCustomer(customer);

        final int assetCount = 2;
        final int relationsCnt = 4;
        final int deviceEntitiesCnt = assetCount * relationsCnt;

        List<Asset> assets = new ArrayList<>();
        for (int i = 0; i < assetCount; i++) {
            Asset building = new Asset();
            building.setTenantId(tenantId);
            building.setCustomerId(customer.getId());
            building.setName("Building _" + i);
            building.setType("building");
            building = assetService.saveAsset(building);
            assets.add(building);
        }

        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < deviceEntitiesCnt; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setCustomerId(customer.getId());
            device.setName("Test device " + i);
            device.setType("default");
            Device savedDevice = deviceService.saveDevice(device);
            devices.add(savedDevice);
        }

        for (int i = 0; i < assetCount; i++) {
            for (int j = 0; j < relationsCnt; j++) {
                EntityRelation relationEntity = new EntityRelation();
                relationEntity.setFrom(assets.get(i).getId());
                relationEntity.setTo(devices.get(j + (i * relationsCnt)).getId());
                relationEntity.setTypeGroup(RelationTypeGroup.COMMON);
                relationEntity.setType("contains");
                relationService.saveRelation(tenantId, relationEntity);
            }
        }

        RelationEntityTypeFilter relationEntityTypeFilter = new RelationEntityTypeFilter("contains", Collections.singletonList(EntityType.DEVICE));
        RelationsQueryFilter filter = new RelationsQueryFilter();
        filter.setFilters(Collections.singletonList(relationEntityTypeFilter));
        filter.setDirection(EntitySearchDirection.FROM);
        EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, null);
        List<KeyFilter> keyFiltersEqualString = createStringKeyFilters("name", EntityKeyType.ENTITY_FIELD, StringOperation.STARTS_WITH, "Test device ");

        for (Asset asset : assets) {
            filter.setRootEntity(AliasEntityId.fromEntityId(asset.getId()));

            EntityDataQuery query = new EntityDataQuery(filter, pageLink, Collections.emptyList(), Collections.emptyList(), keyFiltersEqualString);
            findByQueryAndCheck(customer.getId(), query, relationsCnt);
            countByQueryAndCheck(customer.getId(), query, relationsCnt);
        }
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
        findByQueryAndCheck(query, devices.size());

        deviceTypeFilter.setDeviceNameFilter("%test%");
        findByQueryAndCheck(query, devices.size());

        deviceTypeFilter.setDeviceNameFilter("test%");
        findByQueryAndCheck(query, 0);

        deviceTypeFilter.setDeviceNameFilter("test");
        findByQueryAndCheck(query, 0);
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
        findByQueryAndCheck(query, devices.size());

        deviceTypeFilter.setDeviceNameFilter("test%");
        findByQueryAndCheck(query, 0);

        deviceTypeFilter.setDeviceNameFilter("%test");
        findByQueryAndCheck(query, 0);
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
        findByQueryAndCheck(query, assets.size());

        assetTypeFilter.setAssetNameFilter("Asset%");
        findByQueryAndCheck(query, assets.size());

        assetTypeFilter.setAssetNameFilter("%Asset%");
        findByQueryAndCheck(query, assets.size());

        assetTypeFilter.setAssetNameFilter("%Asset");
        findByQueryAndCheck(query, 0);
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
        findByQueryAndCheck(query, assets.size());

        assetTypeFilter.setAssetNameFilter("%test%");
        findByQueryAndCheck(query, assets.size());

        assetTypeFilter.setAssetNameFilter("test%");
        findByQueryAndCheck(query, 0);

        assetTypeFilter.setAssetNameFilter("test");
        findByQueryAndCheck(query, 0);
    }

    @Test
    public void testFindEntityDataByQuery_filter_asset_type_name_contains() {
        List<Asset> assets = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            asset.setName("Asset test" + i);
            asset.setType("default");
            asset.setAssetProfileId(assetProfileService.findDefaultAssetProfile(tenantId).getId());
            assets.add(asset);
        }

        assets.forEach(assetService::saveAsset);

        AssetTypeFilter assetTypeFilter = new AssetTypeFilter();
        assetTypeFilter.setAssetType("default");
        assetTypeFilter.setAssetNameFilter("%test%");

        EntityDataPageLink pageLink = new EntityDataPageLink(1000, 0, null, null);

        EntityDataQuery query = new EntityDataQuery(assetTypeFilter, pageLink, null, null, null);
        findByQueryAndCheck(query, assets.size());

        assetTypeFilter.setAssetNameFilter("test%");
        findByQueryAndCheck(query, 0);

        assetTypeFilter.setAssetNameFilter("%test");
        findByQueryAndCheck(query, 0);
    }

    @Test
    public void testFindEntitiesBySingleEntityFilter_customer() {
        List<Device> customerDevices = new ArrayList<>();
        List<Device> tenantDevices = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setCustomerId(customerId);
            device.setName("Device test" + i);
            device.setType("default");
            Device saved = deviceService.saveDevice(device);
            customerDevices.add(saved);
        }

        for (int i = 0; i < 3; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Tenant test device" + i);
            device.setType("default");
            tenantDevices.add(deviceService.saveDevice(device));
        }

        SingleEntityFilter singleEntityFilter = new SingleEntityFilter();
        singleEntityFilter.setSingleEntity(AliasEntityId.fromEntityId(customerDevices.get(0).getId()));
        List<EntityKey> entityFields = List.of(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "name")
        );
        EntityDataPageLink pageLink = new EntityDataPageLink(1000, 0, null, null);
        EntityDataQuery query = new EntityDataQuery(singleEntityFilter, pageLink, entityFields, null, null);

        PageData<EntityData> result = findByQueryAndCheck(query, 1);
        String deviceName = result.getData().get(0).getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue();
        assertThat(deviceName).isEqualTo(customerDevices.get(0).getName());

        // find by customer user with generic permission
        PageData<EntityData> customerResults = findByQueryAndCheck(customerId, query, 1);

        String cutomerDeviceName = customerResults.getData().get(0).getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue();
        assertThat(cutomerDeviceName).isEqualTo(customerDevices.get(0).getName());

        // try to find tenant device by customer user
        SingleEntityFilter tenantDeviceFilter = new SingleEntityFilter();
        tenantDeviceFilter.setSingleEntity(AliasEntityId.fromEntityId(tenantDevices.get(0).getId()));
        EntityDataQuery customerQuery2 = new EntityDataQuery(tenantDeviceFilter, pageLink, entityFields, null, null);
        findByQueryAndCheck(customerId, customerQuery2, 0);
    }

    private List<DeviceId> getResultDeviceIds(PageData<EntityData> result) {
        return result.getData().stream().map(entityData -> (DeviceId) entityData.getEntityId()).collect(Collectors.toList());
    }

    private Device createDevice(CustomerId customerId) {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setCustomerId(customerId);
        device.setName("Device test " + RandomStringUtils.randomAlphabetic(5));
        device.setType("default");
        return device;
    }

    @Test
    public void testFindEntitiesByApiUsageStateFilter() {
        ApiUsageStateFilter apiUsageStateFilter = new ApiUsageStateFilter();

        List<EntityKey> entityFields = List.of(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "name")
        );

        EntityDataPageLink pageLink = new EntityDataPageLink(1000, 0, null, null);
        EntityDataQuery query = new EntityDataQuery(apiUsageStateFilter, pageLink, entityFields, null, null);
        PageData<EntityData> result = findByQueryAndCheck(query, 1);
        String name = result.getData().get(0).getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue();
        assertThat(name).isEqualTo(TEST_TENANT_NAME);

        // find by customer user with generic permissions
        apiUsageStateService.createDefaultApiUsageState(tenantId, customerId);
        PageData<EntityData> customerResult = findByQueryAndCheck(customerId, query, 1);

        String customerResultName = customerResult.getData().get(0).getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue();
        assertThat(customerResultName).isEqualTo(TEST_CUSTOMER_NAME);

        // find by tenant user with customerId filter
        apiUsageStateFilter.setCustomerId(customerId);
        PageData<EntityData> tenantResult = findByQueryAndCheck(query, 1);
        String tenantResultName = tenantResult.getData().get(0).getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue();
        assertThat(tenantResultName).isEqualTo(TEST_CUSTOMER_NAME);
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

        List<ListenableFuture<AttributesSaveResult>> attributeFutures = new ArrayList<>();
        for (int i = 0; i < devices.size(); i++) {
            Device device = devices.get(i);
            for (AttributeScope currentScope : AttributeScope.values()) {
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
            List<String> deviceTemperatures = temperatures.stream().map(aLong -> Long.toString(aLong)).toList();
            findByQueryAndCheckTelemetry(query, currentAttributeKeyType, "temperature", deviceTemperatures);

            pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
            KeyFilter highTemperatureFilter = createNumericKeyFilter("temperature", currentAttributeKeyType, NumericFilterPredicate.NumericOperation.GREATER, 45);
            List<KeyFilter> keyFiltersHighTemperature = Collections.singletonList(highTemperatureFilter);

            query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFiltersHighTemperature);
            findByQueryAndCheckTelemetry(query, currentAttributeKeyType, "temperature", highTemperatures.stream().map(Object::toString).toList());
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

        List<ListenableFuture<AttributesSaveResult>> attributeFutures = new ArrayList<>();
        for (int i = 0; i < devices.size(); i++) {
            Device device = devices.get(i);
            attributeFutures.add(saveLongAttribute(device.getId(), "temperature", temperatures.get(i), AttributeScope.CLIENT_SCOPE));
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
        List<String> deviceTemperatures = greaterTemperatures.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());

        EntityDataPageLink pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFiltersGreaterTemperature);

        findByQueryAndCheckTelemetry(query, EntityKeyType.CLIENT_ATTRIBUTE, "temperature", deviceTemperatures);

        //Greater or equal Operation
        deviceTemperatures = greaterOrEqualTemperatures.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFiltersGreaterOrEqualTemperature);

        findByQueryAndCheckTelemetry(query, EntityKeyType.CLIENT_ATTRIBUTE, "temperature", deviceTemperatures);

        //Less Operation
        deviceTemperatures = lessTemperatures.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFiltersLessTemperature);
        findByQueryAndCheckTelemetry(query, EntityKeyType.CLIENT_ATTRIBUTE, "temperature", deviceTemperatures);

        //Less or equal Operation
        deviceTemperatures = lessOrEqualTemperatures.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFiltersLessOrEqualTemperature);
        findByQueryAndCheckTelemetry(query, EntityKeyType.CLIENT_ATTRIBUTE, "temperature", deviceTemperatures);

        //Equal Operation
        deviceTemperatures = equalTemperatures.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFiltersEqualTemperature);
        findByQueryAndCheckTelemetry(query, EntityKeyType.CLIENT_ATTRIBUTE, "temperature", deviceTemperatures);

        //Not equal Operation
        deviceTemperatures = notEqualTemperatures.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFiltersNotEqualTemperature);
        findByQueryAndCheckTelemetry(query, EntityKeyType.CLIENT_ATTRIBUTE, "temperature", deviceTemperatures);

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

        List<ListenableFuture<TimeseriesSaveResult>> timeseriesFutures = new ArrayList<>();
        for (int i = 0; i < devices.size(); i++) {
            Device device = devices.get(i);
            timeseriesFutures.add(saveTimeseries(device.getId(), "temperature", temperatures.get(i)));
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

        List<String> deviceTemperatures = temperatures.stream().map(aDouble -> Double.toString(aDouble)).collect(Collectors.toList());

        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, null);
        findByQueryAndCheckTelemetry(query, EntityKeyType.TIME_SERIES, "temperature", deviceTemperatures);

        pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        KeyFilter highTemperatureFilter = new KeyFilter();
        highTemperatureFilter.setKey(new EntityKey(EntityKeyType.TIME_SERIES, "temperature"));
        NumericFilterPredicate predicate = new NumericFilterPredicate();
        predicate.setValue(FilterPredicateValue.fromDouble(45));
        predicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperatureFilter.setPredicate(predicate);
        List<KeyFilter> keyFilters = Collections.singletonList(highTemperatureFilter);

        List<String> deviceHighTemperatures = highTemperatures.stream().map(aDouble -> Double.toString(aDouble)).collect(Collectors.toList());

        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFilters);
        findByQueryAndCheckTelemetry(query, EntityKeyType.TIME_SERIES, "temperature", deviceHighTemperatures);

        // change sort order to sort by temperature
        temperatures.sort(Comparator.naturalOrder());
        List<String> expectedSortedList = temperatures.stream().map(aDouble -> Double.toString(aDouble)).collect(Collectors.toList());

        EntityDataSortOrder sortByTempOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.TIME_SERIES, "temperature"), EntityDataSortOrder.Direction.ASC);
        EntityDataPageLink sortByTempPageLink = new EntityDataPageLink(10, 0, null, sortByTempOrder);
        EntityDataQuery querySortByTemp = new EntityDataQuery(filter, sortByTempPageLink, entityFields, latestValues, null);

        List<EntityData> loadedEntities = loadAllData(querySortByTemp, deviceTemperatures.size());
        List<String> entitiesTelemetry = loadedEntities.stream().map(entityData -> entityData.getLatest().get(EntityKeyType.TIME_SERIES).get("temperature").getValue()).toList();
        assertThat(entitiesTelemetry).containsExactlyElementsOf(expectedSortedList);

        // update temperature to long value for one of device
        long longTempValue = -100L;
        saveTimeseries(devices.get(new Random().nextInt(66)).getId(), "temperature", longTempValue).get();
        loadedEntities = loadAllData(querySortByTemp, deviceTemperatures.size());
        entitiesTelemetry = loadedEntities.stream().map(entityData -> entityData.getLatest().get(EntityKeyType.TIME_SERIES).get("temperature").getValue()).toList();
        assertThat(entitiesTelemetry.get(0)).isEqualTo(String.valueOf(longTempValue));

        deviceService.deleteDevicesByTenantId(tenantId);
    }

    @Test
    public void testFindTenantTelemetry() {
        // save timeseries by sys admin
        BasicTsKvEntry timeseries = new BasicTsKvEntry(42L, new DoubleDataEntry("temperature", 45.5));
        timeseriesService.save(TenantId.SYS_TENANT_ID, tenantId, timeseries);

        AttributeKvEntry attr = new BaseAttributeKvEntry(new LongDataEntry("attr", 10L), 42L);
        attributesService.save(TenantId.SYS_TENANT_ID, tenantId, SERVER_SCOPE, List.of(attr));

        SingleEntityFilter singleEntityFilter = new SingleEntityFilter();
        singleEntityFilter.setSingleEntity(AliasEntityId.fromEntityId(tenantId));

        List<EntityKey> entityFields = List.of(
                new EntityKey(ENTITY_FIELD, "name")
        );
        List<EntityKey> latestValues =  List.of(
                new EntityKey(EntityKeyType.TIME_SERIES, "temperature"),
                new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, "attr")
        );

        EntityDataPageLink pageLink = new EntityDataPageLink(1000, 0, null, null);
        EntityDataQuery query = new EntityDataQuery(singleEntityFilter, pageLink, entityFields, latestValues, null);

        findByQueryAndCheckTelemetry(query, EntityKeyType.TIME_SERIES, "temperature", List.of("45.5"));
        findByQueryAndCheckTelemetry(query, EntityKeyType.SERVER_ATTRIBUTE, "attr", List.of("10"));
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

        List<ListenableFuture<AttributesSaveResult>> attributeFutures = new ArrayList<>();
        for (int i = 0; i < devices.size(); i++) {
            Device device = devices.get(i);
            attributeFutures.add(saveStringAttribute(device.getId(), "attributeString", attributeStrings.get(i), AttributeScope.CLIENT_SCOPE));
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
        PageData<EntityData> data = findByQueryAndCheck(query, equalStrings.size());
        List<EntityData> loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(equalStrings.size(), loadedEntities.size());

        List<String> loadedStrings = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE).get("attributeString").getValue()).collect(Collectors.toList());

        Assert.assertTrue(listEqualWithoutOrder(equalStrings, loadedStrings));

        // Not equal Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFiltersNotEqualString);
        data = findByQueryAndCheck(query, notEqualStrings.size());
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(notEqualStrings.size(), loadedEntities.size());

        loadedStrings = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE).get("attributeString").getValue()).collect(Collectors.toList());

        Assert.assertTrue(listEqualWithoutOrder(notEqualStrings, loadedStrings));

        // Starts with Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFiltersStartsWithString);
        data = findByQueryAndCheck(query, startsWithStrings.size());
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(startsWithStrings.size(), loadedEntities.size());

        loadedStrings = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE).get("attributeString").getValue()).collect(Collectors.toList());

        Assert.assertTrue(listEqualWithoutOrder(startsWithStrings, loadedStrings));

        // Ends with Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFiltersEndsWithString);
        data = findByQueryAndCheck(query, endsWithStrings.size());
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(endsWithStrings.size(), loadedEntities.size());

        loadedStrings = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE).get("attributeString").getValue()).collect(Collectors.toList());

        Assert.assertTrue(listEqualWithoutOrder(endsWithStrings, loadedStrings));

        // Contains Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFiltersContainsString);
        data = findByQueryAndCheck(query, containsStrings.size());
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(containsStrings.size(), loadedEntities.size());

        loadedStrings = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE).get("attributeString").getValue()).collect(Collectors.toList());

        Assert.assertTrue(listEqualWithoutOrder(containsStrings, loadedStrings));

        // Not contains Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFiltersNotContainsString);
        data = findByQueryAndCheck(query, notContainsStrings.size());
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(notContainsStrings.size(), loadedEntities.size());

        loadedStrings = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE).get("attributeString").getValue()).collect(Collectors.toList());

        Assert.assertTrue(listEqualWithoutOrder(notContainsStrings, loadedStrings));

        // Device type filters Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, deviceTypeFilters);
        data = findByQueryAndCheck(query, devices.size());
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
        PageData<EntityData> data = findByQueryAndCheck(query, devices.size());
        List<EntityData> loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(devices.size(), loadedEntities.size());

        List<String> loadedStrings = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue()).collect(Collectors.toList());

        List<String> devicesNames = devices.stream().map(Device::getName).collect(Collectors.toList());

        Assert.assertTrue(listEqualWithoutOrder(devicesNames, loadedStrings));

        // Not equal Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, null, keyFiltersNotEqualString);
        data = findByQueryAndCheck(query, devices.size());
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(devices.size(), loadedEntities.size());

        loadedStrings = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue()).collect(Collectors.toList());

        Assert.assertTrue(listEqualWithoutOrder(devicesNames, loadedStrings));

        // Starts with Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, null, keyFiltersStartsWithString);
        data = findByQueryAndCheck(query, devices.size());
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(devices.size(), loadedEntities.size());

        loadedStrings = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue()).collect(Collectors.toList());

        Assert.assertTrue(listEqualWithoutOrder(devicesNames, loadedStrings));

        // Ends with Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, null, keyFiltersEndsWithString);
        data = findByQueryAndCheck(query, devices.size());
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(devices.size(), loadedEntities.size());

        loadedStrings = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue()).collect(Collectors.toList());

        Assert.assertTrue(listEqualWithoutOrder(devicesNames, loadedStrings));

        // Contains Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, null, keyFiltersContainsString);
        data = findByQueryAndCheck(query, devices.size());
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(devices.size(), loadedEntities.size());

        loadedStrings = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue()).collect(Collectors.toList());

        Assert.assertTrue(listEqualWithoutOrder(devicesNames, loadedStrings));

        // Not contains Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, null, keyFiltersNotContainsString);
        data = findByQueryAndCheck(query, devices.size());
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
        PageData<EntityData> data = findByQueryAndCheck(query, devices.size());
        List<EntityData> loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(devices.size(), loadedEntities.size());

        // Device create time filters

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, null, createdTimeFilters);
        data = findByQueryAndCheck(query, devices.size());
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(devices.size(), loadedEntities.size());

        // Device name filters

        pageLink = new EntityDataPageLink(100, 0, null, null);
        query = new EntityDataQuery(filter, pageLink, entityFields, null, nameFilters);
        data = findByQueryAndCheck(query, devices.size());
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(devices.size(), loadedEntities.size());

        deviceService.deleteDevicesByTenantId(tenantId);
    }

    @Test
    public void testFindEntityQuery_for_5000_devices_with_3000_pageSize() {
        int pageSize = 3000;
        int expectedDevicesSize = 4000;
        int unexpectedDevicesSize = 1000;

        for (int i = 0; i < expectedDevicesSize + unexpectedDevicesSize; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            if (i < expectedDevicesSize) {
                device.setName("Device_" + i); // match deviceNameFilter 'D%'
            } else {
                device.setName("Test_" + i); // does not match deviceNameFilter 'D%'
            }
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            Device savedDevice = deviceService.saveDevice(device);

            attributesService.save(tenantId, savedDevice.getId(), AttributeScope.CLIENT_SCOPE,
                    new BaseAttributeKvEntry(System.currentTimeMillis(), new LongDataEntry("telemetry", (long) i)));
        }

        DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceTypes(List.of("default"));
        filter.setDeviceNameFilter("D%");

        EntityDataSortOrder sortOrder = new EntityDataSortOrder(new EntityKey(ATTRIBUTE, "telemetry"), EntityDataSortOrder.Direction.DESC);

        List<KeyFilter> deviceTypeFilters = createStringKeyFilters("type", ENTITY_FIELD, StringFilterPredicate.StringOperation.EQUAL, "default");

        List<KeyFilter> attributeFilters = Collections.singletonList(createNumericKeyFilter("telemetry", ATTRIBUTE, NumericFilterPredicate.NumericOperation.LESS, expectedDevicesSize));

        List<KeyFilter> nameFilters = createStringKeyFilters("name", ENTITY_FIELD, StringFilterPredicate.StringOperation.CONTAINS, "Device");

        List<EntityKey> entityFields = Arrays.asList(new EntityKey(ENTITY_FIELD, "name"), new EntityKey(ENTITY_FIELD, "type"));

        // 1. Device type filters:

        // query with textSearch - optimization is not performing
        EntityDataPageLink originalPageLink = new EntityDataPageLink(pageSize, 0, "Device", sortOrder);
        EntityDataQuery originalQuery = new EntityDataQuery(filter, originalPageLink, entityFields, null, deviceTypeFilters);
        PageData<EntityData> originalData = findByQueryAndCheck(originalQuery, expectedDevicesSize);

        // query without textSearch - optimization is performing
        EntityDataPageLink optimizedPageLink = new EntityDataPageLink(pageSize, 0, null, sortOrder);
        EntityDataQuery optimizedQuery = new EntityDataQuery(filter, optimizedPageLink, entityFields, null, deviceTypeFilters);
        PageData<EntityData> optimizedData = findByQueryAndCheck(optimizedQuery, expectedDevicesSize);
        List<EntityData> loadedEntities = getLoadedEntities(optimizedData, optimizedQuery);
        Assert.assertEquals(expectedDevicesSize, loadedEntities.size());
        loadedEntities = getLoadedEntities(originalData, originalQuery);
        Assert.assertEquals(expectedDevicesSize, loadedEntities.size());
        Assert.assertEquals(pageSize, optimizedData.getData().size());

        for (int i = 0; i < pageSize; i++) {
            EntityData originalElement = originalData.getData().get(i);
            EntityData optimizedElement = optimizedData.getData().get(i);
            Assert.assertEquals(originalElement.getEntityId(), optimizedElement.getEntityId());
            originalElement.getLatest().get(ENTITY_FIELD).forEach((key, value) -> {
                Assert.assertEquals(value.getValue(), optimizedElement.getLatest().get(EntityKeyType.ENTITY_FIELD).get(key).getValue());
                Assert.assertEquals(value.getCount(), optimizedElement.getLatest().get(EntityKeyType.ENTITY_FIELD).get(key).getCount());
            });
        }
        Assert.assertEquals(originalData.getTotalPages(), optimizedData.getTotalPages());
        Assert.assertEquals(originalData.getTotalElements(), optimizedData.getTotalElements());

        // 2. Device attribute filters

        // query with textSearch - optimization is not performing
        originalPageLink = new EntityDataPageLink(pageSize, 0, "Device", sortOrder);
        originalQuery = new EntityDataQuery(filter, originalPageLink, entityFields, null, attributeFilters);
        originalData = findByQuery(originalQuery);

        // query without textSearch - optimization is performing
        optimizedPageLink = new EntityDataPageLink(pageSize, 0, null, sortOrder);
        optimizedQuery = new EntityDataQuery(filter, optimizedPageLink, entityFields, null, attributeFilters);
        optimizedData = findByQuery(optimizedQuery);
        loadedEntities = getLoadedEntities(optimizedData, optimizedQuery);
        Assert.assertEquals(expectedDevicesSize, loadedEntities.size());
        loadedEntities = getLoadedEntities(originalData, originalQuery);
        Assert.assertEquals(expectedDevicesSize, loadedEntities.size());
        Assert.assertEquals(pageSize, optimizedData.getData().size());

        for (int i = 0; i < pageSize; i++) {
            EntityData originalElement = originalData.getData().get(i);
            EntityData optimizedElement = optimizedData.getData().get(i);
            Assert.assertEquals(originalElement.getEntityId(), optimizedElement.getEntityId());
            originalElement.getLatest().get(ENTITY_FIELD).forEach((key, value) -> {
                Assert.assertEquals(value.getValue(), optimizedElement.getLatest().get(EntityKeyType.ENTITY_FIELD).get(key).getValue());
                Assert.assertEquals(value.getCount(), optimizedElement.getLatest().get(EntityKeyType.ENTITY_FIELD).get(key).getCount());
            });
        }
        Assert.assertEquals(originalData.getTotalPages(), optimizedData.getTotalPages());
        Assert.assertEquals(originalData.getTotalElements(), optimizedData.getTotalElements());

        // 3. Device name filters

        // query with textSearch - optimization is not performing
        originalPageLink = new EntityDataPageLink(pageSize, 0, "Device", sortOrder);
        originalQuery = new EntityDataQuery(filter, originalPageLink, entityFields, null, nameFilters);
        originalData = findByQuery(originalQuery);

        // query without textSearch - optimization is performing
        optimizedPageLink = new EntityDataPageLink(pageSize, 0, null, sortOrder);
        optimizedQuery = new EntityDataQuery(filter, optimizedPageLink, entityFields, null, nameFilters);
        optimizedData = findByQuery(optimizedQuery);
        loadedEntities = getLoadedEntities(optimizedData, optimizedQuery);
        Assert.assertEquals(expectedDevicesSize, loadedEntities.size());
        loadedEntities = getLoadedEntities(originalData, originalQuery);
        Assert.assertEquals(expectedDevicesSize, loadedEntities.size());
        Assert.assertEquals(pageSize, optimizedData.getData().size());

        for (int i = 0; i < pageSize; i++) {
            EntityData originalElement = originalData.getData().get(i);
            EntityData optimizedElement = optimizedData.getData().get(i);
            Assert.assertEquals(originalElement.getEntityId(), optimizedElement.getEntityId());
            originalElement.getLatest().get(ENTITY_FIELD).forEach((key, value) -> {
                Assert.assertEquals(value.getValue(), optimizedElement.getLatest().get(EntityKeyType.ENTITY_FIELD).get(key).getValue());
                Assert.assertEquals(value.getCount(), optimizedElement.getLatest().get(EntityKeyType.ENTITY_FIELD).get(key).getCount());
            });
        }
        Assert.assertEquals(originalData.getTotalPages(), optimizedData.getTotalPages());
        Assert.assertEquals(originalData.getTotalElements(), optimizedData.getTotalElements());

        deviceService.deleteDevicesByTenantId(tenantId);
    }

    private Boolean listEqualWithoutOrder(List<String> A, List<String> B) {
        return A.containsAll(B) && B.containsAll(A);
    }

    private List<EntityData> getLoadedEntities(PageData<EntityData> data, EntityDataQuery query) {
        List<EntityData> loadedEntities = new ArrayList<>(data.getData());
        while (data.hasNext()) {
            query = query.next();
            data = findByQuery(query);
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

    private ListenableFuture<AttributesSaveResult> saveLongAttribute(EntityId entityId, String key, long value, AttributeScope scope) {
        KvEntry attrValue = new LongDataEntry(key, value);
        AttributeKvEntry attr = new BaseAttributeKvEntry(attrValue, 42L);
        return attributesService.save(tenantId, entityId, scope, List.of(attr));
    }

    private ListenableFuture<AttributesSaveResult> saveStringAttribute(EntityId entityId, String key, String value, AttributeScope scope) {
        KvEntry attrValue = new StringDataEntry(key, value);
        AttributeKvEntry attr = new BaseAttributeKvEntry(attrValue, 42L);
        return attributesService.save(tenantId, entityId, scope, List.of(attr));
    }

    private ListenableFuture<TimeseriesSaveResult> saveTimeseries(EntityId entityId, String key, Double value) {
        KvEntry telemetryValue = new DoubleDataEntry(key, value);
        BasicTsKvEntry timeseries = new BasicTsKvEntry(42L, telemetryValue);
        return timeseriesService.save(tenantId, entityId, timeseries);
    }

    private ListenableFuture<TimeseriesSaveResult> saveTimeseries(EntityId entityId, String key, Long value) {
        KvEntry telemetryValue = new LongDataEntry(key, value);
        BasicTsKvEntry timeseries = new BasicTsKvEntry(42L, telemetryValue);
        return timeseriesService.save(tenantId, entityId, timeseries);
    }

    protected void createMultiRootHierarchy(List<Asset> buildings, List<Asset> apartments,
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

    @Test
    public void testFindEntitiesWithEntityViewFilter() {
        EntityView entityView = new EntityView();
        entityView.setTenantId(tenantId);
        entityView.setCustomerId(customerId);
        entityView.setName("test");
        entityView.setType("default");
        entityView.setEntityId(new DeviceId(UUID.randomUUID()));
        entityView.setKeys(new TelemetryEntityView(List.of("test"), null));
        entityView.setStartTimeMs(124);
        entityView.setEndTimeMs(256);
        entityView.setExternalId(new EntityViewId(UUID.randomUUID()));
        entityView.setAdditionalInfo(JacksonUtil.newObjectNode().put("test", "test"));
        entityView = entityViewService.saveEntityView(entityView);

        EntityViewTypeFilter entityViewTypeFilter = new EntityViewTypeFilter();
        entityViewTypeFilter.setEntityViewNameFilter("test");
        entityViewTypeFilter.setEntityViewTypes(List.of("non-existing", "default"));
        EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, null);
        List<EntityKey> entityFields = List.of(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "name")
        );
        EntityDataQuery query = new EntityDataQuery(entityViewTypeFilter, pageLink, entityFields, Collections.emptyList(), null);

        PageData<EntityData> relationsResult = findByQueryAndCheck(new CustomerId(EntityId.NULL_UUID), query, 1);
        assertThat(relationsResult.getData().get(0).getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue()).isEqualTo(entityView.getName());

        // find with non existing name
        entityViewTypeFilter.setEntityViewNameFilter("non-existing");
        findByQueryAndCheck(new CustomerId(EntityId.NULL_UUID), query, 0);

        // find with non existing type
        entityViewTypeFilter.setEntityViewNameFilter(null);
        entityViewTypeFilter.setEntityViewTypes(Collections.singletonList("non-existing"));

        findByQueryAndCheck(new CustomerId(EntityId.NULL_UUID), query, 0);
    }

    protected PageData<EntityData> findByQuery(EntityDataQuery query) {
        return findByQuery(new CustomerId(CustomerId.NULL_UUID), query);
    }

    protected PageData<EntityData> findByQuery(CustomerId customerId, EntityDataQuery query) {
        return entityService.findEntityDataByQuery(tenantId, customerId, query);
    }

    protected PageData<EntityData> findByQueryAndCheck(EntityDataQuery query, long expectedResultSize) {
        return findByQueryAndCheck(new CustomerId(CustomerId.NULL_UUID), query, expectedResultSize);
    }

    protected PageData<EntityData> findByQueryAndCheck(CustomerId customerId, EntityDataQuery query, long expectedResultSize) {
        PageData<EntityData> result = entityService.findEntityDataByQuery(tenantId, customerId, query);
        assertThat(result.getTotalElements()).isEqualTo(expectedResultSize);
        return result;
    }

    protected List<EntityData> findByQueryAndCheckTelemetry(EntityDataQuery query, EntityKeyType entityKeyType, String key, List<String> expectedTelemetry) {
        List<EntityData> loadedEntities = loadAllData(query, expectedTelemetry.size());
        List<String> entitiesTelemetry = loadedEntities.stream().map(entityData -> entityData.getLatest().get(entityKeyType).get(key).getValue()).toList();
        assertThat(entitiesTelemetry).containsExactlyInAnyOrderElementsOf(expectedTelemetry);
        return loadedEntities;
    }

    protected List<EntityData> loadAllData(EntityDataQuery query, int expectedSize) {
        PageData<EntityData> data = findByQueryAndCheck(query, expectedSize);
        List<EntityData> loadedEntities = new ArrayList<>(data.getData());
        while (data.hasNext()) {
            query = query.next();
            data = findByQuery(query);
            loadedEntities.addAll(data.getData());
        }
        return loadedEntities;
    }

    protected long countByQuery(CustomerId customerId, EntityCountQuery query) {
        return entityService.countEntitiesByQuery(tenantId, customerId, query);
    }

    protected long countByQueryAndCheck(EntityCountQuery countQuery, int expectedResult) {
        return countByQueryAndCheck(new CustomerId(CustomerId.NULL_UUID), countQuery, expectedResult);
    }

    protected long countByQueryAndCheck(CustomerId customerId, EntityCountQuery query, int expectedResult) {
        long result = countByQuery(customerId, query);
        assertThat(result).isEqualTo(expectedResult);
        return result;
    }

}
