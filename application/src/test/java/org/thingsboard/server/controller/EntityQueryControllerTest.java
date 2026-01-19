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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataPageLink;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.AliasEntityId;
import org.thingsboard.server.common.data.query.DeviceTypeFilter;
import org.thingsboard.server.common.data.query.DynamicValue;
import org.thingsboard.server.common.data.query.DynamicValueSourceType;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.EntityListFilter;
import org.thingsboard.server.common.data.query.EntityTypeFilter;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.NumericFilterPredicate;
import org.thingsboard.server.common.data.query.RelationsQueryFilter;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.query.StringFilterPredicate;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.common.data.queue.QueueStats;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.dao.queue.QueueStatsService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.edqs.util.EdqsRocksDb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
@TestPropertySource(properties = {
        "queue.edqs.sync.enabled=true", // only enabling sync
        "queue.edqs.api.supported=false",
})
public class EntityQueryControllerTest extends AbstractControllerTest {

    private static final String CUSTOMER_USER_EMAIL = "entityQueryCustomer@thingsboard.org";
    private static final String TENANT_PASSWORD = "testPassword1";
    private static final String CUSTOMER_USER_PASSWORD = "customer";
    private static final String TENANT_EMAIL = "entityQueryTenant@thingsboard.org";

    private Tenant savedTenant;
    private User tenantAdmin;

    @Autowired
    private QueueStatsService queueStatsService;

