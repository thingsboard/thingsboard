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
package org.thingsboard.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.pat.ApiKeyInfo;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@DaoSqlTest
public class ApiKeyWebSocketApiTest extends WebSocketApiTest {

    private ApiKey apiKey;

    @Before
    public void setUpApiKey() {
        ApiKeyInfo apiKeyInfo = new ApiKeyInfo();
        apiKeyInfo.setDescription("WS test API key");
        apiKeyInfo.setEnabled(true);
        apiKeyInfo.setUserId(tenantAdminUserId);
        apiKey = doPost("/api/apiKey", apiKeyInfo, ApiKey.class);
    }

    @After
    public void tearDownApiKey() throws Exception {
        loginTenantAdmin();
        doDelete("/api/apiKey/" + apiKey.getId()).andExpect(status().isOk());
    }

    @Override
    protected TbTestWebSocketClient buildAndConnectWebSocketClient() throws URISyntaxException, InterruptedException {
        return buildAndConnectWebSocketClientWithApiKey(apiKey.getValue());
    }

    @Test
    public void testInvalidApiKeyAuthCmd_connectionClosed() throws Exception {
        TbTestWebSocketClient client = new TbTestWebSocketClient(new URI(WS_URL + wsPort + "/api/ws"));
        assertThat(client.connectBlocking(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        try {
            client.authenticateWithApiKey("invalid-key");
            assertThat(client.waitForClose()).isTrue();
        } finally {
            client.close();
        }
    }

}
