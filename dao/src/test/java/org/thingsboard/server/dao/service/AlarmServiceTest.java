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
package org.thingsboard.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.alarm.AlarmCreateOrUpdateActiveRequest;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmPropagationInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.alarm.AlarmQueryV2;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.alarm.AlarmUpdateRequest;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataPageLink;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.DeviceTypeFilter;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityListFilter;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.user.UserService;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

@DaoSqlTest
public class AlarmServiceTest extends AbstractServiceTest {

    @Autowired
    AlarmService alarmService;
    @Autowired
    AssetService assetService;
    @Autowired
    CustomerService customerService;
    @Autowired
    DeviceService deviceService;
    @Autowired
    RelationService relationService;
    @Autowired
    UserService userService;

    public static final String TEST_ALARM = "TEST_ALARM";
    private static final String TEST_TENANT_EMAIL = "testtenant@thingsboard.org";
    private static final String TEST_TENANT_FIRST_NAME = "testtenantfirstname";
    private static final String TEST_TENANT_LAST_NAME = "testtenantlastname";

    @Test
    public void testSaveAndFetchAlarm() throws ExecutionException, InterruptedException {
        AssetId parentId = new AssetId(Uuids.timeBased());
        AssetId childId = new AssetId(Uuids.timeBased());

        EntityRelation relation = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);

        Assert.assertTrue(relationService.saveRelationAsync(tenantId, relation).get());

