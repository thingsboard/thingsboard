/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;

import java.util.List;
import java.util.Optional;

import static org.thingsboard.server.dao.service.Validator.validateEntityId;

@TbCoreComponent
@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultTbCalculatedFieldService extends AbstractTbEntityService implements TbCalculatedFieldService {

    private static final int MAX_ARGUMENT_SIZE = 10;
    private static final int MAX_CALCULATED_FIELD_NUMBER = 10;

    private final CalculatedFieldService calculatedFieldService;

    @Override
    public CalculatedField save(CalculatedField calculatedField, SecurityUser user) throws ThingsboardException {
        ActionType actionType = calculatedField.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = calculatedField.getTenantId();
        try {
            checkCalculatedFieldNumber(tenantId, calculatedField.getEntityId());
            checkEntityExistence(tenantId, calculatedField.getEntityId());
            checkArgumentSize(calculatedField.getConfiguration());
            checkReferencedEntities(calculatedField.getConfiguration(), user);
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

    private void checkEntityExistence(TenantId tenantId, EntityId entityId) {
        switch (entityId.getEntityType()) {
            case ASSET, DEVICE, ASSET_PROFILE, DEVICE_PROFILE ->
                    Optional.ofNullable(entityService.fetchEntity(tenantId, entityId))
                            .orElseThrow(() -> new IllegalArgumentException(entityId.getEntityType().getNormalName() + " with id [" + entityId.getId() + "] does not exist."));
            default ->
                    throw new IllegalArgumentException("Entity type '" + entityId.getEntityType() + "' does not support calculated fields.");
        }
    }

    private <E extends HasId<I> & HasTenantId, I extends EntityId> void checkReferencedEntities(CalculatedFieldConfiguration calculatedFieldConfig, SecurityUser user) throws ThingsboardException {
        List<EntityId> referencedEntityIds = calculatedFieldConfig.getReferencedEntities();
        for (EntityId referencedEntityId : referencedEntityIds) {
            validateEntityId(referencedEntityId, id -> "Invalid entity id " + id);
            E entity = findEntity(user.getTenantId(), referencedEntityId);
            checkNotNull(entity);
            checkEntity(user, entity, Operation.READ);
        }

    }

    private void checkArgumentSize(CalculatedFieldConfiguration calculatedFieldConfig) {
        if (calculatedFieldConfig.getArguments().size() > MAX_ARGUMENT_SIZE) {
            throw new IllegalArgumentException("Too many arguments: " + calculatedFieldConfig.getArguments().size() + ". Max number of argument is " + MAX_ARGUMENT_SIZE);
        }
    }

    private void checkCalculatedFieldNumber(TenantId tenantId, EntityId entityId) {
        int numberOfCalculatedFieldsByEntityId = calculatedFieldService.findCalculatedFieldIdsByEntityId(tenantId, entityId).size();
        if (numberOfCalculatedFieldsByEntityId >= MAX_CALCULATED_FIELD_NUMBER) {
            throw new IllegalArgumentException("Max number of calculated fields for entity is " + MAX_CALCULATED_FIELD_NUMBER);
        }
    }

    private <E extends HasId<I> & HasTenantId, I extends EntityId> E findEntity(TenantId tenantId, EntityId entityId) {
        return switch (entityId.getEntityType()) {
            case TENANT, CUSTOMER, ASSET, DEVICE -> (E) entityService.fetchEntity(tenantId, entityId).orElse(null);
            default ->
                    throw new IllegalArgumentException("Calculated fields do not support entity type '" + entityId.getEntityType() + "' for referenced entities.");
        };
    }

}
