// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.IntegrationInfo;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.gen.integration.ToCoreIntegrationMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class DefaultTbCoreIntegrationApiServiceTest {

    DefaultTbCoreIntegrationApiService service;
    PlatformIntegrationService platformIntegrationService;
    TbCallback callback;
    IntegrationInfo integrationInfo;

    @BeforeEach
    void setUp() {
        platformIntegrationService = mock(PlatformIntegrationService.class);
        callback = mock(TbCallback.class);

        service = new DefaultTbCoreIntegrationApiService(
                null, null, null, null, null,
                null, null, platformIntegrationService, null,
                null, null, null, null, null, null);

        integrationInfo = new IntegrationInfo(new IntegrationId(UUID.randomUUID()));
        integrationInfo.setTenantId(TenantId.fromUUID(UUID.randomUUID()));
        integrationInfo.setName("test-integration");
        integrationInfo.setType(IntegrationType.HTTP);
    }

    @Test
    void testHandleCustomTbMsgProto() {
        TbMsg originalMsg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(new DeviceId(UUID.randomUUID()))
                .metaData(new TbMsgMetaData())
                .data("{\"temperature\":42}")
                .build();

        ToCoreIntegrationMsg msg = ToCoreIntegrationMsg.newBuilder()
                .setIntegration(ProtoUtils.toIntegrationInfoProto(integrationInfo))
                .setCustomTbMsgProto(TbMsg.toProto(originalMsg))
                .build();

        Runnable handler = service.handle(new TbProtoQueueMsg<>(UUID.randomUUID(), msg), callback);

        assertThat(handler).isNotNull();
        handler.run();

        ArgumentCaptor<TbMsg> tbMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(platformIntegrationService).processUplinkData(any(IntegrationInfo.class), tbMsgCaptor.capture(), any());
        verify(callback, never()).onFailure(any());

        TbMsg captured = tbMsgCaptor.getValue();
        assertThat(captured.getId()).isEqualTo(originalMsg.getId());
        assertThat(captured.getType()).isEqualTo(originalMsg.getType());
        assertThat(captured.getData()).isEqualTo(originalMsg.getData());
        assertThat(captured.getOriginator()).isEqualTo(originalMsg.getOriginator());
    }

    @Test
    void testHandleCustomTbMsgProtoWithMetaData() {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("deviceName", "OPC-UA-Device");
        metaData.putValue("source", "opcua");

        TbMsg originalMsg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(new DeviceId(UUID.randomUUID()))
                .metaData(metaData)
                .data("{\"pressure\":100}")
                .build();

        ToCoreIntegrationMsg msg = ToCoreIntegrationMsg.newBuilder()
                .setIntegration(ProtoUtils.toIntegrationInfoProto(integrationInfo))
                .setCustomTbMsgProto(TbMsg.toProto(originalMsg))
                .build();

        Runnable handler = service.handle(new TbProtoQueueMsg<>(UUID.randomUUID(), msg), callback);
        handler.run();

        ArgumentCaptor<TbMsg> tbMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(platformIntegrationService).processUplinkData(any(IntegrationInfo.class), tbMsgCaptor.capture(), any());

        TbMsg captured = tbMsgCaptor.getValue();
        assertThat(captured.getMetaData().getValue("deviceName")).isEqualTo("OPC-UA-Device");
        assertThat(captured.getMetaData().getValue("source")).isEqualTo("opcua");
    }

    @Test
    void testHandleUnsupportedMsgCallsFailure() {
        // A ToCoreIntegrationMsg with integration set but no data field set
        // should call callback.onFailure, not silently drop the message
        ToCoreIntegrationMsg msg = ToCoreIntegrationMsg.newBuilder()
                .setIntegration(ProtoUtils.toIntegrationInfoProto(integrationInfo))
                .build();

        service.handle(new TbProtoQueueMsg<>(UUID.randomUUID(), msg), callback);

        verify(callback).onFailure(any(RuntimeException.class));
        verify(platformIntegrationService, never()).processUplinkData(any(), any(TbMsg.class), any());
    }

}
