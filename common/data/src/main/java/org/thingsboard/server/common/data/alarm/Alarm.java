/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.common.data.alarm;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serial;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Alarm extends BaseData<AlarmId> implements HasName, HasTenantId, HasCustomerId {

    @Serial
    private static final long serialVersionUID = -1935800187424953611L;

    @Schema(description = "JSON object with Tenant Id", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;

    @Schema(description = "JSON object with Customer Id", accessMode = Schema.AccessMode.READ_ONLY)
    private CustomerId customerId;

    @NoXss
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "representing type of the Alarm", example = "High Temperature Alarm")
    @Length(fieldName = "type")
    private String type;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "JSON object with alarm originator id")
    private EntityId originator;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Alarm severity", example = "CRITICAL")
    private AlarmSeverity severity;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Acknowledged", example = "true")
    private boolean acknowledged;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Cleared", example = "false")
    private boolean cleared;
    @Schema(description = "Alarm assignee user id")
    private UserId assigneeId;
    @Schema(description = "Timestamp of the alarm start time, in milliseconds", example = "1634058704565")
    private long startTs;
    @Schema(description = "Timestamp of the alarm end time(last time update), in milliseconds", example = "1634111163522")
    private long endTs;
    @Schema(description = "Timestamp of the alarm acknowledgement, in milliseconds", example = "1634115221948")
    private long ackTs;
    @Schema(description = "Timestamp of the alarm clearing, in milliseconds", example = "1634114528465")
    private long clearTs;
    @Schema(description = "Timestamp of the alarm assignment, in milliseconds", example = "1634115928465")
    private long assignTs;
    @Schema(description = "JSON object with alarm details")
    private transient JsonNode details;
    @Schema(description = "Propagation flag to specify if alarm should be propagated to parent entities of alarm originator", example = "true")
    private boolean propagate;
    @Schema(description = "Propagation flag to specify if alarm should be propagated to the owner (tenant or customer) of alarm originator", example = "true")
    private boolean propagateToOwner;
    @Schema(description = "Propagation flag to specify if alarm should be propagated to the tenant entity", example = "true")
    private boolean propagateToTenant;
    @Schema(description = "JSON array of relation types that should be used for propagation. " +
            "By default, 'propagateRelationTypes' array is empty which means that the alarm will be propagated based on any relation type to parent entities. " +
            "This parameter should be used only in case when 'propagate' parameter is set to true, otherwise, 'propagateRelationTypes' array will be ignored.")
    private List<String> propagateRelationTypes;

    public Alarm() {
        super();
    }

    public Alarm(AlarmId id) {
        super(id);
    }

    public Alarm(Alarm alarm) {
        super(alarm.getId());
        this.createdTime = alarm.getCreatedTime();
        this.tenantId = alarm.getTenantId();
        this.customerId = alarm.getCustomerId();
        this.type = alarm.getType();
        this.originator = alarm.getOriginator();
        this.severity = alarm.getSeverity();
        this.assigneeId = alarm.getAssigneeId();
        this.startTs = alarm.getStartTs();
        this.endTs = alarm.getEndTs();
        this.acknowledged = alarm.isAcknowledged();
        this.ackTs = alarm.getAckTs();
        this.clearTs = alarm.getClearTs();
        this.cleared = alarm.isCleared();
        this.assignTs = alarm.getAssignTs();
        this.details = alarm.getDetails();
        this.propagate = alarm.isPropagate();
        this.propagateToOwner = alarm.isPropagateToOwner();
        this.propagateToTenant = alarm.isPropagateToTenant();
        this.propagateRelationTypes = alarm.getPropagateRelationTypes();
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "representing type of the Alarm", example = "High Temperature Alarm")
    public String getName() {
        return type;
    }

    @Schema(description = "JSON object with the alarm Id. " +
            "Specify this field to update the alarm. " +
            "Referencing non-existing alarm Id will cause error. " +
            "Omit this field to create new alarm.")
    @Override
    public AlarmId getId() {
        return super.getId();
    }


    @Schema(description = "Timestamp of the alarm creation, in milliseconds", example = "1634058704567", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "status of the Alarm", example = "ACTIVE_UNACK", accessMode = Schema.AccessMode.READ_ONLY)
    public AlarmStatus getStatus() {
        return toStatus(cleared, acknowledged);
    }

    public static AlarmStatus toStatus(boolean cleared, boolean acknowledged) {
        if (cleared) {
            return acknowledged ? AlarmStatus.CLEARED_ACK : AlarmStatus.CLEARED_UNACK;
        } else {
            return acknowledged ? AlarmStatus.ACTIVE_ACK : AlarmStatus.ACTIVE_UNACK;
        }
    }

    @JsonIgnore
    public DashboardId getDashboardId() {
        return Optional.ofNullable(getDetails()).map(details -> details.get("dashboardId"))
                .filter(JsonNode::isTextual).map(id -> new DashboardId(UUID.fromString(id.asText()))).orElse(null);
    }

}
