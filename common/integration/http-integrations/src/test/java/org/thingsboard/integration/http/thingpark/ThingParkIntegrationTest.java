// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.http.thingpark;

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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ThingParkIntegrationTest {

    private DownlinkCaptureServer server;
    private NioEventLoopGroup eventLoopGroup;
    private ThingParkIntegration integration;
    private TBDownlinkDataConverter downlinkConverter;

    @BeforeEach
    void setUp() throws Exception {
        server = new DownlinkCaptureServer();
        eventLoopGroup = new NioEventLoopGroup(1);
        IntegrationContext context = mock(IntegrationContext.class);
        when(context.getEventLoopGroup()).thenReturn(eventLoopGroup);
        downlinkConverter = mock(TBDownlinkDataConverter.class);
        integration = new ThingParkIntegration();
        integration.init(new TbIntegrationInitParams(context, buildConfiguration(), null, downlinkConverter));
    }

    @AfterEach
    void tearDown() {
        server.close();
        eventLoopGroup.shutdownGracefully();
    }

    @Test
    void binaryDownlinkIsSentAsHex() throws Exception {
        sendDownlink("BINARY", Base64.getDecoder().decode("Af8="));

        assertThat(server.takeRequest().query()).contains("Payload=01FF");
    }

    @Test
    void textDownlinkIsSentAsIs() throws Exception {
        sendDownlink("TEXT", "0102aabb".getBytes(StandardCharsets.UTF_8));

        assertThat(server.takeRequest().query()).contains("Payload=0102aabb");
    }

    private void sendDownlink(String contentType, byte[] data) throws Exception {
        DownlinkData downlinkData = DownlinkData.builder()
                .contentType(contentType)
                .data(data)
                .metadata(Map.of("DevEUI", "0102030405060708", "FPort", "2"))
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

    private Integration buildConfiguration() {
        ObjectNode root = JacksonUtil.newObjectNode();
        root.putObject("metadata");
        root.put("downlinkUrl", server.getUrl() + "/thingpark/lrc/rest/downlink");

        Integration configuration = new Integration();
        configuration.setId(new IntegrationId(UUID.randomUUID()));
        configuration.setTenantId(TenantId.SYS_TENANT_ID);
        configuration.setName("thingpark");
        configuration.setType(IntegrationType.THINGPARK);
        configuration.setConfiguration(root);
        return configuration;
    }

}
