// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.sync.ie.importing.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.UserPermissionsService;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class RoleImportService extends BaseEntityImportService<RoleId, Role, EntityExportData<Role>> {

    private final RoleService roleService;
    private final UserPermissionsService userPermissionsService;

    @Override
    protected void setOwner(TenantId tenantId, Role role, IdProvider idProvider) {
        role.setTenantId(tenantId);
        role.setCustomerId(idProvider.getInternalId(role.getCustomerId()));
    }

    @Override
    protected Role findExistingEntity(EntitiesImportCtx ctx, Role role, IdProvider idProvider) {
        Role existingRole = super.findExistingEntity(ctx, role, idProvider);
        if (existingRole == null && ctx.isFindExistingByName()) {
            var tenantId = ctx.getTenantId();
            if (role.getOwnerId() == null || role.getOwnerId().getEntityType() == EntityType.TENANT) {
                existingRole = roleService.findRoleByTenantIdAndName(tenantId, role.getName()).orElse(null);
            } else {
                existingRole = roleService.findRoleByByTenantIdAndCustomerIdAndName(tenantId,
                        idProvider.getInternalId(role.getCustomerId()), role.getName()).orElse(null);
            }
        }
        return existingRole;
    }

    @Override
    protected Role prepare(EntitiesImportCtx ctx, Role entity, Role oldEntity, EntityExportData<Role> exportData, IdProvider idProvider) {
        return entity;
    }

    @Override
    protected Role deepCopy(Role role) {
        return new Role(role);
    }

    @Override
    protected Role saveOrUpdate(EntitiesImportCtx ctx, Role role, EntityExportData<Role> exportData, IdProvider idProvider, CompareResult compareResult) {
        return roleService.saveRole(ctx.getTenantId(), role);
    }

    @Override
    protected void onEntitySaved(User user, Role savedRole, Role oldRole) throws ThingsboardException {
        logEntityActionService.logEntityAction(savedRole.getTenantId(), savedRole.getId(), savedRole, null,
                oldRole == null ? ActionType.ADDED : ActionType.UPDATED, user);
        userPermissionsService.onRoleUpdated(savedRole);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ROLE;
    }

}
