// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.http.sigfox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.controller.JsonHttpIntegrationMsg;
import org.thingsboard.integration.api.converter.TBDownlinkDataConverter;
import org.thingsboard.integration.api.data.DownLinkMsg;
import org.thingsboard.integration.api.data.DownlinkData;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SigFoxIntegrationTest {

    private static final String DEVICE_NAME = "device";
    private static final String SIGFOX_DEVICE_ID = "1A2B3C";

    private IntegrationContext context;
    private SigFoxIntegration integration;
    private TBDownlinkDataConverter downlinkConverter;

    @BeforeEach
    void setUp() throws Exception {
        context = mock(IntegrationContext.class);
        downlinkConverter = mock(TBDownlinkDataConverter.class);
        integration = new SigFoxIntegration();
        integration.init(new TbIntegrationInitParams(context, buildConfiguration(), null, downlinkConverter));
    }

    @Test
    void binaryDownlinkIsSentAsHex() throws Exception {
        assertThat(sendDownlink("BINARY", Base64.getDecoder().decode("Af8="))).isEqualTo("01ff");
    }

    @Test
    void textDownlinkIsSentAsIs() throws Exception {
        assertThat(sendDownlink("TEXT", "1234567890123456".getBytes(StandardCharsets.UTF_8))).isEqualTo("1234567890123456");
    }

    private String sendDownlink(String contentType, byte[] data) throws Exception {
        DownlinkData downlinkData = DownlinkData.builder()
                .contentType(contentType)
                .data(data)
                .metadata(Map.of())
                .build();
        when(downlinkConverter.convertDownLink(any(), any(), any())).thenReturn(List.of(downlinkData));

        TbMsg tbMsg = TbMsg.newMsg()
                .type("ATTRIBUTES_UPDATED")
                .originator(new IntegrationId(UUID.randomUUID()))
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data("{}")
                .build();
        IntegrationDownlinkMsg downlinkMsg = mock(IntegrationDownlinkMsg.class);
        when(downlinkMsg.getTbMsg()).thenReturn(tbMsg);
        DownLinkMsg pending = DownLinkMsg.from(downlinkMsg);
        when(context.getDownlinkMsg(DEVICE_NAME)).thenReturn(pending);

        JsonHttpIntegrationMsg uplinkMsg = new JsonHttpIntegrationMsg(Map.of(), JacksonUtil.newObjectNode(), null);
        ResponseEntity<JsonNode> response = ReflectionTestUtils.invokeMethod(integration, "processDownLinkData",
                context, DEVICE_NAME, uplinkMsg, SIGFOX_DEVICE_ID);
        return response.getBody().get(SIGFOX_DEVICE_ID).get("downlinkData").asText();
    }

    private Integration buildConfiguration() {
        ObjectNode root = JacksonUtil.newObjectNode();
        root.putObject("metadata");

        Integration configuration = new Integration();
        configuration.setId(new IntegrationId(UUID.randomUUID()));
        configuration.setTenantId(TenantId.SYS_TENANT_ID);
        configuration.setName("sigfox");
        configuration.setType(IntegrationType.SIGFOX);
        configuration.setConfiguration(root);
        return configuration;
    }

}
