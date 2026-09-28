// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.entitiy.entity.group;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.security.permission.UserPermissionsService;

import java.util.Optional;

@Service
@TbCoreComponent
@AllArgsConstructor
public class DefaultEntityGroupService extends AbstractTbEntityService implements TbEntityGroupService {

    private final GroupPermissionService groupPermissionService;
    private final UserPermissionsService userPermissionsService;
    private final EntityGroupService entityGroupService;

    @Override
    public EntityGroupInfo save(TenantId tenantId, EntityId parentEntityId, EntityGroup entityGroup, User user) throws Exception {
        ActionType actionType = entityGroup.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        try {
            EntityGroup savedEntityGroup = checkNotNull(entityGroupService.saveEntityGroup(tenantId, parentEntityId, entityGroup));
            autoCommit(user, savedEntityGroup.getType(), savedEntityGroup.getId());

            logEntityActionService.logEntityAction(tenantId, savedEntityGroup.getId(), savedEntityGroup, null, actionType, user);
            return checkNotNull(entityGroupService.findEntityGroupInfoById(tenantId, savedEntityGroup.getId()));
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.ENTITY_GROUP), entityGroup, null, actionType, user, e);
            throw e;
        }
    }

    @Override
    public void delete(TenantId tenantId, EntityGroup entityGroup, User user) throws ThingsboardException {
        ActionType actionType = ActionType.DELETED;
        EntityGroupId entityGroupId = entityGroup.getId();
        try {
            entityGroupService.deleteEntityGroup(tenantId, entityGroupId);
            logEntityActionService.logEntityAction(tenantId, entityGroupId, entityGroup, null,
                    actionType, user, entityGroupId.getId().toString());
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.ENTITY_GROUP), entityGroup,
                    null, actionType, user, e, entityGroupId.getId().toString());
            throw e;
        }
    }

    @Override
    public EntityId makePublic(TenantId tenantId, EntityGroup entityGroup, User user) throws ThingsboardException {
        checkPublicEntityGroupType(entityGroup.getType());

        if (entityGroup.isPublic()) {
            throw new ThingsboardException("Entity group is already public!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }

        GroupPermission groupPermission = new GroupPermission();
        groupPermission.setPublic(true);
        groupPermission.setTenantId(tenantId);

        EntityGroup publicUsers = customerService.findOrCreatePublicUserGroup(tenantId, user.getOwnerId());
        Role publicUserEntityGroupRole = customerService.findOrCreatePublicUserEntityGroupRole(tenantId, user.getOwnerId());

        groupPermission.setRoleId(publicUserEntityGroupRole.getId());
        groupPermission.setUserGroupId(publicUsers.getId());
        groupPermission.setEntityGroupId(entityGroup.getId());
        groupPermission.setEntityGroupType(entityGroup.getType());

        EntityId publicCustomerId = publicUsers.getOwnerId();

        JsonNode additionalInfo = entityGroup.getAdditionalInfo();
        if (additionalInfo == null || additionalInfo instanceof NullNode) {
            additionalInfo = JacksonUtil.newObjectNode();
        }
        ((ObjectNode) additionalInfo).put("isPublic", true);
        ((ObjectNode) additionalInfo).put("publicCustomerId", publicCustomerId.getId().toString());
        entityGroup.setAdditionalInfo(additionalInfo);

        GroupPermission savedGroupPermission = groupPermissionService.saveGroupPermission(tenantId, groupPermission);
        entityGroupService.saveEntityGroup(tenantId, entityGroup.getOwnerId(), entityGroup);
        userPermissionsService.onGroupPermissionUpdated(savedGroupPermission);

        logEntityActionService.logEntityAction(tenantId, entityGroup.getId(), null,
                ActionType.MADE_PUBLIC, user, entityGroup.getId().toString(), entityGroup.getName());
        return publicCustomerId;
    }

    @Override
    public void makePrivate(TenantId tenantId, EntityGroup entityGroup, User user) throws ThingsboardException {
        checkPublicEntityGroupType(entityGroup.getType());

        if (!entityGroup.isPublic()) {
            throw new ThingsboardException("Entity group is not public!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }

        Optional<GroupPermission> groupPermission = groupPermissionService.findPublicGroupPermissionByTenantIdAndEntityGroupId(tenantId, entityGroup.getId());
        if (groupPermission.isPresent()) {
            EntityGroup updatedEntityGroup = groupPermissionService.deleteGroupPermission(tenantId, groupPermission.get().getId());
            if (updatedEntityGroup != null) {
                entityGroup = updatedEntityGroup;
            }
            userPermissionsService.onGroupPermissionDeleted(groupPermission.get());
        }

        JsonNode additionalInfo = entityGroup.getAdditionalInfo();
        if (additionalInfo == null) {
            additionalInfo = JacksonUtil.newObjectNode();
        }
        ((ObjectNode) additionalInfo).put("isPublic", false);
        ((ObjectNode) additionalInfo).put("publicCustomerId", "");
        entityGroup.setAdditionalInfo(additionalInfo);

        entityGroup = entityGroupService.saveEntityGroup(tenantId, entityGroup.getOwnerId(), entityGroup);

        logEntityActionService.logEntityAction(tenantId, entityGroup.getId(), null,
                ActionType.MADE_PRIVATE, user, entityGroup.getId().toString(), entityGroup.getName());
    }

    private EntityType checkPublicEntityGroupType(EntityType groupType) throws ThingsboardException {
        if (groupType == null) {
            throw new ThingsboardException("EntityGroup type is required!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        if (groupType != EntityType.ASSET && groupType != EntityType.DEVICE
                && groupType != EntityType.ENTITY_VIEW && groupType != EntityType.EDGE && groupType != EntityType.DASHBOARD) {
            throw new ThingsboardException("Invalid entityGroup type '" + groupType + "'! Only entity groups of types 'ASSET', 'DEVICE', 'ENTITY_VIEW' or 'DASHBOARD' can be public.", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        return groupType;
    }

}
