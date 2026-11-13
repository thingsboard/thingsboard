// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
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
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_END;
import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_START;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_DELETE_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.ROLE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ROLE_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
public class RoleController extends AutoCommitController {

    public static final String ROLE_ID = "roleId";
    public static final String ROLE_SHORT_DESCRIPTION = "Role Contains a set of permissions. Role has two types. " +
            "Generic Role may be assigned to the user group and will provide permissions for all entities of a certain type. " +
            "Group Role may be assigned to both user and entity group and will provides permissions only for the entities that belong to specified entity group. " +
            "The assignment of the Role to the User Group is done using [Group Permission Controller](/swagger-ui.html#/group-permission-controller).";

    public static final String ROLE_PERMISSIONS_DESCRIPTION = "Example of Generic Role with read-only permissions for any resource and all permissions for the 'DEVICE' and 'PROFILE' resources is listed below: \n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"name\": \"Read-Only User\",\n" +
            "  \"type\": \"GENERIC\",\n" +
            "  \"permissions\": {\n" +
            "    \"ALL\": [\n" +
            "      \"READ\",\n" +
            "      \"RPC_CALL\",\n" +
            "      \"READ_CREDENTIALS\",\n" +
            "      \"READ_ATTRIBUTES\",\n" +
            "      \"READ_TELEMETRY\"\n" +
            "    ],\n" +
            "    \"DEVICE\": [\n" +
            "      \"ALL\"\n" +
            "    ]\n" +
            "    \"PROFILE\": [\n" +
            "      \"ALL\"\n" +
            "    ]\n" +
            "  },\n" +
            "  \"additionalInfo\": {\n" +
            "    \"description\": \"Read-only permissions for everything, Write permissions for devices and own profile.\"\n" +
            "  }\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END +
            "\n\nExample of Group Role with read-only permissions. Note that the group role has no association with the resources. The type of the resource is taken from the entity group that this role is assigned to: \n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"name\": \"Entity Group Read-only User\",\n" +
            "  \"type\": \"GROUP\",\n" +
            "  \"permissions\": [\n" +
            "    \"READ\",\n" +
            "    \"RPC_CALL\",\n" +
            "    \"READ_CREDENTIALS\",\n" +
            "    \"READ_ATTRIBUTES\",\n" +
            "    \"READ_TELEMETRY\"\n" +
            "  ],\n" +
            "  \"additionalInfo\": {\n" +
            "    \"description\": \"Read-only permissions.\"\n" +
            "  }\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END + "\n\n";

    @ApiOperation(value = "Get Role by Id (getRoleById)",
            notes = "Fetch the Role object based on the provided Role Id. " +
                    ROLE_SHORT_DESCRIPTION + RBAC_READ_CHECK
    )
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/role/{roleId}")
    public Role getRoleById(
            @Parameter(description = ROLE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ROLE_ID) String strRoleId) throws ThingsboardException {
        checkParameter(ROLE_ID, strRoleId);
        return checkRoleId(new RoleId(toUUID(strRoleId)), Operation.READ);
    }

