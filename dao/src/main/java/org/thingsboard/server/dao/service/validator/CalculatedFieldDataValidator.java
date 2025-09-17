/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.configuration.ArgumentsBasedCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.RelationQueryDynamicSourceConfiguration;
import org.thingsboard.server.common.data.cf.configuration.ScheduledUpdateSupportedCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.cf.CalculatedFieldDao;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;

import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CalculatedFieldDataValidator extends DataValidator<CalculatedField> {

    @Autowired
    private CalculatedFieldDao calculatedFieldDao;

    @Autowired
    private ApiLimitService apiLimitService;

    @Override
    protected void validateDataImpl(TenantId tenantId, CalculatedField calculatedField) {
        validateNumberOfArgumentsPerCF(tenantId, calculatedField);
        validateCalculatedFieldConfiguration(calculatedField);
        validateSchedulingConfiguration(tenantId, calculatedField);
        validateRelationQuerySourceArguments(tenantId, calculatedField);
    }

    @Override
    protected void validateCreate(TenantId tenantId, CalculatedField calculatedField) {
        long maxCFsPerEntity = apiLimitService.getLimit(tenantId, DefaultTenantProfileConfiguration::getMaxCalculatedFieldsPerEntity);
        if (maxCFsPerEntity <= 0) {
            return;
        }
        if (calculatedFieldDao.countCFByEntityId(tenantId, calculatedField.getEntityId()) >= maxCFsPerEntity) {
            throw new DataValidationException("Calculated fields per entity limit reached!");
        }
    }

    @Override
    protected CalculatedField validateUpdate(TenantId tenantId, CalculatedField calculatedField) {
        CalculatedField old = calculatedFieldDao.findById(calculatedField.getTenantId(), calculatedField.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing calculated field!");
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
        Map<String, RelationQueryDynamicSourceConfiguration> relationQueryBasedArguments = argumentsBasedCfg.getArguments().entrySet()
                .stream()
                .filter(entry -> entry.getValue().hasDynamicSource())
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> (RelationQueryDynamicSourceConfiguration) entry.getValue().getRefDynamicSourceConfiguration()));
        if (relationQueryBasedArguments.isEmpty()) {
            return;
        }
        int maxRelationLevel = (int) apiLimitService.getLimit(tenantId, DefaultTenantProfileConfiguration::getMaxRelationLevelPerCfArgument);
        relationQueryBasedArguments.forEach((argumentName, relationQueryDynamicSourceConfiguration) ->
                wrapAsDataValidation(() -> relationQueryDynamicSourceConfiguration.validateMaxRelationLevel(argumentName, maxRelationLevel)));
    }

    private static void wrapAsDataValidation(Runnable validation) {
        try {
            validation.run();
        } catch (IllegalArgumentException e) {
            throw new DataValidationException(e.getMessage(), e);
        }
    }

}
