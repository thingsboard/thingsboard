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
package org.thingsboard.server.dao.calculated_field;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.calculated_field.CalculatedField;
import org.thingsboard.server.common.data.calculated_field.CalculatedFieldLink;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.thingsboard.server.dao.entity.AbstractEntityService.checkConstraintViolation;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Service("CalculatedFieldDaoService")
@Slf4j
@RequiredArgsConstructor
public class BaseCalculatedFieldService implements CalculatedFieldService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_CALCULATED_FIELD_ID = "Incorrect calculatedFieldId ";

    private final CalculatedFieldDao calculatedFieldDao;
    private final CalculatedFieldLinkDao calculatedFieldLinkDao;
    private final DeviceService deviceService;
    private final AssetService assetService;
    private final DeviceProfileService deviceProfileService;
    private final AssetProfileService assetProfileService;
    private final DataValidator<CalculatedField> calculatedFieldDataValidator;
    private final DataValidator<CalculatedFieldLink> calculatedFieldLinkDataValidator;

    @Override
    public CalculatedField save(CalculatedField calculatedField) {
        calculatedFieldDataValidator.validate(calculatedField, CalculatedField::getTenantId);
        try {
            TenantId tenantId = calculatedField.getTenantId();
            checkEntityExistence(tenantId, calculatedField.getEntityId());
            log.trace("Executing save calculated field, [{}]", calculatedField);
            CalculatedField savedCalculatedField = calculatedFieldDao.save(tenantId, calculatedField);
            createOrUpdateCalculatedFieldLink(tenantId, savedCalculatedField);
            return savedCalculatedField;
        } catch (Exception e) {
            checkConstraintViolation(e,
                    "calculated_field_unq_key", "Calculated Field with such name is already in exists!",
                    "calculated_field_external_id_unq_key", "Calculated Field with such external id already exists!");
            throw e;
        }
    }

    @Override
    public CalculatedField findById(TenantId tenantId, CalculatedFieldId calculatedFieldId) {
        log.trace("Executing findById, tenantId [{}], calculatedFieldId [{}]", tenantId, calculatedFieldId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(calculatedFieldId, id -> INCORRECT_CALCULATED_FIELD_ID + id);
        return calculatedFieldDao.findById(tenantId, calculatedFieldId.getId());
    }

    @Override
    public void deleteCalculatedField(TenantId tenantId, CalculatedFieldId calculatedFieldId) {
        log.trace("Executing deleteCalculatedField, tenantId [{}], calculatedFieldId [{}]", tenantId, calculatedFieldId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(calculatedFieldId, id -> INCORRECT_CALCULATED_FIELD_ID + id);
        calculatedFieldDao.removeById(tenantId, calculatedFieldId.getId());
    }

    @Override
    public int deleteAllCalculatedFieldsByEntityId(TenantId tenantId, EntityId entityId) {
        log.trace("Executing deleteAllCalculatedFieldsByEntityId, tenantId [{}], entityId [{}]", tenantId, entityId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(entityId.getId(), id -> "Incorrect entityId " + id);
        List<CalculatedField> calculatedFields = calculatedFieldDao.removeAllByEntityId(tenantId, entityId);
        return calculatedFields.size();
    }

    @Override
    public CalculatedFieldLink saveCalculatedFieldLink(TenantId tenantId, CalculatedFieldLink calculatedFieldLink) {
        calculatedFieldLinkDataValidator.validate(calculatedFieldLink, CalculatedFieldLink::getTenantId);
        try {
            log.trace("Executing save calculated field link, [{}]", calculatedFieldLink);
            return calculatedFieldLinkDao.save(tenantId, calculatedFieldLink);
        } catch (Exception e) {
            checkConstraintViolation(e, "calculated_field_link_unq_key", "Calculated Field for such entity id is already exists!");
            throw e;
        }
    }

    @Override
    public boolean existsByEntityId(TenantId tenantId, EntityId entityId) {
        return calculatedFieldDao.existsByTenantIdAndEntityId(tenantId, entityId);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findById(tenantId, new CalculatedFieldId(entityId.getId())));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.CALCULATED_FIELD;
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
                    throw new IllegalArgumentException("Entity type '" + entityId.getEntityType() + "' is not supported.");
        }
    }

    private void createOrUpdateCalculatedFieldLink(TenantId tenantId, CalculatedField calculatedField) {
        CalculatedFieldLink calculatedFieldLink = calculatedFieldLinkDao.findCalculatedFieldLinkByEntityId(tenantId.getId(), calculatedField.getEntityId().getId());
        saveCalculatedFieldLink(tenantId, Objects.requireNonNullElseGet(calculatedFieldLink, () -> createCalculatedFieldLink(tenantId, calculatedField)));
    }

    private CalculatedFieldLink createCalculatedFieldLink(TenantId tenantId, CalculatedField calculatedField) {
        CalculatedFieldLink calculatedFieldLink = new CalculatedFieldLink();
        calculatedFieldLink.setTenantId(tenantId);
        calculatedFieldLink.setEntityId(calculatedField.getEntityId());
        calculatedFieldLink.setCalculatedFieldId(calculatedField.getId());
        calculatedFieldLink.setConfiguration(calculatedField.getConfiguration());
        return calculatedFieldLink;
    }

}
