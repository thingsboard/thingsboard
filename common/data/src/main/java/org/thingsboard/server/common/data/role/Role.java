// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.role;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Role extends BaseDataWithAdditionalInfo<RoleId> implements HasName, TenantEntity, HasCustomerId, HasOwnerId, ExportableEntity<RoleId>, HasVersion {

    private static final long serialVersionUID = 5582010124562018986L;

    public static final String ROLE_TENANT_ADMIN_NAME = "Tenant Administrator";
    public static final String ROLE_TENANT_USER_NAME = "Tenant User";
    public static final String ROLE_CUSTOMER_ADMIN_NAME = "Customer Administrator";
    public static final String ROLE_CUSTOMER_USER_NAME = "Customer User";
    public static final String ROLE_PUBLIC_USER_NAME = "Public User";
    public static final String ROLE_PUBLIC_USER_ENTITY_GROUP_NAME = "Entity Group Public User";
    public static final String ROLE_READ_ONLY_ENTITY_GROUP_NAME = "Entity Group Read-only User";
    public static final String ROLE_WRITE_ENTITY_GROUP_NAME = "Entity Group Write User";

    @Schema(description = "JSON object with Tenant Id.", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;
    @Schema(description = "JSON object with Customer Id. Optional: when omitted the Role is owned by the tenant. When the request is made by a Customer user, the value is forced to the user's own Customer Id.")
    private CustomerId customerId;
    @NoXss
    @Length(fieldName = "name")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Role Name", example = "Read-Only")
    private String name;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Type of the role: generic or group", example = "GROUP")
    private RoleType type;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
            description = "Set of permissions granted by this role. The JSON shape depends on the role 'type':\n\n" +
                          "* GENERIC — JSON object mapping `Resource` enum names to arrays of `Operation` enum names allowed on that resource. " +
                          "The wildcard entry `{\"ALL\":[\"ALL\"]}` grants every operation on every resource.\n\n" +
                          "* GROUP — JSON array of `Operation` enum names that apply to the entity group this role is bound to via `GroupPermission.entityGroupId`. " +
                          "Only operations with `allowedForGroupRole=true` may appear (see `Operation` enum). The wildcard entry `[\"ALL\"]` grants every supported operation on the bound entity group.",
            example = "{\"DEVICE\":[\"READ\",\"WRITE\",\"READ_TELEMETRY\"],\"DASHBOARD\":[\"READ\"]}")
    private JsonNode permissions;
    @Schema(description = "Operations to subtract from those granted by `permissions`. " +
                          "Only applicable to GENERIC roles — setting this on a GROUP role is rejected by validation. " +
                          "Same shape as the GENERIC variant of `permissions`: a JSON object mapping `Resource` enum names to non-empty arrays of `Operation` enum names. " +
                          "At evaluation time, for each resource the listed operations are removed from the resolved permission set " +
                          "(e.g. `permissions={\"ALL\":[\"ALL\"]}` combined with `excludedPermissions={\"DEVICE\":[\"DELETE\"]}` grants everything except deleting devices). " +
                          "May be null or an empty object when no exclusions apply.",
            example = "{\"DEVICE\":[\"DELETE\"]}")
    private JsonNode excludedPermissions;

    private RoleId externalId;
    private Long version;

    public Role() {
        super();
    }

    public Role(RoleId id) {
        super(id);
    }

    public Role(Role role) {
        super(role);
        setPermissions(role.getPermissions());
        setExcludedPermissions(role.getExcludedPermissions());
        this.externalId = role.getExternalId();
        this.version = role.getVersion();
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
        return EntityType.ROLE;
    }

    @Schema(description = "JSON object with the Role Id. " +
                          "Specify this field to update the Role. " +
                          "Referencing non-existing Role Id will cause error. " +
                          "Omit this field to create new Role.")
    @Override
    public RoleId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the role creation, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Schema(description = "Additional parameters of the role. " +
                          "May include: 'description' (string).",
            implementation = com.fasterxml.jackson.databind.JsonNode.class,
            example = "{\"description\":\"Administrator role\"}")
    @Override
    public JsonNode getAdditionalInfo() {
        return super.getAdditionalInfo();
    }

}
