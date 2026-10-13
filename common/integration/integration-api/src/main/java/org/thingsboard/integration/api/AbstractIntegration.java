// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.DebugModeUtil;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.converter.TBDownlinkDataConverter;
import org.thingsboard.integration.api.converter.TBUplinkDataConverter;
import org.thingsboard.integration.api.data.ContentType;
import org.thingsboard.integration.api.data.DownlinkData;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.integration.api.util.ExceptionUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.event.IntegrationDebugEvent;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.server.gen.integration.AssetUplinkDataProto;
import org.thingsboard.server.gen.integration.DeviceUplinkDataProto;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

@Slf4j
public abstract class AbstractIntegration<T> implements ThingsboardPlatformIntegration<T> {

    @Setter
    protected Integration configuration;
    protected IntegrationContext context;
    protected TBUplinkDataConverter uplinkConverter;
    protected TBDownlinkDataConverter downlinkConverter;
    protected UplinkMetaData<String> metadataTemplate;
    protected IntegrationStatistics integrationStatistics;

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        this.configuration = params.getConfiguration();
        this.context = params.getContext();
        this.uplinkConverter = params.getUplinkConverter();
        this.downlinkConverter = params.getDownlinkConverter();
        Map<String, String> mdMap = new HashMap<>();
        mdMap.put("integrationName", configuration.getName());
        JsonNode metadata = configuration.getConfiguration().get("metadata");
        for (Map.Entry<String, JsonNode> md : metadata.properties()) {
            mdMap.put(md.getKey(), md.getValue().asText());
        }
        this.metadataTemplate = new UplinkMetaData<>(getDefaultUplinkContentType(), mdMap);

