// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.http.chirpstack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import static org.mockito.Mockito.when;

/**
 * Runs the same downlink end-to-end flow as {@link ChirpStackIntegrationEndToEndTest} but
 * across a spread of ChirpStack v4 versions, to discover at which version the current
 * {@link ChirpStackIntegration} request shape (string fPort, no explicit confirmed) starts
 * failing — i.e. where the user-visible fix becomes load-bearing.
 *
 * Versions older than 4.5.0 are not included: ChirpStack only added the {@code create-api-key}
 * CLI subcommand in 4.5, so for 4.0-4.4 the stack lacks a REST-only bootstrap path. The current
 * matrix spans ~2 years of releases (Sep 2023 -> Oct 2025).
 *
 * Each parametrization spins up its own isolated stack (~3-5s), so total runtime scales linearly.
 */
@Slf4j
class ChirpStackVersionCompatibilityTest {

    private static final String DEV_EUI = "0102030405060708";

    @ParameterizedTest(name = "chirpstack:{0}")
    @ValueSource(strings = {"4.5.0", "4.7.0", "4.9.0", "4.15.0", "4.18.0"})
    void downlinkWithStringFPortAndNoConfirmedSucceedsAcrossVersions(String chirpstackVersion) throws Exception {
        try (ChirpStackTestStack stack = new ChirpStackTestStack(chirpstackVersion)) {
            stack.start();
            ChirpStackTestClient cs = new ChirpStackTestClient(stack.getRestApiUrl(), stack.getApiToken());

            String tenantId = cs.createTenant("compat-" + chirpstackVersion);
            String applicationId = cs.createApplication(tenantId, "compat-app");
            String deviceProfileId = cs.createDeviceProfile(tenantId, "compat-profile");
            cs.createDevice(applicationId, deviceProfileId, DEV_EUI, "compat-device");

            ChirpStackIntegration integration = new ChirpStackIntegration();
            IntegrationContext context = mock(IntegrationContext.class);
            TBDownlinkDataConverter downlinkConverter = mock(TBDownlinkDataConverter.class);

            integration.init(new TbIntegrationInitParams(
                    context, buildIntegrationConfig(stack, true), null, downlinkConverter));

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
            assertThat(items)
                    .as("queue items array on chirpstack:%s", chirpstackVersion)
                    .isNotNull();
            assertThat(items.isArray()).isTrue();
            assertThat(items.size())
                    .as("expected exactly one queued downlink on chirpstack:%s", chirpstackVersion)
                    .isEqualTo(1);
            JsonNode stored = items.get(0);
            assertThat(stored.get("fPort").asInt())
                    .as("fPort stored on chirpstack:%s", chirpstackVersion)
                    .isEqualTo(80);
            assertThat(stored.get("data").asText())
                    .as("data stored on chirpstack:%s", chirpstackVersion)
                    .isEqualTo("Kj8=");
        }
    }

    /**
     * After the fPort/confirmed type fix the integration parses metadata "confirmed" string
     * into a JSON boolean before sending. This test confirms that across all supported CS
     * versions the queued item now has the correct boolean confirmed value.
     */
    @ParameterizedTest(name = "chirpstack:{0}")
    @ValueSource(strings = {"4.5.0", "4.7.0", "4.9.0", "4.15.0", "4.18.0"})
    void downlinkWithConfirmedTrueIsStoredAcrossVersions(String chirpstackVersion) throws Exception {
        try (ChirpStackTestStack stack = new ChirpStackTestStack(chirpstackVersion)) {
            stack.start();
            ChirpStackTestClient cs = new ChirpStackTestClient(stack.getRestApiUrl(), stack.getApiToken());

            String tenantId = cs.createTenant("compat-conf-" + chirpstackVersion);
            String applicationId = cs.createApplication(tenantId, "compat-app");
            String deviceProfileId = cs.createDeviceProfile(tenantId, "compat-profile");
            cs.createDevice(applicationId, deviceProfileId, DEV_EUI, "compat-device");

            ChirpStackIntegration integration = new ChirpStackIntegration();
            IntegrationContext context = mock(IntegrationContext.class);
            TBDownlinkDataConverter downlinkConverter = mock(TBDownlinkDataConverter.class);

            integration.init(new TbIntegrationInitParams(
                    context, buildIntegrationConfig(stack, true), null, downlinkConverter));

            Map<String, String> mdWithConfirmed = new HashMap<>();
            mdWithConfirmed.put("DevEUI", DEV_EUI);
            mdWithConfirmed.put("fPort", "80");
            mdWithConfirmed.put("confirmed", "true");

            DownlinkData converted = DownlinkData.builder()
                    .contentType("TEXT")
                    .data("Kj8=".getBytes(StandardCharsets.UTF_8))
                    .metadata(mdWithConfirmed)
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

            JsonNode items = cs.getDeviceQueue(DEV_EUI).get("result");
            assertThat(items.size())
                    .as("expected one queued item on chirpstack:%s with confirmed=true", chirpstackVersion)
                    .isEqualTo(1);
            JsonNode stored = items.get(0);
            assertThat(stored.get("confirmed").isBoolean())
                    .as("confirmed must be JSON boolean on chirpstack:%s", chirpstackVersion)
                    .isTrue();
            assertThat(stored.get("confirmed").asBoolean()).isTrue();
            assertThat(stored.get("fPort").asInt()).isEqualTo(80);
        }
    }

    private Integration buildIntegrationConfig(ChirpStackTestStack stack, boolean useApi4Plus) {
        ObjectNode root = JacksonUtil.newObjectNode();
        root.putObject("metadata");
        ObjectNode client = root.putObject("clientConfiguration");
        client.put("applicationServerUrl", stack.getRestApiUrl());
        client.put("applicationServerAPIToken", stack.getApiToken());
        client.put("useAPI4Plus", useApi4Plus);

        Integration integration = new Integration();
        integration.setId(new IntegrationId(UUID.randomUUID()));
        integration.setTenantId(TenantId.SYS_TENANT_ID);
        integration.setName("compat-chirpstack");
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