    @ApiOperation(value = "Create Or Update Role (saveRole)",
            notes = "Creates or Updates the Role. When creating Role, platform generates Role Id as " + UUID_WIKI_LINK +
                    "The newly created Role id will be present in the response. " +
                    "Specify existing Role id to update the permission. " +
                    "Referencing non-existing Group Permission Id will cause 'Not Found' error." +
                    "\n\n" + ROLE_SHORT_DESCRIPTION + "\n\n" + ROLE_PERMISSIONS_DESCRIPTION +
                    ControllerConstants.RBAC_WRITE_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/role")
    public Role saveRole(
            @Parameter(description = "A JSON value representing the role.", required = true)
            @RequestBody Role role) throws Exception {
        SecurityUser currentUser = getCurrentUser();
        try {
            role.setTenantId(currentUser.getTenantId());
            if (Authority.CUSTOMER_USER.equals(currentUser.getAuthority())) {
                role.setCustomerId(currentUser.getCustomerId());
            }
            checkEntity(role.getId(), role, Resource.ROLE);

            Role savedRole = checkNotNull(roleService.saveRole(getTenantId(), role));

            autoCommit(currentUser, savedRole.getId());

            userPermissionsService.onRoleUpdated(savedRole);

            logEntityActionService.logEntityAction(getTenantId(), savedRole.getId(), savedRole,
                    role.getId() == null ? ActionType.ADDED : ActionType.UPDATED, currentUser);

            return savedRole;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(getTenantId(), emptyId(EntityType.ROLE), role,
                    role.getId() == null ? ActionType.ADDED : ActionType.UPDATED, currentUser, e);
            throw e;
        }
    }

    @ApiOperation(value = "Delete role (deleteRole)",
            notes = "Deletes the role. Referencing non-existing role Id will cause an error." + "\n\n" + RBAC_DELETE_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @DeleteMapping(value = "/role/{roleId}")
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteRole(
            @Parameter(description = ROLE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ROLE_ID) String strRoleId) throws Exception {
        checkParameter(ROLE_ID, strRoleId);
        try {
            RoleId roleId = new RoleId(toUUID(strRoleId));
            Role role = checkRoleId(roleId, Operation.DELETE);

            if (isUsed(role.getId(), getTenantId())) {
                throw new ThingsboardException("Role can't be deleted because it used by user group permissions!", ThingsboardErrorCode.INVALID_ARGUMENTS);
            }
            roleService.deleteRole(getTenantId(), roleId);
            logEntityActionService.logEntityAction(getTenantId(), roleId, role, ActionType.DELETED, getCurrentUser(), strRoleId);

        } catch (Exception e) {
            logEntityActionService.logEntityAction(getTenantId(), emptyId(EntityType.ROLE), ActionType.DELETED, getCurrentUser(), e, strRoleId);
            throw e;
        }
    }

    private boolean isUsed(RoleId roleId, TenantId tenantId) {
        return groupPermissionService.findGroupPermissionByTenantIdAndRoleId(tenantId, roleId, new PageLink(1)).getTotalElements() > 0;
    }

    @ApiOperation(value = "Get Roles (getRoles)",
            notes = "Returns a page of roles that are available for the current user. " + ROLE_SHORT_DESCRIPTION +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/roles", params = {"pageSize", "page"})
    public PageData<Role> getRoles(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true, schema = @Schema(minimum = "1"))
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true, schema = @Schema(minimum = "0"))
            @RequestParam int page,
            @Parameter(description = "Type of the role", schema = @Schema(allowableValues = {"GENERIC", "GROUP"}))
            @RequestParam(required = false) String type,
            @Parameter(description = ROLE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "type", "description"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.ROLE, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);

        if (StringUtils.isNotBlank(type)) {
            if (Authority.TENANT_ADMIN.equals(getCurrentUser().getAuthority())) {
                return checkNotNull(roleService.findRolesByTenantIdAndType(tenantId, pageLink, RoleType.valueOf(type)));
            } else {
                return checkNotNull(roleService.findRolesByTenantIdAndCustomerIdAndType(tenantId, getCurrentUser().getCustomerId(), checkStrRoleType("type", type), pageLink));
            }
        } else {
            if (Authority.TENANT_ADMIN.equals(getCurrentUser().getAuthority())) {
                return checkNotNull(roleService.findRolesByTenantId(tenantId, pageLink));
            } else {
                return checkNotNull(roleService.findRolesByTenantIdAndCustomerId(tenantId, getCurrentUser().getCustomerId(), pageLink));
            }
        }
    }

    @Hidden
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/roles", params = {"roleIds"})
    public List<Role> getRolesByIdsV1(
            @RequestParam("roleIds") String[] strRoleIds) throws Exception {
        checkArrayParameter("roleIds", strRoleIds);
        if (!accessControlService.hasPermission(getCurrentUser(), Resource.ROLE, Operation.READ)) {
            return Collections.emptyList();
        }
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        List<RoleId> roleIds = new ArrayList<>();
        for (String strRoleId : strRoleIds) {
            roleIds.add(new RoleId(toUUID(strRoleId)));
        }
        List<Role> roles = checkNotNull(roleService.findRolesByIdsAsync(tenantId, roleIds).get());
        return filterRolesByReadPermission(roles);
    }

    @ApiOperation(value = "Get Roles By Ids (getRolesByIds)",
            notes = "Returns the list of rows based on their ids. " + "\n\n" + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/roles/list")
    public List<Role> getRolesByIds(
            @Parameter(description = "A list of role ids, separated by comma ','", array = @ArraySchema(schema = @Schema(type = "string")))
            @RequestParam("roleIds") String[] strRoleIds) throws Exception {
        return getRolesByIdsV1(strRoleIds);
    }

    private List<Role> filterRolesByReadPermission(List<Role> roles) {
        return roles.stream().filter(role -> {
            try {
                return accessControlService.hasPermission(getCurrentUser(), Resource.ROLE, Operation.READ, role.getId(), role);
            } catch (ThingsboardException e) {
                return false;
            }
        }).toList();
    }

}