        long ts = System.currentTimeMillis();
        AlarmApiCallResult result = alarmService.createAlarm(AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tenantId)
                .originator(childId)
                .type(TEST_ALARM)
                .severity(AlarmSeverity.CRITICAL)
                .startTs(ts).build());
        Alarm created = result.getAlarm();

        Assert.assertNotNull(created);
        Assert.assertNotNull(created.getId());
        Assert.assertNotNull(created.getOriginator());
        Assert.assertNotNull(created.getSeverity());
        Assert.assertNotNull(created.getStatus());

        Assert.assertEquals(tenantId, created.getTenantId());
        Assert.assertEquals(childId, created.getOriginator());
        Assert.assertEquals(TEST_ALARM, created.getType());
        Assert.assertEquals(AlarmSeverity.CRITICAL, created.getSeverity());
        Assert.assertEquals(AlarmStatus.ACTIVE_UNACK, created.getStatus());
        Assert.assertEquals(ts, created.getStartTs());
        Assert.assertEquals(ts, created.getEndTs());
        Assert.assertEquals(0L, created.getAckTs());
        Assert.assertEquals(0L, created.getClearTs());

        Alarm fetched = alarmService.findAlarmInfoById(tenantId, created.getId());
        Assert.assertEquals(created, fetched);
    }

    @Test
    public void testFindAlarm() throws ExecutionException, InterruptedException {
        AssetId parentId = new AssetId(Uuids.timeBased());
        AssetId childId = new AssetId(Uuids.timeBased());

        EntityRelation relation = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);

        Assert.assertTrue(relationService.saveRelationAsync(tenantId, relation).get());

        long ts = System.currentTimeMillis();
        AlarmApiCallResult result = alarmService.createAlarm(AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tenantId)
                .originator(childId)
                .type(TEST_ALARM)
                .severity(AlarmSeverity.CRITICAL)
                .startTs(ts).build());
        AlarmInfo created = result.getAlarm();

        // Check child relation
        PageData<AlarmInfo> alarms = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(childId)
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build());
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, new AlarmInfo(alarms.getData().get(0)));

        // Check parent relation
        alarms = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(parentId)
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build());
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(0, alarms.getData().size());

        created.setPropagate(true);
        result = alarmService.updateAlarm(AlarmUpdateRequest.fromAlarm(created));
        created = result.getAlarm();

        // Check child relation
        alarms = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(childId)
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build());
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, new AlarmInfo(alarms.getData().get(0)));

        // Check parent relation
        alarms = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(parentId)
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build());
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, new AlarmInfo(alarms.getData().get(0)));

        alarmService.acknowledgeAlarm(tenantId, created.getId(), System.currentTimeMillis());
        created = alarmService.findAlarmInfoById(tenantId, created.getId());

        alarms = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(childId)
                .status(AlarmStatus.ACTIVE_ACK).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build());
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, new AlarmInfo(alarms.getData().get(0)));

        // Check not existing relation
        alarms = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(childId)
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build());
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(0, alarms.getData().size());

        alarmService.clearAlarm(tenantId, created.getId(), System.currentTimeMillis(), null);
        created = alarmService.findAlarmInfoById(tenantId, created.getId());

        alarms = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(childId)
                .status(AlarmStatus.CLEARED_ACK).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build());
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, new AlarmInfo(alarms.getData().get(0)));
    }

    @Test
    public void testFindAlarmV2() throws ExecutionException, InterruptedException {
        AssetId parentId = new AssetId(Uuids.timeBased());
        AssetId childId = new AssetId(Uuids.timeBased());

        EntityRelation relation = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);

        Assert.assertTrue(relationService.saveRelationAsync(tenantId, relation).get());

        long ts = System.currentTimeMillis();
        AlarmApiCallResult result = alarmService.createAlarm(AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tenantId)
                .originator(childId)
                .type(TEST_ALARM)
                .severity(AlarmSeverity.CRITICAL)
                .startTs(ts).build());
        AlarmInfo created = result.getAlarm();

        // Check child relation
        PageData<AlarmInfo> alarms = alarmService.findAlarmsV2(tenantId, AlarmQueryV2.builder()
                .affectedEntityId(childId)
                .severityList(List.of(AlarmSeverity.CRITICAL))
                .statusList(List.of(AlarmSearchStatus.ACTIVE, AlarmSearchStatus.UNACK)).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build());
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, new AlarmInfo(alarms.getData().get(0)));

        // Check parent relation
        alarms = alarmService.findAlarmsV2(tenantId, AlarmQueryV2.builder()
                .affectedEntityId(parentId)
                .severityList(List.of(AlarmSeverity.CRITICAL))
                .statusList(List.of(AlarmSearchStatus.ACTIVE, AlarmSearchStatus.UNACK)).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build());
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(0, alarms.getData().size());

        created.setPropagate(true);
        result = alarmService.updateAlarm(AlarmUpdateRequest.fromAlarm(created));
        created = result.getAlarm();

        // Check child relation
        alarms = alarmService.findAlarmsV2(tenantId, AlarmQueryV2.builder()
                .affectedEntityId(childId)
                .severityList(List.of(AlarmSeverity.CRITICAL))
                .statusList(List.of(AlarmSearchStatus.ACTIVE, AlarmSearchStatus.UNACK)).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build());
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, new AlarmInfo(alarms.getData().get(0)));

        // Check parent relation
        alarms = alarmService.findAlarmsV2(tenantId, AlarmQueryV2.builder()
                .affectedEntityId(parentId)
                .severityList(List.of(AlarmSeverity.CRITICAL))
                .statusList(List.of(AlarmSearchStatus.ACTIVE, AlarmSearchStatus.UNACK)).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build());
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, new AlarmInfo(alarms.getData().get(0)));

        alarmService.acknowledgeAlarm(tenantId, created.getId(), System.currentTimeMillis());
        created = alarmService.findAlarmInfoById(tenantId, created.getId());

        alarms = alarmService.findAlarmsV2(tenantId, AlarmQueryV2.builder()
                .affectedEntityId(childId)
                .severityList(List.of(AlarmSeverity.CRITICAL))
                .statusList(List.of(AlarmSearchStatus.ACTIVE, AlarmSearchStatus.ACK)).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build());
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, new AlarmInfo(alarms.getData().get(0)));

        // Check not existing relation
        alarms = alarmService.findAlarmsV2(tenantId, AlarmQueryV2.builder()
                .affectedEntityId(childId)
                .severityList(List.of(AlarmSeverity.CRITICAL))
                .statusList(List.of(AlarmSearchStatus.ACTIVE, AlarmSearchStatus.UNACK)).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build());
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(0, alarms.getData().size());

        alarmService.clearAlarm(tenantId, created.getId(), System.currentTimeMillis(), null);
        created = alarmService.findAlarmInfoById(tenantId, created.getId());

        alarms = alarmService.findAlarmsV2(tenantId, AlarmQueryV2.builder()
                .affectedEntityId(childId)
                .severityList(List.of(AlarmSeverity.CRITICAL))
                .statusList(List.of(AlarmSearchStatus.CLEARED, AlarmSearchStatus.ACK)).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build());
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, new AlarmInfo(alarms.getData().get(0)));
    }

    @Test
    public void testFindAssignedAlarm() {

        AssetId parentId = new AssetId(Uuids.timeBased());
        AssetId childId = new AssetId(Uuids.timeBased());

        EntityRelation relation = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);

        Assert.assertNotNull(relationService.saveRelation(tenantId, relation));

        long ts = System.currentTimeMillis();
        AlarmApiCallResult result = alarmService.createAlarm(AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tenantId)
                .originator(childId)
                .type(TEST_ALARM)
                .severity(AlarmSeverity.CRITICAL)
                .startTs(ts).build());

        AlarmInfo created = result.getAlarm();

        User tenantUser = new User();
        tenantUser.setTenantId(tenantId);
        tenantUser.setAuthority(Authority.TENANT_ADMIN);
        tenantUser.setEmail(TEST_TENANT_EMAIL);
        tenantUser.setFirstName(TEST_TENANT_FIRST_NAME);
        tenantUser.setLastName(TEST_TENANT_LAST_NAME);
        tenantUser = userService.saveUser(TenantId.SYS_TENANT_ID, tenantUser);

        Assert.assertNotNull(tenantUser);

        AlarmApiCallResult assignmentResult = alarmService.assignAlarm(tenantId, created.getId(), tenantUser.getId(), ts);
        created = assignmentResult.getAlarm();

        PageData<AlarmInfo> alarms = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .assigneeId(tenantUser.getId())
                .pageLink(new TimePageLink(1, 0, "",
                        new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build());
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, alarms.getData().get(0));

        AlarmDataPageLink pageLink = new AlarmDataPageLink();
        pageLink.setPage(0);
        pageLink.setPageSize(10);
        pageLink.setAssigneeId(tenantUser.getId());
        pageLink.setSortOrder(new EntityDataSortOrder(new EntityKey(EntityKeyType.ALARM_FIELD, "assignee")));

        PageData<AlarmData> assignedAlarms = alarmService.findAlarmDataByQueryForEntities(tenantId, toQuery(pageLink), Collections.singletonList(created.getOriginator()));
        Assert.assertNotNull(assignedAlarms.getData());
        Assert.assertEquals(1, assignedAlarms.getData().size());
        Assert.assertEquals(created, new AlarmInfo(assignedAlarms.getData().get(0)));

        User tenantUser2 = new User();
        tenantUser2.setTenantId(tenantId);
        tenantUser2.setAuthority(Authority.TENANT_ADMIN);
        tenantUser2.setEmail(2 + TEST_TENANT_EMAIL);
        tenantUser2.setFirstName(TEST_TENANT_FIRST_NAME);
        tenantUser2.setLastName(TEST_TENANT_LAST_NAME);
        tenantUser2 = userService.saveUser(TenantId.SYS_TENANT_ID, tenantUser2);

        Assert.assertNotNull(tenantUser2);
        pageLink.setAssigneeId(tenantUser2.getId());

        PageData<AlarmData> assignedToNonExistingUserAlarms = alarmService.findAlarmDataByQueryForEntities(tenantId, toQuery(pageLink), Collections.singletonList(created.getOriginator()));
        Assert.assertNotNull(assignedToNonExistingUserAlarms.getData());
        Assert.assertTrue(assignedToNonExistingUserAlarms.getData().isEmpty());

    }

    @Test
    public void testFindCustomerAlarm() {
        Customer customer = new Customer();
        customer.setTitle("TestCustomer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);

        Device tenantDevice = new Device();
        tenantDevice.setName("TestTenantDevice");
        tenantDevice.setType("default");
        tenantDevice.setTenantId(tenantId);
        tenantDevice = deviceService.saveDevice(tenantDevice);

        Device customerDevice = new Device();
        customerDevice.setName("TestCustomerDevice");
        customerDevice.setType("default");
        customerDevice.setTenantId(tenantId);
        customerDevice.setCustomerId(customer.getId());
        customerDevice = deviceService.saveDevice(customerDevice);

        long ts = System.currentTimeMillis();
        AlarmApiCallResult result = alarmService.createAlarm(AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tenantId)
                .originator(tenantDevice.getId())
                .type(TEST_ALARM)
                .severity(AlarmSeverity.CRITICAL)
                .propagation(AlarmPropagationInfo.builder().propagate(true).build())
                .startTs(ts).build());
        AlarmInfo tenantAlarm = result.getAlarm();

        result = alarmService.createAlarm(AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tenantId)
                .originator(customerDevice.getId())
                .type(TEST_ALARM)
                .severity(AlarmSeverity.CRITICAL)
                .propagation(AlarmPropagationInfo.builder().propagate(true).build())
                .startTs(ts).build());
        AlarmInfo deviceAlarm = result.getAlarm();

        AlarmDataPageLink pageLink = new AlarmDataPageLink();
        pageLink.setPage(0);
        pageLink.setPageSize(10);
        pageLink.setSortOrder(new EntityDataSortOrder(new EntityKey(EntityKeyType.ALARM_FIELD, "createdTime")));

        pageLink.setStartTs(0L);
        pageLink.setEndTs(System.currentTimeMillis());
        pageLink.setSearchPropagatedAlarms(true);
        pageLink.setSeverityList(List.of(AlarmSeverity.CRITICAL, AlarmSeverity.WARNING));
        pageLink.setStatusList(List.of(AlarmSearchStatus.ACTIVE));

        PageData<AlarmData> tenantAlarms = alarmService.findAlarmDataByQueryForEntities(tenantId, toQuery(pageLink), List.of(tenantDevice.getId(), customerDevice.getId()));
        Assert.assertEquals(2, tenantAlarms.getData().size());

        PageData<AlarmData> customerAlarms = alarmService.findAlarmDataByQueryForEntities(tenantId, toQuery(pageLink), Collections.singletonList(customerDevice.getId()));
        Assert.assertEquals(1, customerAlarms.getData().size());
        Assert.assertEquals(deviceAlarm, new AlarmInfo(customerAlarms.getData().get(0)));

        PageData<AlarmInfo> alarms = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(tenantDevice.getId())
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(10, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build());
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(tenantAlarm, new AlarmInfo(alarms.getData().get(0)));
    }

    @Test
    public void testFindPropagatedCustomerAssetAlarm() {
        Customer customer = new Customer();
        customer.setTitle("TestCustomer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);

        Device tenantDevice = new Device();
        tenantDevice.setName("TestTenantDevice");
        tenantDevice.setType("default");
        tenantDevice.setTenantId(tenantId);
        tenantDevice = deviceService.saveDevice(tenantDevice);

        Asset customerAsset = new Asset();
        customerAsset.setName("TestCustomerDevice");
        customerAsset.setType("default");
        customerAsset.setTenantId(tenantId);
        customerAsset.setCustomerId(customer.getId());
        customerAsset = assetService.saveAsset(customerAsset);

        EntityRelation relation = new EntityRelation();
        relation.setFrom(customerAsset.getId());
        relation.setTo(tenantDevice.getId());
        relation.setAdditionalInfo(JacksonUtil.newObjectNode());
        relation.setType("Contains");
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        relationService.saveRelation(tenantId, relation);

        long ts = System.currentTimeMillis();
        alarmService.createAlarm(AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tenantId)
                .originator(tenantDevice.getId())
                .type("Not Propagated")
                .severity(AlarmSeverity.CRITICAL)
                .startTs(ts).build());

        AlarmApiCallResult result = alarmService.createAlarm(AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tenantId)
                .originator(tenantDevice.getId())
                .type("Propagated")
                .severity(AlarmSeverity.CRITICAL)
                .propagation(AlarmPropagationInfo.builder().propagate(true).build())
                .startTs(ts).build());
        AlarmInfo customerAlarm = result.getAlarm();

        AlarmDataPageLink pageLink = new AlarmDataPageLink();
        pageLink.setPage(0);
        pageLink.setPageSize(10);
        pageLink.setSortOrder(new EntityDataSortOrder(new EntityKey(EntityKeyType.ALARM_FIELD, "createdTime")));

        pageLink.setStartTs(0L);
        pageLink.setEndTs(System.currentTimeMillis());
        pageLink.setSearchPropagatedAlarms(true);
        pageLink.setSeverityList(List.of(AlarmSeverity.CRITICAL, AlarmSeverity.WARNING));
        pageLink.setStatusList(List.of(AlarmSearchStatus.ACTIVE));

        //TEST that propagated alarms are visible on the asset level.
        PageData<AlarmData> customerAlarms = alarmService.findAlarmDataByQueryForEntities(tenantId, toQuery(pageLink), Collections.singletonList(customerAsset.getId()));
        Assert.assertEquals(1, customerAlarms.getData().size());
        Assert.assertEquals(customerAlarm, new AlarmInfo(customerAlarms.getData().get(0)));
    }

    @Test
    public void testFindPropagatedToOwnerAndTenantAlarm() {
        Customer customer = new Customer();
        customer.setTitle("TestCustomer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);

        Device device = new Device();
        device.setName("TestTenantDevice");
        device.setType("default");
        device.setTenantId(tenantId);
        device.setCustomerId(customer.getId());
        device = deviceService.saveDevice(device);

        long ts = System.currentTimeMillis();

        AlarmApiCallResult result = alarmService.createAlarm(AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tenantId)
                .originator(device.getId())
                .type("Propagated To Tenant")
                .severity(AlarmSeverity.CRITICAL)
                .propagation(AlarmPropagationInfo.builder().propagateToTenant(true).build())
                .startTs(ts).build());
        AlarmInfo tenantAlarm = result.getAlarm();

        result = alarmService.createAlarm(AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tenantId)
                .originator(device.getId())
                .type("Propagated to Customer")
                .severity(AlarmSeverity.CRITICAL)
                .propagation(AlarmPropagationInfo.builder().propagateToOwner(true).build())
                .startTs(ts).build());
        AlarmInfo customerAlarm = result.getAlarm();

        AlarmDataPageLink pageLink = new AlarmDataPageLink();
        pageLink.setPage(0);
        pageLink.setPageSize(10);
        pageLink.setSortOrder(new EntityDataSortOrder(new EntityKey(EntityKeyType.ALARM_FIELD, "createdTime")));

        pageLink.setStartTs(0L);
        pageLink.setEndTs(System.currentTimeMillis());
        pageLink.setSearchPropagatedAlarms(true);
        pageLink.setSeverityList(List.of(AlarmSeverity.CRITICAL, AlarmSeverity.WARNING));
        pageLink.setStatusList(Collections.singletonList(AlarmSearchStatus.ACTIVE));

        //TEST that propagated alarms are visible on the asset level.
        PageData<AlarmData> tenantAlarms = alarmService.findAlarmDataByQueryForEntities(tenantId, toQuery(pageLink), Collections.singletonList(tenantId));
        Assert.assertEquals(1, tenantAlarms.getData().size());
        Assert.assertEquals(tenantAlarm, new AlarmInfo(tenantAlarms.getData().get(0)));

        //TEST that propagated alarms are visible on the asset level.
        PageData<AlarmData> customerAlarms = alarmService.findAlarmDataByQueryForEntities(tenantId, toQuery(pageLink), Collections.singletonList(customer.getId()));
        Assert.assertEquals(1, customerAlarms.getData().size());
        Assert.assertEquals(customerAlarm, new AlarmInfo(customerAlarms.getData().get(0)));
    }

    private AlarmDataQuery toQuery(AlarmDataPageLink pageLink) {
        return toQuery(pageLink, Collections.emptyList());
    }

    private AlarmDataQuery toQuery(AlarmDataPageLink pageLink, List<EntityKey> alarmFields) {
        return new AlarmDataQuery(new DeviceTypeFilter(), pageLink, null, null, null, alarmFields);
    }

    @Test
    public void testFindHighestAlarmSeverity() {
        Customer customer = new Customer();
        customer.setTitle("TestCustomer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);

        Device customerDevice = new Device();
        customerDevice.setName("TestCustomerDevice");
        customerDevice.setType("default");
        customerDevice.setTenantId(tenantId);
        customerDevice.setCustomerId(customer.getId());
        customerDevice = deviceService.saveDevice(customerDevice);

        // no one alarms was created
        Assert.assertNull(alarmService.findHighestAlarmSeverity(tenantId, customerDevice.getId(), null, null, null));

        AlarmApiCallResult result = alarmService.createAlarm(AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tenantId)
                .originator(customerDevice.getId())
                .type(TEST_ALARM)
                .severity(AlarmSeverity.MAJOR)
                .startTs(System.currentTimeMillis()).build());
        AlarmInfo alarm1 = result.getAlarm();
        alarmService.clearAlarm(tenantId, alarm1.getId(), System.currentTimeMillis(), null);

        result = alarmService.createAlarm(AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tenantId)
                .originator(customerDevice.getId())
                .type(TEST_ALARM)
                .severity(AlarmSeverity.MINOR)
                .startTs(System.currentTimeMillis()).build());
        AlarmInfo alarm2 = result.getAlarm();
        alarmService.acknowledgeAlarm(tenantId, alarm2.getId(), System.currentTimeMillis());
        alarmService.clearAlarm(tenantId, alarm2.getId(), System.currentTimeMillis(), null);

        result = alarmService.createAlarm(AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tenantId)
                .originator(customerDevice.getId())
                .type(TEST_ALARM)
                .severity(AlarmSeverity.CRITICAL)
                .startTs(System.currentTimeMillis()).build());
        AlarmInfo alarm3 = result.getAlarm();
        alarmService.acknowledgeAlarm(tenantId, alarm3.getId(), System.currentTimeMillis());

        Assert.assertEquals(AlarmSeverity.MAJOR, alarmService.findHighestAlarmSeverity(tenantId, customerDevice.getId(), AlarmSearchStatus.UNACK, null, null));
        Assert.assertEquals(AlarmSeverity.CRITICAL, alarmService.findHighestAlarmSeverity(tenantId, customerDevice.getId(), null, null, null));
        Assert.assertEquals(AlarmSeverity.MAJOR, alarmService.findHighestAlarmSeverity(tenantId, customerDevice.getId(), null, AlarmStatus.CLEARED_UNACK, null));
        Assert.assertEquals(AlarmSeverity.CRITICAL, alarmService.findHighestAlarmSeverity(tenantId, customerDevice.getId(), AlarmSearchStatus.ACTIVE, null, null));
        Assert.assertEquals(AlarmSeverity.MINOR, alarmService.findHighestAlarmSeverity(tenantId, customerDevice.getId(), null, AlarmStatus.CLEARED_ACK, null));
    }

    @Test
    public void testFindAlarmUsingAlarmDataQuery() throws ExecutionException, InterruptedException {
        AssetId parentId = new AssetId(Uuids.timeBased());
        AssetId parentId2 = new AssetId(Uuids.timeBased());
        AssetId childId = new AssetId(Uuids.timeBased());

        EntityRelation relation = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);
        EntityRelation relation2 = new EntityRelation(parentId2, childId, EntityRelation.CONTAINS_TYPE);

        Assert.assertTrue(relationService.saveRelationAsync(tenantId, relation).get());
        Assert.assertTrue(relationService.saveRelationAsync(tenantId, relation2).get());

        long ts = System.currentTimeMillis();
        AlarmApiCallResult result = alarmService.createAlarm(AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tenantId)
                .originator(childId)
                .type(TEST_ALARM)
                .severity(AlarmSeverity.CRITICAL)
                .startTs(ts).build());
        AlarmInfo created = result.getAlarm();

        AlarmDataPageLink pageLink = new AlarmDataPageLink();
        pageLink.setPage(0);
        pageLink.setPageSize(10);
        pageLink.setSortOrder(new EntityDataSortOrder(new EntityKey(EntityKeyType.ALARM_FIELD, "createdTime")));

        pageLink.setStartTs(0L);
        pageLink.setEndTs(System.currentTimeMillis());
        pageLink.setSearchPropagatedAlarms(false);
        pageLink.setSeverityList(List.of(AlarmSeverity.CRITICAL, AlarmSeverity.WARNING));
        pageLink.setStatusList(List.of(AlarmSearchStatus.ACTIVE));

        PageData<AlarmData> alarms = alarmService.findAlarmDataByQueryForEntities(tenantId, toQuery(pageLink), Collections.singletonList(childId));

        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, new AlarmInfo(alarms.getData().get(0)));

        pageLink.setPage(0);
        pageLink.setPageSize(10);
        pageLink.setSortOrder(new EntityDataSortOrder(new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime")));

        pageLink.setStartTs(0L);
        pageLink.setEndTs(System.currentTimeMillis());
        pageLink.setSearchPropagatedAlarms(false);
        pageLink.setSeverityList(List.of(AlarmSeverity.CRITICAL, AlarmSeverity.WARNING));
        pageLink.setStatusList(List.of(AlarmSearchStatus.ACTIVE));

        alarms = alarmService.findAlarmDataByQueryForEntities(tenantId, toQuery(pageLink), Collections.singletonList(childId));
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, new AlarmInfo(alarms.getData().get(0)));

        pageLink.setSearchPropagatedAlarms(true);
        alarms = alarmService.findAlarmDataByQueryForEntities(tenantId, toQuery(pageLink), Collections.singletonList(childId));
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, new AlarmInfo(alarms.getData().get(0)));

        // Check child relation
        created.setPropagate(true);
        result = alarmService.updateAlarm(AlarmUpdateRequest.fromAlarm(created));
        created = result.getAlarm();

        // Check child relation
        pageLink.setPage(0);
        pageLink.setPageSize(10);
        pageLink.setSortOrder(new EntityDataSortOrder(new EntityKey(EntityKeyType.ALARM_FIELD, "createdTime")));

        pageLink.setStartTs(0L);
        pageLink.setEndTs(System.currentTimeMillis());
        pageLink.setSearchPropagatedAlarms(true);
        pageLink.setSeverityList(List.of(AlarmSeverity.CRITICAL, AlarmSeverity.WARNING));
        pageLink.setStatusList(List.of(AlarmSearchStatus.ACTIVE));

        alarms = alarmService.findAlarmDataByQueryForEntities(tenantId, toQuery(pageLink), Collections.singletonList(childId));
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, new AlarmInfo(alarms.getData().get(0)));

        // Check parent relation
        pageLink.setPage(0);
        pageLink.setPageSize(10);
        pageLink.setSortOrder(new EntityDataSortOrder(new EntityKey(EntityKeyType.ALARM_FIELD, "createdTime")));

        pageLink.setStartTs(0L);
        pageLink.setEndTs(System.currentTimeMillis());
        pageLink.setSearchPropagatedAlarms(true);
        pageLink.setSeverityList(List.of(AlarmSeverity.CRITICAL, AlarmSeverity.WARNING));
        pageLink.setStatusList(List.of(AlarmSearchStatus.ACTIVE));

        alarms = alarmService.findAlarmDataByQueryForEntities(tenantId, toQuery(pageLink), Collections.singletonList(parentId));
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, new AlarmInfo(alarms.getData().get(0)));

        PageData<AlarmInfo> alarmsInfoData = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(childId)
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(10, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build());
        Assert.assertNotNull(alarmsInfoData.getData());
        Assert.assertEquals(1, alarmsInfoData.getData().size());
        Assert.assertEquals(created, new AlarmInfo(alarmsInfoData.getData().get(0)));

        alarmsInfoData = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(parentId)
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(10, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build());
        Assert.assertNotNull(alarmsInfoData.getData());
        Assert.assertEquals(1, alarmsInfoData.getData().size());
        Assert.assertEquals(created, new AlarmInfo(alarmsInfoData.getData().get(0)));

        alarmsInfoData = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(parentId2)
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(10, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build());
        Assert.assertNotNull(alarmsInfoData.getData());
        Assert.assertEquals(1, alarmsInfoData.getData().size());
        Assert.assertEquals(created, new AlarmInfo(alarmsInfoData.getData().get(0)));

        pageLink.setPage(0);
        pageLink.setPageSize(10);
        pageLink.setSortOrder(new EntityDataSortOrder(new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime")));

        pageLink.setStartTs(0L);
        pageLink.setEndTs(System.currentTimeMillis());
        pageLink.setSearchPropagatedAlarms(true);
        pageLink.setSeverityList(List.of(AlarmSeverity.CRITICAL, AlarmSeverity.WARNING));
        pageLink.setStatusList(List.of(AlarmSearchStatus.ACTIVE));

        alarms = alarmService.findAlarmDataByQueryForEntities(tenantId, toQuery(pageLink), Collections.singletonList(parentId));
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, new AlarmInfo(alarms.getData().get(0)));

        created = alarmService.acknowledgeAlarm(tenantId, created.getId(), System.currentTimeMillis()).getAlarm();

        pageLink.setPage(0);
        pageLink.setPageSize(10);
        pageLink.setSortOrder(new EntityDataSortOrder(new EntityKey(EntityKeyType.ALARM_FIELD, "createdTime")));

        pageLink.setStartTs(0L);
        pageLink.setEndTs(System.currentTimeMillis());
        pageLink.setSearchPropagatedAlarms(true);
        pageLink.setSeverityList(List.of(AlarmSeverity.CRITICAL, AlarmSeverity.WARNING));
        pageLink.setStatusList(List.of(AlarmSearchStatus.ACTIVE));

        alarms = alarmService.findAlarmDataByQueryForEntities(tenantId, toQuery(pageLink), Collections.singletonList(childId));
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, new AlarmInfo(alarms.getData().get(0)));
    }

    @Test
    public void testCountAlarmsUsingAlarmDataQuery() {
        AssetId childId = new AssetId(Uuids.timeBased());

        long ts = System.currentTimeMillis();
        AlarmApiCallResult result = alarmService.createAlarm(AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tenantId)
                .originator(childId)
                .type(TEST_ALARM)
                .severity(AlarmSeverity.CRITICAL)
                .startTs(ts).build());
        AlarmInfo created = result.getAlarm();

        AlarmCountQuery countQuery = AlarmCountQuery.builder()
                .startTs(0L)
                .endTs(System.currentTimeMillis())
                .searchPropagatedAlarms(false)
                .severityList(List.of(AlarmSeverity.CRITICAL, AlarmSeverity.WARNING))
                .statusList(List.of(AlarmSearchStatus.ACTIVE))
                .build();

        long alarmsCount = alarmService.countAlarmsByQuery(tenantId, null, countQuery);

        Assert.assertEquals(1, alarmsCount);

        countQuery = AlarmCountQuery.builder()
                .startTs(0L)
                .endTs(System.currentTimeMillis())
                .searchPropagatedAlarms(true)
                .severityList(List.of(AlarmSeverity.CRITICAL, AlarmSeverity.WARNING))
                .statusList(List.of(AlarmSearchStatus.ACTIVE))
                .build();

        alarmsCount = alarmService.countAlarmsByQuery(tenantId, null, countQuery);

        Assert.assertEquals(1, alarmsCount);

        created = alarmService.acknowledgeAlarm(tenantId, created.getId(), System.currentTimeMillis()).getAlarm();

        alarmsCount = alarmService.countAlarmsByQuery(tenantId, null, countQuery);

        Assert.assertEquals(1, alarmsCount);

        alarmService.clearAlarm(tenantId, created.getId(), System.currentTimeMillis(), null);
        created = alarmService.findAlarmInfoById(tenantId, created.getId());

        alarmsCount = alarmService.countAlarmsByQuery(tenantId, null, countQuery);

        Assert.assertEquals(0, alarmsCount);

        countQuery = AlarmCountQuery.builder()
                .startTs(0L)
                .endTs(System.currentTimeMillis())
                .searchPropagatedAlarms(true)
                .severityList(List.of(AlarmSeverity.CRITICAL, AlarmSeverity.WARNING))
                .statusList(List.of(AlarmSearchStatus.ACTIVE, AlarmSearchStatus.CLEARED))
                .build();

        alarmsCount = alarmService.countAlarmsByQuery(tenantId, null, countQuery);

        Assert.assertEquals(1, alarmsCount);
    }

    @Test
    public void testDeleteAlarm() throws ExecutionException, InterruptedException {
        AssetId parentId = new AssetId(Uuids.timeBased());
        AssetId childId = new AssetId(Uuids.timeBased());

        EntityRelation relation = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);

        Assert.assertNotNull(relationService.saveRelation(tenantId, relation));

        long ts = System.currentTimeMillis();
        AlarmApiCallResult result = alarmService.createAlarm(AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tenantId)
                .originator(childId)
                .type(TEST_ALARM)
                .severity(AlarmSeverity.CRITICAL)
                .propagation(AlarmPropagationInfo.builder().propagate(true).build())
                .startTs(ts).build());
        AlarmInfo created = result.getAlarm();

        PageData<AlarmInfo> alarms = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(childId)
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build());
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, new AlarmInfo(alarms.getData().get(0)));

        // Check parent relation
        alarms = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(parentId)
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build());
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, new AlarmInfo(alarms.getData().get(0)));

        Assert.assertTrue("Alarm was not deleted when expected", alarmService.delAlarm(tenantId, created.getId()).isSuccessful());

        Alarm fetched = alarmService.findAlarmByIdAsync(tenantId, created.getId()).get();

        Assert.assertNull("Alarm was returned when it was expected to be null", fetched);

        alarms = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(childId)
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build());
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(0, alarms.getData().size());

        // Check parent relation
        alarms = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(parentId)
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build());
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(0, alarms.getData().size());
    }

    @Test
    public void testCountAlarmsForEntities() throws ExecutionException, InterruptedException {
        AssetId parentId = new AssetId(Uuids.timeBased());
        AssetId childId = new AssetId(Uuids.timeBased());

        EntityRelation relation = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);

        Assert.assertTrue(relationService.saveRelationAsync(tenantId, relation).get());

        long ts = System.currentTimeMillis();
        AlarmApiCallResult result = alarmService.createAlarm(AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tenantId)
                .originator(childId)
                .type(TEST_ALARM)
                .severity(AlarmSeverity.CRITICAL)
                .startTs(ts).build());
        AlarmInfo created = result.getAlarm();
        created.setPropagate(true);
        result = alarmService.updateAlarm(AlarmUpdateRequest.fromAlarm(created));
        created = result.getAlarm();

        EntityListFilter entityListFilter = new EntityListFilter();
        entityListFilter.setEntityList(List.of(childId.getId().toString(), parentId.getId().toString()));
        entityListFilter.setEntityType(EntityType.ASSET);
        AlarmCountQuery countQuery = new AlarmCountQuery(entityListFilter);
        countQuery.setStartTs(0L);
        countQuery.setEndTs(System.currentTimeMillis());

        long alarmsCount = alarmService.countAlarmsByQuery(tenantId, null, countQuery, List.of(childId));
        Assert.assertEquals(1, alarmsCount);

        countQuery.setSearchPropagatedAlarms(true);

        alarmsCount = alarmService.countAlarmsByQuery(tenantId, null, countQuery, List.of(parentId));
        Assert.assertEquals(1, alarmsCount);

        created = alarmService.acknowledgeAlarm(tenantId, created.getId(), System.currentTimeMillis()).getAlarm();

        countQuery.setStatusList(List.of(AlarmSearchStatus.UNACK));
        alarmsCount = alarmService.countAlarmsByQuery(tenantId, null, countQuery, List.of(childId));
        Assert.assertEquals(0, alarmsCount);

        alarmService.clearAlarm(tenantId, created.getId(), System.currentTimeMillis(), null);

        countQuery.setStatusList(List.of(AlarmSearchStatus.CLEARED));
        alarmsCount = alarmService.countAlarmsByQuery(tenantId, null, countQuery, List.of(childId));
        Assert.assertEquals(1, alarmsCount);
    }

}
