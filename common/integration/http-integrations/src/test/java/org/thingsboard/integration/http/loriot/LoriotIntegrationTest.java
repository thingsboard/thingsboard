// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.http.loriot;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.channel.nio.NioEventLoopGroup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.converter.TBDownlinkDataConverter;
import org.thingsboard.integration.api.data.DownlinkData;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.integration.http.DownlinkCaptureServer;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoriotIntegrationTest {

    private DownlinkCaptureServer server;
    private NioEventLoopGroup eventLoopGroup;
    private LoriotIntegration integration;
    private TBDownlinkDataConverter downlinkConverter;

    @BeforeEach
    void setUp() throws Exception {
        server = new DownlinkCaptureServer();
        eventLoopGroup = new NioEventLoopGroup(1);
        IntegrationContext context = mock(IntegrationContext.class);
        when(context.getEventLoopGroup()).thenReturn(eventLoopGroup);
        downlinkConverter = mock(TBDownlinkDataConverter.class);
        integration = new LoriotIntegration();
        integration.init(new TbIntegrationInitParams(context, buildConfiguration(), null, downlinkConverter));
    }

    @AfterEach
    void tearDown() {
        server.close();
        eventLoopGroup.shutdownGracefully();
    }

    @Test
    void binaryDownlinkIsSentAsHex() throws Exception {
        sendDownlink("BINARY", Base64.getDecoder().decode("Af8="), false);

        assertThat(sentData()).isEqualTo("01ff");
    }

    @Test
    void binaryDownlinkIsSentAsHexWhenIsHexEncodedIsSet() throws Exception {
        sendDownlink("BINARY", Base64.getDecoder().decode("Af8="), true);

        assertThat(sentData()).isEqualTo("01ff");
    }

    @Test
    void textDownlinkWithIsHexEncodedIsSentAsIs() throws Exception {
        sendDownlink("TEXT", "0102aabb".getBytes(StandardCharsets.UTF_8), true);

        assertThat(sentData()).isEqualTo("0102aabb");
    }

    @Test
    void textDownlinkWithoutIsHexEncodedIsSentAsHexOfText() throws Exception {
        sendDownlink("TEXT", "ON".getBytes(StandardCharsets.UTF_8), false);

        assertThat(sentData()).isEqualTo("4f4e");
    }

    private void sendDownlink(String contentType, byte[] data, boolean isHexEncoded) throws Exception {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("EUI", "0102030405060708");
        metadata.put("port", "2");
        if (isHexEncoded) {
            metadata.put("isHexEncoded", "true");
        }
        DownlinkData downlinkData = DownlinkData.builder()
                .contentType(contentType)
                .data(data)
                .metadata(metadata)
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
        integration.onDownlinkMsg(downlinkMsg);
    }

    private String sentData() throws InterruptedException {
        return JacksonUtil.toJsonNode(server.takeRequest().body()).get("data").asText();
    }

    private Integration buildConfiguration() {
        ObjectNode root = JacksonUtil.newObjectNode();
        root.putObject("metadata");
        root.put("server", "eu1");
        root.put("appId", "app");
        root.put("token", "token");
        root.put("sendDownlink", true);
        root.put("createLoriotOutput", false);
        root.put("loriotDownlinkUrl", server.getUrl() + "/1/rest");

        Integration configuration = new Integration();
        configuration.setId(new IntegrationId(UUID.randomUUID()));
        configuration.setTenantId(TenantId.SYS_TENANT_ID);
        configuration.setName("loriot");
        configuration.setType(IntegrationType.LORIOT);
        configuration.setConfiguration(root);
        return configuration;
    }

}
