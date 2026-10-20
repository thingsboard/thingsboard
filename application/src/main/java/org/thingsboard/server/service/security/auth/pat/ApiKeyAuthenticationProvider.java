// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.security.auth.pat;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.pat.ApiKeyService;
import org.thingsboard.server.service.security.auth.AbstractAuthenticationProvider;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.ApiKeyAuthRequest;
import org.thingsboard.server.service.security.permission.UserPermissionsService;
import org.thingsboard.server.service.user.cache.UserAuthDetailsCache;

@Component
public class ApiKeyAuthenticationProvider extends AbstractAuthenticationProvider {

    private final ApiKeyService apiKeyService;

    public ApiKeyAuthenticationProvider(ApiKeyService apiKeyService, UserAuthDetailsCache userAuthDetailsCache,
                                        UserPermissionsService userPermissionsService, CustomerService customerService) {
        super(customerService, userAuthDetailsCache, userPermissionsService);
        this.apiKeyService = apiKeyService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ApiKeyAuthRequest apiKeyAuthRequest = (ApiKeyAuthRequest) authentication.getCredentials();
        SecurityUser securityUser = authenticate(apiKeyAuthRequest.apiKey(), apiKeyAuthRequest.userId(), apiKeyAuthRequest.customerId());
        return new ApiKeyAuthenticationToken(securityUser);
    }

    public SecurityUser authenticate(String key) {
        return authenticate(key, null, null);
    }

    private SecurityUser authenticate(String key, UserId userIdInternal, CustomerId customerIdInternal) {
        if (StringUtils.isEmpty(key)) {
            throw new BadCredentialsException("Empty API key");
        }
        ApiKey apiKey = apiKeyService.findApiKeyByValue(key);
        if (apiKey == null) {
            throw new BadCredentialsException("User not found for the provided API key");
        }
        if (!apiKey.isEnabled()) {
            throw new DisabledException("API key auth is not active");
        }
        if (apiKey.getExpirationTime() != 0 && apiKey.getExpirationTime() < System.currentTimeMillis()) {
            throw new CredentialsExpiredException("API key is expired");
        }

        ResolvedUser resolvedUser = resolveUser(apiKey, userIdInternal, customerIdInternal);

        SecurityUser securityUser;

        if (resolvedUser.isPublicCustomer()) {
            securityUser = super.authenticateByPublicId(resolvedUser.customerId().toString(), "Internal API key", null);
        } else {
            securityUser = authenticateByUserId(resolvedUser.tenantId(), resolvedUser.userId(), apiKey.isInternal() ? apiKey.getPermissions() : null, apiKey.isInternal());
        }

        return securityUser;
    }

    private ResolvedUser resolveUser(ApiKey apiKey, UserId userIdInternal, CustomerId customerIdInternal) {
        if (apiKey.isInternal()) {
            if (userIdInternal != null && !userIdInternal.isNullUid()) {
                return new ResolvedUser(TenantId.SYS_TENANT_ID, userIdInternal, null, false);
            } else if (customerIdInternal != null && !customerIdInternal.isNullUid()) {
                return new ResolvedUser(TenantId.SYS_TENANT_ID, null, customerIdInternal, true);
            }
        }

        // Use API key's user (for regular keys or internal without additional headers)
        return new ResolvedUser(apiKey.getTenantId(), apiKey.getUserId(), null, false);
    }

    private record ResolvedUser(TenantId tenantId, UserId userId, CustomerId customerId, boolean isPublicCustomer) {}

    @Override
    public boolean supports(Class<?> authentication) {
        return ApiKeyAuthenticationToken.class.isAssignableFrom(authentication);
    }

}
