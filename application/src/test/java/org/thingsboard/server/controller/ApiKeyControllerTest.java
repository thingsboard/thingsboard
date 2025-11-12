/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

        doDelete("/api/apiKey/" + savedApiKey.getId()).andExpect(status().isOk());
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
