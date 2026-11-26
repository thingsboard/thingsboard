// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.ArgumentsBasedCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.RelationPathQueryDynamicSourceConfiguration;
import org.thingsboard.server.common.data.cf.configuration.ScheduledUpdateSupportedCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.aggregation.RelatedEntitiesAggregationCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.aggregation.single.EntityAggregationCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.cf.CalculatedFieldDao;
import org.thingsboard.server.dao.entity.EntityDaoRegistry;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
        validateNumberOfArgumentsPerCF(tenantId, calculatedField);
        validateCalculatedFieldConfiguration(calculatedField);
        validateSchedulingConfiguration(tenantId, calculatedField);
        validateRelationQuerySourceArguments(tenantId, calculatedField);
        validateRelatedAggregationConfiguration(tenantId, calculatedField);
        validateEntityAggregationConfiguration(tenantId, calculatedField);
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
        if (calculatedField.getType() == CalculatedFieldType.ALARM) {
            return;
        }
        long maxCFsPerEntity = apiLimitService.getLimit(tenantId, DefaultTenantProfileConfiguration::getMaxCalculatedFieldsPerEntity);
        if (maxCFsPerEntity <= 0) {
            return;
        }
        if (calculatedFieldDao.countByEntityIdAndTypeNot(tenantId, calculatedField.getEntityId(), CalculatedFieldType.ALARM) >= maxCFsPerEntity) {
            throw new DataValidationException("Calculated fields per entity limit reached!");
        }
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
        return old;
    }

    private void validateNumberOfArgumentsPerCF(TenantId tenantId, CalculatedField calculatedField) {
        if (!(calculatedField instanceof ArgumentsBasedCalculatedFieldConfiguration argumentsBasedCfg)) {
            return;
        }
        long maxArgumentsPerCF = apiLimitService.getLimit(tenantId, DefaultTenantProfileConfiguration::getMaxArgumentsPerCF);
        if (maxArgumentsPerCF <= 0) {
            return;
        }
        if (argumentsBasedCfg.getArguments().size() > maxArgumentsPerCF) {
            throw new DataValidationException("Calculated field arguments limit reached!");
        }
    }

    private void validateCalculatedFieldConfiguration(CalculatedField calculatedField) {
        wrapAsDataValidation(calculatedField.getConfiguration()::validate);
    }

    private void validateSchedulingConfiguration(TenantId tenantId, CalculatedField calculatedField) {
        if (!(calculatedField.getConfiguration() instanceof ScheduledUpdateSupportedCalculatedFieldConfiguration scheduledUpdateCfg)
                || !scheduledUpdateCfg.isScheduledUpdateEnabled()) {
            return;
        }
        long minAllowedScheduledUpdateInterval = apiLimitService.getLimit(tenantId, DefaultTenantProfileConfiguration::getMinAllowedScheduledUpdateIntervalInSecForCF);
        wrapAsDataValidation(() -> scheduledUpdateCfg.validate(minAllowedScheduledUpdateInterval));
    }

    private void validateRelationQuerySourceArguments(TenantId tenantId, CalculatedField calculatedField) {
        if (!(calculatedField.getConfiguration() instanceof ArgumentsBasedCalculatedFieldConfiguration argumentsBasedCfg)) {
            return;
        }
        Map<String, RelationPathQueryDynamicSourceConfiguration> relationQueryBasedArguments = argumentsBasedCfg.getArguments().entrySet()
                .stream()
                .filter(entry -> entry.getValue().hasRelationQuerySource())
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> (RelationPathQueryDynamicSourceConfiguration) entry.getValue().getRefDynamicSourceConfiguration()));
        if (relationQueryBasedArguments.isEmpty()) {
            return;
        }
        int maxRelationLevel = (int) apiLimitService.getLimit(tenantId, DefaultTenantProfileConfiguration::getMaxRelationLevelPerCfArgument);
        relationQueryBasedArguments.forEach((argumentName, relationQueryDynamicSourceConfiguration) ->
                wrapAsDataValidation(() -> relationQueryDynamicSourceConfiguration.validateMaxRelationLevel(argumentName, maxRelationLevel)));
    }

    private void validateRelatedAggregationConfiguration(TenantId tenantId, CalculatedField calculatedField) {
        if (!(calculatedField.getConfiguration() instanceof RelatedEntitiesAggregationCalculatedFieldConfiguration aggConfiguration)) {
            return;
        }
        long minDeduplicationInterval = apiLimitService.getLimit(tenantId, DefaultTenantProfileConfiguration::getMinAllowedDeduplicationIntervalInSecForCF);
        if (aggConfiguration.getDeduplicationIntervalInSec() < minDeduplicationInterval) {
            throw new IllegalArgumentException("Deduplication interval is less than configured " +
                    "minimum allowed interval in tenant profile: " + minDeduplicationInterval);
        }
    }

    private void validateEntityAggregationConfiguration(TenantId tenantId, CalculatedField calculatedField) {
        if (!(calculatedField.getConfiguration() instanceof EntityAggregationCalculatedFieldConfiguration aggConfiguration)) {
            return;
        }
        long minAggregationIntervalInSec = apiLimitService.getLimit(tenantId, DefaultTenantProfileConfiguration::getMinAllowedAggregationIntervalInSecForCF);
        if (minAggregationIntervalInSec <= 0) {
            return;
        }
        if (aggConfiguration.getInterval().getCurrentIntervalDurationMillis() < TimeUnit.SECONDS.toMillis(minAggregationIntervalInSec)) {
            throw new IllegalArgumentException("Aggregation interval duration is less than configured " +
                    "minimum allowed aggregation interval in tenant profile: " + minAggregationIntervalInSec + " sec.");
        }
    }

    private static void wrapAsDataValidation(Runnable validation) {
        try {
            validation.run();
        } catch (IllegalArgumentException e) {
            throw new DataValidationException(e.getMessage(), e);
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
