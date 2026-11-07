// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.http.thingpark;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.converter.TBDownlinkDataConverter;
import org.thingsboard.integration.api.data.DownlinkData;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ThingParkIntegrationEnterpriseTest {

    private static final String DEV_EUI = "0102030405060708";
    private static final String DEVICE_REF = "42";

    private HttpServer server;
    private final List<String> downlinkBodies = new CopyOnWriteArrayList<>();

    private ThingParkIntegrationEnterprise integration;
    private TBDownlinkDataConverter downlinkConverter;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/admin/latest/api/oauth/token", exchange ->
                respond(exchange, "{\"expires_in\": 3600, \"token_type\": \"Bearer\", \"access_token\": \"test-token\"}"));
        server.createContext("/core/latest/api/devices", exchange -> {
            if (exchange.getRequestURI().getPath().endsWith("/downlinkMessages")) {
                downlinkBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                respond(exchange, "{}");
            } else {
                respond(exchange, "[{\"EUI\": \"" + DEV_EUI + "\", \"ref\": \"" + DEVICE_REF + "\"}]");
            }
        });
        server.start();

        integration = new ThingParkIntegrationEnterprise();
        downlinkConverter = mock(TBDownlinkDataConverter.class);
        integration.init(new TbIntegrationInitParams(mock(IntegrationContext.class), buildIntegrationConfig(), null, downlinkConverter));
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void downlinkUsesFPortFromConverterMetadata() throws Exception {
        sendDownlink("7");
        sendDownlink("7");

        assertThat(downlinkBodies).hasSize(2);
        assertThat(downlinkBodies).allSatisfy(body -> {
            assertThat(JacksonUtil.toJsonNode(body).get("targetPorts").asText()).isEqualTo("7");
            assertThat(JacksonUtil.toJsonNode(body).get("payloadHex").asText()).isEqualTo("6F6E");
        });
    }

    @Test
    void downlinkRotatesTargetPortsWhenFPortIsAbsent() throws Exception {
        sendDownlink(null);
        sendDownlink(null);

        assertThat(downlinkBodies).hasSize(2);
        assertThat(JacksonUtil.toJsonNode(downlinkBodies.get(0)).get("targetPorts").asText()).isEqualTo("1");
        assertThat(JacksonUtil.toJsonNode(downlinkBodies.get(1)).get("targetPorts").asText()).isEqualTo("2");
    }

    private void sendDownlink(String fPort) throws Exception {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("DevEUI", DEV_EUI);
        if (fPort != null) {
            metadata.put("FPort", fPort);
        }
        DownlinkData converted = DownlinkData.builder()
                .contentType("JSON")
                .data(("{\"DevEUI\": \"" + DEV_EUI + "\", \"payload\": \"6F6E\"}").getBytes(StandardCharsets.UTF_8))
                .metadata(metadata)
                .build();
        when(downlinkConverter.convertDownLink(any(), any(), any())).thenReturn(List.of(converted));

        TbMsg tbMsg = TbMsg.newMsg()
                .id(UUID.randomUUID())
                .type("ATTRIBUTES_UPDATED")
                .originator(new IntegrationId(UUID.randomUUID()))
                .copyMetaData(new TbMsgMetaData())
                .data("{\"powerState\": \"on\"}")
                .build();
        IntegrationDownlinkMsg downlink = mock(IntegrationDownlinkMsg.class);
        when(downlink.getTbMsg()).thenReturn(tbMsg);

        integration.onDownlinkMsg(downlink);
    }

    private Integration buildIntegrationConfig() {
        ObjectNode root = JacksonUtil.newObjectNode();
        root.putObject("metadata");
        root.put("downlinkUrl", "http://localhost:" + server.getAddress().getPort());
        root.put("clientIdNew", "test-client");
        root.put("clientSecret", "test-secret");

        Integration integration = new Integration();
        integration.setId(new IntegrationId(UUID.randomUUID()));
        integration.setTenantId(TenantId.SYS_TENANT_ID);
        integration.setName("test-tpe");
        integration.setType(IntegrationType.TPE);
        integration.setConfiguration(root);
        return integration;
    }

    private static void respond(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

}
