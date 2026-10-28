// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.pat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.pat.ApiKeyInfo;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.user.UserService;

/**
 * Verifies the exact cache interactions of {@link ApiKeyService} rotation: which {@link ApiKeyCacheKey}s are evicted
 * and how many times. Lives in this package because {@link ApiKeyCacheKey} is package-private.
 */
@DaoSqlTest
public class ApiKeyCacheEvictionTest extends AbstractServiceTest {

    @Autowired
    ApiKeyService apiKeyService;

    @Autowired
    UserService userService;

    @MockitoSpyBean
    TbTransactionalCache<ApiKeyCacheKey, ApiKey> apiKeyCache;

    private UserId userId;

    @Before
    public void before() {
        User tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(tenantId);
        tenantAdmin.setEmail("tenant-api-key-cache@thingsboard.org");
        User user = userService.saveUser(TenantId.SYS_TENANT_ID, tenantAdmin);
        userId = user.getId();
    }

    @After
    public void after() {
        apiKeyService.deleteByTenantId(tenantId);
        User user = userService.findUserById(tenantId, userId);
        userService.deleteUser(tenantId, user);
    }

    @Test
    public void testRotateInternalApiKey_evictsOldValueExactlyOnce() {
        ApiKeyInfo apiKeyInfo = new ApiKeyInfo();
        apiKeyInfo.setTenantId(TenantId.SYS_TENANT_ID);
        apiKeyInfo.setUserId(userId);
        apiKeyInfo.setDescription("Internal key for rotation eviction counting");
        apiKeyInfo.setEnabled(true);
        apiKeyInfo.setInternal(true);
        ApiKey savedApiKey = apiKeyService.saveApiKey(TenantId.SYS_TENANT_ID, apiKeyInfo);
        String originalValue = savedApiKey.getValue();
        Mockito.clearInvocations(apiKeyCache);

        apiKeyService.rotateInternalApiKey(TenantId.SYS_TENANT_ID, savedApiKey);

        Mockito.verify(apiKeyCache).evict(ApiKeyCacheKey.of(originalValue));
        Mockito.verify(apiKeyCache, Mockito.times(1)).evict(Mockito.any(ApiKeyCacheKey.class));
    }

}
