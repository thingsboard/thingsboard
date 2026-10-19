// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.entitiy;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.alarm.AlarmCreateOrUpdateActiveRequest;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.AttributesSaveResult;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TimeseriesSaveResult;
import org.thingsboard.server.common.data.objects.TelemetryEntityView;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.permission.MergedGroupPermissionInfo;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.query.AliasEntityId;
import org.thingsboard.server.common.data.query.ApiUsageStateFilter;
import org.thingsboard.server.common.data.query.AssetSearchQueryFilter;
import org.thingsboard.server.common.data.query.AssetTypeFilter;
import org.thingsboard.server.common.data.query.DeviceSearchQueryFilter;
import org.thingsboard.server.common.data.query.DeviceTypeFilter;
import org.thingsboard.server.common.data.query.EdgeSearchQueryFilter;
import org.thingsboard.server.common.data.query.EdgeTypeFilter;
import org.thingsboard.server.common.data.query.EntitiesByGroupNameFilter;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityGroupFilter;
import org.thingsboard.server.common.data.query.EntityGroupListFilter;
import org.thingsboard.server.common.data.query.EntityGroupNameFilter;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.EntityListFilter;
import org.thingsboard.server.common.data.query.EntityNameFilter;
import org.thingsboard.server.common.data.query.EntityTypeFilter;
import org.thingsboard.server.common.data.query.EntityViewTypeFilter;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.NumericFilterPredicate;
import org.thingsboard.server.common.data.query.RelationsQueryFilter;
import org.thingsboard.server.common.data.query.SchedulerEventFilter;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.query.StateEntityOwnerFilter;
import org.thingsboard.server.common.data.query.StringFilterPredicate;
import org.thingsboard.server.common.data.query.StringFilterPredicate.StringOperation;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetDao;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.blob.BlobEntityService;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceDao;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeDao;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.sql.query.EntityMapping;
import org.thingsboard.server.dao.sql.query.DefaultEntityQueryRepository;
import org.thingsboard.server.dao.sql.relation.RelationRepository;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateService;
import org.thingsboard.server.dao.user.UserDao;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetTypeService;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.thingsboard.server.common.data.AttributeScope.SERVER_SCOPE;
import static org.thingsboard.server.common.data.permission.Resource.ALL;
import static org.thingsboard.server.common.data.permission.Resource.CUSTOMER;
import static org.thingsboard.server.common.data.query.EntityKeyType.ATTRIBUTE;
import static org.thingsboard.server.common.data.query.EntityKeyType.ENTITY_FIELD;

@Slf4j
@DaoSqlTest
public class EntityServiceTest extends AbstractControllerTest {

    static final int ENTITY_COUNT = 5;
    public static final String TEST_CUSTOMER_NAME = "Test customer";
    public static final String OTHER_TEST_CUSTOMER_NAME = "Other test customer";

    @Autowired
    AssetService assetService;
    @Autowired
    UserService userService;
    @Autowired
    AttributesService attributesService;
    @Autowired
    BlobEntityService blobEntityService;
    @Autowired
    CustomerService customerService;
    @Autowired
    DeviceService deviceService;
    @Autowired
    DashboardService dashboardService;
    @Autowired
    EdgeService edgeService;
    @Getter
    @Autowired
    EntityGroupService entityGroupService;
    @Autowired
    EntityService entityService;
    @Autowired
    RelationRepository relationRepository;
    @Autowired
    DefaultEntityQueryRepository entityQueryRepository;
    @Autowired
    RelationService relationService;
    @Autowired
    TimeseriesService timeseriesService;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    DeviceDao deviceDao;
    @Autowired
    DeviceProfileService deviceProfileService;
    @Autowired
    AssetDao assetDao;
    @Autowired
    AssetProfileService assetProfileService;
    @Autowired
    EntityViewService entityViewService;
    @Autowired
    EdgeDao edgeDao;
    @Autowired
    CustomerDao customerDao;
    @Autowired
    UserDao userDao;
    @Autowired
    ApiUsageStateService apiUsageStateService;
    @Autowired
    SchedulerEventService schedulerEventService;
    @Autowired
    RoleService roleService;
    @Autowired
    AlarmService alarmService;
    @Autowired
    WidgetTypeService widgetTypeService;

    protected MergedUserPermissions mergedUserPermissionsPE;
    private CustomerId customerId;
    private CustomerId otherCustomerId;

    @Before
    public void before() {
        Map<Resource, Set<Operation>> genericPermissions = new HashMap<>();
        genericPermissions.put(Resource.resourceFromEntityType(EntityType.DEVICE), Collections.singleton(Operation.ALL));
        genericPermissions.put(Resource.resourceFromEntityType(EntityType.ASSET), Collections.singleton(Operation.ALL));
        genericPermissions.put(Resource.DEVICE_GROUP, Collections.singleton(Operation.ALL));
        genericPermissions.put(Resource.USER, Collections.singleton(Operation.ALL));
        genericPermissions.put(Resource.API_USAGE_STATE, Collections.singleton(Operation.ALL));
        genericPermissions.put(Resource.TENANT, Collections.singleton(Operation.ALL));
        genericPermissions.put(Resource.CUSTOMER, Collections.singleton(Operation.ALL));
        genericPermissions.put(Resource.ENTITY_VIEW, Collections.singleton(Operation.ALL));
        genericPermissions.put(Resource.DASHBOARD, Collections.singleton(Operation.ALL));
        genericPermissions.put(Resource.EDGE, Collections.singleton(Operation.ALL));
        genericPermissions.put(Resource.EDGE_GROUP, Collections.singleton(Operation.ALL));
        genericPermissions.put(Resource.SCHEDULER_EVENT, Collections.singleton(Operation.ALL));
        mergedUserPermissionsPE = new MergedUserPermissions(genericPermissions, Collections.emptyMap());

        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle(TEST_CUSTOMER_NAME);
        customer = customerService.saveCustomer(customer);
        customerId = customer.getId();

        Customer otherCustomer = new Customer();
        otherCustomer.setTenantId(tenantId);
        otherCustomer.setTitle(OTHER_TEST_CUSTOMER_NAME);
        otherCustomer = customerService.saveCustomer(otherCustomer);
        otherCustomerId = otherCustomer.getId();
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
    public void testCountEntitiesByCustomerUser() {
        EntityGroup deviceGroup = new EntityGroup();
        deviceGroup.setName("Device Tenant Level Group");
        deviceGroup.setOwnerId(customerId);
        deviceGroup.setTenantId(tenantId);
        deviceGroup.setType(EntityType.DEVICE);
        deviceGroup = entityGroupService.saveEntityGroup(tenantId, tenantId, deviceGroup);

        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setCustomerId(customerId);
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            Device savedDevice = deviceService.saveDevice(device);
            devices.add(savedDevice);
            if (i % 2 == 0) {
                entityGroupService.addEntityToEntityGroup(tenantId, deviceGroup.getId(), savedDevice.getId());
            }
        }

        DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceTypes(List.of("default"));
        filter.setDeviceNameFilter("");

        EntityCountQuery countQuery = new EntityCountQuery(filter);

        //count entities by customer user with group permission
        HashMap<EntityGroupId, MergedGroupPermissionInfo> groupPermission = new HashMap<>();
        groupPermission.put(deviceGroup.getId(), new MergedGroupPermissionInfo(EntityType.DEVICE, Set.of(Operation.READ)));
        MergedUserPermissions mergedGroupPermission = new MergedUserPermissions(Collections.emptyMap(), groupPermission);

        long count = countByQueryAndCheck(customerId, mergedGroupPermission, countQuery, 10);

        //count entities by customer user with generic permission
        Map<Resource, Set<Operation>> genericDevicePermission = Map.of(Resource.DEVICE, Set.of(Operation.READ),
                Resource.DEVICE_GROUP, Set.of(Operation.READ));
        MergedUserPermissions mergedGenericPermission = new MergedUserPermissions(genericDevicePermission, Collections.emptyMap());

        countByQueryAndCheck(customerId, mergedGenericPermission, countQuery, 20);

        // count entities by customer user with no permission
        MergedUserPermissions mergedPermission = new MergedUserPermissions(Collections.emptyMap(), Collections.emptyMap());
        countByQueryAndCheck(customerId, mergedPermission, countQuery, 0);

        // count entities by other customer user with generic and group permission
        MergedUserPermissions otherCustomerUserPermission = new MergedUserPermissions(genericDevicePermission, groupPermission);
        countByQueryAndCheck(otherCustomerId, otherCustomerUserPermission, countQuery, 10);
    }

    @Test
    public void testFindEntitiesByGroupQueryPE() throws InterruptedException, ExecutionException {
        EntityGroup evenDeviceGroup = new EntityGroup();
        evenDeviceGroup.setName("Even");
        evenDeviceGroup.setOwnerId(tenantId);
        evenDeviceGroup.setType(EntityType.DEVICE);
        evenDeviceGroup = entityGroupService.saveEntityGroup(tenantId, tenantId, evenDeviceGroup);
        EntityGroup oddDeviceGroup = new EntityGroup();
        oddDeviceGroup.setName("Odd");
        oddDeviceGroup.setOwnerId(tenantId);
        oddDeviceGroup.setType(EntityType.DEVICE);
        oddDeviceGroup = entityGroupService.saveEntityGroup(tenantId, tenantId, oddDeviceGroup);

        List<Device> evenDevices = new ArrayList<>();
        List<Device> oddDevices = new ArrayList<>();
        for (int i = 0; i < 97; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            device = deviceService.saveDevice(device);
            Thread.sleep(1);
            EntityGroup group;
            if (i % 2 == 0) {
                evenDevices.add(device);
                group = evenDeviceGroup;
            } else {
                oddDevices.add(device);
                group = oddDeviceGroup;
            }
            entityGroupService.addEntityToEntityGroup(tenantId, group.getId(), device.getId());
        }

        EntityGroupFilter evenFilter = new EntityGroupFilter();
        evenFilter.setEntityGroup(evenDeviceGroup.getId().getId().toString());
        evenFilter.setGroupType(EntityType.DEVICE);

        EntityGroupFilter oddFilter = new EntityGroupFilter();
        oddFilter.setEntityGroup(oddDeviceGroup.getId().getId().toString());
        oddFilter.setGroupType(EntityType.DEVICE);

        EntityCountQuery evenCountQuery = new EntityCountQuery(evenFilter);
        EntityCountQuery oddCountQuery = new EntityCountQuery(oddFilter);

        countByQueryAndCheck(evenCountQuery, evenDevices.size());
        countByQueryAndCheck(oddCountQuery, oddDevices.size());

        List<Long> temperatures = new ArrayList<>();
        Random random = new Random();
        List<ListenableFuture<AttributesSaveResult>> attributeFutures = new ArrayList<>();
        long lastUpdateTs = System.currentTimeMillis() - 1024 * 1024;
        for (int i = 0; i < evenDevices.size(); i++) {
            Device device = evenDevices.get(i);
            long temp = random.nextLong();
            attributeFutures.add(saveLongAttribute(device.getId(), "temperature", temp - 1, lastUpdateTs++, AttributeScope.CLIENT_SCOPE));
            attributeFutures.add(saveLongAttribute(device.getId(), "temperature", temp, lastUpdateTs++, AttributeScope.SHARED_SCOPE));
            temperatures.add(temp);
        }
        Futures.successfulAsList(attributeFutures).get();

        List<String> deviceTemperatures = temperatures.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());

