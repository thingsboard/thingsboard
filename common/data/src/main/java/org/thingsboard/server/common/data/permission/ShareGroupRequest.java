// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.permission;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RoleId;

import java.util.List;

@Schema
@Data
public class ShareGroupRequest {

    @Schema(description = "In case 'allUserGroup' is set to true, " +
            "this property specifies the owner of the user group 'All'. Either Tenant or Customer Id.")
    private final EntityId ownerId;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Indicate that the group should be shared with user group 'All' " +
            "that belongs to Tenant or Customer (see 'ownerId' property description).", name = "")
    private final boolean allUserGroup;

    @Schema(description = "In case 'allUserGroup' is set to false, " +
            "this property specifies the specific user group that the entity group should be shared with.")
    private final EntityGroupId userGroupId;

    @Schema(description = "Used if 'roleIds' property is not present. " +
            "if the value is 'true', creates role with read-only permissions. If the value is 'false', creates role with write permissions.")
    private final boolean readElseWrite;

    @Schema(description = "List of group role Ids that should be used to share the entity group with the user group. " +
            "If not set, the platform will create new role (see 'readElseWrite' property description)")
    private final List<RoleId> roleIds;

}
