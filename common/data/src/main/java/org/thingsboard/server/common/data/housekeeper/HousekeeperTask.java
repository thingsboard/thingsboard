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
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "taskType", visible = true, include = JsonTypeInfo.As.EXISTING_PROPERTY, defaultImpl = HousekeeperTask.class)
@JsonSubTypes({
        @Type(name = "DELETE_TS_HISTORY", value = TsHistoryDeletionHousekeeperTask.class),
        @Type(name = "DELETE_LATEST_TS", value = LatestTsDeletionHousekeeperTask.class),
        @Type(name = "DELETE_TENANT_ENTITIES", value = TenantEntitiesDeletionHousekeeperTask.class),
        @Type(name = "DELETE_ENTITIES", value = EntitiesDeletionHousekeeperTask.class),
        @Type(name = "DELETE_ALARMS", value = AlarmsDeletionHousekeeperTask.class),
        @Type(name = "UNASSIGN_ALARMS", value = AlarmsUnassignHousekeeperTask.class)
})
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HousekeeperTask implements Serializable {

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

    public static HousekeeperTask deleteTenantEntities(TenantId tenantId, EntityType entityType) {
        return new TenantEntitiesDeletionHousekeeperTask(tenantId, entityType);
    }

    public static HousekeeperTask deleteCalculatedFields(TenantId tenantId, EntityId entityId) {
        return new HousekeeperTask(tenantId, entityId, HousekeeperTaskType.DELETE_CALCULATED_FIELDS);
    }

    public static HousekeeperTask deleteJobs(TenantId tenantId, EntityId entityId) {
        return new HousekeeperTask(tenantId, entityId, HousekeeperTaskType.DELETE_JOBS);
    }

    @JsonIgnore
    public String getDescription() {
        return taskType.getDescription() + " for " + entityId.getEntityType().getNormalName().toLowerCase() + " " + entityId.getId();
    }

}
