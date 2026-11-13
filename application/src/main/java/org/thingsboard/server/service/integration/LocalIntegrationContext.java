// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration;

import com.fasterxml.jackson.databind.JsonNode;
import io.netty.channel.EventLoopGroup;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.IntegrationRateLimitService;
import org.thingsboard.integration.api.converter.ConverterContext;
import org.thingsboard.integration.api.data.DownLinkMsg;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.integration.api.util.IntegrationMqttClientSettingsComponent;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.event.Event;
import org.thingsboard.server.common.data.event.IntegrationDebugEvent;
import org.thingsboard.server.common.data.event.RawDataEvent;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.gen.integration.AssetUplinkDataProto;
import org.thingsboard.server.gen.integration.DeviceUplinkDataProto;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

@Data
@Slf4j
public class LocalIntegrationContext implements IntegrationContext {

    private static final String DEVICE_VIEW_NAME_ENDING = "_View";

    private final IntegrationContextComponent ctx;
    private final Integration configuration;
    private final ConverterContext uplinkConverterContext;
    private final ConverterContext downlinkConverterContext;

    private final IntegrationMqttClientSettingsComponent integrationMqttClientSettingsComponent;

    public LocalIntegrationContext(
            IntegrationContextComponent ctx,
            Integration configuration,
            IntegrationMqttClientSettingsComponent integrationMqttClientSettingsComponent
    ) {
        this.ctx = ctx;
        this.configuration = configuration;
        this.integrationMqttClientSettingsComponent = integrationMqttClientSettingsComponent;

        var tenantId = configuration.getTenantId();
        var converterContextComponent = ctx.getConverterContextComponent();

        uplinkConverterContext = new LocalConverterContext(converterContextComponent, tenantId, configuration.getDefaultConverterId());
        downlinkConverterContext = new LocalConverterContext(converterContextComponent, tenantId, configuration.getDownlinkConverterId());
    }

    @Override
    public void processUplinkData(DeviceUplinkDataProto data, IntegrationCallback<Void> callback) {
        ctx.getPlatformIntegrationService().processUplinkData(configuration, data, callback).run();
    }

    @Override
    public void processUplinkData(AssetUplinkDataProto data, IntegrationCallback<Void> callback) {
        ctx.getPlatformIntegrationService().processUplinkData(configuration, data, callback).run();
    }

    @Override
    public void processCustomMsg(TbMsg tbMsg, IntegrationCallback<Void> callback) {
        ctx.getPlatformIntegrationService().process(configuration.getTenantId(), tbMsg, callback);
    }

    @Override
    public void saveEvent(IntegrationDebugEvent event, IntegrationCallback<Void> callback) {
        doSaveEvent(event, callback);
    }

    @Override
    public void saveLifecycleEvent(ComponentLifecycleEvent lcEvent, Exception e) {
        ctx.getEventStorageService().persistLifecycleEvent(configuration.getTenantId(), configuration.getId(), lcEvent, e);
    }

    @Override
    public void saveRawDataEvent(String deviceName, String type, String uid, JsonNode body, IntegrationCallback<Void> callback) {
        Device device = ctx.getDeviceService().findDeviceByTenantIdAndName(configuration.getTenantId(), deviceName);
        if (device != null) {
            doSaveEvent(RawDataEvent.builder()
                    .tenantId(configuration.getTenantId())
                    .entityId(device.getId().getId())
                    .serviceId(getServiceId())
                    .uuid(uid)
                    .messageType(type)
                    .message(body.toString())
                    .build(), callback);
        }
    }

    @Override
    public DownLinkMsg getDownlinkMsg(String deviceName) {
        Device device = ctx.getDeviceService().findDeviceByTenantIdAndName(configuration.getTenantId(), deviceName);
        if (device != null) {
            return ctx.getDownlinkCacheService().get(configuration.getId(), device.getId());
        } else {
            return null;
        }
    }

    @Override
    public DownLinkMsg putDownlinkMsg(IntegrationDownlinkMsg msg) {
        return ctx.getDownlinkCacheService().put(msg);
    }

    @Override
    public void removeDownlinkMsg(String deviceName) {
        Device device = ctx.getDeviceService().findDeviceByTenantIdAndName(configuration.getTenantId(), deviceName);
        if (device != null) {
            ctx.getDownlinkCacheService().remove(configuration.getId(), device.getId());
        }
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    private void doSaveEvent(Event event, IntegrationCallback<Void> callback) {
        DonAsynchron.withCallback(ctx.getEventService().saveAsync(event), res -> callback.onSuccess(null), callback::onError);
    }

    @Override
    public String getServiceId() {
        return ctx.getServiceInfoProvider().getServiceId();
    }

    @Override
    public EventLoopGroup getEventLoopGroup() {
        return ctx.getEventLoopGroup();
    }

    @Override
    public ScheduledExecutorService getScheduledExecutorService() {
        return ctx.getScheduledExecutorService();
    }

    @Override
    public ExecutorService getExecutorService() {
        return ctx.getGeneralExecutorService();
    }

    @Override
    public ExecutorService getCallBackExecutorService() {
        return ctx.getCallBackExecutorService();
    }

    @Override
    public boolean isExceptionStackTraceEnabled() {
        return ctx.isExceptionStackTraceEnabled();
    }

    @Override
    public void onUplinkMessageProcessed(boolean success) {
        if (configuration != null) {
            ctx.getIntegrationStatisticsService().onUplinkMsg(configuration.getType(), success);
        }
    }

    @Override
    public void onDownlinkMessageProcessed(boolean success) {
        if (configuration != null) {
            ctx.getIntegrationStatisticsService().onDownlinkMsg(configuration.getType(), success);
        }
    }

    @Override
    public Optional<IntegrationRateLimitService> getRateLimitService() {
        return Optional.of(ctx.getRateLimitService());
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

    @Override
    public int getIntegrationConnectTimeoutSec() {
        return integrationMqttClientSettingsComponent.getIntegrationConnectTimeoutSec();
    }

}
