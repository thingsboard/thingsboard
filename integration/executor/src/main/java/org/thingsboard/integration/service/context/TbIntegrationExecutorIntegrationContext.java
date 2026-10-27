// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.service.context;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.ByteString;
import io.netty.channel.EventLoopGroup;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.IntegrationRateLimitService;
import org.thingsboard.integration.api.IntegrationStatisticsService;
import org.thingsboard.integration.api.converter.ConverterContext;
import org.thingsboard.integration.api.data.DownLinkMsg;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.integration.api.util.IntegrationMqttClientSettingsComponent;
import org.thingsboard.integration.api.util.LogSettingsComponent;
import org.thingsboard.integration.service.api.IntegrationApiService;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.JavaSerDesUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.event.Event;
import org.thingsboard.server.common.data.event.IntegrationDebugEvent;
import org.thingsboard.server.common.data.event.RawDataEvent;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.gen.integration.AssetUplinkDataProto;
import org.thingsboard.server.gen.integration.DeviceUplinkDataProto;
import org.thingsboard.server.gen.integration.IntegrationInfoProto;
import org.thingsboard.server.gen.integration.TbEventSource;
import org.thingsboard.server.gen.integration.TbIntegrationEventProto;
import org.thingsboard.server.service.integration.EventStorageService;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
public class TbIntegrationExecutorIntegrationContext implements IntegrationContext {

    private final String serviceId;
    private final IntegrationApiService apiService;
    private final IntegrationStatisticsService statisticsService;
    private final TbIntegrationExecutorContextComponent contextComponent;
    private final Integration configuration;
    private final IntegrationInfoProto integrationInfoProto;

    private final LogSettingsComponent logSettingsComponent;
    private final IntegrationMqttClientSettingsComponent integrationMqttClientSettingsComponent;

    private final EventStorageService eventStorageService;

    @Getter
    @Value("${integrations.init.connection_timeout_sec:10}")
    private int integrationConnectTimeoutSec;

    public TbIntegrationExecutorIntegrationContext(
            String serviceId,
            IntegrationApiService apiService,
            IntegrationStatisticsService statisticsService,
            TbIntegrationExecutorContextComponent contextComponent,
            LogSettingsComponent logSettingsComponent,
            IntegrationMqttClientSettingsComponent integrationMqttClientSettingsComponent,
            Integration configuration,
            EventStorageService eventStorageService
    ) {
        this.serviceId = serviceId;
        this.apiService = apiService;
        this.statisticsService = statisticsService;
        this.contextComponent = contextComponent;
        this.configuration = configuration;
        this.logSettingsComponent = logSettingsComponent;
        this.integrationMqttClientSettingsComponent = integrationMqttClientSettingsComponent;
        this.integrationInfoProto = ProtoUtils.toIntegrationInfoProto(configuration);
        this.eventStorageService = eventStorageService;
    }

    @Override
    public String getServiceId() {
        return serviceId;
    }

    @Override
    public ConverterContext getUplinkConverterContext() {
        return new TbIntegrationExecutorConverterContext(configuration.getDefaultConverterId(), TbEventSource.UPLINK_CONVERTER);
    }

    @Override
    public ConverterContext getDownlinkConverterContext() {
        return new TbIntegrationExecutorConverterContext(configuration.getDownlinkConverterId(), TbEventSource.DOWNLINK_CONVERTER);
    }

    @Override
    public void processUplinkData(DeviceUplinkDataProto uplinkData, IntegrationCallback<Void> callback) {
        log.trace("Received device uplink: {}", uplinkData);
        apiService.sendUplinkData(configuration, integrationInfoProto, uplinkData, callback);
    }

    @Override
    public void processUplinkData(AssetUplinkDataProto uplinkData, IntegrationCallback<Void> callback) {
        log.trace("Received asset uplink: {}", uplinkData);
        apiService.sendUplinkData(configuration, integrationInfoProto, uplinkData, callback);
    }

    @Override
    public void processCustomMsg(TbMsg msg, IntegrationCallback<Void> callback) {
        apiService.sendUplinkData(configuration, integrationInfoProto, msg, callback);
    }

    @Override
    public void saveEvent(IntegrationDebugEvent event, IntegrationCallback<Void> callback) {
        doSaveEvent(TbEventSource.INTEGRATION, configuration.getId(), event, null, callback);
    }

