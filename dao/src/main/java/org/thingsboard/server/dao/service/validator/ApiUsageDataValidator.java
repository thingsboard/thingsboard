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
package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;

@Component
public class ApiUsageDataValidator extends DataValidator<ApiUsageState> {

    @Lazy
    @Autowired
    private TenantService tenantService;

    @Override
    protected void validateDataImpl(TenantId requestTenantId, ApiUsageState apiUsageState) {
        if (apiUsageState.getTenantId() == null) {
            throw new DataValidationException("ApiUsageState should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(apiUsageState.getTenantId()) && !requestTenantId.equals(TenantId.SYS_TENANT_ID)) {
                throw new DataValidationException("ApiUsageState is referencing to non-existent tenant!");
            }
        }
        if (apiUsageState.getEntityId() == null) {
            throw new DataValidationException("UsageRecord should be assigned to entity!");
        } else if (apiUsageState.getEntityId().getEntityType() != EntityType.TENANT && apiUsageState.getEntityId().getEntityType() != EntityType.CUSTOMER) {
            throw new DataValidationException("Only Tenant and Customer Usage Records are supported!");
        }
    }
}
