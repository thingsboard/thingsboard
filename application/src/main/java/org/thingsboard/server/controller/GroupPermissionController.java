// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.GroupPermissionInfo;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.ENTITY_GROUP_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.GROUP_PERMISSION_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_DELETE_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
public class GroupPermissionController extends BaseController {

    public static final String GROUP_PERMISSION_ID = "groupPermissionId";
    public static final String GROUP_PERMISSION_DESCRIPTION = "Group permission entity represents list of allowed operations for certain User Group to perform against certain Entity Group. " +
            "Basically, this entity wires three other entities: \n\n" +
            " * Role that defines set of allowed operations;\n" +
            " * User Group that defines set of users who may perform the operations; \n" +
            " * Entity Group that defines set of entities which will be accessible to users;\n\n" +
            "There are two types of group permissions depending on the Role type: \n\n" +
            " * **Generic permission** — uses a GENERIC role. The 'entityGroupId' field must be null (or omitted). " +
            "Grants the role's operations to all entities within the owner's scope " +
            "(all tenant entities for tenant admins, only customer-owned entities for customer users).\n" +
            " * **Group permission** — uses a GROUP role. The 'entityGroupId' field must reference a valid entity group. " +
            "Grants the role's operations only to entities in the specified group.\n\n" +
            "Assigning a GENERIC role with a non-null 'entityGroupId' will cause an error. " +
            "The 'entityGroupType' field is auto-populated from the referenced entity group and should not be set manually. " +
            "Duplicate permissions (same userGroupId + roleId + entityGroupId combination) are rejected.\n\n";

    public static final String GROUP_PERMISSION_INFO_DESCRIPTION = GROUP_PERMISSION_DESCRIPTION + " Group Permission Info object extends the Group Permissions with the full information about Role and User and/or Entity Groups. ";

