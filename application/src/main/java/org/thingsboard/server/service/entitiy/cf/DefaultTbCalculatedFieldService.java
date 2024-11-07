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

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldConfig;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.CalculatedFieldLinkId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.service.Validator.validateEntityId;

@TbCoreComponent
@Service
@Slf4j
public class DefaultTbCalculatedFieldService extends AbstractTbEntityService implements TbCalculatedFieldService {

    private final Map<CalculatedFieldId, CalculatedField> calculatedFields;
    private final Map<CalculatedFieldLinkId, CalculatedFieldLink> calculatedFieldLinks;
    private final CalculatedFieldService calculatedFieldService;

    public DefaultTbCalculatedFieldService(CalculatedFieldService calculatedFieldService) {
        this.calculatedFields = calculatedFieldService.findAll().stream().collect(Collectors.toMap(CalculatedField::getId, cf -> cf));
        this.calculatedFieldLinks = calculatedFieldService.findAllCalculatedFieldLinks().stream().collect(Collectors.toMap(CalculatedFieldLink::getId, cfl -> cfl));
        this.calculatedFieldService = calculatedFieldService;
    }

    @Override
    public void onMsg() {

    }

    @Override
    public CalculatedField save(CalculatedField calculatedField, SecurityUser user) throws ThingsboardException {
        ActionType actionType = calculatedField.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = calculatedField.getTenantId();
        try {
            checkEntityExistence(tenantId, calculatedField.getEntityId());
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
            case ASSET -> Optional.ofNullable(assetService.findAssetById(tenantId, (AssetId) entityId))
                    .orElseThrow(() -> new IllegalArgumentException("Asset with id [" + entityId.getId() + "] does not exist."));
            case DEVICE -> Optional.ofNullable(deviceService.findDeviceById(tenantId, (DeviceId) entityId))
                    .orElseThrow(() -> new IllegalArgumentException("Device with id [" + entityId.getId() + "] does not exist."));
            case ASSET_PROFILE ->
                    Optional.ofNullable(assetProfileService.findAssetProfileById(tenantId, (AssetProfileId) entityId))
                            .orElseThrow(() -> new IllegalArgumentException("Asset Profile with id [" + entityId.getId() + "] does not exist."));
            case DEVICE_PROFILE ->
                    Optional.ofNullable(deviceProfileService.findDeviceProfileById(tenantId, (DeviceProfileId) entityId))
                            .orElseThrow(() -> new IllegalArgumentException("Device Profile with id [" + entityId.getId() + "] does not exist."));
            default ->
                    throw new IllegalArgumentException("Entity type '" + entityId.getEntityType() + "' does not support calculated fields.");
        }
    }

    private <E extends HasId<I> & HasTenantId, I extends EntityId> void checkReferencedEntities(CalculatedFieldConfig calculatedFieldConfig, SecurityUser user) throws ThingsboardException {
        List<EntityId> referencedEntityIds = calculatedFieldConfig.getArguments().values().stream()
                .map(CalculatedFieldConfig.Argument::getEntityId)
                .filter(Objects::nonNull)
                .toList();
        for (EntityId referencedEntityId : referencedEntityIds) {
            validateEntityId(referencedEntityId, id -> "Invalid entity id " + id);
            E entity = findEntity(user.getTenantId(), referencedEntityId);
            checkNotNull(entity);
            checkEntity(user, entity, Operation.READ);
        }

    }

    private <E extends HasId<I> & HasTenantId, I extends EntityId> E findEntity(TenantId tenantId, EntityId entityId) {
        return (E) switch (entityId.getEntityType()) {
            case TENANT -> tenantService.findTenantById((TenantId) entityId);
            case CUSTOMER -> customerService.findCustomerById(tenantId, (CustomerId) entityId);
            case ASSET -> assetService.findAssetById(tenantId, (AssetId) entityId);
            case DEVICE -> deviceService.findDeviceById(tenantId, (DeviceId) entityId);
            default -> throw new IllegalArgumentException("Calculated fields do not support entity type '" + entityId.getEntityType() + "' for referenced entities.");
        };
    }

}
