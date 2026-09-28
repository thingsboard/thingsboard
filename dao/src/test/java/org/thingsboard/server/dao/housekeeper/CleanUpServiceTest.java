// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.housekeeper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTask;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTaskType;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.msg.housekeeper.HousekeeperClient;
import org.thingsboard.server.dao.eventsourcing.ActionCause;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.relation.RelationService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for the Housekeeper-task enqueue paths in {@link CleanUpService}. The listener is invoked directly,
 * bypassing the {@code @TransactionalEventListener} machinery, so commit timing and rollback suppression are
 * covered separately by {@link CleanUpServiceTransactionalTest}.
 *
 * <p><b>Alarm comments:</b> {@code alarm_comment} is no longer FK-linked to {@code alarm} (it stays
 * coordinator-local partitioned under Citus, since {@code alarm} is distributed on {@code originator_id}), so
 * deleting an alarm can no longer rely on an {@code ON DELETE CASCADE} to remove its comments. Instead, alarm
 * deletion enqueues a {@code DELETE_ALARM_COMMENTS} task that cleans the comments asynchronously. The tests pin
 * the enqueue payload and routing without needing Citus, ALARM's exclusion from the generic related-data cleanup,
 * and the suppression half: a deletion with {@code cause == BULK_DELETION} must enqueue nothing, since bulk
 * deletions batch the comment cleanup into a single list task at the orchestrator.
 *
 * <p><b>AI data deletion:</b> user and tenant deletions enqueue {@code DELETE_AI_USER_DATA} /
 * {@code DELETE_AI_TENANT_DATA} tasks so the AI service hard-deletes the corresponding data (GDPR). The per-user
 * task must fire even when {@code cause == TENANT_DELETION}: AI-side chats are keyed only by user id, so the
 * per-user fan-out is a tenant wipe's only route to them. The only suppression is the sys tenant, to which AI is
 * structurally unavailable, so no AI data can exist for it.
 */
class CleanUpServiceTest {

