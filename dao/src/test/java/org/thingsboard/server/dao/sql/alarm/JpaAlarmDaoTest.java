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
package org.thingsboard.server.dao.sql.alarm;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.alarm.AlarmCreateOrUpdateActiveRequest;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.alarm.AlarmDao;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.dao.tenant.TenantProfileDao;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by Valerii Sosliuk on 5/21/2017.
 */
@Slf4j
public class JpaAlarmDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private AlarmDao alarmDao;

    @Autowired
    protected TenantProfileDao tenantProfileDao;

    @Autowired
    protected TenantDao tenantDao;

    @Test
    public void testFindLatestByOriginatorAndType() throws ExecutionException, InterruptedException, TimeoutException {
        log.info("Current system time in millis = {}", System.currentTimeMillis());
        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        UUID originator1Id = UUID.fromString("d4b68f41-3e96-11e7-a884-898080180d6b");
        UUID originator2Id = UUID.fromString("d4b68f42-3e96-11e7-a884-898080180d6b");
        UUID alarm1Id = UUID.fromString("d4b68f43-3e96-11e7-a884-898080180d6b");
        UUID alarm2Id = UUID.fromString("d4b68f44-3e96-11e7-a884-898080180d6b");
        UUID alarm3Id = UUID.fromString("d4b68f45-3e96-11e7-a884-898080180d6b");
        // The find method does not filter by tenant. It is just using the tenantId for rate limits if any.
        var alarmsBeforeSave = alarmDao.find(tenantId).stream().filter(a -> a.getTenantId().equals(tenantId)).collect(Collectors.toList());
        int alarmCountBeforeSave = alarmsBeforeSave.size();
        saveAlarm(alarm1Id, tenantId.getId(), originator1Id, "TEST_ALARM");
        //The timestamp of the startTime should be different in order for test to always work
        Thread.sleep(1);
        saveAlarm(alarm2Id, tenantId.getId(), originator1Id, "TEST_ALARM");
        saveAlarm(alarm3Id, tenantId.getId(), originator2Id, "TEST_ALARM");
        var alarmsAfterSave = alarmDao.find(tenantId).stream().filter(a -> a.getTenantId().equals(tenantId)).collect(Collectors.toList());
        int alarmCountAfterSave = alarmsAfterSave.size();
        int diff = alarmCountAfterSave - alarmCountBeforeSave;
        if (diff != 3) {
            System.out.println("test");
        }
        assertEquals(3, diff);
        ListenableFuture<Alarm> future = alarmDao
                .findLatestByOriginatorAndTypeAsync(tenantId, new DeviceId(originator1Id), "TEST_ALARM");
        Alarm alarm = future.get(30, TimeUnit.SECONDS);
        assertNotNull(alarm);
        assertEquals(alarm2Id, alarm.getId().getId());
    }

    @Test
    public void createOrUpdateActiveAlarm() {
        Tenant tenant = createTenant();
        TenantId tenantId = tenant.getId();
        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        AlarmCreateOrUpdateActiveRequest request = AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tenantId)
                .originator(deviceId)
                .type("ALARM_TYPE")
                .severity(AlarmSeverity.MAJOR)
                .build();
        AlarmApiCallResult result = alarmDao.createOrUpdateActiveAlarm(request, true);
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertTrue(result.isCreated());
        assertTrue(result.isModified());
        assertNotNull(result.getAlarm());
        UUID newAlarmId = result.getAlarm().getUuidId();
        AlarmInfo afterSave = alarmDao.findAlarmInfoById(tenantId, newAlarmId);
        assertEquals(afterSave, result.getAlarm());

        request = AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tenantId)
                .originator(deviceId)
                .type("ALARM_TYPE")
                .severity(AlarmSeverity.CRITICAL)
                .build();
        result = alarmDao.createOrUpdateActiveAlarm(request, true);
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertFalse(result.isCreated());
        assertTrue(result.isModified());
        assertNotNull(result.getAlarm());
        assertEquals(newAlarmId, result.getAlarm().getUuidId());
        afterSave = alarmDao.findAlarmInfoById(tenantId, newAlarmId);
        assertEquals(afterSave, result.getAlarm());

        alarmDao.clearAlarm(tenantId, result.getAlarm().getId(), System.currentTimeMillis(), result.getAlarm().getDetails());

        request = AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tenantId)
                .originator(deviceId)
                .type("ALARM_TYPE")
                .severity(AlarmSeverity.CRITICAL)
                .build();
        result = alarmDao.createOrUpdateActiveAlarm(request, true);
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertTrue(result.isCreated());
        assertTrue(result.isModified());
        assertNotNull(result.getAlarm());
        assertNotEquals(newAlarmId, result.getAlarm().getUuidId());
        afterSave = alarmDao.findAlarmInfoById(tenantId, result.getAlarm().getUuidId());
        assertEquals(afterSave, result.getAlarm());

        alarmDao.clearAlarm(tenantId, result.getAlarm().getId(), System.currentTimeMillis(), result.getAlarm().getDetails());

        request = AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tenantId)
                .originator(deviceId)
                .type("ALARM_TYPE2")
                .severity(AlarmSeverity.CRITICAL)
                .build();
        result = alarmDao.createOrUpdateActiveAlarm(request, true);
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertTrue(result.isCreated());
        assertTrue(result.isModified());
        assertNotNull(result.getAlarm());
        assertNotEquals(newAlarmId, result.getAlarm().getUuidId());

        tenantDao.removeById(TenantId.SYS_TENANT_ID, tenant.getUuidId());
        tenantProfileDao.removeById(TenantId.SYS_TENANT_ID, tenant.getTenantProfileId().getId());
    }

    @Test
    public void testCantCreateAlarmIfCreateIsDisabled() {
        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        AlarmCreateOrUpdateActiveRequest request = AlarmCreateOrUpdateActiveRequest.builder()
                .tenantId(tenantId)
                .originator(deviceId)
                .type("ALARM_TYPE")
                .severity(AlarmSeverity.MAJOR)
                .build();
        AlarmApiCallResult result = alarmDao.createOrUpdateActiveAlarm(request, false);
        assertFalse(result.isSuccessful());
    }

    @Test
    public void testAckAlarmProcedure() {
        UUID tenantId = UUID.randomUUID();
        UUID originator1Id = UUID.fromString("d4b68f41-3e96-11e7-a884-898080180d6b");
        UUID alarm1Id = UUID.fromString("d4b68f43-3e96-11e7-a884-898080180d6b");
        Alarm alarm = saveAlarm(alarm1Id, tenantId, originator1Id, "TEST_ALARM");
        long ackTs = System.currentTimeMillis();
        AlarmApiCallResult result = alarmDao.acknowledgeAlarm(alarm.getTenantId(), alarm.getId(), ackTs);
        AlarmInfo afterSave = alarmDao.findAlarmInfoById(alarm.getTenantId(), alarm.getUuidId());
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertTrue(result.isModified());
        assertNotNull(result.getAlarm());
        assertEquals(afterSave, result.getAlarm());
        assertEquals(ackTs, result.getAlarm().getAckTs());
        assertTrue(result.getAlarm().isAcknowledged());
        result = alarmDao.acknowledgeAlarm(alarm.getTenantId(), alarm.getId(), ackTs + 1);
        assertNotNull(result);
        assertNotNull(result.getAlarm());
        assertEquals(afterSave, result.getAlarm());
        assertTrue(result.isSuccessful());
        assertFalse(result.isModified());
        assertEquals(ackTs, result.getAlarm().getAckTs());
        assertTrue(result.getAlarm().isAcknowledged());
    }

    @Test
    public void testClearAlarmProcedure() {
        UUID tenantId = UUID.randomUUID();
        UUID originator1Id = UUID.fromString("d4b68f41-3e96-11e7-a884-898080180d6b");
        UUID alarm1Id = UUID.fromString("d4b68f43-3e96-11e7-a884-898080180d6b");
        Alarm alarm = saveAlarm(alarm1Id, tenantId, originator1Id, "TEST_ALARM");
        long clearTs = System.currentTimeMillis();
        var details = JacksonUtil.newObjectNode().put("test", 123);
        AlarmApiCallResult result = alarmDao.clearAlarm(alarm.getTenantId(), alarm.getId(), clearTs, details);
        AlarmInfo afterSave = alarmDao.findAlarmInfoById(alarm.getTenantId(), alarm.getUuidId());
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertTrue(result.isCleared());
        assertNotNull(result.getAlarm());
        assertEquals(afterSave, result.getAlarm());
        assertEquals(clearTs, result.getAlarm().getClearTs());
        assertTrue(result.getAlarm().isCleared());
        assertEquals(details, result.getAlarm().getDetails());
        result = alarmDao.clearAlarm(alarm.getTenantId(), alarm.getId(), clearTs + 1, JacksonUtil.newObjectNode());
        assertNotNull(result);
        assertNotNull(result.getAlarm());
        assertEquals(afterSave, result.getAlarm());
        assertTrue(result.isSuccessful());
        assertFalse(result.isCleared());
        assertEquals(clearTs, result.getAlarm().getClearTs());
        assertTrue(result.getAlarm().isCleared());
    }

    @Test
    public void testClearAlarmWithoutDetailsProcedure() {
        UUID tenantId = UUID.randomUUID();
        UUID originator1Id = UUID.fromString("d4b68f41-3e96-11e7-a884-898080180d6b");
        UUID alarm1Id = UUID.fromString("d4b68f43-3e96-11e7-a884-898080180d6b");
        Alarm alarm = saveAlarm(alarm1Id, tenantId, originator1Id, "TEST_ALARM");
        long clearTs = System.currentTimeMillis();
        AlarmApiCallResult result = alarmDao.clearAlarm(alarm.getTenantId(), alarm.getId(), clearTs, null);
        AlarmInfo afterSave = alarmDao.findAlarmInfoById(alarm.getTenantId(), alarm.getUuidId());
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertTrue(result.isCleared());
        assertNotNull(result.getAlarm());
        assertEquals(afterSave, result.getAlarm());
        assertEquals(clearTs, result.getAlarm().getClearTs());
        assertTrue(result.getAlarm().isCleared());
        assertEquals(alarm.getDetails(), result.getAlarm().getDetails());
        result = alarmDao.clearAlarm(alarm.getTenantId(), alarm.getId(), clearTs + 1, JacksonUtil.newObjectNode());
        assertNotNull(result);
        assertNotNull(result.getAlarm());
        assertEquals(afterSave, result.getAlarm());
        assertTrue(result.isSuccessful());
        assertFalse(result.isCleared());
        assertEquals(clearTs, result.getAlarm().getClearTs());
        assertTrue(result.getAlarm().isCleared());
    }

    @Test
    public void testAssignAlarmProcedure() {
        UUID tenantId = UUID.randomUUID();
        ;
        UUID originator1Id = UUID.fromString("d4b68f41-3e96-11e7-a884-898080180d6b");
        UUID alarmId = UUID.fromString("d4b68f43-3e96-11e7-a884-898080180d6b");
        UserId userId1 = new UserId(UUID.fromString("d4b68f43-3e96-11e7-a884-898080180d7b"));
        UserId userId2 = new UserId(UUID.fromString("d4b68f43-3e96-11e7-a884-898080180d8b"));
        Alarm alarm = saveAlarm(alarmId, tenantId, originator1Id, "TEST_ALARM");
        long assignTs = System.currentTimeMillis();
        AlarmApiCallResult result = alarmDao.assignAlarm(alarm.getTenantId(), alarm.getId(), userId1, assignTs);
        AlarmInfo afterSave = alarmDao.findAlarmInfoById(alarm.getTenantId(), alarm.getUuidId());
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertTrue(result.isModified());
        assertNotNull(result.getAlarm());
        assertEquals(afterSave, result.getAlarm());
        assertEquals(assignTs, result.getAlarm().getAssignTs());
        assertNotNull(result.getAlarm().getAssigneeId());
        assertEquals(userId1, result.getAlarm().getAssigneeId());
        result = alarmDao.assignAlarm(alarm.getTenantId(), alarm.getId(), userId1, assignTs + 1);
        afterSave = alarmDao.findAlarmInfoById(alarm.getTenantId(), alarm.getUuidId());
        assertNotNull(result);
        assertNotNull(result.getAlarm());
        assertEquals(afterSave, result.getAlarm());
        assertTrue(result.isSuccessful());
        assertFalse(result.isModified());
        assertEquals(assignTs, result.getAlarm().getAssignTs());
        assertNotNull(result.getAlarm().getAssigneeId());
        assertEquals(userId1, result.getAlarm().getAssigneeId());
        result = alarmDao.assignAlarm(alarm.getTenantId(), alarm.getId(), userId2, assignTs + 1);
        afterSave = alarmDao.findAlarmInfoById(alarm.getTenantId(), alarm.getUuidId());
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertTrue(result.isModified());
        assertNotNull(result.getAlarm());
        assertEquals(afterSave, result.getAlarm());
        assertEquals(assignTs + 1, result.getAlarm().getAssignTs());
        assertNotNull(result.getAlarm().getAssigneeId());
        assertEquals(userId2, result.getAlarm().getAssigneeId());

        result = alarmDao.unassignAlarm(alarm.getTenantId(), alarm.getId(), assignTs + 1);
        afterSave = alarmDao.findAlarmInfoById(alarm.getTenantId(), alarm.getUuidId());
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertTrue(result.isModified());
        assertNotNull(result.getAlarm());
        assertEquals(afterSave, result.getAlarm());
        assertNull(result.getAlarm().getAssigneeId());

        result = alarmDao.unassignAlarm(alarm.getTenantId(), alarm.getId(), assignTs + 1);
        afterSave = alarmDao.findAlarmInfoById(alarm.getTenantId(), alarm.getUuidId());
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertFalse(result.isModified());
        assertNotNull(result.getAlarm());
        assertEquals(afterSave, result.getAlarm());
        assertNull(result.getAlarm().getAssigneeId());
    }

    private Alarm saveAlarm(UUID id, UUID tenantId, UUID deviceId, String type) {
        Alarm alarm = new Alarm();
        alarm.setId(new AlarmId(id));
        alarm.setTenantId(TenantId.fromUUID(tenantId));
        alarm.setOriginator(new DeviceId(deviceId));
        alarm.setType(type);
        alarm.setPropagate(true);
        alarm.setStartTs(System.currentTimeMillis());
        alarm.setEndTs(System.currentTimeMillis());
        alarm.setAcknowledged(false);
        alarm.setCleared(false);
        alarm.setDetails(JacksonUtil.newObjectNode().put("a", UUID.randomUUID().toString()).set("b", JacksonUtil.newObjectNode().put("a", "[}/.`1321421!@@$$(%&&$")));
        return alarmDao.save(TenantId.fromUUID(tenantId), alarm);
    }

    private Tenant createTenant() {
        TenantProfile tenantProfile = new TenantProfile();
        tenantProfile.setName("My tenant profile " + UUID.randomUUID());
        TenantProfileData profileData = new TenantProfileData();
        profileData.setConfiguration(new DefaultTenantProfileConfiguration());
        tenantProfile.setProfileData(profileData);
        var savedTenantProfile = tenantProfileDao.save(TenantId.SYS_TENANT_ID, tenantProfile);
        assertNotNull(savedTenantProfile);

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant " + UUID.randomUUID());
        tenant.setTenantProfileId(savedTenantProfile.getId());
        Tenant savedTenant = tenantDao.save(TenantId.SYS_TENANT_ID, tenant);

        assertNotNull(savedTenant);

        return savedTenant;
    }

}
