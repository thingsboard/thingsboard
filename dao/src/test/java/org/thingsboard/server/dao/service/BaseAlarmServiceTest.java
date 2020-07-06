/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataPageLink;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.alarm.AlarmOperationResult;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

public abstract class BaseAlarmServiceTest extends AbstractServiceTest {

    public static final String TEST_ALARM = "TEST_ALARM";
    private TenantId tenantId;

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
    public void testSaveAndFetchAlarm() throws ExecutionException, InterruptedException {
        AssetId parentId = new AssetId(Uuids.timeBased());
        AssetId childId = new AssetId(Uuids.timeBased());

        EntityRelation relation = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);

        Assert.assertTrue(relationService.saveRelationAsync(tenantId, relation).get());

        long ts = System.currentTimeMillis();
        Alarm alarm = Alarm.builder().tenantId(tenantId).originator(childId)
                .type(TEST_ALARM)
                .severity(AlarmSeverity.CRITICAL).status(AlarmStatus.ACTIVE_UNACK)
                .startTs(ts).build();

        AlarmOperationResult result = alarmService.createOrUpdateAlarm(alarm);
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

        Alarm fetched = alarmService.findAlarmByIdAsync(tenantId, created.getId()).get();
        Assert.assertEquals(created, fetched);
    }

    @Test
    public void testFindAlarm() throws ExecutionException, InterruptedException {
        AssetId parentId = new AssetId(Uuids.timeBased());
        AssetId childId = new AssetId(Uuids.timeBased());

        EntityRelation relation = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);

        Assert.assertTrue(relationService.saveRelationAsync(tenantId, relation).get());

        long ts = System.currentTimeMillis();
        Alarm alarm = Alarm.builder().tenantId(tenantId).originator(childId)
                .type(TEST_ALARM)
                .propagate(false)
                .severity(AlarmSeverity.CRITICAL).status(AlarmStatus.ACTIVE_UNACK)
                .startTs(ts).build();

        AlarmOperationResult result = alarmService.createOrUpdateAlarm(alarm);
        Alarm created = result.getAlarm();

        // Check child relation
        PageData<AlarmInfo> alarms = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(childId)
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build()).get();
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, alarms.getData().get(0));

        // Check parent relation
        alarms = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(parentId)
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build()).get();
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(0, alarms.getData().size());

        created.setPropagate(true);
        result = alarmService.createOrUpdateAlarm(created);
        created = result.getAlarm();

        // Check child relation
        alarms = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(childId)
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build()).get();
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, alarms.getData().get(0));

        // Check parent relation
        alarms = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(parentId)
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build()).get();
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, alarms.getData().get(0));

        alarmService.ackAlarm(tenantId, created.getId(), System.currentTimeMillis()).get();
        created = alarmService.findAlarmByIdAsync(tenantId, created.getId()).get();

        alarms = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(childId)
                .status(AlarmStatus.ACTIVE_ACK).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build()).get();
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, alarms.getData().get(0));

        // Check not existing relation
        alarms = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(childId)
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build()).get();
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(0, alarms.getData().size());

        alarmService.clearAlarm(tenantId, created.getId(), null, System.currentTimeMillis()).get();
        created = alarmService.findAlarmByIdAsync(tenantId, created.getId()).get();

        alarms = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(childId)
                .status(AlarmStatus.CLEARED_ACK).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build()).get();
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, alarms.getData().get(0));
    }

    @Test
    public void testFindCustomerAlarm() throws ExecutionException, InterruptedException {
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
        Alarm tenantAlarm = Alarm.builder().tenantId(tenantId)
                .originator(tenantDevice.getId())
                .type(TEST_ALARM)
                .propagate(true)
                .severity(AlarmSeverity.CRITICAL).status(AlarmStatus.ACTIVE_UNACK)
                .startTs(ts).build();
        AlarmOperationResult result = alarmService.createOrUpdateAlarm(tenantAlarm);
        tenantAlarm = result.getAlarm();

        Alarm deviceAlarm = Alarm.builder().tenantId(tenantId)
                .originator(customerDevice.getId())
                .type(TEST_ALARM)
                .propagate(true)
                .severity(AlarmSeverity.CRITICAL).status(AlarmStatus.ACTIVE_UNACK)
                .startTs(ts).build();
        result = alarmService.createOrUpdateAlarm(deviceAlarm);
        deviceAlarm = result.getAlarm();

        AlarmDataPageLink pageLink = new AlarmDataPageLink();
        pageLink.setPage(0);
        pageLink.setPageSize(10);
        pageLink.setSortOrder(new EntityDataSortOrder(new EntityKey(EntityKeyType.ALARM_FIELD, "createdTime")));

        pageLink.setStartTs(0L);
        pageLink.setEndTs(System.currentTimeMillis());
        pageLink.setSearchPropagatedAlarms(true);
        pageLink.setSeverityList(Arrays.asList(AlarmSeverity.CRITICAL, AlarmSeverity.WARNING));
        pageLink.setStatusList(Arrays.asList(AlarmSearchStatus.ACTIVE));

        PageData<AlarmData> tenantAlarms = alarmService.findAlarmDataByQueryForEntities(tenantId, new CustomerId(CustomerId.NULL_UUID), pageLink, Arrays.asList(tenantDevice.getId(), customerDevice.getId()));
        Assert.assertEquals(2, tenantAlarms.getData().size());

        PageData<AlarmData> customerAlarms = alarmService.findAlarmDataByQueryForEntities(tenantId, customer.getId(), pageLink, Arrays.asList(tenantDevice.getId(), customerDevice.getId()));
        Assert.assertEquals(1, customerAlarms.getData().size());
        Assert.assertEquals(deviceAlarm, customerAlarms.getData().get(0));

    }

    @Test
    public void testFindAlarmUsingAlarmDataQuery() throws ExecutionException, InterruptedException {
        AssetId parentId = new AssetId(Uuids.timeBased());
        AssetId childId = new AssetId(Uuids.timeBased());

        EntityRelation relation = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);

        Assert.assertTrue(relationService.saveRelationAsync(tenantId, relation).get());

        long ts = System.currentTimeMillis();
        Alarm alarm = Alarm.builder().tenantId(tenantId).originator(childId)
                .type(TEST_ALARM)
                .propagate(false)
                .severity(AlarmSeverity.CRITICAL)
                .status(AlarmStatus.ACTIVE_UNACK)
                .startTs(ts).build();

        AlarmOperationResult result = alarmService.createOrUpdateAlarm(alarm);
        Alarm created = result.getAlarm();

        AlarmDataPageLink pageLink = new AlarmDataPageLink();
        pageLink.setPage(0);
        pageLink.setPageSize(1);
        pageLink.setSortOrder(new EntityDataSortOrder(new EntityKey(EntityKeyType.ALARM_FIELD, "createdTime")));

        pageLink.setStartTs(0L);
        pageLink.setEndTs(System.currentTimeMillis());
        pageLink.setSearchPropagatedAlarms(false);
        pageLink.setSeverityList(Arrays.asList(AlarmSeverity.CRITICAL, AlarmSeverity.WARNING));
        pageLink.setStatusList(Arrays.asList(AlarmSearchStatus.ACTIVE));

        PageData<AlarmData> alarms = alarmService.findAlarmDataByQueryForEntities(tenantId, new CustomerId(CustomerId.NULL_UUID), pageLink, Collections.singletonList(childId));

        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, alarms.getData().get(0));

        pageLink.setPage(0);
        pageLink.setPageSize(1);
        pageLink.setSortOrder(new EntityDataSortOrder(new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime")));

        pageLink.setStartTs(0L);
        pageLink.setEndTs(System.currentTimeMillis());
        pageLink.setSearchPropagatedAlarms(false);
        pageLink.setSeverityList(Arrays.asList(AlarmSeverity.CRITICAL, AlarmSeverity.WARNING));
        pageLink.setStatusList(Arrays.asList(AlarmSearchStatus.ACTIVE));

        alarms = alarmService.findAlarmDataByQueryForEntities(tenantId, new CustomerId(CustomerId.NULL_UUID), pageLink, Collections.singletonList(childId));
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, alarms.getData().get(0));

        // Check child relation
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, new Alarm(alarms.getData().get(0)));

        created.setPropagate(true);
        result = alarmService.createOrUpdateAlarm(created);
        created = result.getAlarm();

        // Check child relation
        pageLink.setPage(0);
        pageLink.setPageSize(1);
        pageLink.setSortOrder(new EntityDataSortOrder(new EntityKey(EntityKeyType.ALARM_FIELD, "createdTime")));

        pageLink.setStartTs(0L);
        pageLink.setEndTs(System.currentTimeMillis());
        pageLink.setSearchPropagatedAlarms(true);
        pageLink.setSeverityList(Arrays.asList(AlarmSeverity.CRITICAL, AlarmSeverity.WARNING));
        pageLink.setStatusList(Arrays.asList(AlarmSearchStatus.ACTIVE));

        alarms = alarmService.findAlarmDataByQueryForEntities(tenantId, new CustomerId(CustomerId.NULL_UUID), pageLink, Collections.singletonList(childId));

        // Check parent relation
        pageLink.setPage(0);
        pageLink.setPageSize(1);
        pageLink.setSortOrder(new EntityDataSortOrder(new EntityKey(EntityKeyType.ALARM_FIELD, "createdTime")));

        pageLink.setStartTs(0L);
        pageLink.setEndTs(System.currentTimeMillis());
        pageLink.setSearchPropagatedAlarms(true);
        pageLink.setSeverityList(Arrays.asList(AlarmSeverity.CRITICAL, AlarmSeverity.WARNING));
        pageLink.setStatusList(Arrays.asList(AlarmSearchStatus.ACTIVE));

        alarms = alarmService.findAlarmDataByQueryForEntities(tenantId, new CustomerId(CustomerId.NULL_UUID), pageLink, Collections.singletonList(parentId));
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, alarms.getData().get(0));

        pageLink.setPage(0);
        pageLink.setPageSize(1);
        pageLink.setSortOrder(new EntityDataSortOrder(new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime")));

        pageLink.setStartTs(0L);
        pageLink.setEndTs(System.currentTimeMillis());
        pageLink.setSearchPropagatedAlarms(true);
        pageLink.setSeverityList(Arrays.asList(AlarmSeverity.CRITICAL, AlarmSeverity.WARNING));
        pageLink.setStatusList(Arrays.asList(AlarmSearchStatus.ACTIVE));

        alarms = alarmService.findAlarmDataByQueryForEntities(tenantId, new CustomerId(CustomerId.NULL_UUID), pageLink, Collections.singletonList(parentId));
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, alarms.getData().get(0));

        alarmService.ackAlarm(tenantId, created.getId(), System.currentTimeMillis()).get();
        created = alarmService.findAlarmByIdAsync(tenantId, created.getId()).get();

        pageLink.setPage(0);
        pageLink.setPageSize(1);
        pageLink.setSortOrder(new EntityDataSortOrder(new EntityKey(EntityKeyType.ALARM_FIELD, "createdTime")));

        pageLink.setStartTs(0L);
        pageLink.setEndTs(System.currentTimeMillis());
        pageLink.setSearchPropagatedAlarms(true);
        pageLink.setSeverityList(Arrays.asList(AlarmSeverity.CRITICAL, AlarmSeverity.WARNING));
        pageLink.setStatusList(Arrays.asList(AlarmSearchStatus.ACTIVE));

        alarms = alarmService.findAlarmDataByQueryForEntities(tenantId, new CustomerId(CustomerId.NULL_UUID), pageLink, Collections.singletonList(childId));
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, alarms.getData().get(0));
    }

    @Test
    public void testDeleteAlarm() throws ExecutionException, InterruptedException {
        AssetId parentId = new AssetId(Uuids.timeBased());
        AssetId childId = new AssetId(Uuids.timeBased());

        EntityRelation relation = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);

        Assert.assertTrue(relationService.saveRelationAsync(tenantId, relation).get());

        long ts = System.currentTimeMillis();
        Alarm alarm = Alarm.builder().tenantId(tenantId).originator(childId)
                .type(TEST_ALARM)
                .propagate(true)
                .severity(AlarmSeverity.CRITICAL).status(AlarmStatus.ACTIVE_UNACK)
                .startTs(ts).build();

        AlarmOperationResult result = alarmService.createOrUpdateAlarm(alarm);
        Alarm created = result.getAlarm();

        PageData<AlarmInfo> alarms = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(childId)
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build()).get();
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, alarms.getData().get(0));

        // Check parent relation
        alarms = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(parentId)
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build()).get();
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, alarms.getData().get(0));

        List<EntityRelation> toAlarmRelations = relationService.findByTo(tenantId, created.getId(), RelationTypeGroup.ALARM);
        Assert.assertEquals(2, toAlarmRelations.size());

        List<EntityRelation> fromChildRelations = relationService.findByFrom(tenantId, childId, RelationTypeGroup.ALARM);
        Assert.assertEquals(1, fromChildRelations.size());

        List<EntityRelation> fromParentRelations = relationService.findByFrom(tenantId, parentId, RelationTypeGroup.ALARM);
        Assert.assertEquals(1, fromParentRelations.size());


        Assert.assertTrue("Alarm was not deleted when expected", alarmService.deleteAlarm(tenantId, created.getId()).isSuccessful());

        Alarm fetched = alarmService.findAlarmByIdAsync(tenantId, created.getId()).get();

        Assert.assertNull("Alarm was returned when it was expected to be null", fetched);

        alarms = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(childId)
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build()).get();
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(0, alarms.getData().size());

        // Check parent relation
        alarms = alarmService.findAlarms(tenantId, AlarmQuery.builder()
                .affectedEntityId(parentId)
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(1, 0, "",
                                new SortOrder("createdTime", SortOrder.Direction.DESC), 0L, System.currentTimeMillis())
                ).build()).get();
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(0, alarms.getData().size());

        toAlarmRelations = relationService.findByTo(tenantId, created.getId(), RelationTypeGroup.ALARM);
        Assert.assertEquals(0, toAlarmRelations.size());

        fromChildRelations = relationService.findByFrom(tenantId, childId, RelationTypeGroup.ALARM);
        Assert.assertEquals(0, fromChildRelations.size());

        fromParentRelations = relationService.findByFrom(tenantId, childId, RelationTypeGroup.ALARM);
        Assert.assertEquals(0, fromParentRelations.size());

    }
}
