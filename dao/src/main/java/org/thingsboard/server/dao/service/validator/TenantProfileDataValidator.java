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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantProfileDao;
import org.thingsboard.server.dao.tenant.TenantProfileService;

@Component
public class TenantProfileDataValidator extends DataValidator<TenantProfile> {

    @Autowired
    private TenantProfileDao tenantProfileDao;

    @Autowired
    @Lazy
    private TenantProfileService tenantProfileService;

    @Override
    protected void validateDataImpl(TenantId tenantId, TenantProfile tenantProfile) {
        if (StringUtils.isEmpty(tenantProfile.getName())) {
            throw new DataValidationException("Tenant profile name should be specified!");
        }
        if (tenantProfile.getProfileData() == null) {
            throw new DataValidationException("Tenant profile data should be specified!");
        }
        if (tenantProfile.getProfileData().getConfiguration() == null) {
            throw new DataValidationException("Tenant profile data configuration should be specified!");
        }
        if (tenantProfile.isDefault()) {
            TenantProfile defaultTenantProfile = tenantProfileService.findDefaultTenantProfile(tenantId);
            if (defaultTenantProfile != null && !defaultTenantProfile.getId().equals(tenantProfile.getId())) {
                throw new DataValidationException("Another default tenant profile is present!");
            }
        }
    }

    @Override
    protected void validateUpdate(TenantId tenantId, TenantProfile tenantProfile) {
        TenantProfile old = tenantProfileDao.findById(TenantId.SYS_TENANT_ID, tenantProfile.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing tenant profile!");
        } else if (old.isIsolatedTbRuleEngine() != tenantProfile.isIsolatedTbRuleEngine()) {
            throw new DataValidationException("Can't update isolatedTbRuleEngine property!");
        } else if (old.isIsolatedTbCore() != tenantProfile.isIsolatedTbCore()) {
            throw new DataValidationException("Can't update isolatedTbCore property!");
        }
    }
}
