/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.security.auth.pat;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.pat.ApiKeyInfo;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@DaoSqlTest
public class ApiKeyAuthenticationProviderTest extends AbstractControllerTest {

    ApiKey savedApiKey;

    @Before
    public void setUp() throws Exception {
        loginTenantAdmin();

        ApiKeyInfo apiKeyInfo = constructApiKeyInfo();
        savedApiKey = doPost("/api/apiKey", apiKeyInfo, ApiKey.class);
        setApiKey(savedApiKey.getValue());
    }

    @After
    public void cleanUp() throws Exception {
        resetApiKey();
        doDelete("/api/apiKey/" + savedApiKey.getId()).andExpect(status().isOk());
    }

    @Test
    public void testSaveEdgeWithApiKey() throws Exception {
        Edge edge = constructEdge("My edge", "default");

        Mockito.reset(tbClusterService, auditLogService);

        Edge savedEdge = doPostWithApiKey("/api/edge", edge, Edge.class);

        Assert.assertNotNull(savedEdge);
        Assert.assertNotNull(savedEdge.getId());
        Assert.assertTrue(savedEdge.getCreatedTime() > 0);
        Assert.assertEquals(tenantId, savedEdge.getTenantId());
        Assert.assertNotNull(savedEdge.getCustomerId());
        Assert.assertEquals(NULL_UUID, savedEdge.getCustomerId().getId());
        Assert.assertEquals(edge.getName(), savedEdge.getName());

        testNotifyEdgeStateChangeEventManyTimeMsgToEdgeServiceNever(savedEdge, savedEdge.getId(), savedEdge.getId(),
                tenantId, tenantAdminUser.getCustomerId(), tenantAdminUser.getId(), tenantAdminUser.getEmail(),
                ActionType.ADDED, 2);

        savedEdge.setName("My new edge");
        doPostWithApiKey("/api/edge", savedEdge, Edge.class);

        Edge foundEdge = doGetWithApiKey("/api/edge/" + savedEdge.getId().getId().toString(), Edge.class);
        Assert.assertEquals(foundEdge.getName(), savedEdge.getName());

        testNotifyEdgeStateChangeEventManyTimeMsgToEdgeServiceNever(foundEdge, foundEdge.getId(), foundEdge.getId(),
                tenantId, tenantAdminUser.getCustomerId(), tenantAdminUser.getId(), tenantAdminUser.getEmail(),
                ActionType.UPDATED, 1);

        doDeleteWithApiKey("/api/edge/" + savedEdge.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testUnauthorizedWhenKeyDisabled() throws Exception {
        ApiKeyInfo disabledApiKeyInfo = doPut("/api/apiKey/" + savedApiKey.getId().getId() + "/enabled/false", Boolean.FALSE, ApiKeyInfo.class);
        Assert.assertFalse(disabledApiKeyInfo.isEnabled());
        doGetWithApiKey("/api/admin/featuresInfo").andExpect(status().isUnauthorized());
    }

    @Test
    public void testUnauthorizedWhenKeyExpired() throws Exception {
        ApiKeyInfo apiKeyInfo = constructApiKeyInfo();
        apiKeyInfo.setExpirationTime(System.currentTimeMillis() - 1000);
        ApiKey savedApiKeyWithBad = doPost("/api/apiKey", apiKeyInfo, ApiKey.class);
        setApiKey(savedApiKeyWithBad.getValue());
        doPost("/api/apiKey", savedApiKey, ApiKeyInfo.class);
        doGetWithApiKey("/api/admin/featuresInfo").andExpect(status().isUnauthorized());
    }

    @Test
    public void testUnauthorizedWhenUserCredentialsDisabled() throws Exception {
        User newUser = new User();
        newUser.setAuthority(Authority.TENANT_ADMIN);
        newUser.setTenantId(tenantId);
        newUser.setEmail("testUser" + RandomStringUtils.secure().nextAlphanumeric(10) + "@thingsboard.org");
        newUser.setFirstName("Test");
        newUser.setLastName("User");
        User savedUser = createUser(newUser, "testPassword1");

        ApiKeyInfo apiKeyInfo = new ApiKeyInfo();
        apiKeyInfo.setDescription("Test API key for user credentials test");
        apiKeyInfo.setEnabled(true);
        apiKeyInfo.setUserId(savedUser.getId());
        ApiKey testApiKey = doPost("/api/apiKey", apiKeyInfo, ApiKey.class);
        setApiKey(testApiKey.getValue());

        doGetWithApiKey("/api/admin/repositorySettings/exists").andExpect(status().isOk());

        doPost("/api/user/" + savedUser.getId().getId() + "/userCredentialsEnabled?userCredentialsEnabled=false").andExpect(status().isOk());

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> doGetWithApiKey("/api/admin/repositorySettings/exists").andExpect(status().isUnauthorized()));

        resetApiKey();
        doDelete("/api/apiKey/" + testApiKey.getId()).andExpect(status().isOk());
        loginSysAdmin();
        doDelete("/api/user/" + savedUser.getId().getId()).andExpect(status().isOk());
    }

    @Test
    public void testUnauthorizedWhenUserDeleted() throws Exception {
        User newUser = new User();
        newUser.setAuthority(Authority.TENANT_ADMIN);
        newUser.setTenantId(tenantId);
        newUser.setEmail("testUser" + RandomStringUtils.secure().nextAlphanumeric(10) + "@thingsboard.org");
        newUser.setFirstName("Test");
        newUser.setLastName("User");
        User savedUser = createUser(newUser, "testPassword1");

        ApiKeyInfo apiKeyInfo = new ApiKeyInfo();
        apiKeyInfo.setDescription("Test API key for user deletion test");
        apiKeyInfo.setEnabled(true);
        apiKeyInfo.setUserId(savedUser.getId());
        ApiKey testApiKey = doPost("/api/apiKey", apiKeyInfo, ApiKey.class);
        setApiKey(testApiKey.getValue());

        doGetWithApiKey("/api/admin/repositorySettings/exists").andExpect(status().isOk());

        loginSysAdmin();
        doDelete("/api/user/" + savedUser.getId().getId()).andExpect(status().isOk());

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> doGetWithApiKey("/api/admin/repositorySettings/exists").andExpect(status().isUnauthorized()));
    }

    private ApiKeyInfo constructApiKeyInfo() {
        ApiKeyInfo apiKeyInfo = new ApiKeyInfo();
        apiKeyInfo.setDescription("New API key description");
        apiKeyInfo.setEnabled(true);
        apiKeyInfo.setUserId(tenantAdminUserId);
        return apiKeyInfo;
    }

}
