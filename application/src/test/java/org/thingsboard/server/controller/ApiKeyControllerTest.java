// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.pat.ApiKeyInfo;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class ApiKeyControllerTest extends AbstractControllerTest {

    @Before
    public void setUp() throws Exception {
        loginTenantAdmin();
    }

    @Test
    public void testSaveApiKey() throws Exception {
        ApiKeyInfo apiKeyInfo = constructApiKeyInfo("New API key description", true);

        doPost("/api/apiKey", apiKeyInfo, ApiKey.class);

        PageData<ApiKeyInfo> pageData = doGetTypedWithPageLink("/api/apiKeys/" + tenantAdminUserId + "?", new TypeReference<>() {}, new PageLink(10, 0));
        Assert.assertEquals(1, pageData.getData().size());

        ApiKeyInfo savedApiKey = pageData.getData().get(0);
        Assert.assertNotNull(savedApiKey);
        Assert.assertEquals(apiKeyInfo.getDescription(), savedApiKey.getDescription());
        Assert.assertEquals(apiKeyInfo.isEnabled(), savedApiKey.isEnabled());
        Assert.assertEquals(tenantId, savedApiKey.getTenantId());
        Assert.assertEquals(tenantAdminUser.getId(), savedApiKey.getUserId());

        String newDescription = "Updated API Key Description";
        savedApiKey.setDescription(newDescription);
        ApiKey updatedApiKey = doPost("/api/apiKey", savedApiKey, ApiKey.class);
        Assert.assertNotNull(updatedApiKey);
        Assert.assertEquals(newDescription, updatedApiKey.getDescription());
        Assert.assertNull("Verify we do not expose API key value on update", updatedApiKey.getValue());

        doDelete("/api/apiKey/" + updatedApiKey.getId()).andExpect(status().isOk());
    }

    @Test
    public void tesFindUserApiKeys() throws Exception {
        PageData<ApiKeyInfo> pageData = doGetTypedWithPageLink("/api/apiKeys/" + tenantAdminUserId + "?", new TypeReference<>() {}, new PageLink(10, 0));
        Assert.assertTrue(pageData.getData().isEmpty());

        ApiKeyInfo apiKeyInfo = constructApiKeyInfo("Test API key description", true);
        int expectedSize = 10;
        for (int i = 0; i < expectedSize; i++) {
            doPost("/api/apiKey", apiKeyInfo, ApiKey.class);
        }

        PageData<ApiKeyInfo> pageData2 = doGetTypedWithPageLink("/api/apiKeys/" + tenantAdminUserId + "?", new TypeReference<>() {}, new PageLink(10, 0));
        Assert.assertEquals(expectedSize, pageData2.getData().size());

        pageData2.getData().forEach(apiKey -> {
            try {
                doDelete("/api/apiKey/" + apiKey.getId()).andExpect(status().isOk());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testUpdateApiKeyDescription() throws Exception {
        ApiKeyInfo apiKeyInfo = constructApiKeyInfo("Test API key description", true);
        doPost("/api/apiKey", apiKeyInfo, ApiKey.class);

        PageData<ApiKeyInfo> pageData = doGetTypedWithPageLink("/api/apiKeys/" + tenantAdminUserId + "?", new TypeReference<>() {}, new PageLink(10, 0));
        Assert.assertEquals(1, pageData.getData().size());

        ApiKeyInfo savedApiKey = pageData.getData().get(0);

        String newDescription = "Updated API Key Description";

        ApiKeyInfo updatedApiKeyInfo = doPut("/api/apiKey/" + savedApiKey.getId().getId() + "/description", newDescription, ApiKeyInfo.class);
        Assert.assertNotNull(updatedApiKeyInfo);
        Assert.assertEquals(newDescription, updatedApiKeyInfo.getDescription());

        doDelete("/api/apiKey/" + savedApiKey.getId()).andExpect(status().isOk());
    }

    @Test
    public void testEnableApiKey() throws Exception {
        ApiKeyInfo apiKeyInfo = constructApiKeyInfo("Test API key description", true);
        doPost("/api/apiKey", apiKeyInfo, ApiKey.class);

        PageData<ApiKeyInfo> pageData = doGetTypedWithPageLink("/api/apiKeys/" + tenantAdminUserId + "?", new TypeReference<>() {}, new PageLink(10, 0));
        Assert.assertEquals(1, pageData.getData().size());

        ApiKeyInfo savedApiKey = pageData.getData().get(0);

        ApiKeyInfo disabledApiKeyInfo = doPut("/api/apiKey/" + savedApiKey.getId().getId() + "/enabled/false", Boolean.FALSE, ApiKeyInfo.class);
        Assert.assertNotNull(disabledApiKeyInfo);
        Assert.assertFalse(disabledApiKeyInfo.isEnabled());

        ApiKeyInfo enabledApiKeyInfo = doPut("/api/apiKey/" + savedApiKey.getId().getId() + "/enabled/true", Boolean.TRUE, ApiKeyInfo.class);
        Assert.assertNotNull(enabledApiKeyInfo);
        Assert.assertTrue(enabledApiKeyInfo.isEnabled());

        doDelete("/api/apiKey/" + savedApiKey.getId()).andExpect(status().isOk());
    }

    @Test
    public void testDeleteApiKey() throws Exception {
        doDelete("/api/apiKey/" + UUID.randomUUID()).andExpect(status().isNotFound());

        ApiKeyInfo apiKeyInfo = constructApiKeyInfo("Test API key description", false);
        doPost("/api/apiKey", apiKeyInfo, ApiKey.class);

        PageData<ApiKeyInfo> pageData = doGetTypedWithPageLink("/api/apiKeys/" + tenantAdminUserId + "?", new TypeReference<>() {}, new PageLink(10, 0));
        Assert.assertEquals(1, pageData.getData().size());
        ApiKeyInfo savedApiKey = pageData.getData().get(0);

        doDelete("/api/apiKey/" + savedApiKey.getId().getId()).andExpect(status().isOk());
    }

    private ApiKeyInfo constructApiKeyInfo(String description, boolean enabled) {
        ApiKeyInfo apiKeyInfo = new ApiKeyInfo();
        apiKeyInfo.setDescription(description);
        apiKeyInfo.setEnabled(enabled);
        apiKeyInfo.setUserId(tenantAdminUserId);
        return apiKeyInfo;
    }

}
