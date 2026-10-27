// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The embedded server is reached over loopback, which this test's configuration explicitly lists
 * as a trusted proxy, so the X-Forwarded-For header steers the resolved client address end to
 * end: through the capture servlet filter on the WS handshake, the handshake interceptor, and
 * the handler's check.
 */
@DaoSqlTest
@TestPropertySource(properties = {
        "security.sysadmin_allowlist.allowed_addresses=127.0.0.1,::1,203.0.113.0/24",
        "security.sysadmin_allowlist.trusted_proxies=127.0.0.1,::1"
})
public class WsAllowlistTest extends AbstractControllerTest {

    private static final String BLOCKED_IP = "10.0.0.9";
    private static final String ALLOWED_IP = "203.0.113.5";

    @Test
    public void sysAdminWsRejectedWhenResolvedAddressOutsideList() throws Exception {
        loginSysAdmin();
        TbTestWebSocketClient client = connect(BLOCKED_IP);
        try {
            client.authenticate(token);
            assertThat(client.waitForClose()).isTrue();
        } finally {
            client.close();
        }
    }

    @Test
    public void sysAdminWsAcceptedFromAllowedAddress() throws Exception {
        loginSysAdmin();
        TbTestWebSocketClient client = connect(ALLOWED_IP);
        try {
            client.authenticate(token);
            // any command reply proves the session authenticated and stayed open
            JsonNode reply = client.subscribeForAttributes(new DeviceId(UUID.randomUUID()), "SERVER_SCOPE", List.of("attr"));
            assertThat(reply).isNotNull();
            assertThat(client.getCloseCode()).isNull();
        } finally {
            client.close();
        }
    }

    @Test
    public void tenantWsUnaffectedFromOutsideAddress() throws Exception {
        loginTenantAdmin();
        TbTestWebSocketClient client = connect(BLOCKED_IP);
        try {
            client.authenticate(token);
            JsonNode reply = client.subscribeForAttributes(new DeviceId(UUID.randomUUID()), "SERVER_SCOPE", List.of("attr"));
            assertThat(reply).isNotNull();
            assertThat(client.getCloseCode()).isNull();
        } finally {
            client.close();
        }
    }

    private TbTestWebSocketClient connect(String forwardedFor) throws Exception {
        TbTestWebSocketClient client = new TbTestWebSocketClient(new URI(WS_URL + serverPort + "/api/ws"));
        client.addHeader("X-Forwarded-For", forwardedFor);
        assertThat(client.connectBlocking(TIMEOUT, TimeUnit.SECONDS)).isTrue();
        return client;
    }

}
