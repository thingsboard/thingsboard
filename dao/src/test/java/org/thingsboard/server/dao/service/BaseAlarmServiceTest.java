/**
 * Copyright © 2016-2018 The Thingsboard Authors
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

import com.datastax.driver.core.utils.UUIDs;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;

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
        AssetId parentId = new AssetId(UUIDs.timeBased());
        AssetId childId = new AssetId(UUIDs.timeBased());

        EntityRelation relation = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);

        Assert.assertTrue(relationService.saveRelationAsync(relation).get());

        long ts = System.currentTimeMillis();
        Alarm alarm = Alarm.builder().tenantId(tenantId).originator(childId)
                .type(TEST_ALARM)
                .severity(AlarmSeverity.CRITICAL).status(AlarmStatus.ACTIVE_UNACK)
                .startTs(ts).build();

        Alarm created = alarmService.createOrUpdateAlarm(alarm);

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

        Alarm fetched = alarmService.findAlarmByIdAsync(created.getId()).get();
        Assert.assertEquals(created, fetched);
    }

    @Test
    public void testFindAlarm() throws ExecutionException, InterruptedException {
        AssetId parentId = new AssetId(UUIDs.timeBased());
        AssetId childId = new AssetId(UUIDs.timeBased());

        EntityRelation relation = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);

        Assert.assertTrue(relationService.saveRelationAsync(relation).get());

        long ts = System.currentTimeMillis();
        Alarm alarm = Alarm.builder().tenantId(tenantId).originator(childId)
                .type(TEST_ALARM)
                .propagate(false)
                .severity(AlarmSeverity.CRITICAL).status(AlarmStatus.ACTIVE_UNACK)
                .startTs(ts).build();

        Alarm created = alarmService.createOrUpdateAlarm(alarm);

        // Check child relation
        TimePageData<AlarmInfo> alarms = alarmService.findAlarms(AlarmQuery.builder()
                .affectedEntityId(childId)
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(1, 0L, System.currentTimeMillis(), false)
                ).build()).get();
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, alarms.getData().get(0));

        // Check parent relation
        alarms = alarmService.findAlarms(AlarmQuery.builder()
                .affectedEntityId(parentId)
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(1, 0L, System.currentTimeMillis(), false)
                ).build()).get();
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(0, alarms.getData().size());

        created.setPropagate(true);
        created = alarmService.createOrUpdateAlarm(created);

        // Check child relation
        alarms = alarmService.findAlarms(AlarmQuery.builder()
                .affectedEntityId(childId)
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(1, 0L, System.currentTimeMillis(), false)
                ).build()).get();
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, alarms.getData().get(0));

        // Check parent relation
        alarms = alarmService.findAlarms(AlarmQuery.builder()
                .affectedEntityId(parentId)
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(1, 0L, System.currentTimeMillis(), false)
                ).build()).get();
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, alarms.getData().get(0));

        alarmService.ackAlarm(created.getId(), System.currentTimeMillis()).get();
        created = alarmService.findAlarmByIdAsync(created.getId()).get();

        alarms = alarmService.findAlarms(AlarmQuery.builder()
                .affectedEntityId(childId)
                .status(AlarmStatus.ACTIVE_ACK).pageLink(
                        new TimePageLink(1, 0L, System.currentTimeMillis(), false)
                ).build()).get();
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, alarms.getData().get(0));

        // Check not existing relation
        alarms = alarmService.findAlarms(AlarmQuery.builder()
                .affectedEntityId(childId)
                .status(AlarmStatus.ACTIVE_UNACK).pageLink(
                        new TimePageLink(1, 0L, System.currentTimeMillis(), false)
                ).build()).get();
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(0, alarms.getData().size());

        alarmService.clearAlarm(created.getId(), null, System.currentTimeMillis()).get();
        created = alarmService.findAlarmByIdAsync(created.getId()).get();

        alarms = alarmService.findAlarms(AlarmQuery.builder()
                .affectedEntityId(childId)
                .status(AlarmStatus.CLEARED_ACK).pageLink(
                        new TimePageLink(1, 0L, System.currentTimeMillis(), false)
                ).build()).get();
        Assert.assertNotNull(alarms.getData());
        Assert.assertEquals(1, alarms.getData().size());
        Assert.assertEquals(created, alarms.getData().get(0));
    }
}
