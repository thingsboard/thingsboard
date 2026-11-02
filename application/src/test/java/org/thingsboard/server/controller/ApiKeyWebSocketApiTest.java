// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.ThrowingConsumer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.pat.ApiKeyInfo;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.subscription.SubscriptionErrorCode;
import org.thingsboard.server.service.subscription.TbAttributeSubscriptionScope;
import org.thingsboard.server.service.ws.log.cmd.LogsSubscriptionCmd;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
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
        withCustomerWsClient(customerWsClient -> {
            customerWsClient.send(new LogsSubscriptionCmd(1, device.getId().getEntityType().name(),
                    device.getId().getId().toString(), 0));

            JsonNode error = JacksonUtil.toJsonNode(customerWsClient.waitForReply(true));
            assertThat(error.get("errorCode").asInt()).isEqualTo(SubscriptionErrorCode.ACCESS_DENIED.getCode());
        });
    }

    @Test
    @Override
    public void testAttributesSubscription_customerUserNotAssignedDevice() throws Exception {
        sendAttributes(device, TbAttributeSubscriptionScope.SERVER_SCOPE, List.of(
                new BaseAttributeKvEntry(System.currentTimeMillis(), new StringDataEntry("attr", "secret"))
        ));

        withCustomerWsClient(customerWsClient -> {
            JsonNode update = customerWsClient.subscribeForAttributes(device.getId(), TbAttributeSubscriptionScope.SERVER_SCOPE.name(), List.of("attr"));
            assertThat(update.get("errorCode").asInt()).isNotEqualTo(SubscriptionErrorCode.NO_ERROR.getCode());
            assertThat(update.path("data").isEmpty()).isTrue();
        });
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

    // the API key from setUpApiKey is bound to the tenant admin, so access-denied cases need a key bound to the customer user
    private void withCustomerWsClient(ThrowingConsumer<TbTestWebSocketClient> action) throws Exception {
        ApiKeyInfo customerApiKeyInfo = new ApiKeyInfo();
        customerApiKeyInfo.setDescription("WS test customer API key");
        customerApiKeyInfo.setEnabled(true);
        customerApiKeyInfo.setUserId(customerUserId);
        ApiKey customerApiKey = doPost("/api/apiKey", customerApiKeyInfo, ApiKey.class);

        TbTestWebSocketClient customerWsClient = buildAndConnectWebSocketClientWithApiKey(customerApiKey.getValue());
        try {
            action.accept(customerWsClient);
        } finally {
            customerWsClient.close();
            loginTenantAdmin();
            doDelete("/api/apiKey/" + customerApiKey.getId()).andExpect(status().isOk());
        }
    }

}