    @ApiOperation(value = "Get Group Permission (getGroupPermissionById)",
            notes = "Fetch the Group Permission object based on the provided Group Permission Id. " +
                    GROUP_PERMISSION_DESCRIPTION + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/groupPermission/{groupPermissionId}")
    public GroupPermission getGroupPermissionById(
            @Parameter(description = GROUP_PERMISSION_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(GROUP_PERMISSION_ID) String strGroupPermissionId) throws ThingsboardException {
        checkParameter(GROUP_PERMISSION_ID, strGroupPermissionId);
        return checkGroupPermissionId(new GroupPermissionId(toUUID(strGroupPermissionId)), Operation.READ);
    }

    @ApiOperation(value = "Get Group Permission Info (getGroupPermissionInfoById)",
            notes = "Fetch the Group Permission Info object based on the provided Group Permission Id and the flag that controls what additional information to load: User or Entity Group. " +
                    GROUP_PERMISSION_INFO_DESCRIPTION + RBAC_READ_CHECK
    )
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/groupPermission/info/{groupPermissionId}")
    public GroupPermissionInfo getGroupPermissionInfoById(
            @Parameter(description = GROUP_PERMISSION_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(GROUP_PERMISSION_ID) String strGroupPermissionId,
            @Parameter(description = "Load additional information about User('true') or Entity Group('false).", required = true)
            @RequestParam boolean isUserGroup) throws ThingsboardException {
        checkParameter(GROUP_PERMISSION_ID, strGroupPermissionId);
        return checkGroupPermissionInfoId(new GroupPermissionId(toUUID(strGroupPermissionId)), Operation.READ, isUserGroup);
    }

    @ApiOperation(value = "Create Or Update Group Permission (saveGroupPermission)",
            notes = "Creates or Updates the Group Permission. When creating group permission, platform generates Group Permission Id as " + UUID_WIKI_LINK +
                    "The newly created Group Permission id will be present in the response. " +
                    "Specify existing Group Permission id to update the permission. " +
                    "Referencing non-existing Group Permission Id will cause 'Not Found' error." +
                    "\n\n" + GROUP_PERMISSION_DESCRIPTION + ControllerConstants.RBAC_WRITE_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/groupPermission")
    public GroupPermission saveGroupPermission(
            @Parameter(description = "A JSON value representing the group permission.", required = true)
            @RequestBody GroupPermission groupPermission) throws ThingsboardException {
        try {
            groupPermission.setTenantId(getCurrentUser().getTenantId());

            GroupPermission oldGroupPermission = null;
            if (groupPermission.getId() == null) {
                accessControlService
                        .checkPermission(getCurrentUser(), Resource.GROUP_PERMISSION, Operation.CREATE, null, groupPermission);
            } else {
                oldGroupPermission = checkGroupPermissionId(groupPermission.getId(), Operation.WRITE);
            }

            if (groupPermission.isPublic()) {
                throw permissionDenied();
            }

            Role role = checkRoleId(groupPermission.getRoleId(), Operation.READ);
            if (groupPermission.getUserGroupId() != null && !groupPermission.getUserGroupId().isNullUid()) {
                checkEntityGroupId(groupPermission.getUserGroupId(), Operation.WRITE);
            }
            if (groupPermission.getEntityGroupId() != null && !groupPermission.getEntityGroupId().isNullUid()) {
                if (role.getType() == RoleType.GENERIC) {
                    throw new IllegalArgumentException("Can't assign Generic Role to entity group!");
                }
                checkEntityGroupId(groupPermission.getEntityGroupId(), Operation.WRITE);
            }

            boolean alreadyAssigned = isAlreadyAssigned(getTenantId(), groupPermission);
            if (alreadyAssigned) {
                throw new ThingsboardException("Such group permission already exists!", ThingsboardErrorCode.INVALID_ARGUMENTS);
            }

            GroupPermission savedGroupPermission = checkNotNull(groupPermissionService.saveGroupPermission(getTenantId(), groupPermission));

            if (oldGroupPermission != null && !oldGroupPermission.getUserGroupId().equals(savedGroupPermission.getUserGroupId())) {
                userPermissionsService.onGroupPermissionUpdated(oldGroupPermission);
            }
            userPermissionsService.onGroupPermissionUpdated(savedGroupPermission);

            logEntityActionService.logEntityAction(getTenantId(), savedGroupPermission.getId(), savedGroupPermission,
                    groupPermission.getId() == null ? ActionType.ADDED : ActionType.UPDATED, getCurrentUser());

            return savedGroupPermission;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(getTenantId(), emptyId(EntityType.GROUP_PERMISSION), groupPermission,
                    groupPermission.getId() == null ? ActionType.ADDED : ActionType.UPDATED, getCurrentUser(), e);
            throw e;
        }
    }

    @ApiOperation(value = "Delete group permission (deleteGroupPermission)",
            notes = "Deletes the group permission. Referencing non-existing group permission Id will cause an error." + "\n\n" + RBAC_DELETE_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @DeleteMapping(value = "/groupPermission/{groupPermissionId}")
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteGroupPermission(
            @Parameter(description = GROUP_PERMISSION_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(GROUP_PERMISSION_ID) String strGroupPermissionId) throws ThingsboardException {
        checkParameter(GROUP_PERMISSION_ID, strGroupPermissionId);
        try {
            GroupPermissionId groupPermissionId = new GroupPermissionId(toUUID(strGroupPermissionId));
            GroupPermission groupPermission = checkGroupPermissionId(groupPermissionId, Operation.DELETE);
            if (groupPermission.isPublic()) {
                throw permissionDenied();
            }

            checkRoleId(groupPermission.getRoleId(), Operation.READ);
            if (groupPermission.getUserGroupId() != null && !groupPermission.getUserGroupId().isNullUid()) {
                checkEntityGroupId(groupPermission.getUserGroupId(), Operation.WRITE);
            }
            if (groupPermission.getEntityGroupId() != null && !groupPermission.getEntityGroupId().isNullUid()) {
                checkEntityGroupId(groupPermission.getEntityGroupId(), Operation.WRITE);
            }

            groupPermissionService.deleteGroupPermission(getTenantId(), groupPermissionId);
            userPermissionsService.onGroupPermissionDeleted(groupPermission);

            logEntityActionService.logEntityAction(getTenantId(), groupPermissionId, groupPermission,
                    ActionType.DELETED, getCurrentUser(), strGroupPermissionId);
        } catch (Exception e) {
            logEntityActionService.logEntityAction(getTenantId(), emptyId(EntityType.GROUP_PERMISSION),
                    ActionType.DELETED, getCurrentUser(), e, strGroupPermissionId);
            throw e;
        }
    }

    @ApiOperation(value = "Get group permissions by User Group Id (getUserGroupPermissions)",
            notes = "Returns a list of group permission objects that belongs to specified User Group Id. " +
                    GROUP_PERMISSION_INFO_DESCRIPTION + "\n\n" + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/userGroup/{userGroupId}/groupPermissions")
    public List<GroupPermissionInfo> getUserGroupPermissions(
            @Parameter(description = ENTITY_GROUP_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable("userGroupId") String strUserGroupId) throws ThingsboardException, ExecutionException, InterruptedException {
        TenantId tenantId = getCurrentUser().getTenantId();
        EntityGroupId userGroupId = new EntityGroupId(UUID.fromString(strUserGroupId));
        checkEntityGroupId(userGroupId, Operation.READ);
        accessControlService.checkPermission(getCurrentUser(), Resource.GROUP_PERMISSION, Operation.READ);
        List<GroupPermissionInfo> groupPermissions = groupPermissionService.findGroupPermissionInfoListByTenantIdAndUserGroupIdAsync(tenantId, userGroupId).get();
        return applyPermissionInfo(groupPermissions);
    }

    @ApiOperation(value = "Load User Group Permissions (loadUserGroupPermissionInfos)",
            notes = "Enrich a list of group permission objects with the information about Role, User and Entity Groups. " +
                    GROUP_PERMISSION_INFO_DESCRIPTION + "\n\n" + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/userGroup/groupPermissions/info")
    public List<GroupPermissionInfo> loadUserGroupPermissionInfos(
            @Parameter(description = "JSON array of group permission objects", required = true)
            @RequestBody List<GroupPermission> permissions) throws ThingsboardException, ExecutionException, InterruptedException {
        TenantId tenantId = getCurrentUser().getTenantId();
        accessControlService.checkPermission(getCurrentUser(), Resource.GROUP_PERMISSION, Operation.READ);
        List<GroupPermissionInfo> permissionInfoList = groupPermissionService.loadUserGroupPermissionInfoListAsync(tenantId, permissions).get();
        return applyPermissionInfo(permissionInfoList);
    }

    @ApiOperation(value = "Get group permissions by Entity Group Id (getEntityGroupPermissions)",
            notes = "Returns a list of group permission objects that is assigned for the specified Entity Group Id. " +
                    GROUP_PERMISSION_INFO_DESCRIPTION + "\n\n" + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/entityGroup/{entityGroupId}/groupPermissions")
    public List<GroupPermissionInfo> getEntityGroupPermissions(
            @Parameter(description = ENTITY_GROUP_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable("entityGroupId") String strEntityGroupId) throws ThingsboardException, ExecutionException, InterruptedException {
        TenantId tenantId = getCurrentUser().getTenantId();
        EntityGroupId entityGroupId = new EntityGroupId(UUID.fromString(strEntityGroupId));
        checkEntityGroupId(entityGroupId, Operation.READ);
        accessControlService.checkPermission(getCurrentUser(), Resource.GROUP_PERMISSION, Operation.READ);
        List<GroupPermissionInfo> groupPermissions = groupPermissionService.findGroupPermissionInfoListByTenantIdAndEntityGroupIdAsync(tenantId, entityGroupId).get();
        if (getCurrentUser().isCustomerUser()) {
            var customerId = getCurrentUser().getCustomerId();
            Set<EntityId> owners = ownersCacheService.getChildOwners(tenantId, customerId);
            groupPermissions = groupPermissions.stream().filter(gp -> owners.contains(gp.getUserGroupOwnerId())).toList();
        }
        return applyPermissionInfo(groupPermissions);
    }

    private List<GroupPermissionInfo> applyPermissionInfo(List<GroupPermissionInfo> groupPermissions) throws ThingsboardException {
        groupPermissions = groupPermissions.stream().filter(gp -> gp != null && gp.getRole() != null).collect(Collectors.toList());
        for (GroupPermissionInfo groupPermission : groupPermissions) {
            Role role = groupPermission.getRole();
            groupPermission.setReadOnly(!accessControlService.hasPermission(getCurrentUser(), Resource.ROLE, Operation.READ, role.getId(), role));
            if (groupPermission.isPublic()) {
                groupPermission.setReadOnly(true);
            }
        }
        return groupPermissions;
    }

    private boolean isAlreadyAssigned(TenantId tenantId, GroupPermission groupPermission) {
        if (groupPermission.getEntityGroupId() != null) {
            return groupPermissionService.findGroupPermissionByTenantIdAndEntityGroupIdAndUserGroupIdAndRoleId(tenantId,
                    groupPermission.getEntityGroupId(), groupPermission.getUserGroupId(), groupPermission.getRoleId(), new PageLink(1)).getTotalElements() > 0;
        } else {
            return groupPermissionService.findGroupPermissionByTenantIdAndUserGroupIdAndRoleId(tenantId,
                    groupPermission.getUserGroupId(), groupPermission.getRoleId(), new PageLink(1)).getTotalElements() > 0;
        }
    }

}
