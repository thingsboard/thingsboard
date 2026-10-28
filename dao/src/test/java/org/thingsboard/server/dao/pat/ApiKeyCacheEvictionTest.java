// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.pat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.ApiKeyId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.pat.ApiKeyInfo;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.user.UserService;

/**
 * Verifies the exact cache interactions of {@link ApiKeyService} saves: which {@link ApiKeyCacheKey}s are evicted
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
    public void testSaveApiKeyWithNewValue_evictsOldAndNewValueExactlyOnce() {
        ApiKey savedApiKey = apiKeyService.saveApiKey(tenantId, createApiKeyInfo());
        String originalValue = savedApiKey.getValue();
        String newValue = "new-api-key-value-" + StringUtils.randomAlphanumeric(32);
        Mockito.clearInvocations(apiKeyCache);

        apiKeyService.saveApiKey(tenantId, savedApiKey, newValue, false);

        Mockito.verify(apiKeyCache).evict(ApiKeyCacheKey.of(originalValue));
        Mockito.verify(apiKeyCache).evict(ApiKeyCacheKey.of(newValue));
        Mockito.verify(apiKeyCache, Mockito.times(2)).evict(Mockito.any(ApiKeyCacheKey.class));
    }

    @Test
    public void testCreateApiKeyWithExplicitValue_evictsValueExactlyOnce() {
        // the edge uplink creates the key on the cloud with a preset id and the value generated on the edge
        ApiKeyInfo apiKeyInfo = createApiKeyInfo();
        ApiKeyId apiKeyId = new ApiKeyId(Uuids.timeBased());
        apiKeyInfo.setId(apiKeyId);
        apiKeyInfo.setCreatedTime(Uuids.unixTimestamp(apiKeyId.getId()));
        String value = "edge-api-key-value-" + StringUtils.randomAlphanumeric(32);
        Mockito.clearInvocations(apiKeyCache);

        apiKeyService.saveApiKey(tenantId, apiKeyInfo, value, false);

        Mockito.verify(apiKeyCache).evict(ApiKeyCacheKey.of(value));
        Mockito.verify(apiKeyCache, Mockito.times(1)).evict(Mockito.any(ApiKeyCacheKey.class));
    }

    @Test
    public void testDisableApiKey_evictsValueExactlyOnce() {
        ApiKey savedApiKey = apiKeyService.saveApiKey(tenantId, createApiKeyInfo());
        String value = savedApiKey.getValue();
        savedApiKey.setEnabled(false);
        Mockito.clearInvocations(apiKeyCache);

        apiKeyService.saveApiKey(tenantId, savedApiKey);

        Mockito.verify(apiKeyCache).evict(ApiKeyCacheKey.of(value));
        Mockito.verify(apiKeyCache, Mockito.times(1)).evict(Mockito.any(ApiKeyCacheKey.class));
    }

    @Test
    public void testUpdateApiKeyDescription_noEviction() {
        ApiKey savedApiKey = apiKeyService.saveApiKey(tenantId, createApiKeyInfo());
        savedApiKey.setDescription("Updated description, cached state unchanged");
        Mockito.clearInvocations(apiKeyCache);

        apiKeyService.saveApiKey(tenantId, savedApiKey);

        Mockito.verify(apiKeyCache, Mockito.never()).evict(Mockito.any(ApiKeyCacheKey.class));
    }

    private ApiKeyInfo createApiKeyInfo() {
        ApiKeyInfo apiKeyInfo = new ApiKeyInfo();
        apiKeyInfo.setTenantId(tenantId);
        apiKeyInfo.setUserId(userId);
        apiKeyInfo.setDescription("API key cache eviction test");
        apiKeyInfo.setEnabled(true);
        return apiKeyInfo;
    }

}
