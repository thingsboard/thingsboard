// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration.rpc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.gen.MsgProtos;
import org.thingsboard.server.gen.integration.AssetUplinkDataProto;
import org.thingsboard.server.gen.integration.DeviceUplinkDataProto;
import org.thingsboard.server.gen.integration.EntityViewDataProto;
import org.thingsboard.server.gen.integration.UplinkMsg;
import org.thingsboard.server.gen.integration.UplinkResponseMsg;
import org.thingsboard.server.service.integration.IntegrationContextComponent;
import org.thingsboard.server.service.integration.PlatformIntegrationService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class IntegrationGrpcSessionTest {

    IntegrationGrpcSession integrationGrpcSession;
    IntegrationContextComponent ctx;
    PlatformIntegrationService platformIntegrationService;
    Integration configuration;
    Runnable runnable;
    UUID sessionId;
    TenantId tenantId;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        tenantId = TenantId.fromUUID(UUID.randomUUID());
        configuration = new Integration(new IntegrationId(UUID.randomUUID()));
        configuration.setTenantId(tenantId);
        platformIntegrationService = mock(PlatformIntegrationService.class);
        ctx = mock(IntegrationContextComponent.class, Mockito.RETURNS_DEEP_STUBS);
        willReturn(platformIntegrationService).given(ctx).getPlatformIntegrationService();
        integrationGrpcSession = mock(IntegrationGrpcSession.class);
        ReflectionTestUtils.setField(integrationGrpcSession, "ctx", ctx);
        ReflectionTestUtils.setField(integrationGrpcSession, "configuration", configuration);
        ReflectionTestUtils.setField(integrationGrpcSession, "sessionId", sessionId);
        willCallRealMethod().given(integrationGrpcSession).processUplinkMsg(Mockito.any());
        runnable = mock(Runnable.class);
    }

    @Test
    void testProcessUplinkDataDeviceRun() {
        DeviceUplinkDataProto deviceUplinkData = mock(DeviceUplinkDataProto.class);
        UplinkMsg uplinkData = UplinkMsg.newBuilder()
                        .addDeviceData(deviceUplinkData)
                .build();
        willReturn(runnable).given(platformIntegrationService).processUplinkData(configuration, sessionId, deviceUplinkData, null);
        integrationGrpcSession.processUplinkMsg(uplinkData);
        Mockito.verify(runnable).run();
    }

    @Test
    void testProcessUplinkDataAssetRun() {

        AssetUplinkDataProto assetUplinkData = mock(AssetUplinkDataProto.class);
        UplinkMsg uplinkData = UplinkMsg.newBuilder()
                .addAssetData(assetUplinkData)
                .build();
        willReturn(runnable).given(platformIntegrationService).processUplinkData(configuration, assetUplinkData, null);
        integrationGrpcSession.processUplinkMsg(uplinkData);
        Mockito.verify(runnable).run();
    }

    @Test
    void testProcessUplinkDataEntityViewRun() {
        EntityViewDataProto entityViewUplinkData = mock(EntityViewDataProto.class);
        UplinkMsg uplinkData = UplinkMsg.newBuilder()
                .addEntityViewData(entityViewUplinkData)
                .build();
        willReturn(runnable).given(platformIntegrationService).processUplinkData(configuration, entityViewUplinkData, null);
        integrationGrpcSession.processUplinkMsg(uplinkData);
        Mockito.verify(runnable).run();
    }

    @Test
    void testProcessUplinkMsgWithTbMsgProto() {
        TbMsg originalMsg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(new DeviceId(UUID.randomUUID()))
                .metaData(TbMsgMetaData.EMPTY)
                .data("{\"temperature\":25}")
                .build();

        MsgProtos.TbMsgProto tbMsgProto = TbMsg.toProto(originalMsg);
        UplinkMsg uplinkMsg = UplinkMsg.newBuilder()
                .addTbMsgProto(tbMsgProto)
                .build();

        UplinkResponseMsg response = integrationGrpcSession.processUplinkMsg(uplinkMsg);

        assertThat(response.getSuccess()).isTrue();
        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(platformIntegrationService).process(eq(tenantId), msgCaptor.capture(), isNull());
        TbMsg captured = msgCaptor.getValue();
        assertThat(captured.getId()).isEqualTo(originalMsg.getId());
        assertThat(captured.getType()).isEqualTo(originalMsg.getType());
        assertThat(captured.getData()).isEqualTo(originalMsg.getData());
        assertThat(captured.getOriginator()).isEqualTo(originalMsg.getOriginator());
    }

    @Test
    void testProcessUplinkMsgWithMultipleTbMsgProtos() {
        TbMsg msg1 = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(new DeviceId(UUID.randomUUID()))
                .metaData(TbMsgMetaData.EMPTY)
                .data("{\"value\":1}")
                .build();
        TbMsg msg2 = TbMsg.newMsg()
                .type(TbMsgType.POST_ATTRIBUTES_REQUEST)
                .originator(new DeviceId(UUID.randomUUID()))
                .metaData(TbMsgMetaData.EMPTY)
                .data("{\"value\":2}")
                .build();

        UplinkMsg uplinkMsg = UplinkMsg.newBuilder()
                .addTbMsgProto(TbMsg.toProto(msg1))
                .addTbMsgProto(TbMsg.toProto(msg2))
                .build();

        UplinkResponseMsg response = integrationGrpcSession.processUplinkMsg(uplinkMsg);

        assertThat(response.getSuccess()).isTrue();
        verify(platformIntegrationService, Mockito.times(2)).process(eq(tenantId), any(TbMsg.class), isNull());
    }

}
