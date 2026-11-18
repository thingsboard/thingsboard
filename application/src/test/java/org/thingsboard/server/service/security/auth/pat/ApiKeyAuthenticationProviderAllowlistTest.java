// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.security.auth.pat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.pat.ApiKeyService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.UserPermissionsService;
import org.thingsboard.server.common.data.UserAuthDetails;
import org.thingsboard.server.service.user.cache.UserAuthDetailsCache;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ApiKeyAuthenticationProviderAllowlistTest {

    @Mock
    private ApiKeyService apiKeyService;
    @Mock
    private UserAuthDetailsCache userAuthDetailsCache;
    @Mock
    private UserPermissionsService userPermissionsService;
    @Mock
    private CustomerService customerService;

    private ApiKeyAuthenticationProvider provider;
    private final UserId impersonatedUserId = new UserId(UUID.randomUUID());

    @BeforeEach
    void setUp() throws Exception {
        provider = new ApiKeyAuthenticationProvider(apiKeyService, userAuthDetailsCache, userPermissionsService, customerService);

        User impersonated = new User();
        impersonated.setId(impersonatedUserId);
        impersonated.setAuthority(Authority.TENANT_ADMIN);
        impersonated.setTenantId(TenantId.fromUUID(UUID.randomUUID()));
        impersonated.setEmail("tenant@example.com");
        when(userAuthDetailsCache.getUserAuthDetails(any(), eq(impersonatedUserId)))
                .thenReturn(new UserAuthDetails(impersonated, true));
        when(userPermissionsService.getMergedPermissions(any(), anyBoolean())).thenReturn(null);
    }

    @Test
    void internalSysTenantKeyImpersonatingStaysSysAdminScoped() {
        when(apiKeyService.findApiKeyByValue("the-key")).thenReturn(internalKey(TenantId.SYS_TENANT_ID));
        SecurityUser user = authenticateImpersonating();
        assertThat(user.getAuthority()).isEqualTo(Authority.TENANT_ADMIN);
        assertThat(user.isSysAdminOwnedCredential()).isTrue();
    }

    @Test
    void tenantOwnedKeyNotSysAdminScoped() {
        when(apiKeyService.findApiKeyByValue("the-key")).thenReturn(internalKey(TenantId.fromUUID(UUID.randomUUID())));
        SecurityUser user = authenticateImpersonating();
        assertThat(user.isSysAdminOwnedCredential()).isFalse();
    }

    private SecurityUser authenticateImpersonating() {
        ApiKeyAuthenticationToken token = new ApiKeyAuthenticationToken(
                new org.thingsboard.server.service.security.model.token.ApiKeyAuthRequest("the-key", impersonatedUserId, null));
        return (SecurityUser) provider.authenticate(token).getPrincipal();
    }

    private ApiKey internalKey(TenantId ownerTenantId) {
        ApiKey apiKey = new ApiKey();
        apiKey.setTenantId(ownerTenantId);
        apiKey.setUserId(new UserId(UUID.randomUUID()));
        apiKey.setEnabled(true);
        apiKey.setExpirationTime(0);
        apiKey.setInternal(true);
        return apiKey;
    }

}
