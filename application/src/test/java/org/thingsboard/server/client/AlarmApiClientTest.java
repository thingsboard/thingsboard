// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.api.ThingsboardApi.AckAlarmArgs;
import org.thingsboard.client.api.ThingsboardApi.AssignAlarmArgs;
import org.thingsboard.client.api.ThingsboardApi.ClearAlarmArgs;
import org.thingsboard.client.api.ThingsboardApi.DeleteAlarmArgs;
import org.thingsboard.client.api.ThingsboardApi.GetAlarmByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetAlarmInfoByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetAlarmTypesArgs;
import org.thingsboard.client.api.ThingsboardApi.GetAlarmsV2Args;
import org.thingsboard.client.api.ThingsboardApi.GetAllAlarmsArgs;
import org.thingsboard.client.api.ThingsboardApi.GetAllAlarmsV2Args;
import org.thingsboard.client.api.ThingsboardApi.GetHighestAlarmSeverityArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveAlarmArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveDeviceArgs;
import org.thingsboard.client.api.ThingsboardApi.UnassignAlarmArgs;
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
        Device createdDevice1 = client.saveDevice(SaveDeviceArgs.builder()
                .device(device1)
                .build());

        Device device2 = new Device();
        device2.setName("Device_For_Alarm_" + timestamp + "_2");
        device2.setType("thermostat");
        Device createdDevice2 = client.saveDevice(SaveDeviceArgs.builder()
                .device(device2)
                .build());

        // Create 2 alarms (1 for each device)
        for (int i = 0; i < 2; i++) {
            Alarm alarm = new Alarm();
            alarm.setType(((i % 2 == 0) ? "Temperature Alarm" : "Connection Alarm"));
            alarm.setSeverity(((i % 2 == 0) ? AlarmSeverity.CRITICAL : AlarmSeverity.WARNING));
            alarm.setOriginator((i % 2 == 0) ? createdDevice1.getId() : createdDevice2.getId());

            Alarm createdAlarm = client.saveAlarm(SaveAlarmArgs.builder()
                    .alarm(alarm)
                    .build());
            assertNotNull(createdAlarm);
            assertNotNull(createdAlarm.getId());
            assertEquals(alarm.getType(), createdAlarm.getType());
            assertEquals(alarm.getSeverity(), createdAlarm.getSeverity());

            createdAlarms.add(createdAlarm);
        }

        // Get all alarms
        PageDataAlarmInfo allAlarms = client.getAllAlarms(GetAllAlarmsArgs.builder()
                .pageSize(100)
                .page(0)
                .build());

        assertNotNull(allAlarms);
        assertNotNull(allAlarms.getData());
        int initialSize = allAlarms.getData().size();
        assertEquals("Expected at least 2 alarms, but got " + initialSize, 2, initialSize);

        // Get alarms by entity (device1)
        PageDataAlarmInfo device1Alarms = client.getAlarmsV2(GetAlarmsV2Args.builder()
                .entityType(EntityType.DEVICE.toString())
                .entityId(createdDevice1.getId().getId().toString())
                .pageSize(100)
                .page(0)
                .build());
        assertNotNull(device1Alarms);
        assertEquals("Expected 1 alarms for device1", 1, device1Alarms.getData().size());

        // Get alarm by id
        Alarm searchAlarm = createdAlarms.get(0);
        Alarm fetchedAlarm = client.getAlarmById(GetAlarmByIdArgs.builder()
                .alarmId(searchAlarm.getId().getId().toString())
                .build());
        assertEquals(searchAlarm.getType(), fetchedAlarm.getType());
        assertEquals(searchAlarm.getSeverity(), fetchedAlarm.getSeverity());

        // Get alarm info
        AlarmInfo alarmInfo = client.getAlarmInfoById(GetAlarmInfoByIdArgs.builder()
                .alarmId(searchAlarm.getId().getId().toString())
                .build());
        assertNotNull(alarmInfo);
        assertEquals(searchAlarm.getId().getId(), alarmInfo.getId().getId());

        // Acknowledge alarm
        client.ackAlarm(AckAlarmArgs.builder()
                .alarmId(searchAlarm.getId().getId().toString())
                .build());

        // Verify alarm is acknowledged
        Alarm ackedAlarm = client.getAlarmById(GetAlarmByIdArgs.builder()
                .alarmId(searchAlarm.getId().getId().toString())
                .build());
        assertEquals(AlarmStatus.ACTIVE_ACK, ackedAlarm.getStatus());

        // Clear alarm
        client.clearAlarm(ClearAlarmArgs.builder()
                .alarmId(searchAlarm.getId().getId().toString())
                .build());

        // Verify alarm is cleared
        Alarm clearedAlarm = client.getAlarmById(GetAlarmByIdArgs.builder()
                .alarmId(searchAlarm.getId().getId().toString())
                .build());
        assertEquals(AlarmStatus.CLEARED_ACK, clearedAlarm.getStatus());

        // Get highest severity alarm for device
        AlarmSeverity highestSeverity = client.getHighestAlarmSeverity(GetHighestAlarmSeverityArgs.builder()
                .entityType(EntityType.DEVICE.toString())
                .entityId(createdDevice1.getId().getId().toString())
                .build());
        assertNotNull(highestSeverity);
        assertEquals(AlarmSeverity.CRITICAL, highestSeverity);

        // Assign alarm to customer
        client.assignAlarm(AssignAlarmArgs.builder()
                .alarmId(createdAlarms.get(0).getId().getId().toString())
                .assigneeId(clientTenantAdmin.getId().getId().toString())
                .build());

        // Verify assignment
        Alarm assignedAlarm = client.getAlarmById(GetAlarmByIdArgs.builder()
                .alarmId(createdAlarms.get(0).getId().getId().toString())
                .build());
        assertEquals(clientTenantAdmin.getId().getId(), assignedAlarm.getAssigneeId().getId());

        // Unassign alarm
        client.unassignAlarm(UnassignAlarmArgs.builder()
                .alarmId(createdAlarms.get(0).getId().getId().toString())
                .build());

        // Verify unassignment
        Alarm unassignedAlarm = client.getAlarmById(GetAlarmByIdArgs.builder()
                .alarmId(createdAlarms.get(0).getId().getId().toString())
                .build());
        assertNull(unassignedAlarm.getAssigneeId());

        // Get alarm types
        PageDataEntitySubtype pageDataEntitySubtype = client.getAlarmTypes(GetAlarmTypesArgs.builder()
                .pageSize(100)
                .page(0)
                .build());
        assertEquals(2, pageDataEntitySubtype.getData().size());
        List<String> alarmTypes = pageDataEntitySubtype.getData().stream()
                .map(EntitySubtype::getType)
                .collect(Collectors.toList());
        assertTrue(alarmTypes.containsAll(List.of("Temperature Alarm", "Connection Alarm")));

        // Get alarms V2 (alternative endpoint)
        PageDataAlarmInfo alarmsV2 = client.getAlarmsV2(GetAlarmsV2Args.builder()
                .entityType(EntityType.DEVICE.toString())
                .entityId(createdDevice1.getId().getId().toString())
                .pageSize(100)
                .page(0)
                .build());
        assertNotNull(alarmsV2);
        assertEquals(1, alarmsV2.getData().size());

        // Get all alarms V2
        PageDataAlarmInfo allAlarmsV2 = client.getAllAlarmsV2(GetAllAlarmsV2Args.builder()
                .pageSize(100)
                .page(0)
                .build());
        assertEquals(2, allAlarmsV2.getData().size());

        // Delete alarm
        UUID alarmToDeleteId = createdAlarms.get(0).getId().getId();
        client.deleteAlarm(DeleteAlarmArgs.builder()
                .alarmId(alarmToDeleteId.toString())
                .build());

        // Verify the alarm is deleted (should return 404)
        assertReturns404(() ->
                client.getAlarmById(GetAlarmByIdArgs.builder()
                        .alarmId(alarmToDeleteId.toString())
                        .build())
        );

        // Verify count after deletion
        PageDataAlarmInfo alarmsAfterDelete = client.getAllAlarms(GetAllAlarmsArgs.builder()
                .pageSize(100)
                .page(0)
                .build());
        assertEquals(initialSize - 1, alarmsAfterDelete.getData().size());
    }

}
