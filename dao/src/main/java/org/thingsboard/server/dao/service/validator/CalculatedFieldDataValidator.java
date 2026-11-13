// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.cf.CalculatedFieldDao;
import org.thingsboard.server.dao.entity.EntityDaoRegistry;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;

import java.util.EnumSet;
import java.util.Set;

@Component
public class CalculatedFieldDataValidator extends DataValidator<CalculatedField> {

    private static final Set<EntityType> SUPPORTED_ENTITY_TYPES = EnumSet.of(
            EntityType.CUSTOMER, EntityType.ASSET, EntityType.DEVICE, EntityType.ASSET_PROFILE, EntityType.DEVICE_PROFILE);

    @Autowired
    private CalculatedFieldDao calculatedFieldDao;

    @Autowired
    private ApiLimitService apiLimitService;

    @Autowired
    private EntityDaoRegistry entityDaoRegistry;

    @Override
    protected void validateDataImpl(TenantId tenantId, CalculatedField calculatedField) {
        if (calculatedField.getTenantId() == null) {
            throw new DataValidationException("Calculated field should be assigned to tenant!");
        }
        validateEntityTenant(calculatedField.getTenantId(), calculatedField.getEntityId());
        for (EntityId referencedEntityId : calculatedField.getConfiguration().getReferencedEntities()) {
            validateReferencedEntityTenant(calculatedField.getTenantId(), referencedEntityId);
        }
    }

    @Override
    protected void validateCreate(TenantId tenantId, CalculatedField calculatedField) {
        validateNumberOfCFsPerEntity(tenantId, calculatedField.getEntityId());
        validateNumberOfArgumentsPerCF(tenantId, calculatedField);
        validateArgumentNames(calculatedField);
    }

    @Override
    protected CalculatedField validateUpdate(TenantId tenantId, CalculatedField calculatedField) {
        CalculatedField old = calculatedFieldDao.findById(calculatedField.getTenantId(), calculatedField.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing calculated field!");
        }
        if (!old.getTenantId().equals(calculatedField.getTenantId())) {
            throw new DataValidationException("Can't update calculated field tenant id!");
        }
        validateNumberOfArgumentsPerCF(tenantId, calculatedField);
        validateArgumentNames(calculatedField);
        return old;
    }

    private void validateNumberOfCFsPerEntity(TenantId tenantId, EntityId entityId) {
        long maxCFsPerEntity = apiLimitService.getLimit(tenantId, DefaultTenantProfileConfiguration::getMaxCalculatedFieldsPerEntity);
        if (maxCFsPerEntity <= 0) {
            return;
        }
        if (calculatedFieldDao.countCFByEntityId(tenantId, entityId) >= maxCFsPerEntity) {
            throw new DataValidationException("Calculated fields per entity limit reached!");
        }
    }

    private void validateNumberOfArgumentsPerCF(TenantId tenantId, CalculatedField calculatedField) {
        long maxArgumentsPerCF = apiLimitService.getLimit(tenantId, DefaultTenantProfileConfiguration::getMaxArgumentsPerCF);
        if (maxArgumentsPerCF <= 0) {
            return;
        }
        if (calculatedField.getConfiguration().getArguments().size() > maxArgumentsPerCF) {
            throw new DataValidationException("Calculated field arguments limit reached!");
        }
    }

    private void validateArgumentNames(CalculatedField calculatedField) {
        if (calculatedField.getConfiguration().getArguments().containsKey("ctx")) {
            throw new DataValidationException("Argument name 'ctx' is reserved and cannot be used.");
        }
    }

    private void validateReferencedEntityTenant(TenantId cfTenantId, EntityId entityId) {
        EntityType entityType = entityId.getEntityType();
        switch (entityType) {
            case TENANT -> {
                if (!cfTenantId.equals(entityId)) {
                    throw new DataValidationException("Referenced tenant must be the calculated field's tenant!");
                }
            }
            case CUSTOMER, ASSET, DEVICE -> validateEntityTenant(cfTenantId, entityId);
            default -> throw new DataValidationException("Calculated fields do not support '" + entityType + "' for referenced entities.");
        }
    }

    private void validateEntityTenant(TenantId cfTenantId, EntityId entityId) {
        EntityType entityType = entityId.getEntityType();
        if (!SUPPORTED_ENTITY_TYPES.contains(entityType)) {
            throw new DataValidationException("Entity type '" + entityType + "' does not support calculated fields.");
        }
        HasTenantId entity = entityDaoRegistry.<HasTenantId>getDao(entityType).findById(cfTenantId, entityId.getId());
        if (entity == null || !cfTenantId.equals(entity.getTenantId())) {
            throw new DataValidationException(entityType.getNormalName() + " with id [" + entityId.getId() + "] does not belong to the calculated field's tenant!");
        }
    }

}