    @MockitoBean
    private EdqsRocksDb edqsRocksDb;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = saveTenant(tenant);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail(TENANT_EMAIL);
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, TENANT_PASSWORD);
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();
        deleteTenant(savedTenant.getId());
    }

    @Test
    public void testTenantCountEntitiesByQuery() throws Exception {
        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 97; i++) {
            Device device = new Device();
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            devices.add(doPost("/api/device", device, Device.class));
            Thread.sleep(1);
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

        EntityTypeFilter filter2 = new EntityTypeFilter();
        filter2.setEntityType(EntityType.DEVICE);
        countQuery = new EntityCountQuery(filter2);
        countByQueryAndCheck(countQuery, 97);
    }

    @Test
    public void testSysAdminCountEntitiesByQuery() throws Exception {
        loginSysAdmin();

        EntityTypeFilter allDeviceFilter = new EntityTypeFilter();
        allDeviceFilter.setEntityType(EntityType.DEVICE);
        EntityCountQuery query = new EntityCountQuery(allDeviceFilter);
        countByQueryAndCheck(query, 0);

        loginTenantAdmin();

        List<Device> devices = new ArrayList<>();
        String devicePrefix = "Device" + RandomStringUtils.randomAlphabetic(5);
        for (int i = 0; i < 97; i++) {
            Device device = new Device();
            device.setName(devicePrefix + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            devices.add(doPost("/api/device", device, Device.class));
            Thread.sleep(1);
        }
        DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceTypes(List.of("default"));
        filter.setDeviceNameFilter("");

        loginSysAdmin();

        EntityCountQuery countQuery = new EntityCountQuery(filter);
        countByQueryAndCheck(countQuery, 97);

        filter.setDeviceTypes(List.of("unknown"));
        countByQueryAndCheck(countQuery, 0);

        filter.setDeviceTypes(List.of("default"));
        filter.setDeviceNameFilter(devicePrefix + "1");
        countByQueryAndCheck(countQuery, 11);

        EntityListFilter entityListFilter = new EntityListFilter();
        entityListFilter.setEntityType(EntityType.DEVICE);
        entityListFilter.setEntityList(devices.stream().map(Device::getId).map(DeviceId::toString).collect(Collectors.toList()));

        countQuery = new EntityCountQuery(entityListFilter);
        countByQueryAndCheck(countQuery, 97);

        countByQueryAndCheck(countQuery, 97);
    }

    @Test
    public void testEDQForSysAdmin() throws Exception {
        loginSysAdmin();

        ObjectNode tenantAttr = JacksonUtil.newObjectNode();
        tenantAttr.put("attr", "tenantAttrValue");
        doPost("/api/plugins/telemetry/TENANT/" + tenantId + "/attributes/" + AttributeScope.SERVER_SCOPE, tenantAttr);

        loginTenantAdmin();
        Device tenantDevice = new Device();
        tenantDevice.setName("device " + StringUtils.randomAlphanumeric(10));
        tenantDevice.setType("default");
        tenantDevice = doPost("/api/device", tenantDevice, Device.class);

        loginSysAdmin();
        TenantProfile tenantProfile = doPost("/api/tenantProfile", createTenantProfile("Test tenant profile"), TenantProfile.class);

        ObjectNode tenantProfileAttr = JacksonUtil.newObjectNode();
        tenantProfileAttr.put("attr", "tenantProfileAttrValue");
        doPost("/api/plugins/telemetry/TENANT_PROFILE/" + tenantProfile.getId() + "/attributes/" + AttributeScope.SERVER_SCOPE, tenantProfileAttr);

        // check tenant telemetry is accessible for sysadmin
        SingleEntityFilter filter = new SingleEntityFilter();
        filter.setSingleEntity(AliasEntityId.fromEntityId(tenantId));
        EntityDataSortOrder sortOrder = new EntityDataSortOrder(new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.ASC);
        EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        List<EntityKey> entityFields = List.of(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));
        List<EntityKey> latestValues = List.of(new EntityKey(EntityKeyType.ATTRIBUTE, "attr"));
        EntityDataQuery dataQuery = new EntityDataQuery(filter, pageLink, entityFields, latestValues, null);

        PageData<EntityData> loadedTenants = findByQueryAndCheck(dataQuery, 1);
        String retrievedTenantAttr = loadedTenants.getData().get(0).getLatest().get(EntityKeyType.ATTRIBUTE).get("attr").getValue();
        assertThat(retrievedTenantAttr).isEqualTo("tenantAttrValue");

        // check tenant profile telemetry is accessible for sysadmin
        filter.setSingleEntity(AliasEntityId.fromEntityId(tenantProfile.getId()));

        PageData<EntityData> loadedTenantProfiles = findByQueryAndCheck(dataQuery, 1);
        String retrievedProfileAttr = loadedTenantProfiles.getData().get(0).getLatest().get(EntityKeyType.ATTRIBUTE).get("attr").getValue();
        assertThat(retrievedProfileAttr).isEqualTo("tenantProfileAttrValue");

        // check other tenant entities are prohibited
        filter.setSingleEntity(AliasEntityId.fromEntityId(tenantDevice.getId()));
        findByQueryAndCheck(dataQuery, 0);
    }

    private TenantProfile createTenantProfile(String name) {
        TenantProfile tenantProfile = new TenantProfile();
        tenantProfile.setName(name);
        tenantProfile.setDescription(name + " Test");
        TenantProfileData tenantProfileData = new TenantProfileData();
        tenantProfileData.setConfiguration(new DefaultTenantProfileConfiguration());
        tenantProfile.setProfileData(tenantProfileData);
        tenantProfile.setDefault(false);
        tenantProfile.setIsolatedTbRuleEngine(false);
        return tenantProfile;
    }

    @Test
    public void testTenantCountAlarmsByQuery() throws Exception {
        loginTenantAdmin();
        List<Device> devices = new ArrayList<>();
        List<Alarm> alarms = new ArrayList<>();
        for (int i = 0; i < 97; i++) {
            Device device = new Device();
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            devices.add(doPost("/api/device", device, Device.class));
            Thread.sleep(1);
        }

        for (int i = 0; i < devices.size(); i++) {
            Alarm alarm = new Alarm();
            alarm.setOriginator(devices.get(i).getId());
            alarm.setType("alarm" + i);
            alarm.setSeverity(AlarmSeverity.WARNING);
            alarms.add(doPost("/api/alarm", alarm, Alarm.class));
            Thread.sleep(1);
        }
        testCountAlarmsByQuery(alarms);
    }

    @Test
    public void testTenantCountAlarmsWithEntityFilter() throws Exception {
        loginTenantAdmin();
        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Device device = new Device();
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            Device savedDevice = doPost("/api/device", device, Device.class);
            devices.add(savedDevice);
            Thread.sleep(1);

            Alarm alarm = new Alarm();
            alarm.setOriginator(savedDevice.getId());
            alarm.setType("alarm" + i);
            alarm.setSeverity(AlarmSeverity.WARNING);
            doPost("/api/alarm", alarm, Alarm.class);
            Thread.sleep(1);
        }

        List<Asset> assets = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Asset asset = new Asset();
            asset.setName("Asset" + i);
            asset.setType("default");
            asset.setLabel("testLabel" + (int) (Math.random() * 1000));
            Asset savedAsset = doPost("/api/asset", asset, Asset.class);
            assets.add(savedAsset);
            Thread.sleep(1);

            Alarm alarm = new Alarm();
            alarm.setOriginator(savedAsset.getId());
            alarm.setType("alarm" + i);
            alarm.setSeverity(AlarmSeverity.WARNING);
            doPost("/api/alarm", alarm, Alarm.class);
            Thread.sleep(1);
        }

        EntityTypeFilter assetTypeFilter = new EntityTypeFilter();
        assetTypeFilter.setEntityType(EntityType.ASSET);
        AlarmCountQuery assetAlarmQuery = new AlarmCountQuery(assetTypeFilter);

        countAlarmsByQueryAndCheck(assetAlarmQuery, assets.size());

        KeyFilter nameFilter = buildStringKeyFilter(EntityKeyType.ENTITY_FIELD, "name", StringFilterPredicate.StringOperation.STARTS_WITH, "Asset1");
        List<KeyFilter> keyFilters = Collections.singletonList(nameFilter);
        AlarmCountQuery filteredAssetAlarmQuery = new AlarmCountQuery(assetTypeFilter, keyFilters);

        Long filteredAssetAlamCount = doPostWithResponse("/api/alarmsQuery/count", filteredAssetAlarmQuery, Long.class);
        Assert.assertEquals(1, filteredAssetAlamCount.longValue());
    }

    @Test
    public void testCustomerCountAlarmsByQuery() throws Exception {
        loginTenantAdmin();
        List<Device> devices = new ArrayList<>();
        List<Alarm> alarms = new ArrayList<>();
        for (int i = 0; i < 97; i++) {
            Device device = new Device();
            device.setCustomerId(customerId);
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            devices.add(doPost("/api/device", device, Device.class));
            Thread.sleep(1);
        }

        loginCustomerUser();

        for (int i = 0; i < devices.size(); i++) {
            Alarm alarm = new Alarm();
            alarm.setCustomerId(customerId);
            alarm.setOriginator(devices.get(i).getId());
            alarm.setType("alarm" + i);
            alarm.setSeverity(AlarmSeverity.WARNING);
            alarms.add(doPost("/api/alarm", alarm, Alarm.class));
            Thread.sleep(1);
        }
        testCountAlarmsByQuery(alarms);
    }

    @Test
    public void testCustomerCountAlarmsWithEntityFilter() throws Exception {
        loginTenantAdmin();
        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Device device = new Device();
            device.setCustomerId(customerId);
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            devices.add(doPost("/api/device", device, Device.class));
            Thread.sleep(1);
        }

        List<Asset> assets = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Asset asset = new Asset();
            asset.setCustomerId(customerId);
            asset.setName("Asset" + i);
            asset.setType("default");
            asset.setLabel("testLabel" + (int) (Math.random() * 1000));
            assets.add(doPost("/api/asset", asset, Asset.class));
            Thread.sleep(1);
        }

        loginCustomerUser();

        for (int i = 0; i < devices.size(); i++) {
            Alarm alarm = new Alarm();
            alarm.setCustomerId(customerId);
            alarm.setOriginator(devices.get(i).getId());
            alarm.setType("alarm" + i);
            alarm.setSeverity(AlarmSeverity.WARNING);
            doPost("/api/alarm", alarm, Alarm.class);
            Thread.sleep(1);
        }

        for (int i = 0; i < assets.size(); i++) {
            Alarm alarm = new Alarm();
            alarm.setCustomerId(customerId);
            alarm.setOriginator(assets.get(i).getId());
            alarm.setType("alarm" + i);
            alarm.setSeverity(AlarmSeverity.WARNING);
            doPost("/api/alarm", alarm, Alarm.class);
            Thread.sleep(1);
        }

        EntityTypeFilter assetTypeFilter = new EntityTypeFilter();
        assetTypeFilter.setEntityType(EntityType.ASSET);
        AlarmCountQuery assetAlarmQuery = new AlarmCountQuery(assetTypeFilter);

        countAlarmsByQueryAndCheck(assetAlarmQuery, 10);

        KeyFilter nameFilter = buildStringKeyFilter(EntityKeyType.ENTITY_FIELD, "name", StringFilterPredicate.StringOperation.STARTS_WITH, "Asset1");
        List<KeyFilter> keyFilters = Collections.singletonList(nameFilter);
        AlarmCountQuery filteredAssetAlarmQuery = new AlarmCountQuery(assetTypeFilter, keyFilters);

        Long filteredAssetAlamCount = doPostWithResponse("/api/alarmsQuery/count", filteredAssetAlarmQuery, Long.class);
        Assert.assertEquals(1, filteredAssetAlamCount.longValue());
    }

    @Test
    public void testFindTenantAlarmsWithEntityFilter() throws Exception {
        loginTenantAdmin();
        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Device device = new Device();
            device.setCustomerId(customerId);
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            devices.add(doPost("/api/device", device, Device.class));
            Thread.sleep(1);
        }

        List<Asset> assets = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Asset asset = new Asset();
            asset.setCustomerId(customerId);
            asset.setName("Asset" + i);
            asset.setType("default");
            asset.setLabel("testLabel" + (int) (Math.random() * 1000));
            assets.add(doPost("/api/asset", asset, Asset.class));
            Thread.sleep(1);
        }

        for (int i = 0; i < devices.size(); i++) {
            Alarm alarm = new Alarm();
            alarm.setOriginator(devices.get(i).getId());
            alarm.setType("alarm" + i);
            alarm.setSeverity(AlarmSeverity.WARNING);
            doPost("/api/alarm", alarm, Alarm.class);
            Thread.sleep(1);
        }

        List<String> assetAlarmTypes = new ArrayList<>();
        for (int i = 0; i < assets.size(); i++) {
            Alarm alarm = new Alarm();
            alarm.setOriginator(assets.get(i).getId());
            String type = "asset alarm" + i;
            alarm.setType(type);
            assetAlarmTypes.add(type);
            alarm.setSeverity(AlarmSeverity.WARNING);
            doPost("/api/alarm", alarm, Alarm.class);
            Thread.sleep(1);
        }

        AlarmDataPageLink pageLink = new AlarmDataPageLink();
        pageLink.setPage(0);
        pageLink.setPageSize(100);
        pageLink.setSortOrder(new EntityDataSortOrder(new EntityKey(EntityKeyType.ALARM_FIELD, "assignee")));

        List<EntityKey> alarmFields = new ArrayList<>();
        alarmFields.add(new EntityKey(EntityKeyType.ALARM_FIELD, "type"));
        alarmFields.add(new EntityKey(EntityKeyType.ALARM_FIELD, "originatorDisplayName"));

        EntityTypeFilter assetTypeFilter = new EntityTypeFilter();
        assetTypeFilter.setEntityType(EntityType.ASSET);
        AlarmDataQuery assetAlarmQuery = new AlarmDataQuery(assetTypeFilter, pageLink, null, null, null, alarmFields);

        PageData<AlarmData> alarmPageData = findAlarmsByQueryAndCheck(assetAlarmQuery, 10);
        List<String> retrievedAlarmTypes = alarmPageData.getData().stream().map(AlarmData::getType).toList();
        assertThat(retrievedAlarmTypes).containsExactlyInAnyOrderElementsOf(assetAlarmTypes);
        List<String> retrievedAlarmDisplayName = alarmPageData.getData().stream().map(AlarmData::getOriginatorDisplayName).toList();
        assertThat(retrievedAlarmDisplayName).containsExactlyInAnyOrderElementsOf(assets.stream().map(Asset::getLabel).toList());

        KeyFilter nameFilter = buildStringKeyFilter(EntityKeyType.ENTITY_FIELD, "name", StringFilterPredicate.StringOperation.STARTS_WITH, "Asset1");
        List<KeyFilter> keyFilters = Collections.singletonList(nameFilter);
        AlarmDataQuery filteredAssetAlarmQuery = new AlarmDataQuery(assetTypeFilter, pageLink, null, null, keyFilters, alarmFields);
        PageData<AlarmData> filteredAssetAlamData = doPostWithTypedResponse("/api/alarmsQuery/find", filteredAssetAlarmQuery, new TypeReference<>() {
        });
        Assert.assertEquals(1, filteredAssetAlamData.getTotalElements());
    }

    @Test
    public void testFindCustomerAlarmsWithEntityFilter() throws Exception {
        loginTenantAdmin();
        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Device device = new Device();
            device.setCustomerId(customerId);
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            devices.add(doPost("/api/device", device, Device.class));
            Thread.sleep(1);
        }

        List<Asset> assets = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Asset asset = new Asset();
            asset.setCustomerId(customerId);
            asset.setName("Asset" + i);
            asset.setType("default");
            asset.setLabel("testLabel" + (int) (Math.random() * 1000));
            assets.add(doPost("/api/asset", asset, Asset.class));
            Thread.sleep(1);
        }

        loginCustomerUser();

        for (int i = 0; i < devices.size(); i++) {
            Alarm alarm = new Alarm();
            alarm.setCustomerId(customerId);
            alarm.setOriginator(devices.get(i).getId());
            alarm.setType("alarm" + i);
            alarm.setSeverity(AlarmSeverity.WARNING);
            doPost("/api/alarm", alarm, Alarm.class);
            Thread.sleep(1);
        }

        List<String> assetAlarmTypes = new ArrayList<>();
        for (int i = 0; i < assets.size(); i++) {
            Alarm alarm = new Alarm();
            alarm.setCustomerId(customerId);
            alarm.setOriginator(assets.get(i).getId());
            String type = "asset alarm" + i;
            alarm.setType(type);
            assetAlarmTypes.add(type);
            alarm.setSeverity(AlarmSeverity.WARNING);
            doPost("/api/alarm", alarm, Alarm.class);
            Thread.sleep(1);
        }

        AlarmDataPageLink pageLink = new AlarmDataPageLink();
        pageLink.setPage(0);
        pageLink.setPageSize(100);
        pageLink.setSortOrder(new EntityDataSortOrder(new EntityKey(EntityKeyType.ALARM_FIELD, "assignee")));

        EntityTypeFilter assetTypeFilter = new EntityTypeFilter();
        assetTypeFilter.setEntityType(EntityType.ASSET);
        AlarmDataQuery assetAlarmQuery = new AlarmDataQuery(assetTypeFilter, pageLink, null, null, null, Collections.emptyList());

        PageData<AlarmData> alarmPageData = findAlarmsByQueryAndCheck(assetAlarmQuery, 10);
        List<String> retrievedAlarmTypes = alarmPageData.getData().stream().map(Alarm::getType).toList();
        assertThat(retrievedAlarmTypes).containsExactlyInAnyOrderElementsOf(assetAlarmTypes);

        KeyFilter nameFilter = buildStringKeyFilter(EntityKeyType.ENTITY_FIELD, "name", StringFilterPredicate.StringOperation.STARTS_WITH, "Asset1");
        List<KeyFilter> keyFilters = Collections.singletonList(nameFilter);
        AlarmDataQuery filteredAssetAlarmQuery = new AlarmDataQuery(assetTypeFilter, pageLink, null, null, keyFilters, Collections.emptyList());
        PageData<AlarmData> filteredAssetAlamData = doPostWithTypedResponse("/api/alarmsQuery/find", filteredAssetAlarmQuery, new TypeReference<>() {
        });
        Assert.assertEquals(1, filteredAssetAlamData.getTotalElements());
    }

    @Test
    public void testFindAlarmsWithEntityFilterAndLatestValues() throws Exception {
        loginTenantAdmin();
        List<Device> devices = new ArrayList<>();
        List<String> temps = new ArrayList<>();
        List<String> deviceNames = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Device device = new Device();
            device.setCustomerId(customerId);
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            device = doPost("/api/device", device, Device.class);
            devices.add(device);
            deviceNames.add(device.getName());

            int temp = i * 10;
            temps.add(String.valueOf(temp));
            JsonNode content = JacksonUtil.toJsonNode("{\"temperature\": " + temp + "}");
            doPost("/api/plugins/telemetry/" + EntityType.DEVICE.name() + "/" + device.getUuidId() + "/timeseries/SERVER_SCOPE", content)
                    .andExpect(status().isOk());
            Thread.sleep(1);
        }

        for (int i = 0; i < devices.size(); i++) {
            Alarm alarm = new Alarm();
            alarm.setCustomerId(customerId);
            alarm.setOriginator(devices.get(i).getId());
            String type = "device alarm" + i;
            alarm.setType(type);
            alarm.setSeverity(AlarmSeverity.WARNING);
            doPost("/api/alarm", alarm, Alarm.class);
            Thread.sleep(1);
        }

        AlarmDataPageLink pageLink = new AlarmDataPageLink();
        pageLink.setPage(0);
        pageLink.setPageSize(100);
        pageLink.setSortOrder(new EntityDataSortOrder(new EntityKey(EntityKeyType.ALARM_FIELD, "created_time")));

        List<EntityKey> alarmFields = new ArrayList<>();
        alarmFields.add(new EntityKey(EntityKeyType.ALARM_FIELD, "type"));

        List<EntityKey> entityFields = new ArrayList<>();
        entityFields.add(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));

        List<EntityKey> latestValues = new ArrayList<>();
        latestValues.add(new EntityKey(EntityKeyType.TIME_SERIES, "temperature"));

        EntityTypeFilter deviceTypeFilter = new EntityTypeFilter();
        deviceTypeFilter.setEntityType(EntityType.DEVICE);
        AlarmDataQuery deviceAlarmQuery = new AlarmDataQuery(deviceTypeFilter, pageLink, entityFields, latestValues, null, alarmFields);

        PageData<AlarmData> alarmPageData = findAlarmsByQueryAndCheck(deviceAlarmQuery, 10);
        List<String> retrievedAlarmTemps = alarmPageData.getData().stream().map(alarmData -> alarmData.getLatest().get(EntityKeyType.TIME_SERIES).get("temperature").getValue()).toList();
        assertThat(retrievedAlarmTemps).containsExactlyInAnyOrderElementsOf(temps);

        List<String> retrievedDeviceNames = alarmPageData.getData().stream().map(alarmData -> alarmData.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue()).toList();
        assertThat(retrievedDeviceNames).containsExactlyInAnyOrderElementsOf(deviceNames);
    }

    private void testCountAlarmsByQuery(List<Alarm> alarms) throws Exception {
        AlarmCountQuery countQuery = new AlarmCountQuery();

        Long count = doPostWithResponse("/api/alarmsQuery/count", countQuery, Long.class);
        Assert.assertEquals(97, count.longValue());

        countQuery = AlarmCountQuery.builder()
                .typeList(List.of("unknown"))
                .build();

        count = doPostWithResponse("/api/alarmsQuery/count", countQuery, Long.class);
        Assert.assertEquals(0, count.longValue());

        countQuery = AlarmCountQuery.builder()
                .typeList(List.of("alarm1", "alarm2", "alarm3"))
                .build();

        count = doPostWithResponse("/api/alarmsQuery/count", countQuery, Long.class);
        Assert.assertEquals(3, count.longValue());

        countQuery = AlarmCountQuery.builder()
                .typeList(alarms.stream().map(Alarm::getType).collect(Collectors.toList()))
                .build();

        count = doPostWithResponse("/api/alarmsQuery/count", countQuery, Long.class);
        Assert.assertEquals(97, count.longValue());

        countQuery = AlarmCountQuery.builder()
                .severityList(List.of(AlarmSeverity.CRITICAL))
                .build();

        count = doPostWithResponse("/api/alarmsQuery/count", countQuery, Long.class);
        Assert.assertEquals(0, count.longValue());

        countQuery = AlarmCountQuery.builder()
                .severityList(List.of(AlarmSeverity.WARNING))
                .build();

        count = doPostWithResponse("/api/alarmsQuery/count", countQuery, Long.class);
        Assert.assertEquals(97, count.longValue());

        long startTs = alarms.stream().map(Alarm::getCreatedTime).min(Long::compareTo).get();
        long endTs = alarms.stream().map(Alarm::getCreatedTime).max(Long::compareTo).get();

        countQuery = AlarmCountQuery.builder()
                .startTs(startTs - 1)
                .endTs(endTs + 1)
                .build();

        count = doPostWithResponse("/api/alarmsQuery/count", countQuery, Long.class);
        Assert.assertEquals(97, count.longValue());

        countQuery = AlarmCountQuery.builder()
                .startTs(0)
                .endTs(endTs + 1)
                .build();

        count = doPostWithResponse("/api/alarmsQuery/count", countQuery, Long.class);
        Assert.assertEquals(97, count.longValue());

        countQuery = AlarmCountQuery.builder()
                .startTs(0)
                .endTs(System.currentTimeMillis())
                .build();

        count = doPostWithResponse("/api/alarmsQuery/count", countQuery, Long.class);
        Assert.assertEquals(97, count.longValue());

        countQuery = AlarmCountQuery.builder()
                .startTs(endTs + 1)
                .endTs(System.currentTimeMillis())
                .build();

        count = doPostWithResponse("/api/alarmsQuery/count", countQuery, Long.class);
        Assert.assertEquals(0, count.longValue());
    }

    @Test
    public void testSimpleFindEntityDataByQuery() throws Exception {
        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 97; i++) {
            Device device = new Device();
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            devices.add(doPost("/api/device", device, Device.class));
            Thread.sleep(1);
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

        Assert.assertEquals(deviceIds, loadedIds);

        List<String> loadedNames = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue()).collect(Collectors.toList());
        List<String> deviceNames = devices.stream().map(Device::getName).collect(Collectors.toList());

        Assert.assertEquals(deviceNames, loadedNames);

        sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "name"), EntityDataSortOrder.Direction.DESC
        );

        pageLink = new EntityDataPageLink(10, 0, "device1", sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, null, null);
        data = findByQuery(query);
        Assert.assertEquals(11, data.getTotalElements());
        Assert.assertEquals("Device19", data.getData().get(0).getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue());


        EntityTypeFilter filter2 = new EntityTypeFilter();
        filter2.setEntityType(EntityType.DEVICE);

        EntityDataSortOrder sortOrder2 = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.ASC
        );
        EntityDataPageLink pageLink2 = new EntityDataPageLink(10, 0, null, sortOrder2);
        List<EntityKey> entityFields2 = Collections.singletonList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));

        EntityDataQuery query2 = new EntityDataQuery(filter2, pageLink2, entityFields2, null, null);

        PageData<EntityData> data2 = findByQuery(query2);

        Assert.assertEquals(97, data2.getTotalElements());
        Assert.assertEquals(10, data2.getTotalPages());
        Assert.assertTrue(data2.hasNext());
        Assert.assertEquals(10, data2.getData().size());

    }

    @Test
    public void testFindEntityDataByQueryWithNegateParam() throws Exception {
        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 97; i++) {
            Device device = new Device();
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            devices.add(doPost("/api/device", device, Device.class));
            Thread.sleep(1);
        }

        Device mainDevice = new Device();
        mainDevice.setName("Main device");
        mainDevice = doPost("/api/device", mainDevice, Device.class);

        for (int i = 0; i < 10; i++) {
            EntityRelation relation = createFromRelation(mainDevice, devices.get(i), "CONTAINS");
            doPost("/api/relation", relation).andExpect(status().isOk());
        }

        for (int i = 10; i < 97; i++) {
            EntityRelation relation = createFromRelation(mainDevice, devices.get(i), "NOT_CONTAINS");
            doPost("/api/relation", relation).andExpect(status().isOk());
        }

        RelationsQueryFilter filter = new RelationsQueryFilter();
        filter.setRootEntity(AliasEntityId.fromEntityId(mainDevice.getId()));
        filter.setDirection(EntitySearchDirection.FROM);
        filter.setNegate(true);
        filter.setFilters(List.of(new RelationEntityTypeFilter("CONTAINS", List.of(EntityType.DEVICE), false)));

        EntityDataSortOrder sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.ASC
        );
        EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        List<EntityKey> entityFields = Collections.singletonList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));

        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, null, null);
        findByQueryAndCheck(query, 87);

        filter.setFilters(List.of(new RelationEntityTypeFilter("NOT_CONTAINS", List.of(EntityType.DEVICE), false)));
        query = new EntityDataQuery(filter, pageLink, entityFields, null, null);
        findByQueryAndCheck(query, 10);

        filter.setFilters(List.of(new RelationEntityTypeFilter("NOT_CONTAINS", List.of(EntityType.DEVICE), true)));
        query = new EntityDataQuery(filter, pageLink, entityFields, null, null);
        findByQueryAndCheck(query, 87);
    }

    private EntityRelation createFromRelation(Device mainDevice, Device device, String relationType) {
        return new EntityRelation(mainDevice.getId(), device.getId(), relationType);
    }

    @Test
    public void testFindEntityDataByQueryWithAttributes() throws Exception {
        List<Device> devices = new ArrayList<>();
        List<Long> temperatures = new ArrayList<>();
        List<Long> highTemperatures = new ArrayList<>();
        for (int i = 0; i < 67; i++) {
            Device device = new Device();
            String name = "Device" + i;
            device.setName(name);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            devices.add(doPost("/api/device?accessToken=" + name, device, Device.class));
            Thread.sleep(1);
            long temperature = (long) (Math.random() * 100);
            temperatures.add(temperature);
            if (temperature > 45) {
                highTemperatures.add(temperature);
            }
        }
        for (int i = 0; i < devices.size(); i++) {
            Device device = devices.get(i);
            String payload = "{\"temperature\":" + temperatures.get(i) + "}";
            doPost("/api/plugins/telemetry/" + device.getId() + "/" + DataConstants.SHARED_SCOPE, payload, String.class, status().isOk());
        }
        Thread.sleep(1000);

        DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceTypes(List.of("default"));
        filter.setDeviceNameFilter("");

        EntityDataSortOrder sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.ASC
        );
        EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        List<EntityKey> entityFields = Collections.singletonList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));
        List<EntityKey> latestValues = List.of(new EntityKey(EntityKeyType.ATTRIBUTE, "temperature"),
                new EntityKey(EntityKeyType.ATTRIBUTE, "non-existing-attribute"));

        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, null);
        PageData<EntityData> data = findByQueryAndCheck(query, 67);

        List<EntityData> loadedEntities = new ArrayList<>(data.getData());
        while (data.hasNext()) {
            query = query.next();
            data = findByQuery(query);
            loadedEntities.addAll(data.getData());
        }
        Assert.assertEquals(67, loadedEntities.size());

        List<String> loadedTemperatures = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ATTRIBUTE).get("temperature").getValue()).collect(Collectors.toList());
        List<String> deviceTemperatures = temperatures.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());
        Assert.assertEquals(deviceTemperatures, loadedTemperatures);

        // check ts value == 0, value is empty string for non-existing data points
        List<TsValue> loadedNonExistingAttributes = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ATTRIBUTE).get("non-existing-attribute")).toList();
        loadedNonExistingAttributes.forEach(tsValue -> {
            assertThat(tsValue.getTs()).isEqualTo(0L);
            assertThat(tsValue.getValue()).isEqualTo("");
        });

        pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        KeyFilter highTemperatureFilter = new KeyFilter();
        highTemperatureFilter.setKey(new EntityKey(EntityKeyType.ATTRIBUTE, "temperature"));
        highTemperatureFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate predicate = new NumericFilterPredicate();
        predicate.setValue(FilterPredicateValue.fromDouble(45));
        predicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperatureFilter.setPredicate(predicate);
        List<KeyFilter> keyFilters = Collections.singletonList(highTemperatureFilter);

        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFilters);

        data = findByQuery(query);
        loadedEntities = new ArrayList<>(data.getData());
        while (data.hasNext()) {
            query = query.next();
            data = findByQuery(query);
            loadedEntities.addAll(data.getData());
        }
        Assert.assertEquals(highTemperatures.size(), loadedEntities.size());

        List<String> loadedHighTemperatures = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ATTRIBUTE).get("temperature").getValue()).collect(Collectors.toList());
        List<String> deviceHighTemperatures = highTemperatures.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());

        Assert.assertEquals(deviceHighTemperatures, loadedHighTemperatures);
    }

    @Test
    public void testFindEntityDataByQueryWithDynamicValue() throws Exception {
        int numOfDevices = 2;

        for (int i = 0; i < numOfDevices; i++) {
            Device device = new Device();
            String name = "Device" + i;
            device.setName(name);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));

            Device savedDevice1 = doPost("/api/device?accessToken=" + name, device, Device.class);
            JsonNode content = JacksonUtil.toJsonNode("{\"alarmActiveTime\": 1" + i + "}");
            doPost("/api/plugins/telemetry/" + EntityType.DEVICE.name() + "/" + savedDevice1.getUuidId() + "/SERVER_SCOPE", content)
                    .andExpect(status().isOk());
        }
        JsonNode content = JacksonUtil.toJsonNode("{\"dynamicValue\": 0}");
        doPost("/api/plugins/telemetry/" + EntityType.TENANT.name() + "/" + tenantId.getId() + "/SERVER_SCOPE", content)
                .andExpect(status().isOk());


        DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceTypes(List.of("default"));
        filter.setDeviceNameFilter("");

        KeyFilter highTemperatureFilter = new KeyFilter();
        highTemperatureFilter.setKey(new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, "alarmActiveTime"));
        highTemperatureFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate predicate = new NumericFilterPredicate();

        DynamicValue<Double> dynamicValue =
                new DynamicValue<>(DynamicValueSourceType.CURRENT_TENANT, "dynamicValue");
        FilterPredicateValue<Double> predicateValue = new FilterPredicateValue<>(0.0, null, dynamicValue);

        predicate.setValue(predicateValue);
        predicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperatureFilter.setPredicate(predicate);

        List<KeyFilter> keyFilters = Collections.singletonList(highTemperatureFilter);


        EntityDataSortOrder sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.ASC
        );
        EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, sortOrder);

        List<EntityKey> entityFields = Collections.singletonList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));
        List<EntityKey> latestValues = Collections.singletonList(new EntityKey(EntityKeyType.ATTRIBUTE, "alarmActiveTime"));

        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFilters);

        await()
                .alias("data by query")
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> {
                    var data = findByQuery(query);
                    var loadedEntities = new ArrayList<>(data.getData());
                    return loadedEntities.size() == numOfDevices;
                });

        var data = findByQuery(query);
        var loadedEntities = new ArrayList<>(data.getData());

        Assert.assertEquals(numOfDevices, loadedEntities.size());

        for (int i = 0; i < numOfDevices; i++) {
            var entity = loadedEntities.get(i);
            String name = entity.getLatest().get(EntityKeyType.ENTITY_FIELD).getOrDefault("name", new TsValue(0, "Invalid")).getValue();
            String alarmActiveTime = entity.getLatest().get(EntityKeyType.ATTRIBUTE).getOrDefault("alarmActiveTime", new TsValue(0, "-1")).getValue();

            Assert.assertEquals("Device" + i, name);
            Assert.assertEquals("1" + i, alarmActiveTime);
        }
    }

    @Test
    public void givenInvalidEntityDataPageLink_thenReturnError() throws Exception {
        DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceTypes(List.of("default"));
        filter.setDeviceNameFilter("");

        String invalidSortProperty = "created(Time)";
        EntityDataSortOrder sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, invalidSortProperty), EntityDataSortOrder.Direction.ASC
        );
        EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        List<EntityKey> entityFields = Collections.singletonList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));
        List<EntityKey> latestValues = Collections.singletonList(new EntityKey(EntityKeyType.ATTRIBUTE, "temperature"));
        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, null);

        ResultActions result = doPost("/api/entitiesQuery/find", query).andExpect(status().isBadRequest());
        assertThat(getErrorMessage(result)).contains("Invalid").contains("sort property");
    }

    @Test
    public void testFindQueueStatsEntitiesByQuery() throws Exception {
        List<QueueStats> queueStatsList = new ArrayList<>();
        for (int i = 0; i < 97; i++) {
            QueueStats queueStats = new QueueStats();
            queueStats.setQueueName("test" + StringUtils.randomAlphabetic(5));
            queueStats.setServiceId(StringUtils.randomAlphabetic(5));
            queueStats.setTenantId(savedTenant.getTenantId());
            queueStatsList.add(queueStatsService.save(savedTenant.getId(), queueStats));
            Thread.sleep(1);
        }

        EntityTypeFilter entityTypeFilter = new EntityTypeFilter();
        entityTypeFilter.setEntityType(EntityType.QUEUE_STATS);

        EntityDataSortOrder sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "queueName"), EntityDataSortOrder.Direction.ASC
        );
        EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        List<EntityKey> entityFields = Arrays.asList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"), new EntityKey(EntityKeyType.ENTITY_FIELD, "queueName"),
                new EntityKey(EntityKeyType.ENTITY_FIELD, "serviceId"));
        List<KeyFilter> keyFilters = Collections.singletonList(
                getEntityFieldStartsWithFilter("queueName", "test")
        );

        EntityDataQuery query = new EntityDataQuery(entityTypeFilter, pageLink, entityFields, null, keyFilters);

        PageData<EntityData> data = findByQueryAndCheck(query, 97);
        Assert.assertEquals(10, data.getTotalPages());
        Assert.assertTrue(data.hasNext());
        Assert.assertEquals(10, data.getData().size());
        data.getData().forEach(entityData -> {
            String queueName = entityData.getLatest().get(EntityKeyType.ENTITY_FIELD).get("queueName").getValue();
            String serviceId = entityData.getLatest().get(EntityKeyType.ENTITY_FIELD).get("serviceId").getValue();
            assertThat(queueName).isNotBlank();
            assertThat(serviceId).isNotBlank();
            assertThat(entityData.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue()).isEqualTo(queueName + "_" + serviceId);

        });

        EntityCountQuery countQuery = new EntityCountQuery(entityTypeFilter, keyFilters);
        countByQueryAndCheck(countQuery, 97);
    }

    @Test
    public void testFindDevicesCountByOwnerNameAndOwnerType() throws Exception {
        loginTenantAdmin();
        int numOfDevices = 8;

        for (int i = 0; i < numOfDevices; i++) {
            Device device = new Device();
            String name = "Device" + i;
            device.setName(name);
            device.setType("default");

            Device savedDevice = doPost("/api/device?accessToken=" + name, device, Device.class);
            JsonNode content = JacksonUtil.toJsonNode("{\"alarmActiveTime\": 1" + i + "}");
            doPost("/api/plugins/telemetry/" + EntityType.DEVICE.name() + "/" + savedDevice.getUuidId() + "/SERVER_SCOPE", content)
                    .andExpect(status().isOk());
        }

        DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceTypes(List.of("default"));
        filter.setDeviceNameFilter("");

        KeyFilter activeAlarmTimeFilter = getServerAttributeNumericGreaterThanKeyFilter("alarmActiveTime", 5);
        KeyFilter activeAlarmTimeToLongFilter = getServerAttributeNumericGreaterThanKeyFilter("alarmActiveTime", 30);
        KeyFilter tenantOwnerNameFilter = getEntityFieldEqualFilter("ownerName", TEST_TENANT_NAME);
        KeyFilter wrongOwnerNameFilter = getEntityFieldEqualFilter("ownerName", "wrongName");
        KeyFilter tenantOwnerTypeFilter = getEntityFieldEqualFilter("ownerType", "TENANT");
        KeyFilter customerOwnerTypeFilter = getEntityFieldEqualFilter("ownerType", "CUSTOMER");

        // all devices with ownerName = TEST TENANT
        EntityCountQuery query = new EntityCountQuery(filter, List.of(activeAlarmTimeFilter, tenantOwnerNameFilter));
        await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> countByQuery(query),
                result -> result == numOfDevices);

        // all devices with ownerName = TEST TENANT
        EntityCountQuery activeAlarmTimeToLongQuery = new EntityCountQuery(filter, List.of(activeAlarmTimeToLongFilter, tenantOwnerNameFilter));
        countByQueryAndCheck(activeAlarmTimeToLongQuery, 0);

        // all devices with wrong ownerName
        EntityCountQuery wrongTenantNameQuery = new EntityCountQuery(filter, List.of(activeAlarmTimeFilter, wrongOwnerNameFilter));
        countByQueryAndCheck(wrongTenantNameQuery, 0);

        // all devices with owner type = TENANT
        EntityCountQuery tenantEntitiesQuery = new EntityCountQuery(filter, List.of(activeAlarmTimeFilter, tenantOwnerTypeFilter));
        countByQueryAndCheck(tenantEntitiesQuery, numOfDevices);

        // all devices with owner type = CUSTOMER
        EntityCountQuery customerEntitiesQuery = new EntityCountQuery(filter, List.of(activeAlarmTimeFilter, customerOwnerTypeFilter));
        countByQueryAndCheck(customerEntitiesQuery, 0);
    }

    @Test
    public void testFindDevicesByDisplayName() throws Exception {
        loginTenantAdmin();
        int numOfDevices = 3;

        for (int i = 0; i < numOfDevices; i++) {
            Device device = new Device();
            String name = "Device" + i;
            device.setName(name);
            device.setLabel("Device Label " + i);
            device.setType("testFindDevicesByDisplayName");

            Device savedDevice = doPost("/api/device?accessToken=" + name, device, Device.class);
        }

        DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceTypes(List.of("testFindDevicesByDisplayName"));
        filter.setDeviceNameFilter("");

        KeyFilter displayNameFilter = getEntityFieldEqualFilter("displayName", "Device Label " + 0);

        EntityDataSortOrder sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "displayName"), EntityDataSortOrder.Direction.ASC
        );
        EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        List<EntityKey> entityFields = List.of(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"), new EntityKey(EntityKeyType.ENTITY_FIELD, "displayName"));

        // all devices with ownerName = TEST TENANT
        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, Collections.emptyList(), Collections.emptyList());
        checkEntitiesByQuery(query, numOfDevices, (i, entity) -> {
            String name = entity.getLatest().get(EntityKeyType.ENTITY_FIELD).getOrDefault("name", new TsValue(0, "Invalid")).getValue();
            String displayName = entity.getLatest().get(EntityKeyType.ENTITY_FIELD).getOrDefault("displayName", new TsValue(0, "Invalid")).getValue();
            Assert.assertEquals("Device" + i, name);
            Assert.assertEquals("Device Label " + i, displayName);
        });

        // all devices with ownerName = TEST TENANT
        EntityDataQuery displayNameFilterQuery = new EntityDataQuery(filter, pageLink, entityFields, Collections.emptyList(), List.of(displayNameFilter));
        checkEntitiesByQuery(displayNameFilterQuery, 1, (i, entity) -> {
            String name = entity.getLatest().get(EntityKeyType.ENTITY_FIELD).getOrDefault("name", new TsValue(0, "Invalid")).getValue();
            String displayName = entity.getLatest().get(EntityKeyType.ENTITY_FIELD).getOrDefault("displayName", new TsValue(0, "Invalid")).getValue();
            Assert.assertEquals("Device" + i, name);
            Assert.assertEquals("Device Label " + i, displayName);
        });
    }

    @Test
    public void testFindUsersByDisplayName() throws Exception {
        loginTenantAdmin();

        User userA = new User();
        userA.setAuthority(Authority.TENANT_ADMIN);
        userA.setFirstName("John");
        userA.setLastName("Doe");
        userA.setEmail("john.doe@tb.org");
        userA = doPost("/api/user", userA, User.class);
        var aId = userA.getId();

        User userB = new User();
        userB.setAuthority(Authority.TENANT_ADMIN);
        userB.setFirstName("John");
        userB.setEmail("john@tb.org");
        userB = doPost("/api/user", userB, User.class);
        var bId = userB.getId();

        User userC = new User();
        userC.setAuthority(Authority.TENANT_ADMIN);
        userC.setLastName("Doe");
        userC.setEmail("doe@tb.org");
        userC = doPost("/api/user", userC, User.class);
        var cId = userC.getId();

        User userD = new User();
        userD.setAuthority(Authority.TENANT_ADMIN);
        userD.setEmail("noname@tb.org");
        userD = doPost("/api/user", userD, User.class);
        var dId = userD.getId();

        EntityTypeFilter filter = new EntityTypeFilter();
        filter.setEntityType(EntityType.USER);

        EntityDataSortOrder sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "displayName"), EntityDataSortOrder.Direction.ASC
        );
        EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        List<EntityKey> entityFields = List.of(new EntityKey(EntityKeyType.ENTITY_FIELD, "displayName"));

        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, Collections.emptyList(), List.of(getEntityFieldEqualFilter("displayName", "John Doe")));
        checkEntitiesByQuery(query, 1, (i, entity) -> {
            Assert.assertEquals(aId, entity.getEntityId());
            String displayName = entity.getLatest().get(EntityKeyType.ENTITY_FIELD).getOrDefault("displayName", new TsValue(0, "Invalid")).getValue();
            Assert.assertEquals("John Doe", displayName);
        });
        query = new EntityDataQuery(filter, pageLink, entityFields, Collections.emptyList(), List.of(getEntityFieldEqualFilter("displayName", "John")));
        checkEntitiesByQuery(query, 1, (i, entity) -> {
            Assert.assertEquals(bId, entity.getEntityId());
            String displayName = entity.getLatest().get(EntityKeyType.ENTITY_FIELD).getOrDefault("displayName", new TsValue(0, "Invalid")).getValue();
            Assert.assertEquals("John", displayName);
        });
        query = new EntityDataQuery(filter, pageLink, entityFields, Collections.emptyList(), List.of(getEntityFieldEqualFilter("displayName", "Doe")));
        checkEntitiesByQuery(query, 1, (i, entity) -> {
            Assert.assertEquals(cId, entity.getEntityId());
            String displayName = entity.getLatest().get(EntityKeyType.ENTITY_FIELD).getOrDefault("displayName", new TsValue(0, "Invalid")).getValue();
            Assert.assertEquals("Doe", displayName);
        });
        query = new EntityDataQuery(filter, pageLink, entityFields, Collections.emptyList(), List.of(getEntityFieldEqualFilter("displayName", "noname@tb.org")));
        checkEntitiesByQuery(query, 1, (i, entity) -> {
            Assert.assertEquals(dId, entity.getEntityId());
            String displayName = entity.getLatest().get(EntityKeyType.ENTITY_FIELD).getOrDefault("displayName", new TsValue(0, "Invalid")).getValue();
            Assert.assertEquals("noname@tb.org", displayName);
        });
    }

    @Test
    public void testFindDevicesByOwnerNameAndOwnerType() throws Exception {
        loginTenantAdmin();
        int numOfDevices = 3;

        for (int i = 0; i < numOfDevices; i++) {
            Device device = new Device();
            String name = "Device" + i;
            device.setName(name);
            device.setType("default");

            Device savedDevice = doPost("/api/device?accessToken=" + name, device, Device.class);
            JsonNode content = JacksonUtil.toJsonNode("{\"alarmActiveTime\": 1" + i + "}");
            doPost("/api/plugins/telemetry/" + EntityType.DEVICE.name() + "/" + savedDevice.getUuidId() + "/SERVER_SCOPE", content)
                    .andExpect(status().isOk());
        }

        DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceTypes(List.of("default"));
        filter.setDeviceNameFilter("");

        KeyFilter activeAlarmTimeFilter = getServerAttributeNumericGreaterThanKeyFilter("alarmActiveTime", 5);
        KeyFilter tenantOwnerNameFilter = getEntityFieldEqualFilter("ownerName", TEST_TENANT_NAME);
        KeyFilter wrongOwnerNameFilter = getEntityFieldEqualFilter("ownerName", "wrongName");
        KeyFilter tenantOwnerTypeFilter = getEntityFieldEqualFilter("ownerType", "TENANT");
        KeyFilter customerOwnerTypeFilter = getEntityFieldEqualFilter("ownerType", "CUSTOMER");

        EntityDataSortOrder sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.ASC
        );
        EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        List<EntityKey> entityFields = List.of(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"), new EntityKey(EntityKeyType.ENTITY_FIELD, "ownerName"),
                new EntityKey(EntityKeyType.ENTITY_FIELD, "ownerType"));
        List<EntityKey> latestValues = Collections.singletonList(new EntityKey(EntityKeyType.ATTRIBUTE, "alarmActiveTime"));

        // all devices with ownerName = TEST TENANT
        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, List.of(activeAlarmTimeFilter, tenantOwnerNameFilter));
        BiConsumer<Integer, EntityData> checkFunction = (i, entity) -> {
            String name = entity.getLatest().get(EntityKeyType.ENTITY_FIELD).getOrDefault("name", new TsValue(0, "Invalid")).getValue();
            String ownerName = entity.getLatest().get(EntityKeyType.ENTITY_FIELD).getOrDefault("ownerName", new TsValue(0, "Invalid")).getValue();
            String ownerType = entity.getLatest().get(EntityKeyType.ENTITY_FIELD).getOrDefault("ownerType", new TsValue(0, "Invalid")).getValue();
            String alarmActiveTime = entity.getLatest().get(EntityKeyType.ATTRIBUTE).getOrDefault("alarmActiveTime", new TsValue(0, "-1")).getValue();

            Assert.assertEquals("Device" + i, name);
            Assert.assertEquals(TEST_TENANT_NAME, ownerName);
            Assert.assertEquals("TENANT", ownerType);
            Assert.assertEquals("1" + i, alarmActiveTime);
        };
        checkEntitiesByQuery(query, numOfDevices, checkFunction);

        // all devices with wrong ownerName
        EntityDataQuery wrongTenantNameQuery = new EntityDataQuery(filter, pageLink, entityFields, latestValues, List.of(activeAlarmTimeFilter, wrongOwnerNameFilter));
        checkEntitiesByQuery(wrongTenantNameQuery, 0, null);

        // all devices with owner type = TENANT
        EntityDataQuery tenantEntitiesQuery = new EntityDataQuery(filter, pageLink, entityFields, latestValues, List.of(activeAlarmTimeFilter, tenantOwnerTypeFilter));
        checkEntitiesByQuery(tenantEntitiesQuery, numOfDevices, checkFunction);

        // all devices with owner type = CUSTOMER
        EntityDataQuery customerEntitiesQuery = new EntityDataQuery(filter, pageLink, entityFields, latestValues, List.of(activeAlarmTimeFilter, customerOwnerTypeFilter));
        checkEntitiesByQuery(customerEntitiesQuery, 0, null);
    }

    @Test
    public void testFindCustomerDashboards() throws Exception {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("My dashboard");
        Dashboard savedDashboard = doPost("/api/dashboard", dashboard, Dashboard.class);

        Customer customer = new Customer();
        customer.setTitle("My customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);

        //assign dashboard
        doPost("/api/customer/" + savedCustomer.getId().getId().toString()
               + "/dashboard/" + savedDashboard.getId().getId().toString(), Dashboard.class);

        // check entity data query by customer
        User customerUser = new User();
        customerUser.setAuthority(Authority.CUSTOMER_USER);
        customerUser.setTenantId(savedTenant.getId());
        customerUser.setCustomerId(savedCustomer.getId());
        customerUser.setEmail(CUSTOMER_USER_EMAIL);

        createUserAndLogin(customerUser, CUSTOMER_USER_PASSWORD);

        EntityTypeFilter filter = new EntityTypeFilter();
        filter.setEntityType(EntityType.DASHBOARD);

        EntityDataSortOrder sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.ASC);
        EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        List<EntityKey> entityFields = Collections.singletonList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));

        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, null, null);

        findByQueryAndCheck(query, 1);

        // unnassign dashboard
        login(TENANT_EMAIL, TENANT_PASSWORD);
        doDelete("/api/customer/" + savedCustomer.getId().getId().toString() + "/dashboard/" + savedDashboard.getId().getId().toString(), Dashboard.class);

        login(CUSTOMER_USER_EMAIL, CUSTOMER_USER_PASSWORD);
        findByQueryAndCheck(query, 0);
    }

    private void checkEntitiesByQuery(EntityDataQuery query, int expectedNumOfDevices, BiConsumer<Integer, EntityData> checkFunction) throws Exception {
        await()
                .alias("data by query")
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    var data = findByQuery(query);
                    var loadedEntities = new ArrayList<>(data.getData());
                    return loadedEntities.size() == expectedNumOfDevices;
                });
        if (expectedNumOfDevices == 0) {
            return;
        }
        var data = findByQuery(query);
        var loadedEntities = new ArrayList<>(data.getData());

        Assert.assertEquals(expectedNumOfDevices, loadedEntities.size());

        for (int i = 0; i < expectedNumOfDevices; i++) {
            checkFunction.accept(i, loadedEntities.get(i));
        }
    }

    private void checkEntitiesByQuery(EntityDataQuery query, int expectedNumOfDevices, String expectedOwnerName, String expectedOwnerType) throws Exception {
        await()
                .alias("data by query")
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    var data = findByQuery(query);
                    var loadedEntities = new ArrayList<>(data.getData());
                    return loadedEntities.size() == expectedNumOfDevices;
                });
        if (expectedNumOfDevices == 0) {
            return;
        }
        var data = findByQuery(query);
        var loadedEntities = new ArrayList<>(data.getData());

        Assert.assertEquals(expectedNumOfDevices, loadedEntities.size());

        for (int i = 0; i < expectedNumOfDevices; i++) {
            var entity = loadedEntities.get(i);
            String name = entity.getLatest().get(EntityKeyType.ENTITY_FIELD).getOrDefault("name", new TsValue(0, "Invalid")).getValue();
            String ownerName = entity.getLatest().get(EntityKeyType.ENTITY_FIELD).getOrDefault("ownerName", new TsValue(0, "Invalid")).getValue();
            String ownerType = entity.getLatest().get(EntityKeyType.ENTITY_FIELD).getOrDefault("ownerType", new TsValue(0, "Invalid")).getValue();
            String alarmActiveTime = entity.getLatest().get(EntityKeyType.ATTRIBUTE).getOrDefault("alarmActiveTime", new TsValue(0, "-1")).getValue();

            Assert.assertEquals("Device" + i, name);
            Assert.assertEquals(expectedOwnerName, ownerName);
            Assert.assertEquals(expectedOwnerType, ownerType);
            Assert.assertEquals("1" + i, alarmActiveTime);
        }
    }

    protected PageData<EntityData> findByQuery(EntityDataQuery query) throws Exception {
        return doPostWithTypedResponse("/api/entitiesQuery/find", query, new TypeReference<>() {
        });
    }

    protected PageData<AlarmData> findAlarmsByQuery(AlarmDataQuery query) throws Exception {
        return doPostWithTypedResponse("/api/alarmsQuery/find", query, new TypeReference<>() {});
    }

    protected PageData<EntityData> findByQueryAndCheck(EntityDataQuery query, int expectedResultSize) throws Exception {
        PageData<EntityData> result = findByQuery(query);
        assertThat(result.getTotalElements()).isEqualTo(expectedResultSize);
        return result;
    }

    protected PageData<AlarmData> findAlarmsByQueryAndCheck(AlarmDataQuery query, int expectedResultSize) throws Exception {
        PageData<AlarmData> result = findAlarmsByQuery(query);
        assertThat(result.getTotalElements()).isEqualTo(expectedResultSize);
        return result;
    }

    protected Long countByQuery(EntityCountQuery countQuery) throws Exception {
        return doPostWithResponse("/api/entitiesQuery/count", countQuery, Long.class);
    }

    protected Long countAlarmsByQuery(AlarmCountQuery countQuery) throws Exception {
        return doPostWithResponse("/api/alarmsQuery/count", countQuery, Long.class);
    }

    protected Long countByQueryAndCheck(EntityCountQuery query, long expectedResult) throws Exception {
        Long result = countByQuery(query);
        assertThat(result).isEqualTo(expectedResult);
        return result;
    }

    protected Long countAlarmsByQueryAndCheck(AlarmCountQuery query, long expectedResult) throws Exception {
        Long result = countAlarmsByQuery(query);
        assertThat(result).isEqualTo(expectedResult);
        return result;
    }

    private KeyFilter getEntityFieldEqualFilter(String keyName, String value) {
        return getEntityFieldKeyFilter(keyName, value, StringFilterPredicate.StringOperation.EQUAL);
    }

    private KeyFilter getEntityFieldStartsWithFilter(String keyName, String value) {
        return getEntityFieldKeyFilter(keyName, value, StringFilterPredicate.StringOperation.STARTS_WITH);
    }

    private KeyFilter getEntityFieldKeyFilter(String keyName, String value, StringFilterPredicate.StringOperation operation) {
        KeyFilter filter = new KeyFilter();
        filter.setKey(new EntityKey(EntityKeyType.ENTITY_FIELD, keyName));
        filter.setValueType(EntityKeyValueType.STRING);
        StringFilterPredicate predicate = new StringFilterPredicate();
        predicate.setValue(FilterPredicateValue.fromString(value));
        predicate.setOperation(operation);
        filter.setPredicate(predicate);
        return filter;
    }

    private KeyFilter getServerAttributeNumericGreaterThanKeyFilter(String attribute, int value) {
        KeyFilter numericFilter = new KeyFilter();
        numericFilter.setKey(new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, attribute));
        numericFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate predicate = new NumericFilterPredicate();
        predicate.setValue(FilterPredicateValue.fromDouble(value));
        predicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        numericFilter.setPredicate(predicate);
        return numericFilter;
    }

    private KeyFilter buildStringKeyFilter(EntityKeyType entityKeyType, String name, StringFilterPredicate.StringOperation operation, String value) {
        KeyFilter nameFilter = new KeyFilter();
        nameFilter.setKey(new EntityKey(entityKeyType, name));
        StringFilterPredicate predicate = new StringFilterPredicate();
        predicate.setOperation(operation);
        predicate.setValue(FilterPredicateValue.fromString(value));
        nameFilter.setPredicate(predicate);
        return nameFilter;
    }

}
