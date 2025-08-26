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
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.ArgumentsBasedCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.cf.CalculatedFieldDao;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;

@Component
public class CalculatedFieldDataValidator extends DataValidator<CalculatedField> {

    @Autowired
    private CalculatedFieldDao calculatedFieldDao;

    @Autowired
    private ApiLimitService apiLimitService;

    @Override
    protected void validateCreate(TenantId tenantId, CalculatedField calculatedField) {
        validateNumberOfCFsPerEntity(tenantId, calculatedField.getEntityId());
        validateNumberOfArgumentsPerCF(tenantId, calculatedField);
        validateCalculatedFieldConfiguration(calculatedField);
    }

    @Override
    protected CalculatedField validateUpdate(TenantId tenantId, CalculatedField calculatedField) {
        CalculatedField old = calculatedFieldDao.findById(calculatedField.getTenantId(), calculatedField.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing calculated field!");
        }
        validateNumberOfArgumentsPerCF(tenantId, calculatedField);
        validateCalculatedFieldConfiguration(calculatedField);
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
        if (CalculatedFieldType.GEOFENCING.equals(calculatedField.getType()) && maxArgumentsPerCF < 3) {
            throw new DataValidationException("Geofencing calculated field requires at least 3 arguments, but the system limit is " +
                    maxArgumentsPerCF + ". Contact your administrator to increase the limit."
            );
        }
        if (calculatedField.getConfiguration() instanceof ArgumentsBasedCalculatedFieldConfiguration configuration
                && configuration.getArguments().size() > maxArgumentsPerCF) {
            throw new DataValidationException("Calculated field arguments limit reached!");
        }
    }

    private void validateCalculatedFieldConfiguration(CalculatedField calculatedField) {
        if (calculatedField.getConfiguration() instanceof ArgumentsBasedCalculatedFieldConfiguration configuration) {
            if (configuration.getArguments().containsKey("ctx")) {
                throw new DataValidationException("Argument name 'ctx' is reserved and cannot be used.");
            }
            try {
                configuration.validate();
            } catch (IllegalArgumentException e) {
                throw new DataValidationException(e.getMessage(), e);
            }
        }
    }

}
