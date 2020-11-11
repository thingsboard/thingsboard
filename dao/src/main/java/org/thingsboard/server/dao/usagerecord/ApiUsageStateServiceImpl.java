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
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.ApiUsageStateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileConfiguration;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.dao.tenant.TenantProfileDao;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.util.ArrayList;
import java.util.List;

import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class ApiUsageStateServiceImpl extends AbstractEntityService implements ApiUsageStateService {
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    private final ApiUsageStateDao apiUsageStateDao;
    private final TenantProfileDao tenantProfileDao;
    private final TenantDao tenantDao;
    private final TimeseriesService tsService;

    public ApiUsageStateServiceImpl(TenantDao tenantDao, ApiUsageStateDao apiUsageStateDao, TenantProfileDao tenantProfileDao, TimeseriesService tsService) {
        this.tenantDao = tenantDao;
        this.apiUsageStateDao = apiUsageStateDao;
        this.tenantProfileDao = tenantProfileDao;
        this.tsService = tsService;
    }

    @Override
    public void deleteApiUsageStateByTenantId(TenantId tenantId) {
        log.trace("Executing deleteUsageRecordsByTenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        apiUsageStateDao.deleteApiUsageStateByTenantId(tenantId);
    }

    @Override
    public ApiUsageState createDefaultApiUsageState(TenantId tenantId) {
        log.trace("Executing createDefaultUsageRecord [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        ApiUsageState apiUsageState = new ApiUsageState();
        apiUsageState.setTenantId(tenantId);
        apiUsageState.setEntityId(tenantId);
        apiUsageState.setTransportState(ApiUsageStateValue.ENABLED);
        apiUsageState.setReExecState(ApiUsageStateValue.ENABLED);
        apiUsageState.setJsExecState(ApiUsageStateValue.ENABLED);
        apiUsageState.setDbStorageState(ApiUsageStateValue.ENABLED);
        apiUsageStateValidator.validate(apiUsageState, ApiUsageState::getTenantId);

        ApiUsageState saved = apiUsageStateDao.save(apiUsageState.getTenantId(), apiUsageState);

        Tenant tenant = tenantDao.findById(tenantId, tenantId.getId());
        TenantProfile tenantProfile = tenantProfileDao.findById(tenantId, tenant.getTenantProfileId().getId());
        TenantProfileConfiguration configuration = tenantProfile.getProfileData().getConfiguration();
        List<TsKvEntry> profileThresholds = new ArrayList<>();
        for (ApiUsageRecordKey key : ApiUsageRecordKey.values()) {
            profileThresholds.add(new BasicTsKvEntry(saved.getCreatedTime(), new LongDataEntry(key.getApiLimitKey(), configuration.getProfileThreshold(key))));
        }
        tsService.save(tenantId, saved.getId(), profileThresholds, 0L);
        return saved;
    }

    @Override
    public ApiUsageState update(ApiUsageState apiUsageState) {
        log.trace("Executing save [{}]", apiUsageState.getTenantId());
        validateId(apiUsageState.getTenantId(), INCORRECT_TENANT_ID + apiUsageState.getTenantId());
        validateId(apiUsageState.getId(), "Can't save new usage state. Only update is allowed!");
        return apiUsageStateDao.save(apiUsageState.getTenantId(), apiUsageState);
    }

    @Override
    public ApiUsageState findTenantApiUsageState(TenantId tenantId) {
        log.trace("Executing findTenantUsageRecord, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return apiUsageStateDao.findTenantApiUsageState(tenantId.getId());
    }

    @Override
    public ApiUsageState findApiUsageStateById(TenantId tenantId, ApiUsageStateId id) {
        log.trace("Executing findApiUsageStateById, tenantId [{}], apiUsageStateId [{}]", tenantId, id);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(id, "Incorrect apiUsageStateId " + id);
        return apiUsageStateDao.findById(tenantId, id.getId());
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
