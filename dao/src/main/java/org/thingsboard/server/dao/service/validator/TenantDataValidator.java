/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import org.thingsboard.server.common.data.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.dao.tenant.TenantProfileService;

@Component
public class TenantDataValidator extends DataValidator<Tenant> {

    @Value("${zk.enabled}")
    private Boolean zkEnabled;

    @Autowired
    private TenantProfileService tenantProfileService;

    @Autowired
    private TenantDao tenantDao;

    @Override
    protected void validateDataImpl(TenantId tenantId, Tenant tenant) {
        if (StringUtils.isEmpty(tenant.getTitle())) {
            throw new DataValidationException("Tenant title should be specified!");
        }
        if (!StringUtils.isEmpty(tenant.getEmail())) {
            validateEmail(tenant.getEmail());
        }
        validateTenantProfile(tenantId, tenant);
    }

    @Override
    protected void validateUpdate(TenantId tenantId, Tenant tenant) {
        Tenant old = tenantDao.findById(TenantId.SYS_TENANT_ID, tenantId.getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing tenant!");
        }
        validateTenantProfile(tenantId, tenant);
    }

    private void validateTenantProfile(TenantId tenantId, Tenant tenant) {
        TenantProfile tenantProfileById = tenantProfileService.findTenantProfileById(tenantId, tenant.getTenantProfileId());
        if (!zkEnabled && (tenantProfileById.isIsolatedTbCore() || tenantProfileById.isIsolatedTbRuleEngine())) {
            throw new DataValidationException("Can't use isolated tenant profiles in monolith setup!");
        }
    }
}
