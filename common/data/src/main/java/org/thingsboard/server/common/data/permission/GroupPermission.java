// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.permission;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GroupPermission extends BaseData<GroupPermissionId> implements HasName, TenantEntity {

    private static final long serialVersionUID = 5582010124562018986L;

    public static final Map<Resource, List<Operation>> ALL_PERMISSIONS = new HashMap<>();
    static {
        ALL_PERMISSIONS.put(Resource.ALL, Collections.singletonList(Operation.ALL));
    }

    public static final List<Operation> READ_ONLY_GROUP_PERMISSIONS = Arrays.asList(
            Operation.READ,
            Operation.RPC_CALL,
            Operation.READ_CREDENTIALS,
            Operation.READ_ATTRIBUTES,
            Operation.READ_TELEMETRY
    );

    public static final List<Operation> TENANT_READ_ONLY_GROUP_PERMISSIONS = new ArrayList<>(READ_ONLY_GROUP_PERMISSIONS);
    static {
        TENANT_READ_ONLY_GROUP_PERMISSIONS.add(Operation.READ_CALCULATED_FIELD);
    }

    public static final List<Operation> WRITE_GROUP_PERMISSIONS = Collections.singletonList(Operation.ALL);

    public static final Map<Resource, List<Operation>> TENANT_READ_ONLY_USER_PERMISSIONS = new HashMap<>();
    static {
        TENANT_READ_ONLY_USER_PERMISSIONS.put(Resource.ALL, TENANT_READ_ONLY_GROUP_PERMISSIONS);
        TENANT_READ_ONLY_USER_PERMISSIONS.put(Resource.PROFILE, Arrays.asList(Operation.ALL));
    }

    public static final Map<Resource, List<Operation>> READ_ONLY_USER_PERMISSIONS = new HashMap<>();
    static {
        READ_ONLY_USER_PERMISSIONS.put(Resource.ALL, READ_ONLY_GROUP_PERMISSIONS);
        READ_ONLY_USER_PERMISSIONS.put(Resource.PROFILE, Arrays.asList(Operation.ALL));
    }

    public static final Map<Resource, List<Operation>> PUBLIC_USER_PERMISSIONS = new HashMap<>();
    static {
        PUBLIC_USER_PERMISSIONS.put(Resource.DASHBOARD, Arrays.asList(
                Operation.READ
        ));
        PUBLIC_USER_PERMISSIONS.put(Resource.WIDGETS_BUNDLE, Arrays.asList(
                Operation.READ
        ));
        PUBLIC_USER_PERMISSIONS.put(Resource.WIDGET_TYPE, Arrays.asList(
                Operation.READ
        ));
        PUBLIC_USER_PERMISSIONS.put(Resource.ALARM, Arrays.asList(
                Operation.READ
        ));
    }

    public static final List<Operation> PUBLIC_USER_ENTITY_GROUP_PERMISSIONS =
            Arrays.asList(Operation.READ, Operation.RPC_CALL, Operation.READ_ATTRIBUTES, Operation.READ_TELEMETRY);

    private TenantId tenantId;
    @Schema(description = "JSON object with the User Group Id. Represents the user group that will have permissions to perform operations against the corresponding entity group.",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private EntityGroupId userGroupId;
    @Schema(description = "JSON object with the Role Id. Represents the set of permissions. " +
            "The role type (GENERIC or GROUP) determines whether 'entityGroupId' is required.",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private RoleId roleId;
    @Schema(description = "JSON object with the Entity Group Id. Required when using a GROUP role — " +
            "specifies the entity group to which the permissions apply. " +
            "Must be null or omitted when using a GENERIC role.")
    private EntityGroupId entityGroupId;
    @Schema(description = "Type of the entities in the group: DEVICE, ASSET, CUSTOMER, etc. " +
            "Auto-populated from the referenced entity group. Null for generic permissions.",
            accessMode = Schema.AccessMode.READ_ONLY)
    private EntityType entityGroupType;
    @Schema(description = "Public or private permissions. Private by default. " +
            "Public permissions are system-managed and cannot be created via the API.",
            example = "false")
    private boolean isPublic;

    public GroupPermission() {
        super();
    }

    public GroupPermission(GroupPermissionId id) {
        super(id);
    }

    public GroupPermission(GroupPermission groupPermission) {
        super(groupPermission);
        this.tenantId = groupPermission.getTenantId();
        this.userGroupId = groupPermission.getUserGroupId();
        this.roleId = groupPermission.getRoleId();
        this.entityGroupId = groupPermission.getEntityGroupId();
        this.entityGroupType = groupPermission.getEntityGroupType();
        this.isPublic = groupPermission.isPublic();
    }

    @Schema(description = "JSON object with the Tenant Id.", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public TenantId getTenantId() {
        return tenantId;
    }

    @Schema(description = "Name of the Group Permissions. Auto-generated", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Override
    public String getName() {
        if (entityGroupId != null && entityGroupType != null) {
            return String.format("GROUP_[%s]_[%s]_[%s]_[%s]", userGroupId != null ?  userGroupId.toString() : "", roleId.toString(), entityGroupId.toString(), entityGroupType.name());
        } else {
            return String.format("GENERIC_[%s]_[%s]", userGroupId != null ?  userGroupId.toString() : "", roleId.toString());
        }
    }

    @Override
    @JsonIgnore
    public EntityType getEntityType() {
        return EntityType.GROUP_PERMISSION;
    }

    @Schema(description = "JSON object with the Group Permission Id. " +
            "Specify this field to update the Group Permission. " +
            "Referencing non-existing Group Permission Id will cause error. " +
            "Omit this field to create new Group Permission." )
    @Override
    public GroupPermissionId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the group permission creation, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

}
