// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.objects.TelemetryEntityView;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

/**
 * Created by Victor Basanets on 8/27/2017.
 */

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EntityView extends BaseDataWithAdditionalInfo<EntityViewId>
        implements GroupEntity<EntityViewId>, ExportableEntity<EntityViewId>, HasVersion {

    private static final long serialVersionUID = 5582010124562018986L;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "JSON object with the referenced Entity Id (Device or Asset).")
    private EntityId entityId;
    private TenantId tenantId;
    private CustomerId customerId;
    @NoXss
    @Length(fieldName = "name")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Entity View name", example = "A4B72CCDFF33")
    private String name;
    @NoXss
    @Length(fieldName = "type")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Device Profile Name", example = "Temperature Sensor")
    private String type;
    @Schema(description = "Set of telemetry and attribute keys to expose via Entity View.")
    private TelemetryEntityView keys;
    @Schema(description = "Represents the start time of the interval that is used to limit access to target device telemetry. Customer will not be able to see entity telemetry that is outside the specified interval;")
    private long startTimeMs;
    @Schema(description = "Represents the end time of the interval that is used to limit access to target device telemetry. Customer will not be able to see entity telemetry that is outside the specified interval;")
    private long endTimeMs;

    private EntityViewId externalId;
    private Long version;

    public EntityView() {
        super();
    }

    public EntityView(EntityViewId id) {
        super(id);
    }

    public EntityView(EntityView entityView) {
        super(entityView);
        this.entityId = entityView.getEntityId();
        this.tenantId = entityView.getTenantId();
        this.customerId = entityView.getCustomerId();
        this.name = entityView.getName();
        this.type = entityView.getType();
        this.keys = entityView.getKeys();
        this.startTimeMs = entityView.getStartTimeMs();
        this.endTimeMs = entityView.getEndTimeMs();
        this.externalId = entityView.getExternalId();
        this.version = entityView.getVersion();
    }

    @Schema(description = "JSON object with Customer Id. Optional on create: when omitted, defaults to the owner of the target Entity Group or to the current Customer user. Cannot be changed on update via this endpoint; use the Owner API (changeOwnerToCustomer) to re-assign an existing Entity View.")
    @Override
    public CustomerId getCustomerId() {
        return customerId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Schema(description = "JSON object with Tenant Id.", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public TenantId getTenantId() {
        return tenantId;
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

    @Override
    @JsonIgnore
    public EntityType getEntityType() {
        return EntityType.ENTITY_VIEW;
    }


    @Schema(description = "JSON object with the Entity View Id. " +
            "Specify this field to update the Entity View. " +
            "Referencing non-existing Entity View Id will cause error. " +
            "Omit this field to create new Entity View.")
    @Override
    public EntityViewId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the Entity View creation, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Schema(description = "Additional parameters of the entity view. " +
            "May include: 'description' (string).",
            implementation = com.fasterxml.jackson.databind.JsonNode.class,
            example = "{\"description\":\"Temperature readings view\"}")
    @Override
    public JsonNode getAdditionalInfo() {
        return super.getAdditionalInfo();
    }

}