        EntityDataSortOrder sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.ASC
        );
        EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        List<EntityKey> entityFields = Collections.singletonList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));
        List<EntityKey> latestValues = Collections.singletonList(new EntityKey(EntityKeyType.ATTRIBUTE, "temperature"));

        EntityDataQuery query = new EntityDataQuery(evenFilter, pageLink, entityFields, latestValues, null);
        findByQueryAndCheckTelemetry(query, EntityKeyType.ATTRIBUTE, "temperature", deviceTemperatures);
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
    public void testCountHierarchicalUserEntitiesByQuery() {
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
        Assert.assertEquals(5, data.size());
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
    public void testCountEdgeEntitiesByQuery() {
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
        edge.setEdgeLicenseKey(StringUtils.randomAlphanumeric(20));
        edge.setCloudEndpoint("http://localhost:8080");
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
        MatcherAssert.assertThat("ids lenght", ids.length, Matchers.greaterThanOrEqualTo(1));
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
    public void testRelationsQueryReturnsEntityViewFields() {
        // Traverses relations over ENTITY_VIEW rows and asserts their fields come back, so the ENTITY_VIEW
        // arm of the Citus relation-traversal prefetch (the only hasLabel=false arm of prefetchDistributedRows)
        // executes on real Citus via the CitusEntityServiceTest rerun.
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("Device with entity views");
        device.setType("default");
        device = deviceService.saveDevice(device);
        createRelation(tenantId, "Contains", tenantId, device.getId());

        List<EntityView> entityViews = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            EntityView entityView = new EntityView();
            entityView.setTenantId(tenantId);
            entityView.setName("Relation query entity view " + i);
            entityView.setType("entityViewType" + i);
            entityView.setEntityId(device.getId());
            entityView = entityViewService.saveEntityView(entityView);
            entityViews.add(entityView);
            createRelation(tenantId, "Contains", device.getId(), entityView.getId());
        }

        RelationsQueryFilter filter = new RelationsQueryFilter();
        filter.setRootEntity(AliasEntityId.fromEntityId(tenantId));
        filter.setDirection(EntitySearchDirection.FROM);
        filter.setMaxLevel(2);
        filter.setFilters(Collections.singletonList(new RelationEntityTypeFilter("Contains", Collections.singletonList(EntityType.ENTITY_VIEW))));

        List<EntityKey> entityFields = Arrays.asList(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "name"),
                new EntityKey(EntityKeyType.ENTITY_FIELD, "type"));
        EntityDataQuery query = new EntityDataQuery(filter, new EntityDataPageLink(10, 0, null, null), entityFields, null, null);

        PageData<EntityData> result = findByQueryAndCheck(query, entityViews.size());
        Map<EntityId, EntityData> entityDataById = result.getData().stream()
                .collect(Collectors.toMap(EntityData::getEntityId, entityData -> entityData));
        for (EntityView entityView : entityViews) {
            EntityData entityData = entityDataById.get(entityView.getId());
            assertThat(entityData).as("entity view %s must be returned by the relations query", entityView.getName()).isNotNull();
            assertThat(entityData.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue()).isEqualTo(entityView.getName());
            assertThat(entityData.getLatest().get(EntityKeyType.ENTITY_FIELD).get("type").getValue()).isEqualTo(entityView.getType());
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

        // FIXME: this and the tests below submit an empty value to a key filter, which is not a "search text" field.
        //  Establish whether an empty filter value is meant to match every entity, and pin the intended behaviour here.
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

        String suffixes = RandomStringUtils.secure().nextAlphanumeric(5);
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
    public void testFindEntitiesByEntityGroupNameFilter() {
        List<EntityGroup> groups = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            EntityGroup entityGroup = new EntityGroup();
            entityGroup.setName("group test" + i + " name");
            entityGroup.setType(EntityType.DEVICE);
            groups.add(entityGroupService.saveEntityGroup(tenantId, tenantId, entityGroup));
        }

        EntityGroupNameFilter entityGroupNameFilter = new EntityGroupNameFilter();
        entityGroupNameFilter.setGroupType(EntityType.DEVICE);
        entityGroupNameFilter.setEntityGroupNameFilter("group");
        EntityDataPageLink pageLink = new EntityDataPageLink(1000, 0, null, null);
        EntityDataQuery query = new EntityDataQuery(entityGroupNameFilter, pageLink, null, null, null);
        findByQueryAndCheck(query, groups.size());

        entityGroupNameFilter.setEntityGroupNameFilter("group%");
        findByQueryAndCheck(query, groups.size());

        entityGroupNameFilter.setEntityGroupNameFilter("%group%");
        findByQueryAndCheck(query, groups.size());

        entityGroupNameFilter.setEntityGroupNameFilter("%group");
        findByQueryAndCheck(query, 0);

        entityGroupNameFilter.setEntityGroupNameFilter("%name");
        findByQueryAndCheck(query, groups.size());

        entityGroupNameFilter.setEntityGroupNameFilter("name%");
        findByQueryAndCheck(query, 0);

        entityGroupNameFilter.setEntityGroupNameFilter("name");
        findByQueryAndCheck(query, 0);
    }

    @Test
    public void testFindEntitiesBySingleEntityFilter() {
        List<Device> customerDevices = new ArrayList<>();
        List<Device> tenantDevices = new ArrayList<>();

        EntityGroup customerDeviceGroup = new EntityGroup();
        customerDeviceGroup.setName("Customer device group");
        customerDeviceGroup.setOwnerId(customerId);
        customerDeviceGroup.setTenantId(tenantId);
        customerDeviceGroup.setType(EntityType.DEVICE);
        customerDeviceGroup = entityGroupService.saveEntityGroup(tenantId, customerId, customerDeviceGroup);

        for (int i = 0; i < 3; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setCustomerId(customerId);
            device.setName("Device test" + i);
            device.setType("default");
            Device saved = deviceService.saveDevice(device);
            customerDevices.add(saved);
            entityGroupService.addEntityToEntityGroup(tenantId, customerDeviceGroup.getId(), saved.getId());
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
        MergedUserPermissions mergedGenericPermission = new MergedUserPermissions(Map.of(Resource.DEVICE, Set.of(Operation.READ)), Collections.emptyMap());
        PageData<EntityData> customerResults = findByQueryAndCheck(customerId, mergedGenericPermission, query, 1);

        String cutomerDeviceName = customerResults.getData().get(0).getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue();
        assertThat(cutomerDeviceName).isEqualTo(customerDevices.get(0).getName());

        // find by customer user with group permission
        MergedUserPermissions mergedGroupOnlyPermission = new MergedUserPermissions(Collections.emptyMap(), Map.of(customerDeviceGroup.getId(), new MergedGroupPermissionInfo(EntityType.DEVICE, Set.of(Operation.READ))));
        PageData<EntityData> result2 = findByQueryAndCheck(customerId, mergedGroupOnlyPermission, query, 1);

        String resultDeviceName2 = result2.getData().get(0).getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue();
        assertThat(resultDeviceName2).isEqualTo(customerDevices.get(0).getName());

        // try to find tenant device by customer user
        SingleEntityFilter tenantDeviceFilter = new SingleEntityFilter();
        tenantDeviceFilter.setSingleEntity(AliasEntityId.fromEntityId(tenantDevices.get(0).getId()));
        EntityDataQuery customerQuery2 = new EntityDataQuery(tenantDeviceFilter, pageLink, entityFields, null, null);
        findByQueryAndCheck(customerId, mergedGenericPermission, customerQuery2, 0);

        // find by tenant user with group permission
        PageData<EntityData> results3 = findByQueryAndCheck(new CustomerId(EntityId.NULL_UUID), mergedGroupOnlyPermission, query, 1);

        String deviceName3 = results3.getData().get(0).getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue();
        assertThat(deviceName3).isEqualTo(customerDevices.get(0).getName());
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

        // find by customer user with generic permission
        MergedUserPermissions mergedGenericPermission = new MergedUserPermissions(Map.of(CUSTOMER, Set.of(Operation.READ)), Collections.emptyMap());
        PageData<EntityData> customerResults = findByQueryAndCheck(customerId, mergedGenericPermission, query, 1);

        String cutomerDeviceName = customerResults.getData().get(0).getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue();
        assertThat(cutomerDeviceName).isEqualTo(TEST_CUSTOMER_NAME);
    }

    @Test
    public void testFindEntityGroupBySingleEntityFilterWithLatestValues() throws Exception {
        // Pins that a SINGLE_ENTITY filter resolving to an entity group does NOT take the Citus latest-value
        // pushdown arm: entity_group is a reference table (a pushdown anchor by type), and only the entity-group
        // carve-out in buildQueryContext keeps these queries on the coordinator-side join form. The Citus rerun
        // (CitusEntityServiceTest) executes this against a real cluster where an illegal pushdown join would fail.
        EntityGroup deviceGroup = new EntityGroup();
        deviceGroup.setName("Single entity filter group");
        deviceGroup.setOwnerId(tenantId);
        deviceGroup.setTenantId(tenantId);
        deviceGroup.setType(EntityType.DEVICE);
        deviceGroup = entityGroupService.saveEntityGroup(tenantId, tenantId, deviceGroup);

        saveStringAttribute(deviceGroup.getId(), "groupAttribute", "groupValue", SERVER_SCOPE).get(TIMEOUT, TimeUnit.SECONDS);

        SingleEntityFilter singleEntityFilter = new SingleEntityFilter();
        singleEntityFilter.setSingleEntity(AliasEntityId.fromEntityId(deviceGroup.getId()));
        List<EntityKey> entityFields = List.of(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));
        List<EntityKey> latestValues = List.of(new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, "groupAttribute"));
        EntityDataPageLink pageLink = new EntityDataPageLink(1000, 0, null, null);
        EntityDataQuery query = new EntityDataQuery(singleEntityFilter, pageLink, entityFields, latestValues, null);

        PageData<EntityData> result = findByQueryAndCheck(query, 1);
        EntityData entityData = result.getData().get(0);
        assertThat(entityData.getEntityId()).isEqualTo(deviceGroup.getId());
        assertThat(entityData.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue()).isEqualTo(deviceGroup.getName());
        assertThat(entityData.getLatest().get(EntityKeyType.SERVER_ATTRIBUTE).get("groupAttribute").getValue()).isEqualTo("groupValue");
    }

    @Test
    public void testFindEntitiesByEntityGroupListFilter() {
        List<EntityGroup> groups = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            EntityGroup entityGroup = new EntityGroup();
            entityGroup.setName("group test" + i + " name");
            entityGroup.setType(EntityType.DEVICE);
            groups.add(entityGroupService.saveEntityGroup(tenantId, tenantId, entityGroup));
        }

        EntityGroupListFilter entityGroupListFilter = new EntityGroupListFilter();
        entityGroupListFilter.setGroupType(EntityType.DEVICE);
        entityGroupListFilter.setEntityGroupList(List.of(groups.get(0).getId().getId().toString(), groups.get(1).getId().getId().toString()));

        List<EntityKey> entityFields = List.of(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "name")
        );

        EntityDataPageLink pageLink = new EntityDataPageLink(1000, 0, null, null);
        EntityDataQuery query = new EntityDataQuery(entityGroupListFilter, pageLink, entityFields, null, null);

        PageData<EntityData> result = findByQueryAndCheck(query, 2);
        countByQueryAndCheck(query, 2);

        List<String> loadedNames = result.getData().stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue()).collect(Collectors.toList());
        assertThat(loadedNames).containsExactlyInAnyOrder(groups.get(0).getName(), groups.get(1).getName());

        entityGroupListFilter.setEntityGroupList(List.of(UUID.randomUUID().toString()));
        findByQueryAndCheck(query, 0);
    }

    @Test
    public void testFindEntitiesWithEntityGroupListFilterByCustomerUser() {
        List<EntityGroup> groups = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            EntityGroup entityGroup = new EntityGroup();
            entityGroup.setName("group test" + i + " name");
            entityGroup.setType(EntityType.DEVICE);
            entityGroup.setOwnerId(customerId);
            groups.add(entityGroupService.saveEntityGroup(tenantId, customerId, entityGroup));
        }

        EntityGroupListFilter entityGroupListFilter = new EntityGroupListFilter();
        entityGroupListFilter.setGroupType(EntityType.DEVICE);
        entityGroupListFilter.setEntityGroupList(List.of(groups.get(0).getId().getId().toString(), groups.get(1).getId().getId().toString()));

        List<EntityKey> entityFields = List.of(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "name")
        );

        // get groups by customer user with generic permission
        HashMap<Resource, Set<Operation>> genericPermission = new HashMap<>();
        genericPermission.put(Resource.DEVICE, Set.of(Operation.ALL));
        genericPermission.put(Resource.DEVICE_GROUP, Set.of(Operation.ALL));
        MergedUserPermissions mergedGenericPermission = new MergedUserPermissions(genericPermission, Map.of());
        EntityDataPageLink pageLink = new EntityDataPageLink(1000, 0, null, null);
        EntityDataQuery query = new EntityDataQuery(entityGroupListFilter, pageLink, entityFields, null, null);

        PageData<EntityData> result = findByQueryAndCheck(customerId, mergedGenericPermission, query, 2);
        List<String> loadedNames = result.getData().stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue()).collect(Collectors.toList());
        assertThat(loadedNames).containsExactlyInAnyOrder(groups.get(0).getName(), groups.get(1).getName());

        entityGroupListFilter.setEntityGroupList(List.of(UUID.randomUUID().toString()));
        findByQueryAndCheck(query, 0);

        // get groups by customer user with group only permission
        entityGroupListFilter.setEntityGroupList(List.of(groups.get(0).getId().getId().toString(), groups.get(1).getId().getId().toString()));

        HashMap<EntityGroupId, MergedGroupPermissionInfo> groupPermission = new HashMap<>();
        groupPermission.put(groups.get(0).getId(), new MergedGroupPermissionInfo(EntityType.DEVICE, Set.of(Operation.READ)));
        MergedUserPermissions mergedGroupPermission = new MergedUserPermissions(new HashMap<>(), groupPermission);

        PageData<EntityData> result2 = findByQueryAndCheck(customerId, mergedGroupPermission, query, 1);

        List<String> loadedNames2 = result2.getData().stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue()).toList();
        assertThat(loadedNames2).containsOnly(groups.get(0).getName());

        // get groups by customer user with generic and group permission
        MergedUserPermissions mergedGenericAndGroupPermission = new MergedUserPermissions(genericPermission, groupPermission);
        PageData<EntityData> result3 = findByQueryAndCheck(customerId, mergedGenericAndGroupPermission, query, 2);

        List<String> loadedNames3 = result3.getData().stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue()).toList();
        assertThat(loadedNames3).containsExactlyInAnyOrder(groups.get(0).getName(), groups.get(1).getName());
    }

    @Test
    public void testFindEntitiesByGroupNameFilterWhenDeviceOwnerIsTenant() {
        List<Device> devices = new ArrayList<>();

        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName("group test name");
        entityGroup.setType(EntityType.DEVICE);
        EntityGroup savedGroup = entityGroupService.saveEntityGroup(tenantId, tenantId, entityGroup);

        for (int j = 0; j < 10; j++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device test" + j);
            device.setType("default");
            Device savedDevice = deviceService.saveDevice(device);
            devices.add(savedDevice);
            entityGroupService.addEntityToEntityGroup(tenantId, savedGroup.getId(), savedDevice.getId());
        }

        EntitiesByGroupNameFilter entitiesByGroupNameFilter = new EntitiesByGroupNameFilter();
        entitiesByGroupNameFilter.setGroupType(EntityType.DEVICE);
        entitiesByGroupNameFilter.setEntityGroupNameFilter(entityGroup.getName());
        entitiesByGroupNameFilter.setOwnerId(tenantId);

        List<EntityKey> entityFields = List.of(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "name")
        );

        EntityDataPageLink pageLink = new EntityDataPageLink(1000, 0, null, null);
        EntityDataQuery query = new EntityDataQuery(entitiesByGroupNameFilter, pageLink, entityFields, null, null);
        findByQueryAndCheck(query, devices.size());

        entitiesByGroupNameFilter.setEntityGroupNameFilter("wrong name");
        findByQueryAndCheck(query, 0);

        entitiesByGroupNameFilter.setEntityGroupNameFilter(entityGroup.getName());
        entitiesByGroupNameFilter.setOwnerId(customerId);
        findByQueryAndCheck(query, 0);
    }

    @Test
    public void testFindEntitiesByGroupNameFilter() {
        EntityGroup customerGroup = new EntityGroup();
        customerGroup.setName("customers");
        customerGroup.setType(EntityType.CUSTOMER);
        customerGroup = entityGroupService.saveEntityGroup(tenantId, tenantId, customerGroup);

        Customer subCustomer1 = new Customer();
        subCustomer1.setTenantId(tenantId);
        subCustomer1.setOwnerId(customerId);
        subCustomer1.setTitle("EntitiesByGroupNameFilter Customer 1");
        subCustomer1 = customerService.saveCustomer(subCustomer1);
        CustomerId subCustomerId1 = subCustomer1.getId();
        entityGroupService.addEntityToEntityGroup(tenantId, customerGroup.getId(), subCustomerId1);

        Customer subCustomer2 = new Customer();
        subCustomer2.setTenantId(tenantId);
        subCustomer2.setOwnerId(customerId);
        subCustomer2.setTitle("EntitiesByGroupNameFilter Customer 2");
        subCustomer2 = customerService.saveCustomer(subCustomer2);
        CustomerId subCustomerId2 = subCustomer2.getId();
        entityGroupService.addEntityToEntityGroup(tenantId, customerGroup.getId(), subCustomerId2);

        List<DeviceId> subCustomerDevices = new ArrayList<>();
        List<DeviceId> subCustomerDevices2 = new ArrayList<>();

        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName("devices");
        entityGroup.setType(EntityType.DEVICE);
        entityGroup.setOwnerId(subCustomerId1);
        EntityGroup customerDeviceGroup = entityGroupService.saveEntityGroup(tenantId, subCustomerId1, entityGroup);

        EntityGroup entityGroup2 = new EntityGroup();
        entityGroup2.setName("devices");
        entityGroup2.setType(EntityType.DEVICE);
        entityGroup2.setOwnerId(subCustomerId2);
        EntityGroup secondCustomerDeviceGroup = entityGroupService.saveEntityGroup(tenantId, subCustomerId2, entityGroup2);

        for (int i = 0; i < 10; i++) {
            Device device = createDevice(subCustomerId1);
            Device savedDevice = deviceService.saveDevice(device);
            subCustomerDevices.add(savedDevice.getId());
            entityGroupService.addEntityToEntityGroup(tenantId, customerDeviceGroup.getId(), savedDevice.getId());
        }

        for (int i = 0; i < 10; i++) {
            Device device = createDevice(subCustomerId2);
            Device savedDevice = deviceService.saveDevice(device);
            subCustomerDevices2.add(savedDevice.getId());
            entityGroupService.addEntityToEntityGroup(tenantId, secondCustomerDeviceGroup.getId(), savedDevice.getId());
        }

        EntitiesByGroupNameFilter entitiesByGroupNameFilter = new EntitiesByGroupNameFilter();
        entitiesByGroupNameFilter.setGroupType(EntityType.DEVICE);
        entitiesByGroupNameFilter.setEntityGroupNameFilter("devices");
        entitiesByGroupNameFilter.setOwnerId(subCustomerId2);

        List<EntityKey> entityFields = List.of(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "name")
        );
        EntityDataPageLink pageLink = new EntityDataPageLink(1000, 0, null, null);
        EntityDataQuery query = new EntityDataQuery(entitiesByGroupNameFilter, pageLink, entityFields, null, null);

        // generic permission only
        MergedUserPermissions genericPermissionOnly = new MergedUserPermissions(Map.of(ALL, Set.of(Operation.ALL)), Collections.emptyMap());

        PageData<EntityData> result = findByQueryAndCheck(subCustomerId2, genericPermissionOnly, query, subCustomerDevices2.size());
        assertThat(getResultDeviceIds(result)).hasSameElementsAs(subCustomerDevices2);

        // generic + group permission
        MergedUserPermissions genericAndGroupPermissions = new MergedUserPermissions(Map.of(ALL, Set.of(Operation.READ)),
                Map.of(customerDeviceGroup.getId(), new MergedGroupPermissionInfo(EntityType.DEVICE, Set.of(Operation.READ)),
                        secondCustomerDeviceGroup.getId(), new MergedGroupPermissionInfo(EntityType.DEVICE, Set.of(Operation.READ))));

        result = findByQueryAndCheck(customerId, genericAndGroupPermissions, query, subCustomerDevices2.size());
        assertThat(getResultDeviceIds(result)).hasSameElementsAs(subCustomerDevices2);

        // group permission
        MergedUserPermissions groupPermissionOnly = new MergedUserPermissions(Collections.emptyMap(),
                Map.of(customerGroup.getId(), new MergedGroupPermissionInfo(EntityType.CUSTOMER, Set.of(Operation.READ)),
                        customerDeviceGroup.getId(), new MergedGroupPermissionInfo(EntityType.DEVICE, Set.of(Operation.READ)),
                        secondCustomerDeviceGroup.getId(), new MergedGroupPermissionInfo(EntityType.DEVICE, Set.of(Operation.READ))));

        result = findByQueryAndCheck(customerId, groupPermissionOnly, query, subCustomerDevices2.size());
        assertThat(getResultDeviceIds(result)).hasSameElementsAs(subCustomerDevices2);
    }

    @Test
    public void testFindCustomersByEntityTypeFilter() {
        Customer subCustomer1 = new Customer();
        subCustomer1.setTenantId(tenantId);
        subCustomer1.setOwnerId(customerId);
        subCustomer1.setTitle("EntitiesByGroupNameFilter Customer 1");
        subCustomer1 = customerService.saveCustomer(subCustomer1);
        CustomerId subCustomerId1 = subCustomer1.getId();

        Customer subCustomer2 = new Customer();
        subCustomer2.setTenantId(tenantId);
        subCustomer2.setOwnerId(customerId);
        subCustomer2.setTitle("EntitiesByGroupNameFilter Customer 2");
        subCustomer2 = customerService.saveCustomer(subCustomer2);
        CustomerId subCustomerId2 = subCustomer2.getId();

        EntityTypeFilter entityTypeFilter = new EntityTypeFilter();
        entityTypeFilter.setEntityType(EntityType.CUSTOMER);

        List<EntityKey> entityFields = List.of(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "name")
        );
        List<KeyFilter> keyFilters = createStringKeyFilters("name", EntityKeyType.ENTITY_FIELD, StringOperation.STARTS_WITH, "EntitiesByGroupNameFilter");
        EntityDataPageLink pageLink = new EntityDataPageLink(1000, 0, null, null);
        EntityDataQuery query = new EntityDataQuery(entityTypeFilter, pageLink, entityFields, null, keyFilters);

        // generic permission only
        MergedUserPermissions genericPermissionOnly = new MergedUserPermissions(Map.of(ALL, Set.of(Operation.ALL)), Collections.emptyMap());
        PageData<EntityData> result = findByQueryAndCheck(customerId, genericPermissionOnly, query, 2);
        assertThat(getResultCustomerIds(result)).containsExactlyInAnyOrder(subCustomerId1, subCustomerId2);
    }

    private List<DeviceId> getResultDeviceIds(PageData<EntityData> result) {
        return result.getData().stream().map(entityData -> (DeviceId) entityData.getEntityId()).collect(Collectors.toList());
    }

    private List<CustomerId> getResultCustomerIds(PageData<EntityData> result) {
        return result.getData().stream().map(entityData -> (CustomerId) entityData.getEntityId()).collect(Collectors.toList());
    }

    @Test
    public void testFindEntitiesByGroupNameFilterWithoutOwnerId() {
        EntityGroup customerGroup = new EntityGroup();
        customerGroup.setName("customers");
        customerGroup.setType(EntityType.CUSTOMER);
        customerGroup = entityGroupService.saveEntityGroup(tenantId, tenantId, customerGroup);

        Customer subCustomer1 = new Customer();
        subCustomer1.setTenantId(tenantId);
        subCustomer1.setOwnerId(customerId);
        subCustomer1.setTitle("EntitiesByGroupNameFilter Customer 1");
        subCustomer1 = customerService.saveCustomer(subCustomer1);
        CustomerId subCustomerId1 = subCustomer1.getId();
        entityGroupService.addEntityToEntityGroup(tenantId, customerGroup.getId(), subCustomerId1);

        Customer subCustomer2 = new Customer();
        subCustomer2.setTenantId(tenantId);
        subCustomer2.setOwnerId(customerId);
        subCustomer2.setTitle("EntitiesByGroupNameFilter Customer 2");
        subCustomer2 = customerService.saveCustomer(subCustomer2);
        CustomerId subCustomerId2 = subCustomer2.getId();
        entityGroupService.addEntityToEntityGroup(tenantId, customerGroup.getId(), subCustomerId2);

        List<Device> subCustomerDevices = new ArrayList<>();
        List<Device> subCustomerDevices2 = new ArrayList<>();

        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName("devices");
        entityGroup.setType(EntityType.DEVICE);
        entityGroup.setOwnerId(subCustomerId1);
        EntityGroup customerDeviceGroup = entityGroupService.saveEntityGroup(tenantId, subCustomerId1, entityGroup);

        EntityGroup entityGroup2 = new EntityGroup();
        entityGroup2.setName("devices");
        entityGroup2.setType(EntityType.DEVICE);
        entityGroup2.setOwnerId(subCustomerId2);
        EntityGroup secondCustomerDeviceGroup = entityGroupService.saveEntityGroup(tenantId, subCustomerId2, entityGroup2);

        for (int i = 0; i < 10; i++) {
            Device device = createDevice(subCustomerId1);
            Device savedDevice = deviceService.saveDevice(device);
            subCustomerDevices.add(savedDevice);
            entityGroupService.addEntityToEntityGroup(tenantId, customerDeviceGroup.getId(), savedDevice.getId());
        }

        for (int i = 0; i < 10; i++) {
            Device device = createDevice(subCustomerId2);
            Device savedDevice = deviceService.saveDevice(device);
            subCustomerDevices2.add(savedDevice);
            entityGroupService.addEntityToEntityGroup(tenantId, secondCustomerDeviceGroup.getId(), savedDevice.getId());
        }

        EntitiesByGroupNameFilter entitiesByGroupNameFilter = new EntitiesByGroupNameFilter();
        entitiesByGroupNameFilter.setGroupType(EntityType.DEVICE);
        entitiesByGroupNameFilter.setEntityGroupNameFilter("devices");

        List<EntityKey> entityFields = List.of(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "name")
        );

        Map<Resource, Set<Operation>> genericPermissions = new HashMap<>();
        genericPermissions.put(Resource.CUSTOMER_GROUP, Collections.singleton(Operation.ALL));

        Map<EntityGroupId, MergedGroupPermissionInfo> groupPermissions = new HashMap<>();
        groupPermissions.put(customerGroup.getId(), new MergedGroupPermissionInfo(EntityType.CUSTOMER, Set.of(Operation.READ)));
        groupPermissions.put(customerDeviceGroup.getId(), new MergedGroupPermissionInfo(EntityType.DEVICE, Set.of(Operation.READ)));
        groupPermissions.put(secondCustomerDeviceGroup.getId(), new MergedGroupPermissionInfo(EntityType.DEVICE, Set.of(Operation.READ)));
        MergedUserPermissions mergedUserPermissions = new MergedUserPermissions(genericPermissions, groupPermissions);

        EntityDataPageLink pageLink = new EntityDataPageLink(1000, 0, null, null);
        EntityDataQuery query = new EntityDataQuery(entitiesByGroupNameFilter, pageLink, entityFields, null, null);
        findByQueryAndCheck(customerId, mergedUserPermissions, query, 10);

        // find by tenant user
        findByQueryAndCheck(query, 0);
    }

    private Device createDevice(CustomerId customerId) {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setCustomerId(customerId);
        device.setName("Device test " + RandomStringUtils.secure().nextAlphabetic(5));
        device.setType("default");
        return device;
    }

    private Dashboard createDashboard(CustomerId customerId, boolean hideMobile) {
        Dashboard dashboard = new Dashboard();
        dashboard.setTenantId(tenantId);
        dashboard.setCustomerId(customerId);
        dashboard.setTitle("Test dashboard " + RandomStringUtils.randomAlphabetic(5));
        dashboard.setMobileHide(hideMobile);
        return dashboard;
    }

    @Test
    public void testFindEntitiesByGroupNameFilterWhenDeviceOwnerIsCustomer() {
        List<Device> devices = new ArrayList<>();

        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName("group test name");
        entityGroup.setType(EntityType.DEVICE);
        entityGroup.setOwnerId(customerId);
        EntityGroup savedGroup = entityGroupService.saveEntityGroup(tenantId, customerId, entityGroup);

        for (int j = 0; j < 10; j++) {
            Device device = createDevice(customerId);
            Device savedDevice = deviceService.saveDevice(device);
            devices.add(savedDevice);
            entityGroupService.addEntityToEntityGroup(tenantId, savedGroup.getId(), savedDevice.getId());
        }

        EntitiesByGroupNameFilter entitiesByGroupNameFilter = new EntitiesByGroupNameFilter();
        entitiesByGroupNameFilter.setGroupType(EntityType.DEVICE);
        entitiesByGroupNameFilter.setEntityGroupNameFilter(entityGroup.getName());
        entitiesByGroupNameFilter.setOwnerId(customerId);

        List<EntityKey> entityFields = List.of(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "name")
        );

        EntityDataPageLink pageLink = new EntityDataPageLink(1000, 0, null, null);
        EntityDataQuery query = new EntityDataQuery(entitiesByGroupNameFilter, pageLink, entityFields, null, null);
        findByQueryAndCheck(customerId, mergedUserPermissionsPE, query, devices.size());
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
        MergedUserPermissions userPermissions = new MergedUserPermissions(Map.of(Resource.API_USAGE_STATE, Set.of(Operation.ALL)), Collections.emptyMap());
        PageData<EntityData> customerResult = findByQueryAndCheck(customerId, userPermissions, query, 1);

        String customerResultName = customerResult.getData().get(0).getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue();
        assertThat(customerResultName).isEqualTo(TEST_CUSTOMER_NAME);

        // find by tenant user with customerId filter
        apiUsageStateFilter.setCustomerId(customerId);
        PageData<EntityData> tenantResult = findByQueryAndCheck(query, 1);
        String tenantResultName = tenantResult.getData().get(0).getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue();
        assertThat(tenantResultName).isEqualTo(TEST_CUSTOMER_NAME);
    }

    @Test
    public void testFindEntitiesByStateEntityOwnerFilter() {
        List<EntityId> customerEntityIds = new ArrayList<>();
        List<EntityId> tenantEntityIds = new ArrayList<>();

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setCustomerId(customerId);
        device.setName("Device test" + RandomStringUtils.randomAlphanumeric(4));
        device.setType("default");
        DeviceId deviceId = deviceService.saveDevice(device).getId();
        customerEntityIds.add(deviceId);

        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName("Building" + RandomStringUtils.randomAlphabetic(4));
        asset.setType("building");
        asset.setLabel("building label");
        tenantEntityIds.add(assetService.saveAsset(asset).getId());

        User customerUser = new User();
        customerUser.setAuthority(Authority.CUSTOMER_USER);
        customerUser.setTenantId(tenantId);
        customerUser.setCustomerId(customerId);
        customerUser.setEmail("customer" + RandomStringUtils.randomAlphanumeric(5) + "@thingsboard.org");
        customerEntityIds.add(userService.saveUser(tenantId, customerUser).getId());

        tenantEntityIds.add(customerId);
        tenantEntityIds.add(tenantId);

        Dashboard dashboard = new Dashboard();
        dashboard.setTenantId(tenantId);
        dashboard.setCustomerId(customerId);
        dashboard.setTitle("test" + RandomStringUtils.randomAlphanumeric(5));
        dashboard.setConfiguration(JacksonUtil.newObjectNode().put("test", "test"));
        customerEntityIds.add(dashboardService.saveDashboard(dashboard).getId());

        EntityView entityView = new EntityView();
        entityView.setTenantId(tenantId);
        entityView.setCustomerId(customerId);
        entityView.setName("test");
        entityView.setType("default");
        customerEntityIds.add(entityViewService.saveEntityView(entityView).getId());

        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName("group test name");
        entityGroup.setType(EntityType.DEVICE);
        tenantEntityIds.add(entityGroupService.saveEntityGroup(tenantId, tenantId, entityGroup).getId());

        SchedulerEvent schedulerEvent = new SchedulerEvent();
        schedulerEvent.setTenantId(tenantId);
        schedulerEvent.setName("Scheduler Event");
        schedulerEvent.setType("Custom Type");
        schedulerEvent.setSchedule(JacksonUtil.newObjectNode());
        schedulerEvent.setConfiguration(JacksonUtil.newObjectNode());
        tenantEntityIds.add(schedulerEventService.saveSchedulerEvent(schedulerEvent).getId());

        Role role = new Role();
        role.setTenantId(tenantId);
        role.setCustomerId(customerId);
        role.setName("test role name" + RandomStringUtils.randomAlphabetic(5));
        role.setType(RoleType.GENERIC);
        customerEntityIds.add(roleService.saveRole(tenantId, role).getId());

        AlarmApiCallResult alarmApiCallResult = alarmService.createAlarm(AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tenantId)
                .originator(deviceId)
                .type("TEST_ALARM")
                .severity(AlarmSeverity.CRITICAL)
                .startTs(System.currentTimeMillis()).build());
        customerEntityIds.add(alarmApiCallResult.getAlarm().getId());

        BlobEntity blobEntity = new BlobEntity();
        blobEntity.setName("Blob relation query " + RandomStringUtils.randomAlphabetic(5));
        blobEntity.setTenantId(tenantId);
        blobEntity.setContentType("image/png");
        blobEntity.setData(ByteBuffer.allocate(1024));
        blobEntity.setType("Report");
        tenantEntityIds.add(blobEntityService.saveBlobEntity(blobEntity).getId());

        StateEntityOwnerFilter stateEntityOwnerFilter = new StateEntityOwnerFilter();
        stateEntityOwnerFilter.setSingleEntity(AliasEntityId.fromEntityId(device.getId()));

        List<EntityKey> entityFields = List.of(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "name")
        );

        EntityDataPageLink pageLink = new EntityDataPageLink(1000, 0, null, null);
        EntityDataQuery query = new EntityDataQuery(stateEntityOwnerFilter, pageLink, entityFields, null, null);

        //check customer entities owner
        customerEntityIds.forEach(entityId -> {
            stateEntityOwnerFilter.setSingleEntity(AliasEntityId.fromEntityId(entityId));
            PageData<EntityData> result = findByQueryAndCheck(query, 1);
            String ownerName = result.getData().get(0).getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue();
            assertThat(ownerName).isEqualTo(TEST_CUSTOMER_NAME);
        });

        //check tenant entities owner
        tenantEntityIds.forEach(entityId -> {
            stateEntityOwnerFilter.setSingleEntity(AliasEntityId.fromEntityId(entityId));
            PageData<EntityData> result = findByQueryAndCheck(query, 1);
            String ownerName = result.getData().get(0).getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue();
            assertThat(ownerName).isEqualTo(TEST_TENANT_NAME);
        });

        //check that tenant entity is not accessible to customer user
        stateEntityOwnerFilter.setSingleEntity(AliasEntityId.fromEntityId(tenantEntityIds.get(0)));
        EntityCountQuery countQuery = new EntityDataQuery(stateEntityOwnerFilter, pageLink, null, null, null);
        countByQueryAndCheck(customerId, mergedUserPermissionsPE, countQuery, 0);
    }

    @Test
    public void testFindEntitiesBySchedulerEventFilter() {
        SchedulerEvent schedulerEvent = new SchedulerEvent();
        schedulerEvent.setTenantId(tenantId);
        schedulerEvent.setCustomerId(customerId);
        schedulerEvent.setOriginatorId(customerId);
        schedulerEvent.setName("Scheduler Event");
        schedulerEvent.setType("report");
        schedulerEvent.setSchedule(JacksonUtil.newObjectNode());
        schedulerEvent.setConfiguration(JacksonUtil.newObjectNode());
        schedulerEventService.saveSchedulerEvent(schedulerEvent).getId();

        SchedulerEventFilter schedulerEventFilter = new SchedulerEventFilter();
        schedulerEventFilter.setEventType("report");
        schedulerEventFilter.setOriginator(AliasEntityId.fromEntityId(customerId));

        List<EntityKey> entityFields = List.of(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "name")
        );

        EntityDataPageLink pageLink = new EntityDataPageLink(1000, 0, null, null);
        EntityDataQuery query = new EntityDataQuery(schedulerEventFilter, pageLink, entityFields, null, null);

        //find entities by tenant user
        PageData<EntityData> result = findByQueryAndCheck(new CustomerId(CustomerId.NULL_UUID), mergedUserPermissionsPE, query, 1);
        String entityName = result.getData().get(0).getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue();
        assertThat(entityName).isEqualTo(schedulerEvent.getName());

        //find entities by non existing type filter
        schedulerEventFilter.setEventType("non-existing-type");
        findByQueryAndCheck(new CustomerId(CustomerId.NULL_UUID), mergedUserPermissionsPE, query, 0);

        //find entities by other customer id filter
        schedulerEventFilter.setEventType("report");
        schedulerEventFilter.setOriginator(AliasEntityId.fromEntityId(otherCustomerId));
        findByQueryAndCheck(new CustomerId(CustomerId.NULL_UUID), mergedUserPermissionsPE, query, 0);

        // find scheduler events by customer user
        EntityTypeFilter entityTypeFilter = new EntityTypeFilter();
        entityTypeFilter.setEntityType(EntityType.SCHEDULER_EVENT);
        EntityDataQuery entityTypeQuery = new EntityDataQuery(entityTypeFilter, pageLink, entityFields, null, null);

        PageData<EntityData> result4 = entityService.findEntityDataByQuery(tenantId, customerId, mergedUserPermissionsPE, entityTypeQuery);
        assertEquals(1, result4.getTotalElements());
        String schedulerName = result4.getData().get(0).getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue();
        assertThat(schedulerName).isEqualTo(schedulerEvent.getName());
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
                    device.setLabel("label-" + RandomUtils.secure().randomInt(100, 10000));
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
    public void testFindEntityDataByQueryWithoutAttributeReadPermission() throws ExecutionException, InterruptedException {
        EntityGroup group = new EntityGroup();
        group.setName("group to share");
        group.setTenantId(tenantId);
        group.setType(EntityType.DEVICE);
        group.setOwnerId(tenantId);
        group = entityGroupService.saveEntityGroup(tenantId, tenantId, group);

        List<Device> devices = new ArrayList<>();
        List<Long> temperatures = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            Device e = deviceService.saveDevice(device);
            devices.add(e);
            entityGroupService.addEntityToEntityGroup(tenantId, group.getId(), e.getId());
            //TO make sure devices have different created time
            Thread.sleep(1);
            long temperature = (long) (Math.random() * 100);
            temperatures.add(temperature);
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
        List<EntityKey> latestValues = Collections.singletonList(new EntityKey(ATTRIBUTE, "temperature"));
        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, null);

        // find by customer user read entity permission only
        MergedUserPermissions mergedGroupOnlyPermission = new MergedUserPermissions(Collections.emptyMap(), Map.of(group.getId(), new MergedGroupPermissionInfo(EntityType.DEVICE, Set.of(Operation.READ))));
        PageData<EntityData> result = findByQueryAndCheck(customerId, mergedGroupOnlyPermission, query, 5);

        List<String> loadedNames = result.getData().stream().map(entityData ->
                entityData.getLatest().get(ENTITY_FIELD).get("name").getValue()).toList();
        assertThat(loadedNames).containsExactlyInAnyOrderElementsOf(devices.stream().map(Device::getName).collect(Collectors.toList()));
        List<String> loadedTemperatures = result.getData().stream().map(entityData ->
                entityData.getLatest().get(ATTRIBUTE).get("temperature").getValue()).toList();
        assertThat(loadedTemperatures).containsExactlyInAnyOrderElementsOf(List.of("", "", "", "", ""));

        deviceService.deleteDevicesByTenantId(tenantId);
    }

    @Test
    public void testFindEntityDataByRelationQuery_blobEntity_customerLevel() {
        final int deviceCnt = 2;
        final int relationsCnt = 3;
        final int blobEntitiesCnt = deviceCnt * relationsCnt;

        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle("Customer Relation Query");
        customer = customerService.saveCustomer(customer);

        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < deviceCnt; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device relation query " + i);
            device.setCustomerId(customer.getId());
            device.setType("default");
            devices.add(deviceService.saveDevice(device));
        }

        List<BlobEntity> blobEntities = new ArrayList<>();
        for (int i = 0; i < blobEntitiesCnt; i++) {
            BlobEntity blobEntity = new BlobEntity();
            blobEntity.setName("Blob relation query " + i);
            blobEntity.setTenantId(tenantId);
            blobEntity.setContentType("image/png");
            blobEntity.setData(ByteBuffer.allocate(1024));
            blobEntity.setCustomerId(customer.getId());
            blobEntity.setType("Report");
            blobEntities.add(blobEntityService.saveBlobEntity(blobEntity));
        }

        for (int i = 0; i < deviceCnt; i++) {
            for (int j = 0; j < relationsCnt; j++) {
                EntityRelation relationEntity = new EntityRelation();
                relationEntity.setFrom(devices.get(i).getId());
                relationEntity.setTo(blobEntities.get(j + (i * relationsCnt)).getId());
                relationEntity.setTypeGroup(RelationTypeGroup.COMMON);
                relationEntity.setType("fileAttached");
                relationService.saveRelation(tenantId, relationEntity);
            }
        }

        MergedUserPermissions mergedUserPermissions = new MergedUserPermissions(Map.of(ALL, Set.of(Operation.ALL)), Collections.emptyMap());

        RelationEntityTypeFilter relationEntityTypeFilter = new RelationEntityTypeFilter("fileAttached", Collections.singletonList(EntityType.BLOB_ENTITY));
        RelationsQueryFilter filter = new RelationsQueryFilter();
        filter.setFilters(Collections.singletonList(relationEntityTypeFilter));
        filter.setDirection(EntitySearchDirection.FROM);
        EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, null);

        for (Device device : devices) {
            filter.setRootEntity(AliasEntityId.fromEntityId(device.getId()));

            EntityDataQuery query = new EntityDataQuery(filter, pageLink, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
            findByQueryAndCheck(customer.getId(), mergedUserPermissions, query, relationsCnt);
            countByQueryAndCheck(customer.getId(), mergedUserPermissions, query, relationsCnt);
            /*
            To be careful with updating Relation Query while adding new Entity Type,
            this checkup will help to find a place where you could check the correctness of building query
             */
            Assert.assertEquals(53, EntityType.values().length);
        }
    }

    @Test
    public void testFindEntitiesByRelationEntityTypeFilterWithTenantGroupPermission() {
        final int assetCount = 2;
        final int relationsCnt = 4;
        final int deviceEntitiesCnt = assetCount * relationsCnt;

        EntityGroup deviceGroup = new EntityGroup();
        deviceGroup.setName("Device Tenant Level Group");
        deviceGroup.setOwnerId(tenantId);
        deviceGroup.setTenantId(tenantId);
        deviceGroup.setType(EntityType.DEVICE);
        deviceGroup = entityGroupService.saveEntityGroup(tenantId, tenantId, deviceGroup);

        List<Asset> assets = new ArrayList<>();
        for (int i = 0; i < assetCount; i++) {
            Asset building = new Asset();
            building.setTenantId(tenantId);
            building.setName("Building _" + i);
            building.setType("building");
            building = assetService.saveAsset(building);
            assets.add(building);
        }

        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < deviceEntitiesCnt; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Test device " + i);
            device.setType("default");
            Device savedDevice = deviceService.saveDevice(device);
            devices.add(savedDevice);
            if (i % 2 == 0) {
                entityGroupService.addEntityToEntityGroup(tenantId, deviceGroup.getId(), savedDevice.getId());
            }
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

        MergedUserPermissions groupOnlyPermission = new MergedUserPermissions(Collections.emptyMap(),
                Map.of(deviceGroup.getId(), new MergedGroupPermissionInfo(EntityType.DEVICE, Set.of(Operation.READ))));

        RelationEntityTypeFilter relationEntityTypeFilter = new RelationEntityTypeFilter("contains", Collections.singletonList(EntityType.DEVICE));
        RelationsQueryFilter filter = new RelationsQueryFilter();
        filter.setFilters(Collections.singletonList(relationEntityTypeFilter));
        filter.setDirection(EntitySearchDirection.FROM);
        EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, null);
        List<KeyFilter> keyFiltersEqualString = createStringKeyFilters("name", EntityKeyType.ENTITY_FIELD, StringOperation.STARTS_WITH, "Test device ");

        for (Asset asset : assets) {
            filter.setRootEntity(AliasEntityId.fromEntityId(asset.getId()));

            EntityDataQuery query = new EntityDataQuery(filter, pageLink, Collections.emptyList(), Collections.emptyList(), keyFiltersEqualString);
            findByQueryAndCheck(new CustomerId(EntityId.NULL_UUID), groupOnlyPermission, query, relationsCnt / 2);
            countByQueryAndCheck(new CustomerId(EntityId.NULL_UUID), groupOnlyPermission, query, relationsCnt / 2);
        }
    }

    @Test
    public void testFindEntitiesWithRelationEntityTypeFilterByCustomerUser() {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle("Customer Relation Query");
        customer = customerService.saveCustomer(customer);

        final int assetCount = 2;
        final int relationsCnt = 4;
        final int deviceEntitiesCnt = assetCount * relationsCnt;

        EntityGroup deviceGroup = new EntityGroup();
        deviceGroup.setName("Device Tenant Level Group");
        deviceGroup.setOwnerId(customer.getId());
        deviceGroup.setTenantId(tenantId);
        deviceGroup.setType(EntityType.DEVICE);
        deviceGroup = entityGroupService.saveEntityGroup(tenantId, tenantId, deviceGroup);

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
            if (i % 2 == 0) {
                entityGroupService.addEntityToEntityGroup(tenantId, deviceGroup.getId(), savedDevice.getId());
            }
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

        MergedUserPermissions mergedGroupOnlyPermission = new MergedUserPermissions(Collections.emptyMap(), Map.of(deviceGroup.getId(), new MergedGroupPermissionInfo(EntityType.DEVICE, Set.of(Operation.ALL))));
        MergedUserPermissions mergedGenericOnlyPermission = new MergedUserPermissions(Map.of(Resource.ALL, Set.of(Operation.ALL)), Collections.emptyMap());
        MergedUserPermissions mergedGenericAndGroupPermission = new MergedUserPermissions(Map.of(Resource.ALL, Set.of(Operation.ALL)), Map.of(deviceGroup.getId(), new MergedGroupPermissionInfo(EntityType.DEVICE, Set.of(Operation.ALL))));
        RelationEntityTypeFilter relationEntityTypeFilter = new RelationEntityTypeFilter("contains", Collections.singletonList(EntityType.DEVICE));
        RelationsQueryFilter filter = new RelationsQueryFilter();
        filter.setFilters(Collections.singletonList(relationEntityTypeFilter));
        filter.setDirection(EntitySearchDirection.FROM);
        EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, null);
        List<KeyFilter> keyFiltersEqualString = createStringKeyFilters("name", EntityKeyType.ENTITY_FIELD, StringOperation.STARTS_WITH, "Test device ");

        EntityDataQuery query = new EntityDataQuery(filter, pageLink, Collections.emptyList(), Collections.emptyList(), keyFiltersEqualString);

        for (Asset asset : assets) {
            filter.setRootEntity(AliasEntityId.fromEntityId(asset.getId()));

            //check by user with generic permission
            PageData<EntityData> relationsResult = findByQueryAndCheck(customer.getId(), mergedGenericOnlyPermission, query, relationsCnt);
            countByQueryAndCheck(customer.getId(), mergedGenericOnlyPermission, query, relationsCnt);

            //check by user with generic and group permission
            PageData<EntityData> relationsResult1 = findByQueryAndCheck(customer.getId(), mergedGenericAndGroupPermission, query, relationsCnt);
            countByQueryAndCheck(customer.getId(), mergedGenericAndGroupPermission, query, relationsCnt);

            //check by other customer user with group only permission
            PageData<EntityData> relationsResult2 = findByQueryAndCheck(otherCustomerId, mergedGroupOnlyPermission, query, relationsCnt / 2);
            long relationsResultCnt2 = countByQueryAndCheck(otherCustomerId, mergedGroupOnlyPermission, query, relationsCnt / 2);

            Assert.assertEquals(relationsCnt / 2, relationsResult2.getData().size());
            Assert.assertEquals(relationsCnt / 2, relationsResultCnt2);

            //check by other customer user with generic and group only permission
            PageData<EntityData> relationsResult3 = findByQueryAndCheck(otherCustomerId, mergedGenericAndGroupPermission, query, relationsCnt / 2);
            long relationsResultCnt3 = countByQueryAndCheck(otherCustomerId, mergedGenericAndGroupPermission, query, relationsCnt / 2);

            Assert.assertEquals(relationsCnt / 2, relationsResult3.getData().size());
            Assert.assertEquals(relationsCnt / 2, relationsResultCnt3);
        }
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
    public void testSortByNumericTelemetryKeyWithDifferentNullsOrderStrategy() throws ExecutionException, InterruptedException {
        try {
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

            assertNullsOrdering("default",
                    List.of("0", "0", "1", "", ""),
                    List.of("", "", "1", "0", "0"),
                    devices.size());

            assertNullsOrdering("nulls_first",
                    List.of("", "", "0", "0", "1"),
                    List.of("", "", "1", "0", "0"),
                    devices.size());

            assertNullsOrdering("nulls_last",
                    List.of("0", "0", "1", "", ""),
                    List.of("1", "0", "0", "", ""),
                    devices.size());
        } finally {
            deviceService.deleteDevicesByTenantId(tenantId);
        }
    }

    @Test
    public void testSortByBooleanKeyWithDifferentNullsOrderStrategy() throws ExecutionException, InterruptedException {
        try {
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

            assertNullsOrdering("default",
                    List.of("false", "false", "true", "", ""),
                    List.of("", "", "true", "false", "false"),
                    devices.size());

            assertNullsOrdering("nulls_first",
                    List.of("", "", "false", "false", "true"),
                    List.of("", "", "true", "false", "false"),
                    devices.size());

            assertNullsOrdering("nulls_last",
                    List.of("false", "false", "true", "", ""),
                    List.of("true", "false", "false", "", ""),
                    devices.size());
        } finally {
            deviceService.deleteDevicesByTenantId(tenantId);
        }
    }

    private void assertNullsOrdering(String strategy, List<String> expectedAsc, List<String> expectedDesc, int deviceSize) {
        String originalStrategy = entityQueryRepository.getNullsOrderStrategy();
        ReflectionTestUtils.setField(entityQueryRepository, "nullsOrderStrategy", strategy);
        try {
            DeviceTypeFilter filter = new DeviceTypeFilter();
            filter.setDeviceTypes(List.of("default"));
            filter.setDeviceNameFilter("");

            List<EntityKey> entityFields = Collections.singletonList(new EntityKey(ENTITY_FIELD, "name"));
            List<EntityKey> latestValues = Collections.singletonList(new EntityKey(EntityKeyType.TIME_SERIES, "test"));

            EntityDataSortOrder ascSortOrder = new EntityDataSortOrder(
                    new EntityKey(EntityKeyType.TIME_SERIES, "test"), EntityDataSortOrder.Direction.ASC);
            EntityDataQuery ascQuery = new EntityDataQuery(filter,
                    new EntityDataPageLink(10, 0, null, ascSortOrder), entityFields, latestValues, null);
            List<String> ascTelemetry = loadAllData(ascQuery, deviceSize).stream()
                    .map(ed -> ed.getLatest().get(EntityKeyType.TIME_SERIES).get("test").getValue())
                    .toList();
            assertThat(ascTelemetry).as("ASC with strategy '%s'", strategy).containsExactlyElementsOf(expectedAsc);

            EntityDataSortOrder descSortOrder = new EntityDataSortOrder(
                    new EntityKey(EntityKeyType.TIME_SERIES, "test"), EntityDataSortOrder.Direction.DESC);
            EntityDataQuery descQuery = new EntityDataQuery(filter,
                    new EntityDataPageLink(10, 0, null, descSortOrder), entityFields, latestValues, null);
            List<String> descTelemetry = loadAllData(descQuery, deviceSize).stream()
                    .map(ed -> ed.getLatest().get(EntityKeyType.TIME_SERIES).get("test").getValue())
                    .toList();
            assertThat(descTelemetry).as("DESC with strategy '%s'", strategy).containsExactlyElementsOf(expectedDesc);
        } finally {
            ReflectionTestUtils.setField(entityQueryRepository, "nullsOrderStrategy", originalStrategy);
        }
    }

    @Test
    public void testFindTenantTelemetry() throws ExecutionException, InterruptedException, TimeoutException {
        // save timeseries by sys admin
        BasicTsKvEntry timeseries = new BasicTsKvEntry(42L, new DoubleDataEntry("temperature", 45.5));
        timeseriesService.save(TenantId.SYS_TENANT_ID, tenantId, timeseries).get(TIMEOUT, TimeUnit.SECONDS);

        AttributeKvEntry attr = new BaseAttributeKvEntry(new LongDataEntry("attr", 10L), 42L);
        attributesService.save(TenantId.SYS_TENANT_ID, tenantId, SERVER_SCOPE, List.of(attr)).get(TIMEOUT, TimeUnit.SECONDS);

        SingleEntityFilter singleEntityFilter = new SingleEntityFilter();
        singleEntityFilter.setSingleEntity(AliasEntityId.fromEntityId(tenantId));

        List<EntityKey> entityFields = List.of(
                new EntityKey(ENTITY_FIELD, "name")
        );
        List<EntityKey> latestValues = List.of(
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
        return saveLongAttribute(entityId, key, value, 42L, scope);
    }

    private ListenableFuture<AttributesSaveResult> saveLongAttribute(EntityId entityId, String key, long value, long lastUpdateTs, AttributeScope scope) {
        KvEntry attrValue = new LongDataEntry(key, value);
        AttributeKvEntry attr = new BaseAttributeKvEntry(attrValue, lastUpdateTs);
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

    protected ListenableFuture<TimeseriesSaveResult> saveTimeseries(EntityId entityId, String key, Long value) {
        KvEntry telemetryValue = new LongDataEntry(key, value);
        BasicTsKvEntry timeseries = new BasicTsKvEntry(42L, telemetryValue);
        return timeseriesService.save(tenantId, entityId, timeseries);
    }

    protected ListenableFuture<TimeseriesSaveResult> saveTimeseries(EntityId entityId, String key, Boolean value) {
        KvEntry telemetryValue = new BooleanDataEntry(key, value);
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
    public void testFindEntitiesByQuery_customerHierarchySearch() {
        final String deviceNamePrefix = "Customer Device ";

        final int tenantSharedDevicesCnt = 1;
        EntityGroup group = new EntityGroup();
        group.setName("Tenant Level Group");
        group.setOwnerId(tenantId);
        group.setType(EntityType.DEVICE);
        group = entityGroupService.saveEntityGroup(tenantId, tenantId, group);
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName(deviceNamePrefix + " Tenant");
        device = deviceService.saveDevice(device);
        entityGroupService.addEntityToEntityGroup(tenantId, group.getId(), device.getId());

        final int topCustomerDevicesCnt = 3;
        CustomerId topCustomerId = createCustomerAndAddDevices(null, "Top Customer", topCustomerDevicesCnt, deviceNamePrefix);

        final int customerADevicesCnt = 2;
        createCustomerAndAddDevices(topCustomerId, "Sub Customer A", customerADevicesCnt, deviceNamePrefix);

        final int customerBDevicesCnt = 4;
        createCustomerAndAddDevices(topCustomerId, "Sub Customer B", customerBDevicesCnt, deviceNamePrefix);

        MergedUserPermissions mergedUserPermissions = new MergedUserPermissions(Map.of(Resource.DEVICE, Set.of(Operation.ALL)),
                Map.of(group.getId(), new MergedGroupPermissionInfo(EntityType.DEVICE, Set.of(Operation.ALL))));

        final int totalNumberOfDevices = tenantSharedDevicesCnt + topCustomerDevicesCnt + customerADevicesCnt + customerBDevicesCnt;
        EntityDataQuery query = createDataQueryFilterByEntityName(deviceNamePrefix);
        findByQueryAndCheck(topCustomerId, mergedUserPermissions, query, totalNumberOfDevices);
        countByQueryAndCheck(topCustomerId, mergedUserPermissions, query, totalNumberOfDevices);
    }

    @Test
    public void testDeviceEntityMapping() {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setCustomerId(customerId);
        device.setName("test");
        device.setType("default");
        device.setDeviceProfileId(deviceProfileService.findDefaultDeviceProfile(tenantId).getId());
        device.setLabel("label");
        device.setDeviceData(new DeviceData());
        device.setExternalId(new DeviceId(UUID.randomUUID()));
        device.setAdditionalInfo(JacksonUtil.newObjectNode().put("test", "test"));
        device = deviceDao.save(tenantId, device);

        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT " + EntityMapping.deviceMapping.getMappings().keySet().stream()
                .map(field -> "e." + field)
                .collect(Collectors.joining(", ")) + " FROM device e WHERE id = ?", device.getUuidId());
        Device mappedDevice = EntityMapping.deviceMapping.map(row);
        assertThat(mappedDevice.getId()).isEqualTo(device.getId());
        assertThat(mappedDevice.getCreatedTime()).isEqualTo(device.getCreatedTime());
        assertThat(mappedDevice.getTenantId()).isEqualTo(device.getTenantId());
        assertThat(mappedDevice.getCustomerId()).isEqualTo(device.getCustomerId());
        assertThat(mappedDevice.getName()).isEqualTo(device.getName());
        assertThat(mappedDevice.getType()).isEqualTo(device.getType());
        assertThat(mappedDevice.getLabel()).isEqualTo(device.getLabel());
        assertThat(mappedDevice.getAdditionalInfo()).isEqualTo(device.getAdditionalInfo());

        PageData<Device> devices = entityService.findUserEntities(tenantId, device.getCustomerId(), new MergedUserPermissions(
                Map.of(ALL, Set.of(Operation.ALL)), Map.of()
        ), EntityType.DEVICE, Operation.READ, null, new PageLink(100), false, false);
        assertThat(devices.getData()).contains(mappedDevice);

        devices = entityService.findUserEntities(tenantId, device.getCustomerId(), new MergedUserPermissions(
                Map.of(ALL, Set.of(Operation.ALL)), Map.of()
        ), EntityType.DEVICE, Operation.READ, null, new PageLink(100), false, true);
        assertThat(devices.getData()).isNotEmpty().allSatisfy(deviceWithId -> {
            assertThat(deviceWithId.getId()).isEqualTo(mappedDevice.getId());
            assertThat(deviceWithId.getTenantId()).isNull();
            assertThat(deviceWithId.getName()).isNull();
        });
    }

    @Test
    public void testFindDevicesByCustomerUser() {
        Customer subCustomer = new Customer();
        subCustomer.setTenantId(tenantId);
        subCustomer.setOwnerId(customerId);
        subCustomer.setTitle("Sub-customer");
        subCustomer = customerService.saveCustomer(subCustomer);
        CustomerId subCustomerId = subCustomer.getId();

        Customer otherSubCustomer = new Customer();
        otherSubCustomer.setTenantId(tenantId);
        otherSubCustomer.setOwnerId(otherCustomerId);
        otherSubCustomer.setTitle("Other-sub-customer");
        otherSubCustomer = customerService.saveCustomer(otherSubCustomer);
        CustomerId otherSubCustomerId = otherSubCustomer.getId();

        List<Device> subCustomerDevices = new ArrayList<>();
        List<Device> otherSubCustomerDevices = new ArrayList<>();
        List<Device> allDevices = new ArrayList<>();

        EntityGroup deviceGroup = new EntityGroup();
        deviceGroup.setName("devices");
        deviceGroup.setType(EntityType.DEVICE);
        deviceGroup.setOwnerId(subCustomerId);
        deviceGroup = entityGroupService.saveEntityGroup(tenantId, subCustomerId, deviceGroup);

        EntityGroup otherDeviceGroup = new EntityGroup();
        otherDeviceGroup.setName("devices");
        otherDeviceGroup.setType(EntityType.DEVICE);
        otherDeviceGroup.setOwnerId(otherSubCustomerId);
        otherDeviceGroup = entityGroupService.saveEntityGroup(tenantId, otherSubCustomerId, otherDeviceGroup);

        for (int i = 0; i < 10; i++) {
            Device device = createDevice(subCustomerId);
            Device savedDevice = deviceService.saveDevice(device);
            subCustomerDevices.add(savedDevice);
            allDevices.add(savedDevice);
            entityGroupService.addEntityToEntityGroup(tenantId, deviceGroup.getId(), savedDevice.getId());
        }

        for (int i = 0; i < 10; i++) {
            Device device = createDevice(otherSubCustomerId);
            Device savedDevice = deviceService.saveDevice(device);
            otherSubCustomerDevices.add(savedDevice);
            allDevices.add(savedDevice);
            entityGroupService.addEntityToEntityGroup(tenantId, otherDeviceGroup.getId(), savedDevice.getId());
        }

        // get devices by user with generic permission only
        Map<Resource, Set<Operation>> genericPermissions = new HashMap<>();
        genericPermissions.put(Resource.DEVICE_GROUP, Collections.singleton(Operation.ALL));
        genericPermissions.put(Resource.DEVICE, Collections.singleton(Operation.ALL));
        MergedUserPermissions mergedUserPermissions = new MergedUserPermissions(genericPermissions, Collections.emptyMap());

        PageData<Device> devices = entityService.findUserEntities(tenantId, customerId, mergedUserPermissions,
                EntityType.DEVICE, Operation.READ, null, new PageLink(100), false, false);
        assertThat(devices.getData().stream().map(Device::getId).collect(Collectors.toList()))
                .containsExactlyInAnyOrderElementsOf(subCustomerDevices.stream().map(Device::getId).collect(Collectors.toList()));

        //add type filter
        PageData<Device> thermostats = entityService.findUserEntities(tenantId, customerId, mergedUserPermissions,
                EntityType.DEVICE, Operation.READ, "thermostat", new PageLink(100), false, false);
        assertThat(thermostats.getData()).isEmpty();

        //add text search
        PageLink pageLink = new PageLink(100, 0, "wrong search text");
        PageData<Device> testDevices = entityService.findUserEntities(tenantId, customerId, mergedUserPermissions,
                EntityType.DEVICE, Operation.READ, null, pageLink, false, false);
        assertThat(testDevices.getData()).isEmpty();

        //get devices by user with group permission only
        Map<EntityGroupId, MergedGroupPermissionInfo> groupPermissions = new HashMap<>();
        groupPermissions.put(otherDeviceGroup.getId(), new MergedGroupPermissionInfo(EntityType.DEVICE, Set.of(Operation.READ)));
        MergedUserPermissions mergedUserPermissions2 = new MergedUserPermissions(Map.of(), groupPermissions);

        PageData<Device> devices2 = entityService.findUserEntities(tenantId, customerId, mergedUserPermissions2,
                EntityType.DEVICE, Operation.READ, null, new PageLink(100), false, false);
        assertThat(devices2.getData().stream().map(Device::getId).collect(Collectors.toList()))
                .containsExactlyInAnyOrderElementsOf(otherSubCustomerDevices.stream().map(Device::getId).collect(Collectors.toList()));

        //get devices by user with generic and group permission
        MergedUserPermissions mergedUserPermissions3 = new MergedUserPermissions(genericPermissions, groupPermissions);
        PageData<Device> devices3 = entityService.findUserEntities(tenantId, customerId, mergedUserPermissions3,
                EntityType.DEVICE, Operation.READ, null, new PageLink(100), false, false);
        assertThat(devices3.getData().stream().map(Device::getId).collect(Collectors.toList()))
                .containsExactlyInAnyOrderElementsOf(allDevices.stream().map(Device::getId).collect(Collectors.toList()));

        //get devices by tenant user
        MergedUserPermissions mergedUserPermissions4 = new MergedUserPermissions(genericPermissions, groupPermissions);
        PageData<Device> devices4 = entityService.findUserEntities(tenantId, new CustomerId(EntityId.NULL_UUID), mergedUserPermissions4,
                EntityType.DEVICE, Operation.READ, null, new PageLink(100), false, false);
        assertThat(devices4.getData().stream().map(Device::getId).collect(Collectors.toList()))
                .containsExactlyInAnyOrderElementsOf(allDevices.stream().map(Device::getId).collect(Collectors.toList()));
    }

    @Test
    public void testFindDevicesAfterDeletion() {
        Customer subCustomer = new Customer();
        subCustomer.setTenantId(tenantId);
        subCustomer.setOwnerId(customerId);
        subCustomer.setTitle("Sub-customer");
        subCustomer = customerService.saveCustomer(subCustomer);
        CustomerId subCustomerId = subCustomer.getId();

        List<Device> subCustomerDevices = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Device device = createDevice(subCustomerId);
            Device savedDevice = deviceService.saveDevice(device);
            subCustomerDevices.add(savedDevice);
        }

        // get devices
        DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceTypes(List.of("default"));
        List<EntityKey> entityFields = Arrays.asList(new EntityKey(ENTITY_FIELD, "name"), new EntityKey(ENTITY_FIELD, "type"));
        EntityDataPageLink pageLink = new EntityDataPageLink(100, 0, null, null);
        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, null, null);

        findByQueryAndCheck(subCustomerId, mergedUserPermissionsPE, query, subCustomerDevices.size());

        // delete device
        deviceService.deleteDevice(tenantId, subCustomerDevices.get(0).getId());
        subCustomerDevices.remove(0);
        findByQueryAndCheck(subCustomerId, mergedUserPermissionsPE, query, subCustomerDevices.size());
    }

    @Test
    public void testFindMobileDashboardsByCustomerUser() {
        List<Dashboard> mobileDashboards = new ArrayList<>();
        List<Dashboard> allDashboards = new ArrayList<>();

        EntityGroup dashboardGroup = new EntityGroup();
        dashboardGroup.setName("all-dashboards");
        dashboardGroup.setType(EntityType.DASHBOARD);
        dashboardGroup.setOwnerId(customerId);
        dashboardGroup = entityGroupService.saveEntityGroup(tenantId, customerId, dashboardGroup);

        EntityGroup webOnlyDashboards = new EntityGroup();
        webOnlyDashboards.setName("web-only");
        webOnlyDashboards.setType(EntityType.DASHBOARD);
        webOnlyDashboards.setOwnerId(customerId);
        webOnlyDashboards = entityGroupService.saveEntityGroup(tenantId, customerId, webOnlyDashboards);

        for (int i = 0; i < 10; i++) {
            Dashboard dashboard = createDashboard(customerId, false);
            Dashboard savedDashboard = dashboardService.saveDashboard(dashboard);
            mobileDashboards.add(savedDashboard);
            allDashboards.add(savedDashboard);
            entityGroupService.addEntityToEntityGroup(tenantId, dashboardGroup.getId(), savedDashboard.getId());
        }

        for (int i = 0; i < 10; i++) {
            Dashboard dashboard = createDashboard(customerId, true);
            Dashboard savedDashboard = dashboardService.saveDashboard(dashboard);
            allDashboards.add(savedDashboard);
            entityGroupService.addEntityToEntityGroup(tenantId, webOnlyDashboards.getId(), savedDashboard.getId());
        }

        // get dashboards by user with generic permission only
        Map<Resource, Set<Operation>> genericPermissions = new HashMap<>();
        genericPermissions.put(Resource.DASHBOARD, Collections.singleton(Operation.ALL));
        genericPermissions.put(Resource.DASHBOARD_GROUP, Collections.singleton(Operation.ALL));
        MergedUserPermissions mergedUserPermissions = new MergedUserPermissions(genericPermissions, Collections.emptyMap());

        PageLink pageLink = new PageLink(100, 0, null, new SortOrder("title", SortOrder.Direction.ASC));
        PageData<Dashboard> dashboards = entityService.findUserEntities(tenantId, customerId, mergedUserPermissions,
                EntityType.DASHBOARD, Operation.READ, null, pageLink, false, false);
        assertThat(dashboards.getData().stream().map(Dashboard::getId).collect(Collectors.toList()))
                .containsExactlyInAnyOrderElementsOf(allDashboards.stream().sorted(Comparator.comparing(Dashboard::getName)).map(Dashboard::getId).collect(Collectors.toList()));

        // get dashboards by user with group permission
        MergedUserPermissions mergedGroupOnlyPermissions = new MergedUserPermissions(Collections.emptyMap(), Map.of(dashboardGroup.getId(), new MergedGroupPermissionInfo(EntityType.DASHBOARD, Set.of(Operation.READ)),
                webOnlyDashboards.getId(), new MergedGroupPermissionInfo(EntityType.DASHBOARD, Set.of(Operation.READ))));

        PageData<Dashboard> dashboards2 = entityService.findUserEntities(tenantId, customerId, mergedGroupOnlyPermissions,
                EntityType.DASHBOARD, Operation.READ, null, pageLink, false, false);
        assertThat(dashboards2.getData().stream().map(Dashboard::getId).collect(Collectors.toList()))
                .containsExactlyInAnyOrderElementsOf(allDashboards.stream().sorted(Comparator.comparing(Dashboard::getName)).map(Dashboard::getId).collect(Collectors.toList()));

        // get dashboards by other customer user with group permission
        PageData<Dashboard> dashboards3 = entityService.findUserEntities(tenantId, otherCustomerId, mergedGroupOnlyPermissions,
                EntityType.DASHBOARD, Operation.READ, null, pageLink, false, false);
        assertThat(dashboards3.getData().stream().map(Dashboard::getId).collect(Collectors.toList()))
                .containsExactlyInAnyOrderElementsOf(allDashboards.stream().sorted(Comparator.comparing(Dashboard::getName)).map(Dashboard::getId).collect(Collectors.toList()));

        // get devices when mobile=true
        PageData<Dashboard> dashboards4 = entityService.findUserEntities(tenantId, customerId, mergedUserPermissions,
                EntityType.DASHBOARD, Operation.READ, null, pageLink, true, false);
        assertThat(dashboards4.getData().stream().map(Dashboard::getId).collect(Collectors.toList()))
                .containsExactlyInAnyOrderElementsOf(mobileDashboards.stream().map(Dashboard::getId).collect(Collectors.toList()));
    }

    @Test
    public void testAssetEntityMapping() {
        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setCustomerId(customerId);
        asset.setName("test");
        asset.setType("default");
        asset.setAssetProfileId(assetProfileService.findDefaultAssetProfile(tenantId).getId());
        asset.setLabel("label");
        asset.setExternalId(new AssetId(UUID.randomUUID()));
        asset.setAdditionalInfo(JacksonUtil.newObjectNode().put("test", "test"));
        asset = assetDao.save(tenantId, asset);

        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT " + EntityMapping.assetMapping.getMappings().keySet().stream()
                .map(field -> "e." + field)
                .collect(Collectors.joining(", ")) + " FROM asset e WHERE id = ?", asset.getUuidId());
        Asset mappedAsset = EntityMapping.assetMapping.map(row);
        assertThat(mappedAsset.getId()).isEqualTo(asset.getId());
        assertThat(mappedAsset.getCreatedTime()).isEqualTo(asset.getCreatedTime());
        assertThat(mappedAsset.getTenantId()).isEqualTo(asset.getTenantId());
        assertThat(mappedAsset.getCustomerId()).isEqualTo(asset.getCustomerId());
        assertThat(mappedAsset.getName()).isEqualTo(asset.getName());
        assertThat(mappedAsset.getType()).isEqualTo(asset.getType());
        assertThat(mappedAsset.getLabel()).isEqualTo(asset.getLabel());
        assertThat(mappedAsset.getAdditionalInfo()).isEqualTo(asset.getAdditionalInfo());

        PageData<Asset> assets = entityService.findUserEntities(tenantId, asset.getCustomerId(), new MergedUserPermissions(
                Map.of(ALL, Set.of(Operation.ALL)), Map.of()
        ), EntityType.ASSET, Operation.READ, null, new PageLink(100));
        assertThat(assets.getData()).contains(mappedAsset);
    }

    @Test
    public void testEntityViewEntityMapping() {
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

        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT " + EntityMapping.entityViewMapping.getMappings().keySet().stream()
                .map(field -> "e." + field)
                .collect(Collectors.joining(", ")) + " FROM entity_view e WHERE id = ?", entityView.getUuidId());
        EntityView mappedEntityView = EntityMapping.entityViewMapping.map(row);
        assertThat(mappedEntityView.getId()).isEqualTo(entityView.getId());
        assertThat(mappedEntityView.getCreatedTime()).isEqualTo(entityView.getCreatedTime());
        assertThat(mappedEntityView.getTenantId()).isEqualTo(entityView.getTenantId());
        assertThat(mappedEntityView.getCustomerId()).isEqualTo(entityView.getCustomerId());
        assertThat(mappedEntityView.getName()).isEqualTo(entityView.getName());
        assertThat(mappedEntityView.getType()).isEqualTo(entityView.getType());
        assertThat(mappedEntityView.getEntityId()).isEqualTo(entityView.getEntityId());
        assertThat(mappedEntityView.getKeys()).isEqualTo(entityView.getKeys());
        assertThat(mappedEntityView.getStartTimeMs()).isEqualTo(entityView.getStartTimeMs());
        assertThat(mappedEntityView.getEndTimeMs()).isEqualTo(entityView.getEndTimeMs());
        assertThat(mappedEntityView.getAdditionalInfo()).isEqualTo(entityView.getAdditionalInfo());

        PageData<EntityView> entityViews = entityService.findUserEntities(tenantId, entityView.getCustomerId(), new MergedUserPermissions(
                Map.of(ALL, Set.of(Operation.ALL)), Map.of()
        ), EntityType.ENTITY_VIEW, Operation.READ, null, new PageLink(100));
        assertThat(entityViews.getData()).contains(mappedEntityView);
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

        PageData<EntityData> relationsResult = findByQueryAndCheck(new CustomerId(EntityId.NULL_UUID), mergedUserPermissionsPE, query, 1);
        assertThat(relationsResult.getData().get(0).getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue()).isEqualTo(entityView.getName());

        // find with non existing name
        entityViewTypeFilter.setEntityViewNameFilter("non-existing");
        findByQueryAndCheck(new CustomerId(EntityId.NULL_UUID), mergedUserPermissionsPE, query, 0);

        // find with non existing type
        entityViewTypeFilter.setEntityViewNameFilter(null);
        entityViewTypeFilter.setEntityViewTypes(Collections.singletonList("non-existing"));

        findByQueryAndCheck(new CustomerId(EntityId.NULL_UUID), mergedUserPermissionsPE, query, 0);
    }

    @Test
    public void testEdgeEntityMapping() {
        Edge edge = new Edge();
        edge.setTenantId(tenantId);
        edge.setCustomerId(customerId);
        edge.setName("test");
        edge.setType("default");
        edge.setLabel("label");
        edge.setRootRuleChainId(new RuleChainId(UUID.randomUUID()));
        edge.setRoutingKey("routingKey");
        edge.setSecret("secret");
        edge.setEdgeLicenseKey("edgeLicenseKey");
        edge.setCloudEndpoint("cloudEndpoint");
        edge.setAdditionalInfo(JacksonUtil.newObjectNode().put("test", "test"));
        edge = edgeDao.save(tenantId, edge);

        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT " + EntityMapping.edgeMapping.getMappings().keySet().stream()
                .map(field -> "e." + field)
                .collect(Collectors.joining(", ")) + " FROM edge e WHERE id = ?", edge.getUuidId());
        Edge mappedEdge = EntityMapping.edgeMapping.map(row);
        assertThat(mappedEdge.getId()).isEqualTo(edge.getId());
        assertThat(mappedEdge.getCreatedTime()).isEqualTo(edge.getCreatedTime());
        assertThat(mappedEdge.getTenantId()).isEqualTo(edge.getTenantId());
        assertThat(mappedEdge.getCustomerId()).isEqualTo(edge.getCustomerId());
        assertThat(mappedEdge.getName()).isEqualTo(edge.getName());
        assertThat(mappedEdge.getType()).isEqualTo(edge.getType());
        assertThat(mappedEdge.getLabel()).isEqualTo(edge.getLabel());
        assertThat(mappedEdge.getRootRuleChainId()).isEqualTo(edge.getRootRuleChainId());
        assertThat(mappedEdge.getRoutingKey()).isEqualTo(edge.getRoutingKey());
        assertThat(mappedEdge.getSecret()).isEqualTo(edge.getSecret());
        assertThat(mappedEdge.getEdgeLicenseKey()).isEqualTo(edge.getEdgeLicenseKey());
        assertThat(mappedEdge.getCloudEndpoint()).isEqualTo(edge.getCloudEndpoint());
        assertThat(mappedEdge.getAdditionalInfo()).isEqualTo(edge.getAdditionalInfo());

        PageData<Edge> edges = entityService.findUserEntities(tenantId, edge.getCustomerId(), new MergedUserPermissions(
                Map.of(ALL, Set.of(Operation.ALL)), Map.of()
        ), EntityType.EDGE, Operation.READ, null, new PageLink(100));
        assertThat(edges.getData()).contains(mappedEdge);
    }

    @Test
    public void testDashboardEntityMapping() {
        Dashboard dashboard = new Dashboard();
        dashboard.setTenantId(tenantId);
        dashboard.setCustomerId(customerId);
        dashboard.setTitle("test");
        dashboard.setImage("image");
        dashboard.setMobileHide(true);
        dashboard.setMobileOrder(10);
        dashboard.setExternalId(new DashboardId(UUID.randomUUID()));
        dashboard.setConfiguration(JacksonUtil.newObjectNode().put("test", "test"));
        dashboard = dashboardService.saveDashboard(dashboard);

        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT " + EntityMapping.dashboardMapping.getMappings().keySet().stream()
                .map(field -> "e." + field)
                .collect(Collectors.joining(", ")) + " FROM dashboard e WHERE id = ?", dashboard.getUuidId());
        Dashboard mappedDashboard = EntityMapping.dashboardMapping.map(row);
        assertThat(mappedDashboard.getId()).isEqualTo(dashboard.getId());
        assertThat(mappedDashboard.getCreatedTime()).isEqualTo(dashboard.getCreatedTime());
        assertThat(mappedDashboard.getTenantId()).isEqualTo(dashboard.getTenantId());
        assertThat(mappedDashboard.getCustomerId()).isEqualTo(dashboard.getCustomerId());
        assertThat(mappedDashboard.getTitle()).isEqualTo(dashboard.getTitle());
        assertThat(mappedDashboard.getImage()).isEqualTo(dashboard.getImage());
        assertThat(mappedDashboard.isMobileHide()).isEqualTo(dashboard.isMobileHide());
        assertThat(mappedDashboard.getMobileOrder()).isEqualTo(dashboard.getMobileOrder());

        PageData<Dashboard> dashboards = entityService.findUserEntities(tenantId, dashboard.getCustomerId(), new MergedUserPermissions(
                Map.of(ALL, Set.of(Operation.ALL)), Map.of()
        ), EntityType.DASHBOARD, Operation.READ, null, new PageLink(100));
        assertThat(dashboards.getData()).contains(mappedDashboard);
    }

    @Test
    public void testCustomerEntityMapping() {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle("test");
        customer.setParentCustomerId(customerId);
        customer.setCountry("country");
        customer.setState("state");
        customer.setCity("city");
        customer.setAddress("address");
        customer.setAddress2("address2");
        customer.setZip("zip");
        customer.setPhone("phone");
        customer.setEmail("email");
        customer.setExternalId(customerId);
        customer.setAdditionalInfo(JacksonUtil.newObjectNode().put("test", "test"));
        customer = customerDao.save(tenantId, customer);

        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT " + EntityMapping.customerMapping.getMappings().keySet().stream()
                .map(field -> "e." + field)
                .collect(Collectors.joining(", ")) + " FROM customer e WHERE id = ?", customer.getUuidId());
        Customer mappedCustomer = EntityMapping.customerMapping.map(row);
        assertThat(mappedCustomer.getId()).isEqualTo(customer.getId());
        assertThat(mappedCustomer.getCreatedTime()).isEqualTo(customer.getCreatedTime());
        assertThat(mappedCustomer.getTenantId()).isEqualTo(customer.getTenantId());
        assertThat(mappedCustomer.getTitle()).isEqualTo(customer.getTitle());
        assertThat(mappedCustomer.getParentCustomerId()).isEqualTo(customer.getParentCustomerId());
        assertThat(mappedCustomer.getCountry()).isEqualTo(customer.getCountry());
        assertThat(mappedCustomer.getState()).isEqualTo(customer.getState());
        assertThat(mappedCustomer.getCity()).isEqualTo(customer.getCity());
        assertThat(mappedCustomer.getAddress()).isEqualTo(customer.getAddress());
        assertThat(mappedCustomer.getAddress2()).isEqualTo(customer.getAddress2());
        assertThat(mappedCustomer.getZip()).isEqualTo(customer.getZip());
        assertThat(mappedCustomer.getPhone()).isEqualTo(customer.getPhone());
        assertThat(mappedCustomer.getEmail()).isEqualTo(customer.getEmail());
        assertThat(mappedCustomer.getAdditionalInfo()).isEqualTo(customer.getAdditionalInfo());

        PageData<Customer> customers = entityService.findUserEntities(tenantId, customer.getParentCustomerId(), new MergedUserPermissions(
                Map.of(ALL, Set.of(Operation.ALL)), Map.of()
        ), EntityType.CUSTOMER, Operation.READ, null, new PageLink(100));
        assertThat(customers.getData()).contains(mappedCustomer);
    }

    @Test
    public void testUserEntityMapping() {
        User user = new User();
        user.setTenantId(tenantId);
        user.setCustomerId(customerId);
        user.setEmail("email");
        user.setAuthority(Authority.CUSTOMER_USER);
        user.setFirstName("firstName");
        user.setLastName("lastName");
        user.setAdditionalInfo(JacksonUtil.newObjectNode().put("test", "test"));
        user = userDao.save(tenantId, user);

        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT " + EntityMapping.userMapping.getMappings().keySet().stream()
                .map(field -> "e." + field)
                .collect(Collectors.joining(", ")) + " FROM tb_user e WHERE id = ?", user.getUuidId());
        User mappedUser = EntityMapping.userMapping.map(row);
        assertThat(mappedUser.getId()).isEqualTo(user.getId());
        assertThat(mappedUser.getCreatedTime()).isEqualTo(user.getCreatedTime());
        assertThat(mappedUser.getTenantId()).isEqualTo(user.getTenantId());
        assertThat(mappedUser.getCustomerId()).isEqualTo(user.getCustomerId());
        assertThat(mappedUser.getEmail()).isEqualTo(user.getEmail());
        assertThat(mappedUser.getAuthority()).isEqualTo(user.getAuthority());
        assertThat(mappedUser.getFirstName()).isEqualTo(user.getFirstName());
        assertThat(mappedUser.getLastName()).isEqualTo(user.getLastName());
        assertThat(mappedUser.getAdditionalInfo()).isEqualTo(user.getAdditionalInfo());

        PageData<User> users = entityService.findUserEntities(tenantId, user.getCustomerId(), new MergedUserPermissions(
                Map.of(ALL, Set.of(Operation.ALL)), Map.of()
        ), EntityType.USER, Operation.READ, null, new PageLink(100));
        assertThat(users.getData()).contains(mappedUser);
    }

    private EntityDataQuery createDataQueryFilterByEntityName(String deviceNamePrefix) {
        EntityTypeFilter filter = new EntityTypeFilter();
        filter.setEntityType(EntityType.DEVICE);

        ArrayList<KeyFilter> keyFilters = new ArrayList<>();
        KeyFilter keyFilter = new KeyFilter();
        keyFilter.setKey(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));
        keyFilter.setValueType(EntityKeyValueType.STRING);
        StringFilterPredicate predicate = new StringFilterPredicate();
        predicate.setOperation(StringOperation.CONTAINS);
        predicate.setValue(new FilterPredicateValue<>(deviceNamePrefix, null, null));
        predicate.setIgnoreCase(false);
        keyFilter.setPredicate(predicate);
        keyFilters.add(keyFilter);

        ArrayList<EntityKey> entityFields = new ArrayList<>();
        entityFields.add(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));

        EntityDataSortOrder sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.ASC
        );
        EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        return new EntityDataQuery(filter, pageLink, entityFields, Collections.emptyList(), keyFilters);
    }

    private CustomerId createCustomerAndAddDevices(CustomerId parentCustomerId, String customerTitle, int numOfDevices, String deviceNamePrefix) {
        Customer customer = new Customer();
        customer.setTitle(customerTitle);
        customer.setTenantId(tenantId);
        customer.setParentCustomerId(parentCustomerId);
        Customer savedCustomer = customerService.saveCustomer(customer);
        for (int i = 0; i < numOfDevices; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setOwnerId(savedCustomer.getId());
            device.setName(deviceNamePrefix + i + " " + customerTitle);
            deviceService.saveDevice(device);
        }
        return savedCustomer.getId();
    }

    protected PageData<EntityData> findByQuery(EntityDataQuery query) {
        return findByQuery(new CustomerId(CustomerId.NULL_UUID), mergedUserPermissionsPE, query);
    }

    protected PageData<EntityData> findByQuery(CustomerId customerId, MergedUserPermissions permissions, EntityDataQuery query) {
        return entityService.findEntityDataByQuery(tenantId, customerId, permissions, query);
    }

    protected PageData<EntityData> findByQueryAndCheck(EntityDataQuery query, long expectedResultSize) {
        return findByQueryAndCheck(new CustomerId(CustomerId.NULL_UUID), mergedUserPermissionsPE, query, expectedResultSize);
    }

    protected PageData<EntityData> findByQueryAndCheck(CustomerId customerId, MergedUserPermissions permissions, EntityDataQuery query, long expectedResultSize) {
        PageData<EntityData> result = entityService.findEntityDataByQuery(tenantId, customerId, permissions, query);
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

    protected long countByQuery(CustomerId customerId, MergedUserPermissions permissions, EntityCountQuery query) {
        return entityService.countEntitiesByQuery(tenantId, customerId, permissions, query);
    }

    protected long countByQueryAndCheck(EntityCountQuery countQuery, int expectedResult) {
        return countByQueryAndCheck(new CustomerId(CustomerId.NULL_UUID), mergedUserPermissionsPE, countQuery, expectedResult);
    }

    protected long countByQueryAndCheck(CustomerId customerId, MergedUserPermissions permissions, EntityCountQuery query, int expectedResult) {
        long result = countByQuery(customerId, permissions, query);
        assertThat(result).isEqualTo(expectedResult);
        return result;
    }

}
