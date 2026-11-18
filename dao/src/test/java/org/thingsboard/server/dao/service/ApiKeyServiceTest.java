// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.ApiKeyId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.pat.ApiKeyInfo;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.pat.ApiKeyService;
import org.thingsboard.server.dao.user.UserService;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DaoSqlTest
public class ApiKeyServiceTest extends AbstractServiceTest {

    private static final String TEST_API_KEY_DESCRIPTION = "Test API Key Description";

    @Autowired
    ApiKeyService apiKeyService;

    @Autowired
    UserService userService;

    private UserId userId;

    @Before
    public void before() {
        User tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(tenantId);
        tenantAdmin.setEmail("tenant@thingsboard.org");
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
    public void testSaveApiKey() {
        ApiKeyInfo apiKeyInfo = createApiKeyInfo(TEST_API_KEY_DESCRIPTION);
        ApiKey savedApiKey = apiKeyService.saveApiKey(tenantId, apiKeyInfo);

        Assert.assertNotNull(savedApiKey);
        Assert.assertNotNull(savedApiKey.getId());
        Assert.assertEquals(tenantId, savedApiKey.getTenantId());
        Assert.assertEquals(TEST_API_KEY_DESCRIPTION, savedApiKey.getDescription());
        Assert.assertTrue(savedApiKey.isEnabled());
        Assert.assertNotNull(savedApiKey.getValue());
    }

    @Test
    public void testSaveApiKeyWithTooLongDescription() {
        ApiKeyInfo apiKeyInfo = createApiKeyInfo(StringUtils.randomAlphabetic(300));

        assertThatThrownBy(() -> apiKeyService.saveApiKey(tenantId, apiKeyInfo))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("description length must be equal or less than 255");
    }

    @Test
    public void testUpdateDescriptionApiKey() {
        ApiKeyInfo apiKeyInfo = createApiKeyInfo(TEST_API_KEY_DESCRIPTION);
        ApiKey savedApiKey = apiKeyService.saveApiKey(tenantId, apiKeyInfo);

        String newDescription = "Updated API Key Description";
        savedApiKey.setDescription(newDescription);
        ApiKey updatedApiKey = apiKeyService.saveApiKey(tenantId, savedApiKey);

        Assert.assertNotNull(updatedApiKey);
        Assert.assertEquals(savedApiKey.getId(), updatedApiKey.getId());
        Assert.assertEquals(newDescription, updatedApiKey.getDescription());
        Assert.assertEquals(savedApiKey.getValue(), updatedApiKey.getValue());
    }

    @Test
    public void testDisableApiKey() {
        ApiKeyInfo apiKeyInfo = createApiKeyInfo(TEST_API_KEY_DESCRIPTION);
        ApiKey savedApiKey = apiKeyService.saveApiKey(tenantId, apiKeyInfo);

        savedApiKey.setEnabled(false);
        ApiKey disabledApiKey = apiKeyService.saveApiKey(tenantId, savedApiKey);

        Assert.assertNotNull(disabledApiKey);
        Assert.assertEquals(savedApiKey.getId(), disabledApiKey.getId());
        Assert.assertFalse(disabledApiKey.isEnabled());
    }

    @Test
    public void testFindApiKeyById() {
        ApiKeyInfo apiKeyInfo = createApiKeyInfo(TEST_API_KEY_DESCRIPTION);
        ApiKey savedApiKey = apiKeyService.saveApiKey(tenantId, apiKeyInfo);

        ApiKey foundApiKey = apiKeyService.findApiKeyById(tenantId, savedApiKey.getId());

        Assert.assertNotNull(foundApiKey);
        Assert.assertEquals(savedApiKey.getId(), foundApiKey.getId());
        Assert.assertEquals(savedApiKey.getDescription(), foundApiKey.getDescription());
        Assert.assertEquals(savedApiKey.isEnabled(), foundApiKey.isEnabled());
        Assert.assertEquals(savedApiKey.getValue(), foundApiKey.getValue());
    }

    @Test
    public void testFindApiKeyByHash() {
        ApiKeyInfo apiKeyInfo = createApiKeyInfo(TEST_API_KEY_DESCRIPTION);
        ApiKey savedApiKey = apiKeyService.saveApiKey(tenantId, apiKeyInfo);

        ApiKey foundApiKey = apiKeyService.findApiKeyByValue(savedApiKey.getValue());

        Assert.assertNotNull(foundApiKey);
        Assert.assertEquals(savedApiKey.getId(), foundApiKey.getId());
        Assert.assertEquals(savedApiKey.getDescription(), foundApiKey.getDescription());
        Assert.assertEquals(savedApiKey.isEnabled(), foundApiKey.isEnabled());
        Assert.assertEquals(savedApiKey.getValue(), foundApiKey.getValue());
    }

    @Test
    public void testSaveApiKeyWithNewValue_oldValueEvictedFromCache() {
        ApiKeyInfo apiKeyInfo = createApiKeyInfo(TEST_API_KEY_DESCRIPTION);
        ApiKey savedApiKey = apiKeyService.saveApiKey(tenantId, apiKeyInfo);
        String originalValue = savedApiKey.getValue();
        // authentication resolves the key by value and caches the lookup
        Assert.assertNotNull(apiKeyService.findApiKeyByValue(originalValue));

        String newValue = "new-api-key-value-" + StringUtils.randomAlphanumeric(32);
        apiKeyService.saveApiKey(tenantId, savedApiKey, newValue, false);

        Assert.assertNull("Old API key value must not resolve after the value changed", apiKeyService.findApiKeyByValue(originalValue));
        ApiKey foundByNewValue = apiKeyService.findApiKeyByValue(newValue);
        Assert.assertNotNull(foundByNewValue);
        Assert.assertEquals(savedApiKey.getId(), foundByNewValue.getId());
    }

    @Test
    public void testSaveApiKeyWithNewValue_cachedMissForNewValueEvicted() {
        ApiKeyInfo apiKeyInfo = createApiKeyInfo(TEST_API_KEY_DESCRIPTION);
        ApiKey savedApiKey = apiKeyService.saveApiKey(tenantId, apiKeyInfo);

        String newValue = "new-api-key-value-" + StringUtils.randomAlphanumeric(32);
        // a failed authentication attempt caches the miss for the not-yet-saved value
        Assert.assertNull(apiKeyService.findApiKeyByValue(newValue));

        apiKeyService.saveApiKey(tenantId, savedApiKey, newValue, false);

        ApiKey foundByNewValue = apiKeyService.findApiKeyByValue(newValue);
        Assert.assertNotNull("New API key value must resolve after it is saved", foundByNewValue);
        Assert.assertEquals(savedApiKey.getId(), foundByNewValue.getId());
    }

    @Test
    public void testCreateApiKeyWithExplicitValue_cachedMissForValueEvicted() {
        String value = "edge-api-key-value-" + StringUtils.randomAlphanumeric(32);
        // a failed authentication attempt caches the miss before the edge uplink delivers the key
        Assert.assertNull(apiKeyService.findApiKeyByValue(value));

        // the edge uplink creates the key on the cloud with a preset id and the value generated on the edge
        ApiKeyInfo apiKeyInfo = createApiKeyInfo(TEST_API_KEY_DESCRIPTION);
        ApiKeyId apiKeyId = new ApiKeyId(Uuids.timeBased());
        apiKeyInfo.setId(apiKeyId);
        apiKeyInfo.setCreatedTime(Uuids.unixTimestamp(apiKeyId.getId()));
        ApiKey savedApiKey = apiKeyService.saveApiKey(tenantId, apiKeyInfo, value, false);

        ApiKey foundByValue = apiKeyService.findApiKeyByValue(value);
        Assert.assertNotNull("API key value must resolve after the key is created with it", foundByValue);
        Assert.assertEquals(savedApiKey.getId(), foundByValue.getId());
    }

    @Test
    public void testFindApiKeysByUserId() {
        int size = 3;
        for (int i = 0; i < size; i++) {
            ApiKeyInfo apiKeyInfo = createApiKeyInfo("API Key " + i);
            apiKeyService.saveApiKey(tenantId, apiKeyInfo);
        }

        PageLink pageLink = new PageLink(10);
        PageData<ApiKeyInfo> pageData = apiKeyService.findApiKeysByUserId(tenantId, userId, pageLink);

        Assert.assertNotNull(pageData);
        Assert.assertEquals(size, pageData.getData().size());
        Assert.assertEquals(size, pageData.getTotalElements());
    }

    @Test
    public void testDeleteApiKey() {
        ApiKeyInfo apiKeyInfo = createApiKeyInfo(TEST_API_KEY_DESCRIPTION);
        ApiKey savedApiKey = apiKeyService.saveApiKey(tenantId, apiKeyInfo);

        apiKeyService.deleteApiKey(tenantId, savedApiKey, false);

        ApiKey foundApiKey = apiKeyService.findApiKeyById(tenantId, savedApiKey.getId());
        Assert.assertNull(foundApiKey);
    }

    @Test
    public void testDeleteByTenantId() {
        for (int i = 0; i < 3; i++) {
            ApiKeyInfo apiKeyInfo = createApiKeyInfo("API Key " + i);
            apiKeyService.saveApiKey(tenantId, apiKeyInfo);
        }

        apiKeyService.deleteByTenantId(tenantId);

        PageLink pageLink = new PageLink(10);
        PageData<ApiKeyInfo> pageData = apiKeyService.findApiKeysByUserId(tenantId, userId, pageLink);

        Assert.assertNotNull(pageData);
        Assert.assertEquals(0, pageData.getData().size());
        Assert.assertEquals(0, pageData.getTotalElements());
    }

    @Test
    public void testDeleteByUserId() {
        int size = 3;
        for (int i = 0; i < size; i++) {
            ApiKeyInfo apiKeyInfo = createApiKeyInfo("API Key " + i);
            apiKeyService.saveApiKey(tenantId, apiKeyInfo);
        }

        PageData<ApiKeyInfo> pageData = apiKeyService.findApiKeysByUserId(tenantId, userId, new PageLink(10));
        Assert.assertNotNull(pageData);
        Assert.assertEquals(size, pageData.getData().size());
        Assert.assertEquals(size, pageData.getTotalElements());

        apiKeyService.deleteByUserId(tenantId, userId);

        pageData = apiKeyService.findApiKeysByUserId(tenantId, userId, new PageLink(10));
        Assert.assertNotNull(pageData);
        Assert.assertEquals(0, pageData.getData().size());
        Assert.assertEquals(0, pageData.getTotalElements());
    }

    private ApiKeyInfo createApiKeyInfo(String description) {
        ApiKeyInfo apiKeyInfo = new ApiKeyInfo();
        apiKeyInfo.setTenantId(tenantId);
        apiKeyInfo.setUserId(userId);
        apiKeyInfo.setDescription(description);
        apiKeyInfo.setEnabled(true);
        return apiKeyInfo;
    }

}
