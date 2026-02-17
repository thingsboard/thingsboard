/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.entitiy.cf;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Set;

@TbCoreComponent
@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultTbCalculatedFieldService extends AbstractTbEntityService implements TbCalculatedFieldService {

    private final CalculatedFieldService calculatedFieldService;

    @Override
    public CalculatedField save(CalculatedField calculatedField, SecurityUser user) throws ThingsboardException {
        ActionType actionType = calculatedField.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = calculatedField.getTenantId();
        try {
            if (ActionType.UPDATED.equals(actionType)) {
                CalculatedField existingCf = calculatedFieldService.findById(tenantId, calculatedField.getId());
                checkForEntityChange(existingCf, calculatedField);
            }
            checkEntity(tenantId, calculatedField.getEntityId(), calculatedField.getType());
            CalculatedField savedCalculatedField = checkNotNull(calculatedFieldService.save(calculatedField));
            logEntityActionService.logEntityAction(tenantId, savedCalculatedField.getId(), savedCalculatedField, actionType, user);
            return savedCalculatedField;
        } catch (ThingsboardException e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.CALCULATED_FIELD), calculatedField, actionType, user, e);
            throw e;
        }
    }

    @Override
    public CalculatedField findById(CalculatedFieldId calculatedFieldId, SecurityUser user) {
        return calculatedFieldService.findById(user.getTenantId(), calculatedFieldId);
    }

    @Override
    public PageData<CalculatedField> findByTenantIdAndEntityId(TenantId tenantId, EntityId entityId, CalculatedFieldType type, PageLink pageLink) {
        checkEntity(tenantId, entityId, type);
        return calculatedFieldService.findCalculatedFieldsByEntityId(tenantId, entityId, type, pageLink);
    }

    @Override
    @Transactional
    public void delete(CalculatedField calculatedField, SecurityUser user) {
        ActionType actionType = ActionType.DELETED;
        TenantId tenantId = calculatedField.getTenantId();
        CalculatedFieldId calculatedFieldId = calculatedField.getId();
        try {
            calculatedFieldService.deleteCalculatedField(tenantId, calculatedFieldId);
            logEntityActionService.logEntityAction(tenantId, calculatedFieldId, calculatedField, actionType, user, calculatedFieldId.toString());
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.CALCULATED_FIELD), actionType, user, e, calculatedFieldId.toString());
            throw e;
        }
    }

    private void checkForEntityChange(CalculatedField oldCalculatedField, CalculatedField newCalculatedField) {
        if (!oldCalculatedField.getEntityId().equals(newCalculatedField.getEntityId())) {
            throw new IllegalArgumentException("Changing the calculated field target entity after initialization is prohibited.");
        }
    }

    private void checkEntity(TenantId tenantId, EntityId entityId, CalculatedFieldType type) {
        EntityType entityType = entityId.getEntityType();
        Set<CalculatedFieldType> supportedTypes = CalculatedField.SUPPORTED_ENTITIES.get(entityType);
        if (supportedTypes == null || supportedTypes.isEmpty()) {
            throw new IllegalArgumentException("Entity type '" + entityType + "' does not support calculated fields");
        } else if (type != null && !supportedTypes.contains(type)) {
            throw new IllegalArgumentException("Entity type '" + entityType + "' does not support '" + type + "' calculated fields");
        } else if (entityService.fetchEntity(tenantId, entityId).isEmpty()) {
            throw new IllegalArgumentException(entityType.getNormalName() + " with id [" + entityId.getId() + "] does not exist.");
        }
    }

}
