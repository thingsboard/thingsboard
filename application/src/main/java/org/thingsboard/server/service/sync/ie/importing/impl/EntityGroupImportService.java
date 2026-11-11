// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.sync.ie.importing.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.DeviceGroupOtaPackage;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.sync.ie.EntityGroupExportData;
import org.thingsboard.server.common.data.sync.ie.EntityImportResult;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.ota.DeviceGroupOtaPackageService;
import org.thingsboard.server.dao.ota.OtaPackageStateService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.UserPermissionsService;
import org.thingsboard.server.service.sync.ie.importing.MissingEntityException;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class EntityGroupImportService extends BaseEntityImportService<EntityGroupId, EntityGroup, EntityGroupExportData> {

    private static final LinkedHashSet<EntityType> HINTS = new LinkedHashSet<>(Arrays.asList(EntityType.DASHBOARD, EntityType.DEVICE, EntityType.ASSET));
    public static final Pattern CONFIG_PROCESSED_FIELDS_PATTERN = Pattern.compile(".*Id.*");

    private final EntityGroupService entityGroupService;
    private final GroupPermissionService groupPermissionService;
    private final RoleService roleService;
    private final UserPermissionsService userPermissionsService;
    private final DeviceGroupOtaPackageService deviceGroupOtaPackageService;
    private final OtaPackageStateService otaPackageStateService;

    @Override
    protected void setOwner(TenantId tenantId, EntityGroup entityGroup, IdProvider idProvider) {
        EntityId ownerId = entityGroup.getOwnerId();
        if (ownerId == null || ownerId.isNullUid() || ownerId instanceof TenantId) {
            entityGroup.setOwnerId(tenantId);
        } else {
            entityGroup.setOwnerId(idProvider.getInternalId(ownerId));
        }
    }

    @Override
    protected void checkImportPermission(EntitiesImportCtx ctx, EntityGroup entity, EntityGroup existingEntity) throws ThingsboardException {
        if (existingEntity == null) {
            accessControlService.checkEntityGroupPermission(ctx.getUser(), Operation.CREATE, entity);
        } else {
            accessControlService.checkEntityGroupPermission(ctx.getUser(), Operation.WRITE, existingEntity);
        }
    }

    @Override
    protected EntityGroup findExistingEntity(EntitiesImportCtx ctx, EntityGroup entityGroup, IdProvider idProvider) {
        EntityGroup existingEntityGroup = super.findExistingEntity(ctx, entityGroup, idProvider);
        if (existingEntityGroup == null && ctx.getSettings().isFindExistingByName()) {
            EntityId ownerId;
            if (entityGroup.getOwnerId().getEntityType() == EntityType.TENANT) {
                ownerId = ctx.getTenantId();
            } else {
                ownerId = idProvider.getInternalId(entityGroup.getOwnerId());
            }
            existingEntityGroup = entityGroupService.findEntityGroupByTypeAndName(ctx.getTenantId(), ownerId,
                    entityGroup.getType(), entityGroup.getName(), false).orElse(null);
        }
        return existingEntityGroup;
    }

    @Override
    protected EntityGroup prepare(EntitiesImportCtx ctx, EntityGroup entity, EntityGroup oldEntity, EntityGroupExportData exportData, IdProvider idProvider) {
        if (entity.getId() == null && entity.isGroupAll()) {
            throw new IllegalArgumentException("Import of new groups with type All is not allowed. " +
                                               "Consider enabling import option to find existing entities by name");
        }
        replaceIdsRecursively(ctx, idProvider, JacksonUtil.getSafely(entity.getConfiguration(), "actions"), Collections.emptySet(), CONFIG_PROCESSED_FIELDS_PATTERN, HINTS);
        return entity;
    }

    @Override
    protected EntityGroup deepCopy(EntityGroup entityGroup) {
        return new EntityGroup(entityGroup);
    }

    @Override
    protected EntityGroup saveOrUpdate(EntitiesImportCtx ctx, EntityGroup entity, EntityGroupExportData exportData, IdProvider idProvider, CompareResult compareResult) {
        return entityGroupService.saveEntityGroup(ctx.getTenantId(), entity.getOwnerId(), entity);
    }

    @Override
    protected void processAfterSaved(EntitiesImportCtx ctx, EntityImportResult<EntityGroup> importResult, EntityGroupExportData exportData,
                                     IdProvider idProvider) throws ThingsboardException {
        super.processAfterSaved(ctx, importResult, exportData, idProvider);

        importResult.addSaveReferencesCallback(() -> {
            EntityGroup savedGroup = importResult.getSavedEntity();

            if (savedGroup.getType() == EntityType.DEVICE && exportData.getGroupOtaPackages() != null) {
                importGroupOtaPackage(ctx, importResult, savedGroup, exportData.getGroupOtaPackages(), idProvider);
            }

            if (ctx.isSaveUserGroupPermissions() && savedGroup.getType() == EntityType.USER && exportData.getPermissions() != null) {
                importGroupPermissions(ctx, importResult, savedGroup, exportData.getPermissions(), idProvider);
            }

            if (exportData.getMemberIds() != null && !savedGroup.isGroupAll()) {
                wireGroupMembers(ctx, savedGroup, exportData.getMemberIds());
            }
        });
    }

    private void wireGroupMembers(EntitiesImportCtx ctx, EntityGroup savedGroup, List<UUID> memberIds) {
        if (memberIds.isEmpty()) {
            return;
        }
        EntityType memberType = savedGroup.getType();
        List<EntityId> internalIds = new ArrayList<>(memberIds.size());
        for (UUID memberUuid : memberIds) {
            EntityId memberId = EntityIdFactory.getByTypeAndUuid(memberType, memberUuid);
            EntityId internalId = ctx.getInternalId(memberId);
            if (internalId == null) {
                HasId<EntityId> byExternalId = entitiesService.findEntityByTenantIdAndExternalId(ctx.getTenantId(), memberId);
                if (byExternalId != null) {
                    internalId = byExternalId.getId();
                    ctx.putInternalId(memberId, internalId);
                } else {
                    HasId<EntityId> byInternalId = entitiesService.findEntityByTenantIdAndId(ctx.getTenantId(), memberId);
                    if (byInternalId == null) {
                        throw new MissingEntityException(memberId);
                    }
                    internalId = byInternalId.getId();
                }
            }
            internalIds.add(internalId);
        }
        entityGroupService.addEntitiesToEntityGroup(ctx.getTenantId(), savedGroup.getId(), internalIds);
    }

    private boolean permissionsEqual(GroupPermission first, GroupPermission second) {
        return first.getUserGroupId().equals(second.getUserGroupId())
               && (first.getEntityGroupId() == null ? EntityId.NULL_UUID : first.getEntityGroupId().getId()).equals(
                second.getEntityGroupId() == null ? EntityId.NULL_UUID : second.getEntityGroupId().getId())
               && first.getRoleId().equals(second.getRoleId());
    }

    @Override
    protected boolean isUpdateNeeded(EntitiesImportCtx ctx, EntityGroupExportData exportData, EntityGroup prepared, EntityGroup existing) {
        return super.isUpdateNeeded(ctx, exportData, prepared, existing) || (ctx.isSaveUserGroupPermissions() && exportData.getPermissions() != null);
    }

    private void importGroupPermissions(EntitiesImportCtx ctx, EntityImportResult<EntityGroup> importResult, EntityGroup entityGroup, List<GroupPermission> incoming, IdProvider idProvider) {
        TenantId tenantId = ctx.getTenantId();
        List<GroupPermission> permissions = new ArrayList<>(incoming);
        permissions.forEach(permission -> {
            permission.setId(null);
            permission.setTenantId(tenantId);
            permission.setRoleId(idProvider.getInternalId(permission.getRoleId()));
            permission.setUserGroupId(entityGroup.getId());
            permission.setEntityGroupId(idProvider.getInternalId(permission.getEntityGroupId()));
        });

        if (importResult.getOldEntity() != null) {
            List<GroupPermission> existingPermissions = groupPermissionService
                    .findGroupPermissionListByTenantIdAndUserGroupId(tenantId, entityGroup.getId());

            for (GroupPermission existingPermission : existingPermissions) {
                boolean exists = permissions.stream()
                        .anyMatch(p -> permissionsEqual(p, existingPermission));

                if (!exists) {
                    Role role = roleService.findRoleById(tenantId, existingPermission.getRoleId());
                    if (TenantId.SYS_TENANT_ID.equals(role.getOwnerId())) continue;

                    groupPermissionService.deleteGroupPermission(tenantId, existingPermission.getId());

                    importResult.addSendEventsCallback(() -> {
                        userPermissionsService.onGroupPermissionDeleted(existingPermission);
                        entityActionService.logEntityAction(ctx.getUser(), existingPermission.getId(), existingPermission,
                                null, ActionType.DELETED, null, existingPermission.getId().toString());
                    });
                } else {
                    permissions.removeIf(p -> permissionsEqual(p, existingPermission));
                }
            }
        }

        for (GroupPermission permission : permissions) {
            Role role = roleService.findRoleById(tenantId, permission.getRoleId());
            if (TenantId.SYS_TENANT_ID.equals(role.getOwnerId())) continue;

            GroupPermission saved = groupPermissionService.saveGroupPermission(tenantId, permission);

            importResult.addSendEventsCallback(() -> {
                userPermissionsService.onGroupPermissionUpdated(saved);
                entityActionService.logEntityAction(ctx.getUser(), saved.getId(), saved,
                        null, ActionType.ADDED, null);
            });
        }
    }

    private void importGroupOtaPackage(EntitiesImportCtx ctx, EntityImportResult<EntityGroup> importResult, EntityGroup entityGroup, List<DeviceGroupOtaPackage> otaPackages, IdProvider idProvider) {
        List<DeviceGroupOtaPackage> incoming = otaPackages.stream()
                .peek(pkg -> {
                    pkg.setGroupId(entityGroup.getId());
                    pkg.setOtaPackageId(idProvider.getInternalId(pkg.getOtaPackageId()));
                }).toList();

        List<DeviceGroupOtaPackage> existing = deviceGroupOtaPackageService.findDeviceGroupOtaPackageByGroupId(entityGroup.getId());
        for (DeviceGroupOtaPackage otaPackage : existing) {
            boolean notFound = incoming.stream().noneMatch(newPkg -> equalsGroupOtaPackage(newPkg, otaPackage));
            if (notFound) {
                deviceGroupOtaPackageService.deleteDeviceGroupOtaPackage(ctx.getTenantId(), otaPackage);
                importResult.addSendEventsCallback(() -> otaPackageStateService.update(ctx.getTenantId(), null, otaPackage));
            }
        }

        for (DeviceGroupOtaPackage newOtaPackage : incoming) {
            DeviceGroupOtaPackage oldOtaPackage = existing.stream().filter(e -> e.getOtaPackageType() == newOtaPackage.getOtaPackageType()).findFirst().orElse(null);
            if (!equalsGroupOtaPackage(newOtaPackage, oldOtaPackage)) {
                if (oldOtaPackage != null) {
                    newOtaPackage.setId(oldOtaPackage.getId());
                    newOtaPackage.setGroupId(oldOtaPackage.getGroupId());
                } else {
                    newOtaPackage.setId(null);
                    newOtaPackage.setGroupId(entityGroup.getId());
                }
                deviceGroupOtaPackageService.saveDeviceGroupOtaPackage(ctx.getTenantId(), newOtaPackage);
                importResult.addSendEventsCallback(() -> otaPackageStateService.update(ctx.getTenantId(), newOtaPackage, oldOtaPackage));
            }
        }
    }

    private boolean equalsGroupOtaPackage(DeviceGroupOtaPackage a, DeviceGroupOtaPackage b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return Objects.equals(a.getOtaPackageId(), b.getOtaPackageId()) &&
               a.getOtaPackageType() == b.getOtaPackageType() && a.getGroupId().equals(b.getGroupId());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ENTITY_GROUP;
    }

}
