// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.http.chirpstack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Drives the real {@link ChirpStackIntegration} against a live ChirpStack v4 server in
 * Testcontainers — proves the full downlink path (TbMsg -> downlinkConverter -> HTTP POST to CS)
 * end-to-end and locks in the queued-item shape as the contract for any fPort-type changes.
 */
@Slf4j
class ChirpStackIntegrationEndToEndTest {

    private static final String DEV_EUI = "0102030405060708";

    private static ChirpStackTestStack stack;
    private static ChirpStackTestClient cs;

    @BeforeAll
    static void setUpStack() {
        stack = new ChirpStackTestStack();
        stack.start();
        cs = new ChirpStackTestClient(stack.getRestApiUrl(), stack.getApiToken());

        String tenantId = cs.createTenant("e2e-tenant");
        String applicationId = cs.createApplication(tenantId, "e2e-app");
        String deviceProfileId = cs.createDeviceProfile(tenantId, "e2e-profile");
        cs.createDevice(applicationId, deviceProfileId, DEV_EUI, "e2e-device");
    }

    @AfterAll
    static void tearDown() {
        if (stack != null) {
            stack.close();
        }
    }

    @BeforeEach
    void resetQueue() {
        cs.flushDeviceQueue(DEV_EUI);
    }

    @Test
    void downlinkReachesChirpStackAndIsStoredWithCorrectFPort() throws Exception {
        ChirpStackIntegration integration = new ChirpStackIntegration();
        IntegrationContext context = mock(IntegrationContext.class);
        TBDownlinkDataConverter downlinkConverter = mock(TBDownlinkDataConverter.class);

        Integration configuration = buildIntegrationConfig(true);
        integration.init(new TbIntegrationInitParams(context, configuration, null, downlinkConverter));

        DownlinkData converted = DownlinkData.builder()
                .contentType("TEXT")
                .data("Kj8=".getBytes(StandardCharsets.UTF_8))
                .metadata(downlinkMetadata(80))
                .build();
        when(downlinkConverter.convertDownLink(any(), any(), any())).thenReturn(List.of(converted));

        TbMsg tbMsg = TbMsg.newMsg()
                .id(UUID.randomUUID())
                .type("POST_TELEMETRY_REQUEST")
                .originator(new IntegrationId(UUID.randomUUID()))
                .copyMetaData(new TbMsgMetaData(Map.of("DevEUI", DEV_EUI, "fPort", "80")))
                .data("{}")
                .build();
        IntegrationDownlinkMsg downlink = mock(IntegrationDownlinkMsg.class);
        when(downlink.getTbMsg()).thenReturn(tbMsg);

        integration.onDownlinkMsg(downlink);

        JsonNode queue = cs.getDeviceQueue(DEV_EUI);
        JsonNode items = queue.get("result");
        assertThat(items.isArray()).isTrue();
        assertThat(items.size()).isEqualTo(1);
        JsonNode stored = items.get(0);
        assertThat(stored.get("fPort").isInt()).isTrue();
        assertThat(stored.get("fPort").asInt()).isEqualTo(80);
        assertThat(stored.get("data").asText()).isEqualTo("Kj8=");
        assertThat(stored.get("devEui").asText()).isEqualTo(DEV_EUI);
        verify(context).onDownlinkMessageProcessed(true);
    }

    @Test
    void downlinkUsesDeviceQueueItemWrapperWhenUseApi4PlusIsFalse() throws Exception {
        // useAPI4Plus=false makes ChirpStackIntegration send {"deviceQueueItem": {...}} instead of
        // {"queueItem": {...}}. CS v4 expects "queueItem" — exercising this configuration documents
        // that against current CS the false branch fails (HTTP 400 / unknown field), so the
        // integration must surface the failure via reportDownlinkError, not silently drop the msg.
        ChirpStackIntegration integration = new ChirpStackIntegration();
        IntegrationContext context = mock(IntegrationContext.class);
        TBDownlinkDataConverter downlinkConverter = mock(TBDownlinkDataConverter.class);

        Integration configuration = buildIntegrationConfig(false);
        integration.init(new TbIntegrationInitParams(context, configuration, null, downlinkConverter));

        DownlinkData converted = DownlinkData.builder()
                .contentType("TEXT")
                .data("Kj8=".getBytes(StandardCharsets.UTF_8))
                .metadata(downlinkMetadata(80))
                .build();
        when(downlinkConverter.convertDownLink(any(), any(), any())).thenReturn(List.of(converted));

        TbMsg tbMsg = TbMsg.newMsg()
                .id(UUID.randomUUID())
                .type("POST_TELEMETRY_REQUEST")
                .originator(new IntegrationId(UUID.randomUUID()))
                .copyMetaData(new TbMsgMetaData(Map.of("DevEUI", DEV_EUI, "fPort", "80")))
                .data("{}")
                .build();
        IntegrationDownlinkMsg downlink = mock(IntegrationDownlinkMsg.class);
        when(downlink.getTbMsg()).thenReturn(tbMsg);

        integration.onDownlinkMsg(downlink);

        JsonNode queue = cs.getDeviceQueue(DEV_EUI);
        JsonNode items = queue.get("result");
        assertThat(items == null || !items.isArray() || items.size() == 0)
                .as("queue must remain empty when wrapper key is wrong")
                .isTrue();
        verify(context, never()).onDownlinkMessageProcessed(true);
        verify(context).onDownlinkMessageProcessed(false);
    }

    private Integration buildIntegrationConfig(boolean useApi4Plus) {
        ObjectNode root = JacksonUtil.newObjectNode();
        root.putObject("metadata");
        ObjectNode client = root.putObject("clientConfiguration");
        client.put("applicationServerUrl", stack.getRestApiUrl());
        client.put("applicationServerAPIToken", stack.getApiToken());
        client.put("useAPI4Plus", useApi4Plus);

        Integration integration = new Integration();
        integration.setId(new IntegrationId(UUID.randomUUID()));
        integration.setTenantId(TenantId.SYS_TENANT_ID);
        integration.setName("e2e-chirpstack-" + useApi4Plus);
        integration.setType(IntegrationType.CHIRPSTACK);
        integration.setConfiguration(root);
        return integration;
    }

    private static Map<String, String> downlinkMetadata(int fPort) {
        Map<String, String> md = new HashMap<>();
        md.put("DevEUI", DEV_EUI);
        md.put("fPort", String.valueOf(fPort));
        return md;
    }

}
