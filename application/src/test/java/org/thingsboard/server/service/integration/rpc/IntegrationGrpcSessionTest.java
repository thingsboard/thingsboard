// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration.rpc;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.event.Event;
import org.thingsboard.server.common.data.event.IntegrationDebugEvent;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
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
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.exception.ThingsboardRuntimeException;
import org.thingsboard.server.gen.integration.UplinkResponseMsg;
import org.thingsboard.server.service.integration.IntegrationContextComponent;
import org.thingsboard.server.service.integration.PlatformIntegrationService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
        willReturn(runnable).given(platformIntegrationService).processUplinkData(eq(configuration), eq(sessionId), eq(deviceUplinkData), notNull());
        integrationGrpcSession.processUplinkMsg(uplinkData);
        Mockito.verify(runnable).run();
    }

    @Test
    void testFailedUplinkSavesErrorEvent() {
        configuration.setDebugSettings(DebugSettings.failures());
        var eventService = ctx.getEventService();
        willReturn(Futures.immediateVoidFuture()).given(eventService).saveAsync(any());
        stubDebugRateLimit(true, false);

        UplinkResponseMsg response = integrationGrpcSession.processUplinkMsg(forbiddenDeviceUplink());

        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getErrorMsg()).isEqualTo("Creating devices is forbidden!");
        IntegrationDebugEvent event = savedEvent(eventService);
        assertThat(event.getEntityId()).isEqualTo(configuration.getId().getId());
        assertThat(event.getEventType()).isEqualTo("Uplink");
        assertThat(event.getStatus()).isEqualTo("ERROR");
        assertThat(event.getError()).isEqualTo("Creating devices is forbidden!");
        assertThat(event.getMessage()).contains("HTTP check device");
    }

    @Test
    void testFailedUplinkWithDebugOffSavesNoEvent() {
        configuration.setDebugSettings(DebugSettings.off());
        var eventService = ctx.getEventService();
        stubDebugRateLimit(true, false);

        UplinkResponseMsg response = integrationGrpcSession.processUplinkMsg(forbiddenDeviceUplink());

        assertThat(response.getSuccess()).isFalse();
        verify(eventService, never()).saveAsync(any());
    }

    @Test
    void testFailedUplinkOverDebugRateLimitSavesRateLimitEventOnce() {
        configuration.setDebugSettings(DebugSettings.failures());
        var eventService = ctx.getEventService();
        willReturn(Futures.immediateVoidFuture()).given(eventService).saveAsync(any());
        stubDebugRateLimit(false, false);

        integrationGrpcSession.processUplinkMsg(forbiddenDeviceUplink());

        IntegrationDebugEvent event = savedEvent(eventService);
        assertThat(event.getStatus()).isEqualTo("ERROR");
        assertThat(event.getError()).isEqualTo("Integration debug rate limits reached!");
    }

    @Test
    void testFailedUplinkOverDebugRateLimitAlreadyReportedSavesNoEvent() {
        configuration.setDebugSettings(DebugSettings.failures());
        var eventService = ctx.getEventService();
        stubDebugRateLimit(false, true);

        integrationGrpcSession.processUplinkMsg(forbiddenDeviceUplink());

        verify(eventService, never()).saveAsync(any());
    }

    @Test
    void testUplinkProcessingErrorIsLogged() {
        DeviceUplinkDataProto deviceUplinkData = DeviceUplinkDataProto.newBuilder().setDeviceName("HTTP check device").build();
        willReturn(runnable).given(platformIntegrationService).processUplinkData(eq(configuration), eq(sessionId), eq(deviceUplinkData), notNull());
        integrationGrpcSession.processUplinkMsg(UplinkMsg.newBuilder().addDeviceData(deviceUplinkData).build());
        ArgumentCaptor<IntegrationCallback<Void>> callbackCaptor = ArgumentCaptor.captor();
        verify(platformIntegrationService).processUplinkData(eq(configuration), eq(sessionId), eq(deviceUplinkData), callbackCaptor.capture());

        Logger logger = (Logger) LoggerFactory.getLogger(IntegrationGrpcSession.class);
        ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
        try {
            callbackCaptor.getValue().onError(new RuntimeException("Queue is unavailable"));
        } finally {
            logger.detachAppender(logAppender);
        }

        assertThat(logAppender.list).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getThrowableProxy().getMessage()).isEqualTo("Queue is unavailable");
        });
    }

    private void stubDebugRateLimit(boolean withinLimit, boolean limitAlreadyReported) {
        var rateLimitService = ctx.getRateLimitService();
        willReturn(withinLimit).given(rateLimitService).checkLimit(tenantId, configuration.getId(), false);
        willReturn(limitAlreadyReported).given(rateLimitService).alreadyProcessed(configuration.getId(), EntityType.INTEGRATION);
    }

    private UplinkMsg forbiddenDeviceUplink() {
        DeviceUplinkDataProto deviceUplinkData = DeviceUplinkDataProto.newBuilder()
                .setDeviceName("HTTP check device")
                .setDeviceType("default")
                .build();
        willThrow(new ThingsboardRuntimeException("Creating devices is forbidden!", ThingsboardErrorCode.PERMISSION_DENIED))
                .given(platformIntegrationService).processUplinkData(eq(configuration), eq(sessionId), eq(deviceUplinkData), notNull());
        return UplinkMsg.newBuilder().addDeviceData(deviceUplinkData).build();
    }

    private static IntegrationDebugEvent savedEvent(EventService eventService) {
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventService).saveAsync(eventCaptor.capture());
        return (IntegrationDebugEvent) eventCaptor.getValue();
    }

    @Test
    void testProcessUplinkDataAssetRun() {

        AssetUplinkDataProto assetUplinkData = mock(AssetUplinkDataProto.class);
        UplinkMsg uplinkData = UplinkMsg.newBuilder()
                .addAssetData(assetUplinkData)
                .build();
        willReturn(runnable).given(platformIntegrationService).processUplinkData(eq(configuration), eq(assetUplinkData), notNull());
        integrationGrpcSession.processUplinkMsg(uplinkData);
        Mockito.verify(runnable).run();
    }

    @Test
    void testProcessUplinkDataEntityViewRun() {
        EntityViewDataProto entityViewUplinkData = mock(EntityViewDataProto.class);
        UplinkMsg uplinkData = UplinkMsg.newBuilder()
                .addEntityViewData(entityViewUplinkData)
                .build();
        willReturn(runnable).given(platformIntegrationService).processUplinkData(eq(configuration), eq(entityViewUplinkData), notNull());
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
        verify(platformIntegrationService).process(eq(tenantId), msgCaptor.capture(), notNull());
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
        verify(platformIntegrationService, Mockito.times(2)).process(eq(tenantId), any(TbMsg.class), notNull());
    }

}
