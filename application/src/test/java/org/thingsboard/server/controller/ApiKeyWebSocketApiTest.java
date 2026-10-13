// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.pat.ApiKeyInfo;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.subscription.SubscriptionErrorCode;
import org.thingsboard.server.service.ws.log.cmd.LogsSubscriptionCmd;

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
    @Override
    public void testLogsSubscribe_accessDenied() throws Exception {
        // the API key from setUpApiKey is bound to the tenant admin, who can read the device, so reuse won't deny.
        // authenticate the WS session with an API key bound to the customer user, who has no READ_TELEMETRY permission.
        ApiKeyInfo customerApiKeyInfo = new ApiKeyInfo();
        customerApiKeyInfo.setDescription("WS test customer API key");
        customerApiKeyInfo.setEnabled(true);
        customerApiKeyInfo.setUserId(customerUserId);
        ApiKey customerApiKey = doPost("/api/apiKey", customerApiKeyInfo, ApiKey.class);

        TbTestWebSocketClient customerWsClient = buildAndConnectWebSocketClientWithApiKey(customerApiKey.getValue());
        try {
            customerWsClient.send(new LogsSubscriptionCmd(1, device.getId().getEntityType().name(),
                    device.getId().getId().toString(), 0));

            JsonNode error = JacksonUtil.toJsonNode(customerWsClient.waitForReply(true));
            assertThat(error.get("errorCode").asInt()).isEqualTo(SubscriptionErrorCode.ACCESS_DENIED.getCode());
        } finally {
            customerWsClient.close();
            loginTenantAdmin();
            doDelete("/api/apiKey/" + customerApiKey.getId()).andExpect(status().isOk());
        }
    }

    @Test
    public void testInvalidApiKeyAuthCmd_connectionClosed() throws Exception {
        TbTestWebSocketClient client = new TbTestWebSocketClient(new URI(WS_URL + serverPort + "/api/ws"));
        assertThat(client.connectBlocking(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        try {
            client.authenticateWithApiKey("invalid-key");
            assertThat(client.waitForClose()).isTrue();
        } finally {
            client.close();
        }
    }

}
