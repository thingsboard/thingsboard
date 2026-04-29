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
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.model.Alarm;
import org.thingsboard.client.model.AlarmInfo;
import org.thingsboard.client.model.AlarmSeverity;
import org.thingsboard.client.model.AlarmStatus;
import org.thingsboard.client.model.Device;
import org.thingsboard.client.model.EntitySubtype;
import org.thingsboard.client.model.EntityType;
import org.thingsboard.client.model.PageDataAlarmInfo;
import org.thingsboard.client.model.PageDataEntitySubtype;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@DaoSqlTest
public class AlarmApiClientTest extends AbstractApiClientTest {

    @Test
    public void testAlarmLifecycle() throws Exception {
        long timestamp = System.currentTimeMillis();
        List<Alarm> createdAlarms = new ArrayList<>();

        // First, create devices to attach alarms to
        Device device1 = new Device();
        device1.setName("Device_For_Alarm_" + timestamp + "_1");
        device1.setType("default");
        Device createdDevice1 = client.saveDevice(device1, null, null, null, null);

        Device device2 = new Device();
        device2.setName("Device_For_Alarm_" + timestamp + "_2");
        device2.setType("thermostat");
        Device createdDevice2 = client.saveDevice(device2, null, null, null, null);

        // Create 2 alarms (1 for each device)
        for (int i = 0; i < 2; i++) {
            Alarm alarm = new Alarm();
            alarm.setType(((i % 2 == 0) ? "Temperature Alarm" : "Connection Alarm"));
            alarm.setSeverity(((i % 2 == 0) ? AlarmSeverity.CRITICAL : AlarmSeverity.WARNING));
            alarm.setOriginator((i % 2 == 0) ? createdDevice1.getId() : createdDevice2.getId());

            Alarm createdAlarm = client.saveAlarm(alarm);
            assertNotNull(createdAlarm);
            assertNotNull(createdAlarm.getId());
            assertEquals(alarm.getType(), createdAlarm.getType());
            assertEquals(alarm.getSeverity(), createdAlarm.getSeverity());

            createdAlarms.add(createdAlarm);
        }

        // Get all alarms
        PageDataAlarmInfo allAlarms = client.getAllAlarms(100, 0, null, null, null, null, null, null, null, null, null);

        assertNotNull(allAlarms);
        assertNotNull(allAlarms.getData());
        int initialSize = allAlarms.getData().size();
        assertEquals("Expected at least 2 alarms, but got " + initialSize, 2, initialSize);

        // Get alarms by entity (device1)
        PageDataAlarmInfo device1Alarms = client.getAlarmsV2(EntityType.DEVICE.toString(), createdDevice1.getId().getId().toString(), 100, 0, null, null, null, null, null, null, null, null, null);
        assertNotNull(device1Alarms);
        assertEquals("Expected 1 alarms for device1", 1, device1Alarms.getData().size());

        // Get alarm by id
        Alarm searchAlarm = createdAlarms.get(0);
        Alarm fetchedAlarm = client.getAlarmById(searchAlarm.getId().getId().toString());
        assertEquals(searchAlarm.getType(), fetchedAlarm.getType());
        assertEquals(searchAlarm.getSeverity(), fetchedAlarm.getSeverity());

        // Get alarm info
        AlarmInfo alarmInfo = client.getAlarmInfoById(searchAlarm.getId().getId().toString());
        assertNotNull(alarmInfo);
        assertEquals(searchAlarm.getId().getId(), alarmInfo.getId().getId());

        // Acknowledge alarm
        client.ackAlarm(searchAlarm.getId().getId().toString());

        // Verify alarm is acknowledged
        Alarm ackedAlarm = client.getAlarmById(searchAlarm.getId().getId().toString());
        assertEquals(AlarmStatus.ACTIVE_ACK, ackedAlarm.getStatus());

        // Clear alarm
        client.clearAlarm(searchAlarm.getId().getId().toString());

        // Verify alarm is cleared
        Alarm clearedAlarm = client.getAlarmById(searchAlarm.getId().getId().toString());
        assertEquals(AlarmStatus.CLEARED_ACK, clearedAlarm.getStatus());

        // Get highest severity alarm for device
        AlarmSeverity highestSeverity = client.getHighestAlarmSeverity(EntityType.DEVICE.toString(), createdDevice1.getId().getId().toString(), null, null, null);
        assertNotNull(highestSeverity);
        assertEquals(AlarmSeverity.CRITICAL, highestSeverity);

        // Assign alarm to customer
        client.assignAlarm(createdAlarms.get(0).getId().getId().toString(), clientTenantAdmin.getId().getId().toString());

        // Verify assignment
        Alarm assignedAlarm = client.getAlarmById(createdAlarms.get(0).getId().getId().toString());
        assertEquals(clientTenantAdmin.getId().getId(), assignedAlarm.getAssigneeId().getId());

        // Unassign alarm
        client.unassignAlarm(createdAlarms.get(0).getId().getId().toString());

        // Verify unassignment
        Alarm unassignedAlarm = client.getAlarmById(createdAlarms.get(0).getId().getId().toString());
        assertNull(unassignedAlarm.getAssigneeId());

        // Get alarm types
        PageDataEntitySubtype pageDataEntitySubtype = client.getAlarmTypes(100, 0, null, null);
        assertEquals(2, pageDataEntitySubtype.getData().size());
        List<String> alarmTypes = pageDataEntitySubtype.getData().stream()
                .map(EntitySubtype::getType)
                .collect(Collectors.toList());
        assertTrue(alarmTypes.containsAll(List.of("Temperature Alarm", "Connection Alarm")));

        // Get alarms V2 (alternative endpoint)
        PageDataAlarmInfo alarmsV2 = client.getAlarmsV2(EntityType.DEVICE.toString(), createdDevice1.getId().getId().toString(), 100, 0, null, null, null, null, null, null, null, null, null);
        assertNotNull(alarmsV2);
        assertEquals(1, alarmsV2.getData().size());

        // Get all alarms V2
        PageDataAlarmInfo allAlarmsV2 = client.getAllAlarmsV2(100, 0, null, null, null, null, null, null, null, null, null);
        assertEquals(2, allAlarmsV2.getData().size());

        // Delete alarm
        UUID alarmToDeleteId = createdAlarms.get(0).getId().getId();
        client.deleteAlarm(alarmToDeleteId.toString());

        // Verify the alarm is deleted (should return 404)
        assertReturns404(() ->
                client.getAlarmById(alarmToDeleteId.toString())
        );

        // Verify count after deletion
        PageDataAlarmInfo alarmsAfterDelete = client.getAllAlarms(100, 0, null, null, null, null, null, null, null, null, null);
        assertEquals(initialSize - 1, alarmsAfterDelete.getData().size());
    }

}
