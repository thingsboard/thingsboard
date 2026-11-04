// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.permission.AllowedPermissionsInfo;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.thingsboard.server.controller.ControllerConstants.ENTITY_ID;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_TYPE;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_TYPE_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.OPERATION;
import static org.thingsboard.server.controller.ControllerConstants.OPERATION_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_READ_CHECK;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
public class UserPermissionsController extends BaseController {

    @ApiOperation(value = "Get Permissions (getAllowedPermissions)",
            notes = "Returns a complex object that describes:\n\n" +
                    " * all possible (both granted and not granted) permissions for the authority of the user (Tenant or Customer);\n" +
                    " * all granted permissions for the user;\n\n " +
                    "The result impacts UI behavior and hides certain UI elements if user has no permissions to invoke the related operations. " +
                    "Nevertheless, all API calls check the permissions each time they are executed on the server side." +
                    PAGE_DATA_PARAMETERS + "\n\n" + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/permissions/allowedPermissions")
    public AllowedPermissionsInfo getAllowedPermissions() throws ThingsboardException {
        Set<Resource> allowedResources = Resource.resourcesByAuthority.get(getCurrentUser().getAuthority());
        Map<Resource, Set<Operation>> operationsByResource = new HashMap<>();
        allowedResources.forEach(resource -> operationsByResource.put(resource, Resource.operationsByResource.get(resource)));
        return new AllowedPermissionsInfo(operationsByResource,
                Operation.allowedForGroupRoleOperations,
                Operation.allowedForGroupOwnerOnlyOperations,
                Operation.allowedForGroupOwnerOnlyGroupOperations,
                allowedResources, getCurrentUser().getUserPermissions(), getCurrentUser().getOwnerId());
    }

    @ApiOperation(value = "Check permission for specified entity (hasEntityPermission)",
            notes = "Returns true if the user has permission to perform the operation, and false otherwise.\n")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/permission/{entityType}/{entityId}/{operation}")
    public boolean hasEntityPermission(
            @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, schema = @Schema(defaultValue = "DEVICE"))
            @PathVariable(ENTITY_TYPE) EntityType entityType,
            @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_ID) String strEntityId,
            @Parameter(description = OPERATION_PARAM_DESCRIPTION, required = true, schema = @Schema(allowableValues = {"ALL", "CREATE", "READ", "WRITE", "DELETE",
                    "RPC_CALL", "READ_CREDENTIALS", "WRITE_CREDENTIALS", "READ_ATTRIBUTES", "WRITE_ATTRIBUTES", "READ_TELEMETRY", "WRITE_TELEMETRY","ADD_TO_GROUP",
                    "REMOVE_FROM_GROUP", "CHANGE_OWNER", "IMPERSONATE", "CLAIM_DEVICES", "SHARE_GROUP", "ASSIGN_TO_TENANT", "READ_CALCULATED_FIELD", "WRITE_CALCULATED_FIELD"}))
            @PathVariable(OPERATION) Operation operation) {
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, strEntityId);
        try {
            checkEntityId(entityId, operation);
        } catch (ThingsboardException e) {
            return false;
        }
        return true;
    }

}
