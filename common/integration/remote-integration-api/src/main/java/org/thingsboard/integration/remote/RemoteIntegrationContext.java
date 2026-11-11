// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.remote;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.ByteString;
import io.netty.channel.EventLoopGroup;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.EventUtil;
import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.IntegrationRateLimitService;
import org.thingsboard.integration.api.converter.ConverterContext;
import org.thingsboard.integration.api.data.DownLinkMsg;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.integration.api.util.IntegrationMqttClientSettingsComponent;
import org.thingsboard.integration.storage.EventStorage;
import org.thingsboard.server.common.data.JavaSerDesUtil;
import org.thingsboard.server.common.data.event.Event;
import org.thingsboard.server.common.data.event.IntegrationDebugEvent;
import org.thingsboard.server.common.data.event.LifecycleEvent;
import org.thingsboard.server.common.data.event.RawDataEvent;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.gen.integration.AssetUplinkDataProto;
import org.thingsboard.server.gen.integration.DeviceUplinkDataProto;
import org.thingsboard.server.gen.integration.TbEventProto;
import org.thingsboard.server.gen.integration.TbEventSource;
import org.thingsboard.server.gen.integration.UplinkMsg;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

@Data
@Slf4j
@RequiredArgsConstructor
public class RemoteIntegrationContext implements IntegrationContext {

    private static final String REMOTE_INTEGRATION_CACHE = "remoteIntegration";

    private final EventStorage eventStorage;
    private final Integration configuration;
    private final String clientId;
    private final int port;
    private final ConverterContext uplinkConverterContext;
    private final ConverterContext downlinkConverterContext;

    private final IntegrationMqttClientSettingsComponent integrationMqttClientSettingsComponent;

    private final ScheduledExecutorService scheduledExecutorService;
    private final ExecutorService generalExecutorService;
    private final ExecutorService callBackExecutorService;


    public RemoteIntegrationContext(
            EventStorage eventStorage,
            ScheduledExecutorService scheduledExecutorService,
            ExecutorService generalExecutorService,
            ExecutorService callBackExecutorService,
            Integration configuration,
            String clientId,
            int port,
            IntegrationMqttClientSettingsComponent integrationMqttClientSettingsComponent
    ) {
        this.eventStorage = eventStorage;
        this.configuration = configuration;
        this.clientId = clientId;
        this.port = port;
        this.integrationMqttClientSettingsComponent = integrationMqttClientSettingsComponent;

        uplinkConverterContext = new RemoteConverterContext(eventStorage, true, clientId, port);
        downlinkConverterContext = new RemoteConverterContext(eventStorage, false, clientId, port);
        this.scheduledExecutorService = scheduledExecutorService;
        this.generalExecutorService = generalExecutorService;
        this.callBackExecutorService = callBackExecutorService;
    }

    @Override
    public String getServiceId() {
        return "[" + clientId + ":" + port + "]";
    }

    @Override
    public void processUplinkData(DeviceUplinkDataProto msg, IntegrationCallback<Void> callback) {
        eventStorage.write(UplinkMsg.newBuilder().addDeviceData(msg).build(), callback);
    }

    @Override
    public void processUplinkData(AssetUplinkDataProto msg, IntegrationCallback<Void> callback) {
        eventStorage.write(UplinkMsg.newBuilder().addAssetData(msg).build(), callback);
    }

    @Override
    public void processCustomMsg(TbMsg msg, IntegrationCallback<Void> callback) {
        eventStorage.write(UplinkMsg.newBuilder().addTbMsgProto(TbMsg.toProto(msg)).build(), callback);
    }

    @Override
    public void saveEvent(IntegrationDebugEvent event, IntegrationCallback<Void> callback) {
        doSaveEvent(TbEventSource.INTEGRATION, event, null, callback);
    }

    @Override
    public void saveLifecycleEvent(ComponentLifecycleEvent event, Exception e) {
        var lcEvent = LifecycleEvent.builder()
                .tenantId(configuration.getTenantId())
                .entityId(configuration.getId().getId())
                .serviceId(getServiceId())
                .lcEventType(event.name());
        if (e != null) {
            lcEvent.success(false).error(EventUtil.toString(e));
        } else {
            lcEvent.success(true);
        }

        eventStorage.write(UplinkMsg.newBuilder()
                .addEventsData(TbEventProto.newBuilder()
                        .setSource(TbEventSource.INTEGRATION)
                        .setEvent(ByteString.copyFrom(JavaSerDesUtil.encode(lcEvent.build())))
                        .build())
                .build(), null);
    }

    @Override
    public void saveRawDataEvent(String deviceName, String type, String uid, JsonNode body, IntegrationCallback<Void> callback) {
        doSaveEvent(TbEventSource.DEVICE, RawDataEvent.builder()
                .tenantId(configuration.getTenantId())
                .serviceId(getServiceId())
                .uuid(uid)
                .messageType(type)
                .message(body.toString())
                .build(), deviceName, callback);
    }

    @Override
    public EventLoopGroup getEventLoopGroup() {
        return null;
    }

    @Override
    public DownLinkMsg getDownlinkMsg(String deviceName) {
        return null;
    }

    @Override
    public DownLinkMsg putDownlinkMsg(IntegrationDownlinkMsg msg) {
        return null;
    }

    @Override
    public void removeDownlinkMsg(String deviceName) {}

    @Override
    public ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutorService;
    }

    @Override
    public ExecutorService getExecutorService() {
        return generalExecutorService;
    }

    @Override
    public ExecutorService getCallBackExecutorService() {
        return callBackExecutorService;
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public boolean isExceptionStackTraceEnabled() {
        return true;
    }

    @Override
    public void onUplinkMessageProcessed(boolean success) {
        // Statistics for remote integrations is not supported
    }

    @Override
    public void onDownlinkMessageProcessed(boolean success) {
        // Statistics for remote integrations is not supported
    }

    @Override
    public Optional<IntegrationRateLimitService> getRateLimitService() {
        return Optional.empty();
    }

    private void doSaveEvent(TbEventSource tbEventSource, Event event, String deviceName, IntegrationCallback<Void> callback) {
        var builder = TbEventProto.newBuilder()
                .setSource(tbEventSource)
                .setEvent(ByteString.copyFrom(JavaSerDesUtil.encode(event)));
        if (deviceName != null) {
            builder.setDeviceName(deviceName);
        }
        eventStorage.write(UplinkMsg.newBuilder()
                .addEventsData(builder.build())
                .build(), callback);
    }

    @Override
    public int getMqttClientRetransmissionMaxAttempts() {
        return integrationMqttClientSettingsComponent.getRetransmissionMaxAttempts();
    }

    @Override
    public long getMqttClientRetransmissionInitialDelayMillis() {
        return integrationMqttClientSettingsComponent.getRetransmissionInitialDelayMillis();
    }

    @Override
    public double getMqttClientRetransmissionJitterFactor() {
        return integrationMqttClientSettingsComponent.getRetransmissionJitterFactor();
    }

    @Override
    public int getIntegrationConnectTimeoutSec() {
        return integrationMqttClientSettingsComponent.getIntegrationConnectTimeoutSec();
    }

    @Override
    public int getBackPressureHighWatermark() {
        return integrationMqttClientSettingsComponent.getBackPressureHighWatermark();
    }

    @Override
    public int getBackPressureLowWatermark() {
        return integrationMqttClientSettingsComponent.getBackPressureLowWatermark();
    }

}
