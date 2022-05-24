/**
 * Copyright © 2016-2022 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.entitiy.entityRelation;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.security.model.SecurityUser;

@Service
@TbCoreComponent
@AllArgsConstructor
@Slf4j
public class DefaultTbEntityRelationService extends AbstractTbEntityService implements TbEntityRelationService {
    @Override
    public void save(EntityRelation relation, SecurityUser user) throws ThingsboardException {
        TenantId tenantId = user.getTenantId();
        try {
            relationService.saveRelation(tenantId, relation);
            notificationEntityService.notifyCreateOrUpdateOrDelete(relation.getFrom(), null, user, ActionType.RELATION_ADD_OR_UPDATE, null, relation);
            notificationEntityService.notifyCreateOrUpdateOrDelete(relation.getTo(), null, user, ActionType.RELATION_ADD_OR_UPDATE, null, relation);
        } catch (Exception e) {
            notificationEntityService.notifyCreateOrUpdateOrDelete(relation.getFrom(), null, user, ActionType.RELATION_ADD_OR_UPDATE, e, relation);
            notificationEntityService.notifyCreateOrUpdateOrDelete(relation.getTo(), null, user, ActionType.RELATION_ADD_OR_UPDATE, e, relation);
            throw handleException(e);
        }
    }

    @Override
    public void delete(EntityRelation relation, SecurityUser user) throws ThingsboardException {
        try {
            Boolean found = relationService.deleteRelation(user.getTenantId(), relation.getFrom(), relation.getTo(), relation.getType(), relation.getTypeGroup());
            if (!found) {
                throw new ThingsboardException("Requested item wasn't found!", ThingsboardErrorCode.ITEM_NOT_FOUND);
            }
            notificationEntityService.notifyCreateOrUpdateOrDelete(relation.getFrom(), null, user, ActionType.RELATION_DELETED, null, relation);
            notificationEntityService.notifyCreateOrUpdateOrDelete(relation.getTo(), null, user, ActionType.RELATION_DELETED, null, relation);
        } catch (Exception e) {
            notificationEntityService.notifyCreateOrUpdateOrDelete(relation.getFrom(), null, user, ActionType.RELATION_DELETED, e, relation);
            notificationEntityService.notifyCreateOrUpdateOrDelete(relation.getTo(), null, user, ActionType.RELATION_DELETED, e, relation);
            throw handleException(e);
        }
    }
}