    @Override
    public void saveLifecycleEvent(ComponentLifecycleEvent lcEvent, Exception e) {
        eventStorageService.persistLifecycleEvent(configuration.getTenantId(), configuration.getId(), lcEvent, e);
    }

    @Override
    public void saveRawDataEvent(String deviceName, String type, String uid, JsonNode body, IntegrationCallback<Void> callback) {
        doSaveEvent(TbEventSource.DEVICE, configuration.getTenantId(), RawDataEvent.builder()
                .tenantId(configuration.getTenantId())
                .serviceId(getServiceId())
                .uuid(uid)
                .messageType(type)
                .message(body.toString())
                .build(), deviceName, callback);
    }

    @Override
    public EventLoopGroup getEventLoopGroup() {
        return contextComponent.getEventLoopGroup();
    }

    @Override
    public ScheduledExecutorService getScheduledExecutorService() {
        return contextComponent.getScheduledExecutorService();
    }

    @Override
    public void onUplinkMessageProcessed(boolean success) {
        if (configuration != null) {
            statisticsService.onUplinkMsg(configuration.getType(), success);
        }
    }

    @Override
    public void onDownlinkMessageProcessed(boolean success) {
        if (configuration != null) {
            statisticsService.onDownlinkMsg(configuration.getType(), success);
        }
    }

    @Override
    public Optional<IntegrationRateLimitService> getRateLimitService() {
        return Optional.of(contextComponent.getRateLimitService());
    }

    @Override
    public ExecutorService getExecutorService() {
        return contextComponent.getGeneralExecutorService();
    }

    @Override
    public ExecutorService getCallBackExecutorService() {
        return contextComponent.getCallBackExecutorService();
    }

    @Override
    public DownLinkMsg getDownlinkMsg(String deviceName) {
        Device device = contextComponent.findCachedDeviceByTenantIdAndName(configuration.getTenantId(), deviceName);
        if (device != null) {
            return contextComponent.getDownlinkCacheService().get(configuration.getId(), device.getId());
        } else {
            return null;
        }
    }

    @Override
    public DownLinkMsg putDownlinkMsg(IntegrationDownlinkMsg msg) {
        return contextComponent.getDownlinkCacheService().put(msg);
    }

    @Override
    public void removeDownlinkMsg(String deviceName) {
        Device device = contextComponent.findCachedDeviceByTenantIdAndName(configuration.getTenantId(), deviceName);
        if (device != null) {
            contextComponent.getDownlinkCacheService().remove(configuration.getId(), device.getId());
        }
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public boolean isExceptionStackTraceEnabled() {
        return logSettingsComponent.isExceptionStackTraceEnabled();
    }

    private void doSaveEvent(TbEventSource tbEventSource, EntityId entityId, Event event, String deviceName, IntegrationCallback<Void> callback) {
        var builder = TbIntegrationEventProto.newBuilder()
                .setSource(tbEventSource)
                .setEvent(ByteString.copyFrom(JavaSerDesUtil.encode(event)));
        builder.setTenantIdMSB(configuration.getTenantId().getId().getMostSignificantBits());
        builder.setTenantIdLSB(configuration.getTenantId().getId().getLeastSignificantBits());
        if (event.getEntityId() != null) {
            builder.setEventSourceIdMSB(event.getEntityId().getMostSignificantBits());
            builder.setEventSourceIdLSB(event.getEntityId().getLeastSignificantBits());

        }
        if (StringUtils.isNotEmpty(deviceName)) {
            builder.setDeviceName(deviceName);
        }
        apiService.sendEventData(configuration.getTenantId(), entityId, builder.build(), callback);
    }

    @RequiredArgsConstructor
    private class TbIntegrationExecutorConverterContext implements ConverterContext {

        private final ConverterId converterId;
        private final TbEventSource eventSource;

        @Override
        public String getServiceId() {
            return serviceId;
        }

        @Override
        public void saveEvent(Event event, IntegrationCallback<Void> callback) {
            TbIntegrationExecutorIntegrationContext.this.doSaveEvent(eventSource, converterId, event, null, callback);
        }

        @Override
        public Optional<IntegrationRateLimitService> getRateLimitService() {
            return Optional.of(contextComponent.getRateLimitService());
        }

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
    public int getBackPressureHighWatermark() {
        return integrationMqttClientSettingsComponent.getBackPressureHighWatermark();
    }

    @Override
    public int getBackPressureLowWatermark() {
        return integrationMqttClientSettingsComponent.getBackPressureLowWatermark();
    }

}
