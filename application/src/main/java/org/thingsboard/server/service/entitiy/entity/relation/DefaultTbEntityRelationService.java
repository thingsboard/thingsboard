// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.entitiy.entity.relation;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

@Service
@TbCoreComponent
@AllArgsConstructor
@Slf4j
public class DefaultTbEntityRelationService extends AbstractTbEntityService implements TbEntityRelationService {

    private final RelationService relationService;

    @Override
    public EntityRelation save(TenantId tenantId, CustomerId customerId, EntityRelation relation, User user) throws ThingsboardException {
        ActionType actionType = ActionType.RELATION_ADD_OR_UPDATE;
        try {
            var savedRelation = relationService.saveRelation(tenantId, relation);
            logEntityActionService.logEntityRelationAction(tenantId, customerId,
                    savedRelation, user, actionType, null, savedRelation);
            return savedRelation;
        } catch (Exception e) {
            logEntityActionService.logEntityRelationAction(tenantId, customerId,
                    relation, user, actionType, e, relation);
            throw e;
        }
    }

    @Override
    public EntityRelation delete(TenantId tenantId, CustomerId customerId, EntityRelation relation, User user) throws ThingsboardException {
        ActionType actionType = ActionType.RELATION_DELETED;
        try {
            var found = relationService.deleteRelation(tenantId, relation.getFrom(), relation.getTo(), relation.getType(), relation.getTypeGroup());
            if (found == null) {
                throw new ThingsboardException("Requested item wasn't found!", ThingsboardErrorCode.ITEM_NOT_FOUND);
            }
            logEntityActionService.logEntityRelationAction(tenantId, customerId, found, user, actionType, null, found);
            return found;
        } catch (Exception e) {
            logEntityActionService.logEntityRelationAction(tenantId, customerId,
                    relation, user, actionType, e, relation);
            throw e;
        }
    }

    @Override
    public void deleteCommonRelations(TenantId tenantId, CustomerId customerId, EntityId entityId, User user) throws ThingsboardException {
        try {
            relationService.deleteEntityCommonRelations(tenantId, entityId);
            logEntityActionService.logEntityAction(tenantId, entityId, null, customerId, ActionType.RELATIONS_DELETED, user);
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, entityId, null, customerId,
                    ActionType.RELATIONS_DELETED, user, e);
            throw e;
        }
    }
}
