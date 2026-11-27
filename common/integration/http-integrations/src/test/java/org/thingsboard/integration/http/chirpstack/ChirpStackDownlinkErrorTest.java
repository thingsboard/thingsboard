// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.http.chirpstack;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.converter.TBDownlinkDataConverter;
import org.thingsboard.integration.api.data.DownlinkData;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.event.IntegrationDebugEvent;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

class ChirpStackDownlinkErrorTest {

    private static final String SERVER_URL = "http://chirpstack.test:8090";
    private static final String DEV_EUI = "0102030405060708";
    private static final String QUEUE_URL = SERVER_URL + "/api/devices/" + DEV_EUI + "/queue";

    private ChirpStackIntegration integration;
    private IntegrationContext context;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() throws Exception {
        integration = new ChirpStackIntegration();
        context = mock(IntegrationContext.class);
        TBDownlinkDataConverter downlinkConverter = mock(TBDownlinkDataConverter.class);
        integration.init(new TbIntegrationInitParams(context, integrationConfig(), null, downlinkConverter));

        DownlinkData converted = DownlinkData.builder()
                .contentType("TEXT")
                .data("Kj8=".getBytes(StandardCharsets.UTF_8))
                .metadata(Map.of("DevEUI", DEV_EUI, "fPort", "80"))
                .build();
        when(downlinkConverter.convertDownLink(any(), any(), any())).thenReturn(List.of(converted));

        server = MockRestServiceServer.bindTo((RestTemplate) ReflectionTestUtils.getField(integration, "httpClient")).build();
    }

    @Test
    void unauthorizedNamesTokenAndMissingDevice() {
        server.expect(requestTo(QUEUE_URL)).andRespond(withUnauthorizedRequest());

        integration.onDownlinkMsg(downlink());

        assertThat(downlinkError()).isEqualTo("ChirpStack rejected downlink (401 UNAUTHORIZED): " +
                "check the Application server API token and that the device exists in ChirpStack");
    }

    @Test
    void otherRejectionShowsResponseBody() {
        server.expect(requestTo(QUEUE_URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"code\":3,\"message\":\"invalid fPort\"}"));

        integration.onDownlinkMsg(downlink());

        assertThat(downlinkError()).isEqualTo("ChirpStack rejected downlink (400 BAD_REQUEST): {\"code\":3,\"message\":\"invalid fPort\"}");
    }

    @Test
    void unreachableServerNamesUrl() {
        server.expect(requestTo(QUEUE_URL)).andRespond(withException(new ConnectException("Connection refused")));

        integration.onDownlinkMsg(downlink());

        assertThat(downlinkError()).isEqualTo("ChirpStack is not reachable: " + QUEUE_URL + " (ConnectException: Connection refused)");
    }

    private String downlinkError() {
        server.verify();
        ArgumentCaptor<IntegrationDebugEvent> captor = ArgumentCaptor.forClass(IntegrationDebugEvent.class);
        verify(context).saveEvent(captor.capture(), any());
        IntegrationDebugEvent event = captor.getValue();
        assertThat(event.getEventType()).isEqualTo("Downlink");
        assertThat(event.getStatus()).isEqualTo("ERROR");
        return event.getError();
    }

    private static IntegrationDownlinkMsg downlink() {
        TbMsg tbMsg = TbMsg.newMsg()
                .id(UUID.randomUUID())
                .type("ATTRIBUTES_UPDATED")
                .originator(new IntegrationId(UUID.randomUUID()))
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data("{}")
                .build();
        IntegrationDownlinkMsg downlink = mock(IntegrationDownlinkMsg.class);
        when(downlink.getTbMsg()).thenReturn(tbMsg);
        return downlink;
    }

    private static Integration integrationConfig() {
        ObjectNode root = JacksonUtil.newObjectNode();
        root.putObject("metadata");
        ObjectNode client = root.putObject("clientConfiguration");
        client.put("applicationServerUrl", SERVER_URL);
        client.put("applicationServerAPIToken", "test-token");
        client.put("useAPI4Plus", true);

        Integration integration = new Integration();
        integration.setId(new IntegrationId(UUID.randomUUID()));
        integration.setTenantId(TenantId.SYS_TENANT_ID);
        integration.setName("chirpstack");
        integration.setType(IntegrationType.CHIRPSTACK);
        integration.setDebugSettings(DebugSettings.failures());
        integration.setConfiguration(root);
        return integration;
    }

}
