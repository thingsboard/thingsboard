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
package org.thingsboard.server.service.ttl;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.alarm.AlarmDao;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DaoSqlTest
@TestPropertySource(properties = {
        "sql.ttl.alarms.removal_batch_size=5"
})
public class AlarmsCleanUpServiceTest extends AbstractControllerTest {

    @SpyBean
    private AlarmsCleanUpService alarmsCleanUpService;
    @SpyBean
    private AlarmService alarmService;
    @Autowired
    private AlarmDao alarmDao;

    private Logger cleanUpServiceLoggerSpy;

    @Before
    public void beforeEach() throws Exception {
        cleanUpServiceLoggerSpy = Mockito.spy(LoggerFactory.getLogger(AlarmsCleanUpService.class));
        willReturn(cleanUpServiceLoggerSpy).given(alarmsCleanUpService).getLogger();
    }

    @Test
    public void testAlarmsCleanUp() throws Exception {
        int ttlDays = 1;
        updateDefaultTenantProfileConfig(profileConfiguration -> {
            profileConfiguration.setAlarmsTtlDays(ttlDays);
        });

        loginTenantAdmin();
        Device device = createDevice("device_0", "device_0");
        int count = 100;
        long ts = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(ttlDays) - TimeUnit.MINUTES.toMillis(10);
        List<AlarmId> outdatedAlarms = new ArrayList<>();
        List<AlarmId> freshAlarms = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Alarm alarm = Alarm.builder()
                    .tenantId(tenantId)
                    .originator(device.getId())
                    .cleared(false)
                    .acknowledged(false)
                    .severity(AlarmSeverity.CRITICAL)
                    .type("outdated_alarm_" + i)
                    .startTs(ts)
                    .endTs(ts)
                    .build();
            alarm.setId(new AlarmId(UUID.randomUUID()));
            alarm.setCreatedTime(ts);

            outdatedAlarms.add(alarmDao.save(tenantId, alarm).getId());

            alarm.setType("fresh_alarm_" + i);
            alarm.setStartTs(System.currentTimeMillis());
            alarm.setEndTs(alarm.getStartTs());
            alarm.setId(new AlarmId(UUID.randomUUID()));
            alarm.setCreatedTime(alarm.getStartTs());
            freshAlarms.add(alarmDao.save(tenantId, alarm).getId());
        }

        alarmsCleanUpService.cleanUp();

        for (AlarmId outdatedAlarm : outdatedAlarms) {
            verify(alarmService).delAlarm(eq(tenantId), eq(outdatedAlarm), eq(false));
        }
        for (AlarmId freshAlarm : freshAlarms) {
            verify(alarmService, never()).delAlarm(eq(tenantId), eq(freshAlarm), eq(false));
        }

        verify(cleanUpServiceLoggerSpy).info(startsWith("Removed {} outdated alarm"), eq((long) count), eq(tenantId), any());
    }

}