    private HousekeeperClient housekeeperClient;
    private RelationService relationService;
    private CleanUpService cleanUpService;

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        housekeeperClient = mock(HousekeeperClient.class);
        relationService = mock(RelationService.class);
        cleanUpService = new CleanUpService(Optional.of(housekeeperClient), relationService);
    }

    @Test
    void alarmDeletionEnqueuesAlarmCommentsCleanupTask() {
        AlarmId alarmId = new AlarmId(UUID.randomUUID());
        DeleteEntityEvent<Alarm> event = DeleteEntityEvent.<Alarm>builder()
                .tenantId(tenantId)
                .entityId(alarmId)
                .build();

        cleanUpService.handleEntityDeletionEvent(event);

        ArgumentCaptor<HousekeeperTask> captor = ArgumentCaptor.forClass(HousekeeperTask.class);
        verify(housekeeperClient, times(1)).submitTask(captor.capture());

        HousekeeperTask submitted = captor.getValue();
        assertThat(submitted.getTaskType())
                .as("alarm deletion must enqueue the alarm-comment cleanup task")
                .isEqualTo(HousekeeperTaskType.DELETE_ALARM_COMMENTS);
        assertThat(submitted.getEntityId())
                .as("the cleanup task must target the deleted alarm")
                .isEqualTo(alarmId);
        assertThat(submitted.getTenantId()).isEqualTo(tenantId);

        // ALARM is in the skippedEntities set, so no relation/attribute/telemetry cleanup is performed for it.
        verify(relationService, never()).deleteEntityRelations(tenantId, alarmId);
    }

    @Test
    void bulkAlarmDeletionDoesNotEnqueuePerAlarmCommentsCleanupTask() {
        AlarmId alarmId = new AlarmId(UUID.randomUUID());
        DeleteEntityEvent<Alarm> event = DeleteEntityEvent.<Alarm>builder()
                .tenantId(tenantId)
                .entityId(alarmId)
                .cause(ActionCause.BULK_DELETION)
                .build();

        cleanUpService.handleEntityDeletionEvent(event);

        // Bulk deletions batch the comment cleanup into a single list task at the orchestrator,
        // so a per-alarm DELETE_ALARM_COMMENTS task must NOT be submitted here.
        verify(housekeeperClient, never()).submitTask(any());
    }

    @Test
    void userDeletionEnqueuesAiUserDataDeletionTask() {
        User user = user(tenantId);

        cleanUpService.handleEntityDeletionEvent(DeleteEntityEvent.<Object>builder()
                .tenantId(tenantId)
                .entityId(user.getId())
                .entity(user)
                .build());

        HousekeeperTask task = submittedTask(HousekeeperTaskType.DELETE_AI_USER_DATA);
        assertThat(task.getTenantId()).isEqualTo(tenantId);
        assertThat(task.getEntityId()).isEqualTo(user.getId());
    }

    @Test
    void userDeletionEnqueuesAiUserDataDeletionTask_evenWhenCausedByTenantDeletion() {
        User user = user(tenantId);

        cleanUpService.handleEntityDeletionEvent(DeleteEntityEvent.<Object>builder()
                .tenantId(tenantId)
                .entityId(user.getId())
                .entity(user)
                .cause(ActionCause.TENANT_DELETION)
                .build());

        HousekeeperTask task = submittedTask(HousekeeperTaskType.DELETE_AI_USER_DATA);
        assertThat(task.getTenantId()).isEqualTo(tenantId);
        assertThat(task.getEntityId()).isEqualTo(user.getId());
        assertThat(submittedTasks())
                .as("alarm unassign fan-out is suppressed on tenant deletion, but the AI wipe must still fire per user")
                .noneMatch(t -> t.getTaskType() == HousekeeperTaskType.UNASSIGN_ALARMS);
    }

    @Test
    void tenantDeletionEnqueuesAiTenantDataDeletionTask() {
        cleanUpService.handleEntityDeletionEvent(DeleteEntityEvent.<Object>builder()
                .tenantId(tenantId)
                .entityId(tenantId)
                .build());

        HousekeeperTask task = submittedTask(HousekeeperTaskType.DELETE_AI_TENANT_DATA);
        assertThat(task.getTenantId()).isEqualTo(tenantId);
        assertThat(task.getEntityId()).isEqualTo(tenantId);
    }

    @Test
    void allCleanupTasksAreStillEnqueued_whenRelationCleanupFails() {
        User user = user(tenantId);
        doThrow(new RuntimeException("DB is down")).when(relationService).deleteEntityRelations(tenantId, user.getId());

        cleanUpService.handleEntityDeletionEvent(DeleteEntityEvent.<Object>builder()
                .tenantId(tenantId)
                .entityId(user.getId())
                .entity(user)
                .build());

        // A failure in the synchronous relation cleanup must not suppress any of the subsequent task enqueues.
        assertThat(submittedTasks())
                .extracting(HousekeeperTask::getTaskType)
                .containsExactlyInAnyOrder(
                        HousekeeperTaskType.DELETE_ATTRIBUTES,
                        HousekeeperTaskType.DELETE_TELEMETRY,
                        HousekeeperTaskType.DELETE_EVENTS,
                        HousekeeperTaskType.DELETE_ALARMS,
                        HousekeeperTaskType.DELETE_CALCULATED_FIELDS,
                        HousekeeperTaskType.UNASSIGN_ALARMS,
                        HousekeeperTaskType.DELETE_AI_USER_DATA);
    }

    @Test
    void noAiDataDeletionTaskIsEnqueued_whenDeletedUserBelongsToSysTenant() {
        User user = user(TenantId.SYS_TENANT_ID);

        cleanUpService.handleEntityDeletionEvent(DeleteEntityEvent.<Object>builder()
                .tenantId(TenantId.SYS_TENANT_ID)
                .entityId(user.getId())
                .entity(user)
                .build());

        assertThat(submittedTasks()).noneMatch(task ->
                task.getTaskType() == HousekeeperTaskType.DELETE_AI_USER_DATA);
    }

    private static User user(TenantId tenantId) {
        User user = new User(new UserId(UUID.randomUUID()));
        user.setTenantId(tenantId);
        return user;
    }

    private List<HousekeeperTask> submittedTasks() {
        ArgumentCaptor<HousekeeperTask> captor = ArgumentCaptor.forClass(HousekeeperTask.class);
        verify(housekeeperClient, atLeast(0)).submitTask(captor.capture());
        return captor.getAllValues();
    }

    private HousekeeperTask submittedTask(HousekeeperTaskType taskType) {
        List<HousekeeperTask> tasks = submittedTasks().stream()
                .filter(task -> task.getTaskType() == taskType)
                .toList();
        assertThat(tasks).hasSize(1);
        return tasks.get(0);
    }
}
