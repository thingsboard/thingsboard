// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.housekeeper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "taskType", visible = true, include = JsonTypeInfo.As.EXISTING_PROPERTY, defaultImpl = HousekeeperTask.class)
@JsonSubTypes({
        @Type(name = "DELETE_TS_HISTORY", value = TsHistoryDeletionHousekeeperTask.class),
        @Type(name = "DELETE_LATEST_TS", value = LatestTsDeletionHousekeeperTask.class),
        @Type(name = "DELETE_TENANT_ENTITIES", value = TenantEntitiesDeletionHousekeeperTask.class),
        @Type(name = "DELETE_ENTITIES", value = EntitiesDeletionHousekeeperTask.class),
        @Type(name = "DELETE_ALARMS", value = AlarmsDeletionHousekeeperTask.class),
        @Type(name = "DELETE_ALARM_COMMENTS", value = AlarmCommentsDeletionHousekeeperTask.class),
        @Type(name = "UNASSIGN_ALARMS", value = AlarmsUnassignHousekeeperTask.class),
        @Type(name = "CLEANUP_ENTITIES", value = EntitiesCleanupHousekeeperTask.class)
})
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HousekeeperTask implements Serializable {

    @Serial
    private static final long serialVersionUID = -2585974110832225152L;

    private TenantId tenantId;
    private EntityId entityId;
    private HousekeeperTaskType taskType;
    private long ts;

    protected HousekeeperTask(@NonNull TenantId tenantId, @NonNull EntityId entityId, @NonNull HousekeeperTaskType taskType) {
        this.tenantId = tenantId;
        this.entityId = entityId;
        this.taskType = taskType;
        this.ts = System.currentTimeMillis();
    }

    public static HousekeeperTask deleteAttributes(TenantId tenantId, EntityId entityId) {
        return new HousekeeperTask(tenantId, entityId, HousekeeperTaskType.DELETE_ATTRIBUTES);
    }

    public static HousekeeperTask deleteTelemetry(TenantId tenantId, EntityId entityId) {
        return new HousekeeperTask(tenantId, entityId, HousekeeperTaskType.DELETE_TELEMETRY);
    }

    public static HousekeeperTask deleteEvents(TenantId tenantId, EntityId entityId) {
        return new HousekeeperTask(tenantId, entityId, HousekeeperTaskType.DELETE_EVENTS);
    }

    public static HousekeeperTask unassignAlarms(User user) {
        return new AlarmsUnassignHousekeeperTask(user);
    }

    public static HousekeeperTask deleteAlarms(TenantId tenantId, EntityId entityId) {
        return new AlarmsDeletionHousekeeperTask(tenantId, entityId);
    }

    public static HousekeeperTask deleteAlarmComments(TenantId tenantId, AlarmId alarmId) {
        return new AlarmCommentsDeletionHousekeeperTask(tenantId, alarmId);
    }

    // contextEntityId is description/log context only (the deleted originator, the tenant for TTL cleanup, etc.);
    // the deletion itself keys off the alarms list.
    public static HousekeeperTask deleteAlarmComments(TenantId tenantId, EntityId contextEntityId, List<UUID> alarms) {
        return new AlarmCommentsDeletionHousekeeperTask(tenantId, contextEntityId, alarms);
    }

    public static HousekeeperTask deleteTenantEntities(TenantId tenantId, EntityType entityType) {
        return new TenantEntitiesDeletionHousekeeperTask(tenantId, entityType);
    }

    public static HousekeeperTask deleteCalculatedFields(TenantId tenantId, EntityId entityId) {
        return new HousekeeperTask(tenantId, entityId, HousekeeperTaskType.DELETE_CALCULATED_FIELDS);
    }

    public static HousekeeperTask deleteJobs(TenantId tenantId, EntityId entityId) {
        return new HousekeeperTask(tenantId, entityId, HousekeeperTaskType.DELETE_JOBS);
    }

    public static HousekeeperTask deleteAiUserData(TenantId tenantId, UserId userId) {
        return new HousekeeperTask(tenantId, userId, HousekeeperTaskType.DELETE_AI_USER_DATA);
    }

    public static HousekeeperTask deleteAiTenantData(TenantId tenantId) {
        return new HousekeeperTask(tenantId, tenantId, HousekeeperTaskType.DELETE_AI_TENANT_DATA);
    }

    @JsonIgnore
    public String getDescription() {
        return taskType.getDescription() + " for " + entityId.getEntityType().getNormalName().toLowerCase() + " " + entityId.getId();
    }

}
