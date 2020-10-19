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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.UsageRecord;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserCredentialsId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantDao;

import java.util.HashMap;
import java.util.Map;

import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;
import static org.thingsboard.server.dao.service.Validator.validateString;

@Service
@Slf4j
public class UsageRecordServiceImpl extends AbstractEntityService implements UsageRecordService {
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    private final UsageRecordDao usageRecordDao;
    private final TenantDao tenantDao;

    public UsageRecordServiceImpl(TenantDao tenantDao, UsageRecordDao usageRecordDao) {
        this.tenantDao = tenantDao;
        this.usageRecordDao = usageRecordDao;
    }

    @Override
    public void deleteUsageRecordsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteUsageRecordsByTenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        usageRecordDao.deleteUsageRecordsByTenantId(tenantId);
    }

    @Override
    public void createDefaultUsageRecord(TenantId tenantId) {
        log.trace("Executing createDefaultUsageRecord [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        UsageRecord usageRecord = new UsageRecord();
        usageRecord.setTenantId(tenantId);
        usageRecord.setEntityId(tenantId);
        usageRecordValidator.validate(usageRecord, UsageRecord::getTenantId);
        usageRecordDao.save(usageRecord.getTenantId(), usageRecord);
    }

    @Override
    public UsageRecord findTenantUsageRecord(TenantId tenantId) {
        log.trace("Executing findTenantUsageRecord, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return usageRecordDao.findTenantUsageRecord(tenantId.getId());
    }

    private DataValidator<UsageRecord> usageRecordValidator =
            new DataValidator<UsageRecord>() {
                @Override
                protected void validateDataImpl(TenantId requestTenantId, UsageRecord usageRecord) {
                    if (usageRecord.getTenantId() == null) {
                        throw new DataValidationException("UsageRecord should be assigned to tenant!");
                    } else {
                        Tenant tenant = tenantDao.findById(requestTenantId, usageRecord.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("Asset is referencing to non-existent tenant!");
                        }
                    }
                    if (usageRecord.getEntityId() == null) {
                        throw new DataValidationException("UsageRecord should be assigned to entity!");
                    } else if (!EntityType.TENANT.equals(usageRecord.getEntityId().getEntityType())) {
                        throw new DataValidationException("Only Tenant Usage Records are supported!");
                    } else if (!usageRecord.getTenantId().getId().equals(usageRecord.getEntityId().getId())) {
                        throw new DataValidationException("Can't assign one Usage Record to multiple tenants!");
                    }
                }
            };

}
