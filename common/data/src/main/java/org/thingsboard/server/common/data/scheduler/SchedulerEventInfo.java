// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.scheduler;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasOwnerId;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serial;

@Schema
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class SchedulerEventInfo extends BaseDataWithAdditionalInfo<SchedulerEventId> implements HasName, TenantEntity, HasCustomerId, HasOwnerId, HasVersion, ExportableEntity<SchedulerEventId> {

    @Serial
    private static final long serialVersionUID = 2807343040519549363L;

    @Schema(description = "JSON object with Tenant Id", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;
    @Schema(description = "JSON object with Customer Id. Optional: when omitted the Scheduler Event is owned by the tenant. When the request is made by a Customer user, the value is forced to the user's own Customer Id.")
    private CustomerId customerId;
    @Schema(description = "JSON object with Originator Id", accessMode = Schema.AccessMode.READ_ONLY)
    private EntityId originatorId;
    @NoXss
    @Length(fieldName = "name")
    @Schema(description = "scheduler event name", example = "Weekly Dashboard Report")
    private String name;
    @Schema(description = "scheduler event type", example = "generateReport")
    @NoXss
    @Length(fieldName = "type")
    private String type;
    @Schema(description = "a JSON value with schedule time configuration", implementation = com.fasterxml.jackson.databind.JsonNode.class)
    private JsonNode schedule;

    @Schema(description = "Enable/disable scheduler", example = "true")
    @Length(fieldName = "enabled")
    private boolean enabled = true;

    private SchedulerEventId externalId;

    private Long version;

    public SchedulerEventInfo() {
        super();
    }

    public SchedulerEventInfo(SchedulerEventId id) {
        super(id);
    }

    public SchedulerEventInfo(SchedulerEventInfo schedulerEventInfo) {
        super(schedulerEventInfo);
        this.tenantId = schedulerEventInfo.getTenantId();
        this.customerId = schedulerEventInfo.getCustomerId();
        this.originatorId = schedulerEventInfo.getOriginatorId();
        this.name = schedulerEventInfo.getName();
        this.type = schedulerEventInfo.getType();
        this.enabled = schedulerEventInfo.isEnabled();
        this.setSchedule(schedulerEventInfo.getSchedule());
        this.version = schedulerEventInfo.getVersion();
        this.externalId = schedulerEventInfo.getExternalId();
    }

    @Schema(description = "JSON object with the scheduler event Id. " +
            "Specify this field to update the scheduler event. " +
            "Referencing non-existing scheduler event Id will cause error. " +
            "Omit this field to create new scheduler event")
    @Override
    public SchedulerEventId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the scheduler event creation, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Schema(description = "Additional parameters of the scheduler event", implementation = com.fasterxml.jackson.databind.JsonNode.class)
    @Override
    public JsonNode getAdditionalInfo() {
        return super.getAdditionalInfo();
    }

    @Override
    public String getName() {
        return name;
    }

    @Schema(description = "JSON object with Customer or Tenant Id", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public EntityId getOwnerId() {
        return customerId != null && !customerId.isNullUid() ? customerId : tenantId;
    }

    @Override
    public void setOwnerId(EntityId entityId) {
        if (EntityType.CUSTOMER.equals(entityId.getEntityType())) {
            this.customerId = new CustomerId(entityId.getId());
        } else {
            this.customerId = new CustomerId(CustomerId.NULL_UUID);
        }
    }

    @JsonIgnore
    public SchedulerEventDescriptor toDescriptor() {
        long startTime = schedule.get("startTime").asLong();
        String timezone = schedule.get("timezone").asText();
        JsonNode repeatNode = schedule.get("repeat");
        SchedulerRepeat repeat = null;
        if (repeatNode != null) {
            try {
                repeat = mapper.treeToValue(repeatNode, SchedulerRepeat.class);
            } catch (Exception e) {
                log.error("Failed to read scheduler config for {}", this, e);
            }
        }
        return new SchedulerEventDescriptor(startTime, timezone, repeat);
    }

    @Override
    @JsonIgnore
    public EntityType getEntityType() {
        return EntityType.SCHEDULER_EVENT;
    }

}
