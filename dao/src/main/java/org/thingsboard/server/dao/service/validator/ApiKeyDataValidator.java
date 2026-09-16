// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.service.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.pat.ApiKeyDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.user.UserService;

@Component
@RequiredArgsConstructor
public class ApiKeyDataValidator extends DataValidator<ApiKey> {

    private final ApiKeyDao apiKeyDao;
    private final TenantService tenantService;
    private final UserService userService;

    @Override
    protected void validateDataImpl(TenantId tenantId, ApiKey apiKey) {
        if (apiKey.getId() != null) {
            if (apiKey.getUuidId() == null) {
                throw new DataValidationException("API Key UUID should be specified!");
            }
            if (apiKey.getId().isNullUid()) {
                throw new DataValidationException("API key UUID must not be the reserved null value!");
            }
        }

        if (apiKey.getTenantId() == null || apiKey.getTenantId().getId() == null) {
            throw new DataValidationException("API key should be assigned to tenant!");
        }
        if (!TenantId.SYS_TENANT_ID.equals(apiKey.getTenantId()) && !tenantService.tenantExists(apiKey.getTenantId())) {
            throw new DataValidationException("API key reference a non-existent tenant!");
        }

        if (apiKey.getUserId() == null || apiKey.getUserId().getId() == null) {
            throw new DataValidationException("API key should be assigned to user!");
        }
        if (userService.findUserById(apiKey.getTenantId(), apiKey.getUserId()) == null) {
            throw new DataValidationException("API key reference a non-existent user!");
        }
    }

    @Override
    protected ApiKey validateUpdate(TenantId tenantId, ApiKey apiKey) {
        ApiKey old = apiKeyDao.findById(tenantId, apiKey.getUuidId());
        if (old == null) {
            throw new DataValidationException("Cannot update non-existent API key!");
        }
        if (!old.getUserId().equals(apiKey.getUserId())) {
            throw new DataValidationException("Cannot update API key user id!");
        }
        if (old.getExpirationTime() != apiKey.getExpirationTime()) {
            throw new DataValidationException("Cannot update API key expiration time!");
        }
        return old;
    }

}
