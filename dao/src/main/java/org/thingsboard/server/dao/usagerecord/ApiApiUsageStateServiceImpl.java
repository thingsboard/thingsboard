/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.dao.usagerecord;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantDao;

import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class ApiApiUsageStateServiceImpl extends AbstractEntityService implements ApiUsageStateService {
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    private final ApiUsageStateDao apiUsageStateDao;
    private final TenantDao tenantDao;

    public ApiApiUsageStateServiceImpl(TenantDao tenantDao, ApiUsageStateDao apiUsageStateDao) {
        this.tenantDao = tenantDao;
        this.apiUsageStateDao = apiUsageStateDao;
    }

    @Override
    public void deleteApiUsageStateByTenantId(TenantId tenantId) {
        log.trace("Executing deleteUsageRecordsByTenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        apiUsageStateDao.deleteApiUsageStateByTenantId(tenantId);
    }

    @Override
    public void createDefaultApiUsageState(TenantId tenantId) {
        log.trace("Executing createDefaultUsageRecord [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        ApiUsageState apiUsageState = new ApiUsageState();
        apiUsageState.setTenantId(tenantId);
        apiUsageState.setEntityId(tenantId);
        apiUsageStateValidator.validate(apiUsageState, ApiUsageState::getTenantId);
        apiUsageStateDao.save(apiUsageState.getTenantId(), apiUsageState);
    }

    @Override
    public ApiUsageState findTenantApiUsageState(TenantId tenantId) {
        log.trace("Executing findTenantUsageRecord, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return apiUsageStateDao.findTenantApiUsageState(tenantId.getId());
    }

    private DataValidator<ApiUsageState> apiUsageStateValidator =
            new DataValidator<ApiUsageState>() {
                @Override
                protected void validateDataImpl(TenantId requestTenantId, ApiUsageState apiUsageState) {
                    if (apiUsageState.getTenantId() == null) {
                        throw new DataValidationException("ApiUsageState should be assigned to tenant!");
                    } else {
                        Tenant tenant = tenantDao.findById(requestTenantId, apiUsageState.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("Asset is referencing to non-existent tenant!");
                        }
                    }
                    if (apiUsageState.getEntityId() == null) {
                        throw new DataValidationException("UsageRecord should be assigned to entity!");
                    } else if (!EntityType.TENANT.equals(apiUsageState.getEntityId().getEntityType())) {
                        throw new DataValidationException("Only Tenant Usage Records are supported!");
                    } else if (!apiUsageState.getTenantId().getId().equals(apiUsageState.getEntityId().getId())) {
                        throw new DataValidationException("Can't assign one Usage Record to multiple tenants!");
                    }
                }
            };

}
