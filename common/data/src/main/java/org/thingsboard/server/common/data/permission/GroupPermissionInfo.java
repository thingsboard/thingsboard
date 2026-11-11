// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.permission;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.role.Role;

import java.io.Serial;

@Data
public class GroupPermissionInfo extends GroupPermission {

    @Serial
    private static final long serialVersionUID = 2807343092519543363L;

    @Schema(description = "Represent set of permissions.")
    private Role role;

    @Schema(description = "Entity Group Name.")
    private String entityGroupName;
    @Schema(description = "Entity Group Owner Id (Tenant or Customer).")
    private EntityId entityGroupOwnerId;
    @Schema(description = "Name of the entity group owner (Tenant or Customer title).")
    private String entityGroupOwnerName;

    @Schema(description = "User Group Name.")
    private String userGroupName;
    @Schema(description = "User Group Owner Id (Tenant or Customer).")
    private EntityId userGroupOwnerId;
    @Schema(description = "Name of the user group owner (Tenant or Customer title).")
    private String userGroupOwnerName;

    @Schema(description = "Shortcut to check if read operations allowed.")
    private boolean isReadOnly;

    public GroupPermissionInfo() {
        super();
    }

    public GroupPermissionInfo(GroupPermission groupPermission) {
        super(groupPermission);
    }

}
