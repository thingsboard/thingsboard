// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.housekeeper.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.alarm.AlarmRef;
import org.thingsboard.server.common.data.housekeeper.AlarmsUnassignHousekeeperTask;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.msg.housekeeper.HousekeeperClient;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.sql.citus.CitusSettings;
import org.thingsboard.server.service.entitiy.alarm.TbAlarmService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AlarmsUnassignTaskProcessorTest {

    @Mock
    private TbAlarmService tbAlarmService;
    @Mock
    private AlarmService alarmService;
    @Mock
    private CitusSettings citusSettings;
    @Mock
    private HousekeeperClient housekeeperClient;

    private AlarmsUnassignTaskProcessor processor;

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    private final UserId userId = new UserId(UUID.randomUUID());

    @BeforeEach
    public void setUp() {
        processor = new AlarmsUnassignTaskProcessor(tbAlarmService, alarmService, citusSettings);
        ReflectionTestUtils.setField(processor, "housekeeperClient", housekeeperClient);
    }

    @Test
    public void whenCitusEnabled_thenBulkUnassignOnceWithoutResubmission() throws Exception {
        when(citusSettings.isEnabled()).thenReturn(true);
        AlarmsUnassignHousekeeperTask task = new AlarmsUnassignHousekeeperTask(tenantId, userId, "John", (List<AlarmRef>) null);

        processor.process(task);

        verify(alarmService).unassignAlarmsByAssignee(tenantId, userId, task.getTs());
        verify(alarmService, never()).findAlarmRefsByAssigneeId(any(), any(), anyLong(), any(), anyInt());
        verifyNoInteractions(tbAlarmService, housekeeperClient);
    }

    @Test
    public void whenPlainPostgresRootTask_thenPaginatesAssigneeIndex() throws Exception {
        when(citusSettings.isEnabled()).thenReturn(false);
        List<AlarmRef> firstPage = List.of(
                new AlarmRef(new AlarmId(UUID.randomUUID()), new DeviceId(UUID.randomUUID()), 100L),
                new AlarmRef(new AlarmId(UUID.randomUUID()), new DeviceId(UUID.randomUUID()), 200L));
        when(alarmService.findAlarmRefsByAssigneeId(eq(tenantId), eq(userId), anyLong(), any(), anyInt()))
                .thenReturn(firstPage)
                .thenReturn(List.of());
        AlarmsUnassignHousekeeperTask task = new AlarmsUnassignHousekeeperTask(tenantId, userId, "John", (List<AlarmRef>) null);

        processor.process(task);

        // The first scan starts from the zero cursor; the second advances it to the last ref of the first page.
        AlarmRef lastRef = firstPage.get(firstPage.size() - 1);
        verify(alarmService).findAlarmRefsByAssigneeId(eq(tenantId), eq(userId), eq(0L), isNull(), anyInt());
        verify(alarmService).findAlarmRefsByAssigneeId(eq(tenantId), eq(userId), eq(lastRef.createdTime()), eq(lastRef.alarmId()), anyInt());

        ArgumentCaptor<AlarmsUnassignHousekeeperTask> batchTaskCaptor = ArgumentCaptor.forClass(AlarmsUnassignHousekeeperTask.class);
        verify(housekeeperClient).submitTask(batchTaskCaptor.capture());
        AlarmsUnassignHousekeeperTask batchTask = batchTaskCaptor.getValue();
        assertThat(batchTask.getTenantId()).isEqualTo(tenantId);
        assertThat(batchTask.getEntityId()).isEqualTo(userId);
        assertThat(batchTask.getUserTitle()).isEqualTo("John");
        assertThat(batchTask.getAlarmRefs()).isEqualTo(firstPage);

        verify(alarmService, never()).unassignAlarmsByAssignee(any(), any(), anyLong());
        verifyNoInteractions(tbAlarmService);
    }

    @Test
    public void whenPlainPostgresBatchTask_thenDelegatesToTbAlarmService() throws Exception {
        when(citusSettings.isEnabled()).thenReturn(false);
        List<AlarmRef> refs = List.of(new AlarmRef(new AlarmId(UUID.randomUUID()), new DeviceId(UUID.randomUUID()), 100L));
        AlarmsUnassignHousekeeperTask task = new AlarmsUnassignHousekeeperTask(tenantId, userId, "John", refs);

        processor.process(task);

        verify(tbAlarmService).unassignDeletedUserAlarms(tenantId, userId, "John", refs, task.getTs());
        verify(alarmService, never()).unassignAlarmsByAssignee(any(), any(), anyLong());
        verify(alarmService, never()).findAlarmRefsByAssigneeId(any(), any(), anyLong(), any(), anyInt());
    }

    @Test
    public void whenLegacyTaskWithAlarmIdsOnly_thenDelegatesToTbAlarmServiceByIds() throws Exception {
        when(citusSettings.isEnabled()).thenReturn(false);
        AlarmsUnassignHousekeeperTask task = new AlarmsUnassignHousekeeperTask(tenantId, userId, "John", (List<AlarmRef>) null);
        // In-flight tasks from pre-4.3.x nodes carry only alarm ids; only the deserializer sets the field in production.
        List<UUID> alarmIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        task.setAlarms(alarmIds);

        processor.process(task);

        verify(tbAlarmService).unassignDeletedUserAlarmsByIds(tenantId, userId, "John", alarmIds, task.getTs());
        verify(alarmService, never()).unassignAlarmsByAssignee(any(), any(), anyLong());
        verify(alarmService, never()).findAlarmRefsByAssigneeId(any(), any(), anyLong(), any(), anyInt());
        verifyNoInteractions(housekeeperClient);
    }

}
