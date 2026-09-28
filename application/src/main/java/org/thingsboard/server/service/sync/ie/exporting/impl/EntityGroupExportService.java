// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.sync.ie.exporting.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.DeviceGroupOtaPackage;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.sync.ie.EntityGroupExportData;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.ota.DeviceGroupOtaPackageService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.thingsboard.server.service.sync.ie.importing.impl.EntityGroupImportService.CONFIG_PROCESSED_FIELDS_PATTERN;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class EntityGroupExportService extends BaseEntityExportService<EntityGroupId, EntityGroup, EntityGroupExportData> {

    private final GroupPermissionService groupPermissionService;
    private final RoleService roleService;
    private final DeviceGroupOtaPackageService deviceGroupOtaPackageService;
    private final EntityGroupService entityGroupService;

    @Override
    protected void setAdditionalExportData(EntitiesExportCtx<?> ctx, EntityGroup entityGroup, EntityGroupExportData exportData) throws ThingsboardException {
        super.setAdditionalExportData(ctx, entityGroup, exportData);
        var exportSettings = ctx.getSettings();
        exportData.setGroupEntities(exportSettings.isExportGroupEntities());
        if (exportSettings.isEmbedGroupMembers() && exportSettings.isExportGroupEntities() && !entityGroup.isGroupAll()) {
            exportData.setMemberIds(collectMemberIds(ctx, entityGroup));
        }
        if (exportSettings.isExportPermissions() && entityGroup.getType() == EntityType.USER) {
            List<GroupPermission> permissions = groupPermissionService.findGroupPermissionListByTenantIdAndUserGroupId(ctx.getTenantId(), entityGroup.getId()).stream()
                    .filter(permission -> {
                        Role role = roleService.findRoleById(ctx.getTenantId(), permission.getRoleId());
                        return !role.getOwnerId().equals(TenantId.SYS_TENANT_ID);
                    })
                    .peek(permission -> {
                        permission.setUserGroupId(getExternalIdOrElseInternal(ctx, permission.getUserGroupId()));
                        permission.setRoleId(getExternalIdOrElseInternal(ctx, permission.getRoleId()));
                        permission.setEntityGroupId(getExternalIdOrElseInternal(ctx, permission.getEntityGroupId()));
                    })
                    .collect(Collectors.toList());
            exportData.setPermissions(permissions);
        }
        if (entityGroup.getType() == EntityType.DEVICE) {
            List<DeviceGroupOtaPackage> packages = deviceGroupOtaPackageService.findDeviceGroupOtaPackageByGroupId(entityGroup.getId())
                    .stream()
                    .filter(Objects::nonNull)
                    .peek(pkg -> {
                        pkg.setOtaPackageId(getExternalIdOrElseInternal(ctx, pkg.getOtaPackageId()));
                        pkg.setGroupId(getExternalIdOrElseInternal(ctx, pkg.getGroupId()));
                    }).toList();
            if (!packages.isEmpty()) {
                exportData.setGroupOtaPackages(packages);
            }
        }
        replaceUuidsRecursively(ctx, JacksonUtil.getSafely(exportData.getEntity().getConfiguration(), "actions"), Collections.emptySet(), CONFIG_PROCESSED_FIELDS_PATTERN);
    }

    @Override
    protected void setRelatedEntities(EntitiesExportCtx<?> ctx, EntityGroup entityGroup, EntityGroupExportData exportData) {
        if (entityGroup.getOwnerId().getEntityType() == EntityType.CUSTOMER) {
            entityGroup.setOwnerId(getExternalIdOrElseInternal(ctx, entityGroup.getOwnerId()));
        }
    }

    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.ENTITY_GROUP);
    }

    private List<UUID> collectMemberIds(EntitiesExportCtx<?> ctx, EntityGroup entityGroup) {
        List<UUID> memberIds = new ArrayList<>();
        PageLink pageLink = new PageLink(1000);
        PageData<EntityId> page;
        do {
            page = entityGroupService.findEntityIds(ctx.getTenantId(), entityGroup.getType(), entityGroup.getId(), pageLink);
            for (EntityId memberId : page.getData()) {
                EntityId externalId = getExternalIdOrElseInternal(ctx, memberId);
                memberIds.add(externalId.getId());
            }
            pageLink = pageLink.nextPageLink();
        } while (page.hasNext());
        return memberIds;
    }

}
