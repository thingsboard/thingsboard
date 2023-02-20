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
package org.thingsboard.server.dao.sql.alarm;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.alarm.AlarmDao;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by Valerii Sosliuk on 5/21/2017.
 */
@Slf4j
public class JpaAlarmDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private AlarmDao alarmDao;


    @Test
    public void testFindLatestByOriginatorAndType() throws ExecutionException, InterruptedException, TimeoutException {
        log.info("Current system time in millis = {}", System.currentTimeMillis());
        UUID tenantId = UUID.fromString("d4b68f40-3e96-11e7-a884-898080180d6b");
        UUID originator1Id = UUID.fromString("d4b68f41-3e96-11e7-a884-898080180d6b");
        UUID originator2Id = UUID.fromString("d4b68f42-3e96-11e7-a884-898080180d6b");
        UUID alarm1Id = UUID.fromString("d4b68f43-3e96-11e7-a884-898080180d6b");
        UUID alarm2Id = UUID.fromString("d4b68f44-3e96-11e7-a884-898080180d6b");
        UUID alarm3Id = UUID.fromString("d4b68f45-3e96-11e7-a884-898080180d6b");
        int alarmCountBeforeSave = alarmDao.find(TenantId.fromUUID(tenantId)).size();
        saveAlarm(alarm1Id, tenantId, originator1Id, "TEST_ALARM");
        //The timestamp of the startTime should be different in order for test to always work
        Thread.sleep(1);
        saveAlarm(alarm2Id, tenantId, originator1Id, "TEST_ALARM");
        saveAlarm(alarm3Id, tenantId, originator2Id, "TEST_ALARM");
        int alarmCountAfterSave = alarmDao.find(TenantId.fromUUID(tenantId)).size();
        assertEquals(3, alarmCountAfterSave - alarmCountBeforeSave);
        ListenableFuture<Alarm> future = alarmDao
                .findLatestByOriginatorAndTypeAsync(TenantId.fromUUID(tenantId), new DeviceId(originator1Id), "TEST_ALARM");
        Alarm alarm = future.get(30, TimeUnit.SECONDS);
        assertNotNull(alarm);
        assertEquals(alarm2Id, alarm.getId().getId());
    }

    @Test
    public void testAckAlarmProcedure() {
        UUID tenantId = UUID.fromString("d4b68f40-3e96-11e7-a884-898080180d6b");
        UUID originator1Id = UUID.fromString("d4b68f41-3e96-11e7-a884-898080180d6b");
        UUID alarm1Id = UUID.fromString("d4b68f43-3e96-11e7-a884-898080180d6b");
        Alarm alarm = saveAlarm(alarm1Id, tenantId, originator1Id, "TEST_ALARM");
        AlarmInfo alarmInfo = alarmDao.acknowledgeAlarm(alarm.getTenantId(), alarm.getId());
        assertNotNull(alarmInfo);
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
        return alarmDao.save(TenantId.fromUUID(tenantId), alarm);
    }

}
