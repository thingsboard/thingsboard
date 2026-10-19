// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantDao;

@Component
public class TenantDataValidator extends DataValidator<Tenant> {

    @Autowired
    private TenantDao tenantDao;

    @Override
    protected void validateDataImpl(TenantId tenantId, Tenant tenant) {
        validateString("Tenant title", tenant.getTitle());
        if (!StringUtils.isEmpty(tenant.getEmail())) {
            validateEmail(tenant.getEmail());
        }
    }

    @Override
    protected Tenant validateUpdate(TenantId tenantId, Tenant tenant) {
        Tenant old = tenantDao.findById(TenantId.SYS_TENANT_ID, tenantId.getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing tenant!");
        }
        return old;
    }

}