        if (integrationStatistics == null) {
            this.integrationStatistics = new IntegrationStatistics(context);
        }
    }

    protected ContentType getDefaultUplinkContentType() {
        return ContentType.JSON;
    }

    @Override
    public void update(TbIntegrationInitParams params) throws Exception {
        destroy();
        init(params);
    }

    @Override
    public Integration getConfiguration() {
        return configuration;
    }

    @Override
    public void validateConfiguration(Integration configuration, boolean allowLocalNetworkHosts) throws ThingsboardException {
        if (configuration == null || configuration.getConfiguration() == null) {
            throw new IllegalArgumentException("Integration configuration is empty!");
        }
        if (!configuration.isRemote()) {
            doValidateConfiguration(configuration.getConfiguration(), allowLocalNetworkHosts);
        }
    }

    @Override
    public void checkConnection(Integration integration, IntegrationContext ctx) throws ThingsboardException {
        if (integration == null || integration.getConfiguration() == null) {
            throw new IllegalArgumentException("Integration configuration is empty!");
        }
        if (!integration.isRemote()) {
            try {
                doCheckConnection(integration, ctx);
            } finally {
                destroy();
            }
        }
    }

    @Override
    public void onDownlinkMsg(IntegrationDownlinkMsg msg) {

    }

    @Override
    public IntegrationStatistics popStatistics() {
        IntegrationStatistics statistics = this.integrationStatistics;
        this.integrationStatistics = new IntegrationStatistics(context);
        return statistics;
    }

    @Override
    public ListenableFuture<Void> processAsync(T msg) {
        throw new RuntimeException("Process async not implemented");
    }

    protected <T> T getClientConfiguration(Integration configuration, Class<T> clazz) {
        JsonNode clientConfiguration = configuration.getConfiguration().get("clientConfiguration");
        return getClientConfiguration(clientConfiguration, clazz);
    }

    protected <T> T getClientConfiguration(JsonNode clientConfiguration, Class<T> clazz) {
        if (clientConfiguration == null) {
            throw new IllegalArgumentException("clientConfiguration field is missing!");
        } else {
            return JacksonUtil.convertValue(clientConfiguration, clazz);
        }
    }

    protected void doValidateConfiguration(JsonNode configuration, boolean allowLocalNetworkHosts) throws ThingsboardException {

    }

    protected void doCheckConnection(Integration integration, IntegrationContext ctx) throws ThingsboardException {

    }

    protected ListenableFuture<Void> processUplinkData(IntegrationContext context, UplinkData data) {
        if (data.isAsset()) {
            return processAssetUplinkData(context, data);
        } else {
            return processDeviceUplinkData(context, data);
        }
    }

    private ListenableFuture<Void> processDeviceUplinkData(IntegrationContext context, UplinkData data) {
        SettableFuture<Void> result = SettableFuture.create();

        String entityName = data.getDeviceName();
        TenantId tenantId = configuration.getTenantId();

        try {
            context.getRateLimitService().ifPresent(rls -> rls.checkLimitPerDevice(tenantId, entityName, data::toString));
        } catch (TbRateLimitsException e) {
            log.warn("[{}][{}] Rate limit exceeded: {}", tenantId, entityName, e.getMessage());
            result.setException(e);
            return result;
        }

        DeviceUplinkDataProto.Builder builder = DeviceUplinkDataProto.newBuilder()
                .setDeviceName(entityName)
                .setDeviceType(data.getDeviceType());
        if (StringUtils.isNotEmpty(data.getDeviceLabel())) {
            builder.setDeviceLabel(data.getDeviceLabel());
        }
        if (StringUtils.isNotEmpty(data.getCustomerName())) {
            builder.setCustomerName(data.getCustomerName());
        }
        if (StringUtils.isNotEmpty(data.getGroupName())) {
            builder.setGroupName(data.getGroupName());
        }
        if (data.getTelemetry() != null) {
            builder.setPostTelemetryMsg(data.getTelemetry());
        }
        if (data.getAttributesUpdate() != null) {
            builder.setPostAttributesMsg(data.getAttributesUpdate());
        }
        context.processUplinkData(builder.build(), new IntegrationCallback<>() {
            @Override
            public void onSuccess(Void msg) {
                log.debug("[{}][{}] Successfully processed msg: {}", tenantId, data.getDeviceName(), data);
                result.set(null);
            }

            @Override
            public void onError(Throwable e) {
                log.error("[{}][{}] Failed to process msg: {}", tenantId, data.getDeviceName(), data, e);
                result.setException(e);
            }
        });
        return result;
    }

    private ListenableFuture<Void> processAssetUplinkData(IntegrationContext context, UplinkData data) {
        SettableFuture<Void> result = SettableFuture.create();

        String entityName = data.getAssetName();
        TenantId tenantId = configuration.getTenantId();

        try {
            context.getRateLimitService().ifPresent(rls -> rls.checkLimitPerAsset(tenantId, entityName, data::toString));
        } catch (TbRateLimitsException e) {
            log.warn("[{}][{}] Rate limit exceeded: {}", tenantId, entityName, e.getMessage());
            result.setException(e);
            return result;
        }

        AssetUplinkDataProto.Builder builder = AssetUplinkDataProto.newBuilder()
                .setAssetName(entityName).setAssetType(data.getAssetType());
        if (StringUtils.isNotEmpty(data.getAssetLabel())) {
            builder.setAssetLabel(data.getAssetLabel());
        }
        if (StringUtils.isNotEmpty(data.getCustomerName())) {
            builder.setCustomerName(data.getCustomerName());
        }
        if (StringUtils.isNotEmpty(data.getGroupName())) {
            builder.setGroupName(data.getGroupName());
        }
        if (data.getTelemetry() != null) {
            builder.setPostTelemetryMsg(data.getTelemetry());
        }
        if (data.getAttributesUpdate() != null) {
            builder.setPostAttributesMsg(data.getAttributesUpdate());
        }
        context.processUplinkData(builder.build(), new IntegrationCallback<>() {
            @Override
            public void onSuccess(Void msg) {
                log.debug("[{}][{}] Successfully processed msg: {}", tenantId, data.getAssetName(), data);
                result.set(null);
            }

            @Override
            public void onError(Throwable e) {
                log.error("[{}][{}] Failed to process msg: {}", tenantId, data.getAssetName(), data, e);
                result.setException(e);
            }
        });
        return result;
    }

    protected void processUplinkDataBlocking(IntegrationContext context, UplinkData data) throws Exception {
        try {
            processUplinkData(context, data).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof Exception cause) {
                throw cause;
            }
            throw new RuntimeException(e.getCause());
        }
    }

    protected static boolean isLocalNetworkHost(String host) {
        try {
            InetAddress address = InetAddress.getByName(host);
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress() ||
                    address.isSiteLocalAddress()) {
                return true;
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Unable to resolve provided hostname: " + host);
        }
        return false;
    }

    protected void persistDebug(IntegrationContext context, String type, ContentType messageType, Supplier<String> message, String status, Throwable exception) {
        persistDebug(context, type, messageType.name(), message, null, status, exception);
    }

    protected void persistDebug(IntegrationContext context, String type, ContentType messageType, String message, String status, Throwable exception) {
        persistDebug(context, type, messageType.name(), null, message, status, exception);
    }

    protected void persistDebug(IntegrationContext context, String type, String messageType, Supplier<String> msgSupplier, String message, String status, Throwable exception) {
        try {
            doPersistDebug(context, type, messageType, msgSupplier != null ? msgSupplier.get() : message, status, exception);
        } catch (Exception e) {
            log.warn("[{}] Failed to persist debug message", configuration, e);
        }
    }

    private void doPersistDebug(IntegrationContext context, String type, String messageType, String message, String status, Throwable exception) {
        if (!DebugModeUtil.isDebugAvailable(configuration, status)) {
            return;
        }
        IntegrationId integrationId = configuration.getId();
        if (exception instanceof TbRateLimitsException) {
            EntityType limitedEntity = ((TbRateLimitsException) exception).getEntityType();
            if (context.getRateLimitService().get().alreadyProcessed(integrationId, limitedEntity)) {
                log.trace("[{}] [{}] [{}] Rate limited debug event already sent.", configuration.getTenantId(), integrationId, limitedEntity);
                return;
            }
        } else if (!context.getRateLimitService().map(s -> s.checkLimit(configuration.getTenantId(), integrationId, false)).orElse(true)) {
            if (context.getRateLimitService().get().alreadyProcessed(integrationId, EntityType.INTEGRATION)) {
                log.trace("[{}] [{}] [{}] Rate limited debug event already sent.", configuration.getTenantId(), integrationId, EntityType.INTEGRATION);
                return;
            } else {
                exception = new TbRateLimitsException(EntityType.INTEGRATION, "Integration debug rate limits reached!");
                status = "ERROR";
            }
        }

        var event = IntegrationDebugEvent.builder()
                .tenantId(configuration.getTenantId())
                .entityId(configuration.getId().getId())
                .serviceId(context.getServiceId())
                .eventType(type)
                .messageType(messageType)
                .message(message)
                .status(status);
        if (exception != null) {
            event.error(toString(exception));
        }

        context.saveEvent(event.build(), new DebugEventCallback());
    }

    protected String toString(Throwable e) {
        return ExceptionUtil.toString(e, configuration.getId(), context.isExceptionStackTraceEnabled());
    }

    protected ListenableFuture<List<UplinkData>> convertToUplinkDataListAsync(IntegrationContext context, byte[] data, UplinkMetaData md) {
        try {
            Optional<IntegrationRateLimitService> rateLimitService = context.getRateLimitService();
            rateLimitService.ifPresent(s -> s.checkLimit(configuration.getTenantId(), () -> new String(data)));
            return this.uplinkConverter.convertUplink(context.getUplinkConverterContext(), data, md, context.getCallBackExecutorService());
        } catch (Throwable t) {
            if (log.isDebugEnabled()) {
                log.debug("[{}][{}] Failed to apply uplink data converter function for data: {} and metadata: {}", configuration.getId(), configuration.getName(), Base64.getEncoder().encodeToString(data), md);
            }
            return Futures.immediateFailedFuture(t);
        }
    }

    //Please, prefer async method convertToUplinkDataListAsync
    protected List<UplinkData> convertToUplinkDataList(IntegrationContext context, byte[] data, UplinkMetaData<String> md) throws Exception {
        try {
            return convertToUplinkDataListAsync(context, data, md).get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof TbRateLimitsException rateLimitsException) {
                throw rateLimitsException;
            } else {
                throw e;
            }
        }
    }

    protected void reportDownlinkOk(IntegrationContext context, DownlinkData data) {
        context.onDownlinkMessageProcessed(true);
        integrationStatistics.incMessagesProcessed();
        String status = downlinkConverter != null ? "OK" : "FAILURE";
        Supplier<String> msgSupplier = () -> {
            ObjectNode json = JacksonUtil.newObjectNode();
            if (data.getMetadata() != null && !data.getMetadata().isEmpty()) {
                json.set("metadata", JacksonUtil.valueToTree(data.getMetadata()));
            }
            json.set("payload", getDownlinkPayloadJson(data));
            return JacksonUtil.toString(json);
        };
        persistDebug(context, "Downlink", ContentType.JSON, msgSupplier, status, null);
    }

    protected void reportDownlinkError(IntegrationContext context, TbMsg msg, String status, Throwable exception) {
        if (!status.equals("OK")) {
            context.onDownlinkMessageProcessed(false);
            integrationStatistics.incErrorsOccurred();
            if (log.isDebugEnabled()) {
                log.debug("[{}][{}] Failed to apply downlink data converter function for data: {} and metadata: {}", configuration.getId(), configuration.getName(), msg.getData(), msg.getMetaData());
            }
            persistDebug(context, "Downlink", ContentType.JSON, () -> JacksonUtil.toString(msg), status, exception);
        }
    }

    protected JsonNode getDownlinkPayloadJson(DownlinkData data) {
        String contentType = data.getContentType();
        if ("JSON".equals(contentType)) {
            return JacksonUtil.fromBytes(data.getData());
        } else if ("TEXT".equals(contentType)) {
            return new TextNode(new String(data.getData(), StandardCharsets.UTF_8));
        } else { //BINARY
            return new TextNode(Base64.getEncoder().encodeToString(data.getData()));
        }
    }

    protected <T> void logDownlink(IntegrationContext context, String updateType, T msg) {
        String status = downlinkConverter != null ? "OK" : "FAILURE";
        persistDebug(context, updateType, ContentType.JSON, () -> JacksonUtil.toString(msg), status, null);
    }

    private static class DebugEventCallback implements IntegrationCallback<Void> {

        @Override
        public void onSuccess(Void msg) {
            if (log.isDebugEnabled()) {
                log.debug("Event has been saved successfully!");
            }
        }

        @Override
        public void onError(Throwable e) {
            log.error("Failed to save the debug event!", e);
        }

    }

}
