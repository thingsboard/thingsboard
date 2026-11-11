// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.permission;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.Map;
import java.util.Set;

@Schema
@Data
@AllArgsConstructor
public class AllowedPermissionsInfo {

    @Schema(description = "Static map (vocabulary) of allowed operations by resource type")
    private Map<Resource, Set<Operation>> operationsByResource;
    @Schema(description = "Static set (vocabulary) of allowed operations for group roles")
    private Set<Operation> allowedForGroupRoleOperations;
    @Schema(description = "Static set (vocabulary) of allowed operations for group owner")
    private Set<Operation> allowedForGroupOwnerOnlyOperations;
    @Schema(description = "Static set (vocabulary) of allowed group operations for group owner")
    private Set<Operation> allowedForGroupOwnerOnlyGroupOperations;
    @Schema(description = "Static set (vocabulary) of all possibly allowed resources. Static and depends only on the authority of the user")
    private Set<Resource> allowedResources;
    @Schema(description = "JSON object with merged permission for all generic and group roles assigned to all user groups the user belongs to")
    private MergedUserPermissions userPermissions;
    @Schema(description = "Owner Id of the user (Tenant or Customer)")
    private EntityId userOwnerId;

}
