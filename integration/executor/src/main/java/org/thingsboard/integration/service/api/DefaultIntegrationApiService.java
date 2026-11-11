// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.service.api;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.AssetCacheInfo;
import org.thingsboard.server.common.data.AssetProfileCacheInfo;
import org.thingsboard.server.common.data.DeviceCacheInfo;
import org.thingsboard.server.common.data.DeviceProfileCacheInfo;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.stats.MessagesStats;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsType;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.common.transport.util.JsonUtils;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.gen.integration.AssetUplinkDataProto;
import org.thingsboard.server.gen.integration.DeviceUplinkDataProto;
import org.thingsboard.server.gen.integration.IntegrationApiRequestMsg;
import org.thingsboard.server.gen.integration.IntegrationApiResponseMsg;
import org.thingsboard.server.gen.integration.IntegrationInfoProto;
import org.thingsboard.server.gen.integration.TbIntegrationEventProto;
import org.thingsboard.server.gen.integration.TbIntegrationTsDataProto;
import org.thingsboard.server.gen.integration.ToCoreIntegrationMsg;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.DeviceActivityProto;
import org.thingsboard.server.gen.transport.TransportProtos.GetQueueRoutingInfoResponseMsg;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.TbQueueRequestTemplate;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.common.TbRuleEngineProducerService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.service.cache.IntegrationExecutorAssetCache;
import org.thingsboard.server.service.cache.IntegrationExecutorAssetProfileCache;
import org.thingsboard.server.service.cache.IntegrationExecutorDeviceCache;
import org.thingsboard.server.service.cache.IntegrationExecutorDeviceProfileCache;
import org.thingsboard.server.service.data.EntityCacheKey;
import org.thingsboard.server.service.data.EntityUplinkData;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;

@RequiredArgsConstructor
@Service
@Slf4j
public class DefaultIntegrationApiService implements IntegrationApiService {

    private static final String DEVICE_NAME_META = "deviceName";
    private static final String DEVICE_TYPE_META = "deviceType";
    private static final String ASSET_NAME_META = "assetName";
    private static final String ASSET_TYPE_META = "assetType";
    private static final String TS_META = "ts";

    private final StatsFactory statsFactory;
    @Lazy
    private final PartitionService partitionService;
    private final TbQueueProducerProvider producerProvider;
    @Lazy
    private final TbRuleEngineProducerService ruleEngineProducerService;
    @Lazy
    private final TbApiUsageReportClient apiUsageReportClient;

    private final IntegrationExecutorDeviceProfileCache deviceProfileCache;
    private final IntegrationExecutorAssetProfileCache assetProfileCache;
    private final IntegrationExecutorDeviceCache deviceCache;
    private final IntegrationExecutorAssetCache assetCache;

    private final TbQueueRequestTemplate<TbProtoQueueMsg<IntegrationApiRequestMsg>, TbProtoQueueMsg<IntegrationApiResponseMsg>> apiTemplate;

    @Value("${queue.integration_api.max_callback_threads:10}")
    private int maxCallbackThreads;

    private ExecutorService callbackExecutor;

    protected MessagesStats tbCoreProducerStats;
    protected MessagesStats ruleEngineProducerStats;

    private final Gson gson = new Gson();

    @PostConstruct
    public void init() {
        this.callbackExecutor = ThingsBoardExecutors.newWorkStealingPool(maxCallbackThreads, "integration-uplink-callback");
        this.tbCoreProducerStats = statsFactory.createMessagesStats(StatsType.CORE.getName() + ".producer");
        this.ruleEngineProducerStats = statsFactory.createMessagesStats(StatsType.RULE_ENGINE.getName() + ".producer");
    }

    @PreDestroy
    public void destroy() {
        callbackExecutor.shutdown();
    }

    @Override
    public void sendUplinkData(Integration integration, IntegrationInfoProto proto, DeviceUplinkDataProto data, IntegrationCallback<Void> callback) {
        EntityUplinkData entityUplinkData = EntityUplinkData.builder()
                .name(data.getDeviceName())
                .type(data.getDeviceType())
                .label(data.getDeviceLabel())
                .customerName(data.getCustomerName())
                .groupName(data.getGroupName())
                .build();
        Futures.addCallback(deviceCache.getEntity(proto, entityUplinkData), new FutureCallback<>() {
            @Override
            public void onSuccess(DeviceCacheInfo device) {
                if (device == null) {
                    callback.onError(new RuntimeException("Device not found or failed to create: " + data.getDeviceName()));
                    return;
                }
                try {
                    // Integration ID is stored in SessionInfoProto's sessionId fields (repurposed)
                    TransportProtos.SessionInfoProto sessionInfo = ProtoUtils.toSessionInfo(integration.getUuidId(), device);
                    onActivity(device.tenantId(), new DeviceId(device.id()), integration, proto);
                    dispatchUplink(data.hasPostTelemetryMsg(), data.hasPostAttributesMsg(), callback,
                            composite -> process(sessionInfo, data.getPostTelemetryMsg(), composite),
                            composite -> process(sessionInfo, data.getPostAttributesMsg(), composite));
                } catch (Exception e) {
                    callback.onError(new RuntimeException("Failed to process device uplink data: " + data.getDeviceName(), e));
                }
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onError(new RuntimeException("Failed to get device or create device: " + proto, t));
            }
        }, callbackExecutor);
    }

    @Override
    public void sendUplinkData(Integration integration, IntegrationInfoProto proto, AssetUplinkDataProto data, IntegrationCallback<Void> callback) {
        EntityUplinkData entityUplinkData = EntityUplinkData.builder()
                .name(data.getAssetName())
                .type(data.getAssetType())
                .label(data.getAssetLabel())
                .customerName(data.getCustomerName())
                .groupName(data.getGroupName())
                .build();
        Futures.addCallback(assetCache.getEntity(proto, entityUplinkData), new FutureCallback<>() {
            @Override
            public void onSuccess(AssetCacheInfo asset) {
                if (asset == null) {
                    callback.onError(new RuntimeException("Asset not found or failed to create: " + data.getAssetName()));
                    return;
                }
                try {
                    dispatchUplink(data.hasPostTelemetryMsg(), data.hasPostAttributesMsg(), callback,
                            composite -> process(asset, data.getPostTelemetryMsg(), integration.getId(), composite),
                            composite -> process(asset, data.getPostAttributesMsg(), integration.getId(), composite));
                } catch (Exception e) {
                    callback.onError(new RuntimeException("Failed to process asset uplink data: " + data.getAssetName(), e));
                }
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onError(new RuntimeException("Failed to get or create asset: " + proto, t));
            }
        }, callbackExecutor);
    }

    private void dispatchUplink(boolean hasTelemetry, boolean hasAttributes, IntegrationCallback<Void> callback,
                                Consumer<IntegrationCallback<Void>> telemetryProcessor, Consumer<IntegrationCallback<Void>> attributesProcessor) {
        if (!hasTelemetry && !hasAttributes) {
            callback.onSuccess(null);
            return;
        }
        int operationCount = (hasTelemetry ? 1 : 0) + (hasAttributes ? 1 : 0);
        CompositeCallback compositeCallback = new CompositeCallback(operationCount, callback);
        if (hasTelemetry) {
            telemetryProcessor.accept(compositeCallback);
        }
        if (hasAttributes) {
            attributesProcessor.accept(compositeCallback);
        }
    }

    private void onActivity(TenantId tenantId, DeviceId deviceId, Integration integration, IntegrationInfoProto proto) {
        DeviceActivityProto deviceActivityProto = DeviceActivityProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setLastActivityTime(System.currentTimeMillis()).build();
        sendDeviceActivityData(integration, proto, deviceActivityProto, null);
    }

    @Override
    public void sendUplinkData(Integration integration, IntegrationInfoProto proto, TbMsg data, IntegrationCallback<Void> callback) {
        sendUplinkData(integration, proto, data, (b, _) -> b.setCustomTbMsgProto(TbMsg.toProto(data)).build(), true, callback);
    }

    @Override
    public void sendDeviceActivityData(Integration integration, IntegrationInfoProto proto, DeviceActivityProto data, IntegrationCallback<Void> callback) {
        sendUplinkData(integration, proto, data, (b, _) -> b.setActivityProto(data).build(), false, callback);
    }

    @Override
    public List<GetQueueRoutingInfoResponseMsg> getQueueRoutingInfo(TransportProtos.GetAllQueueRoutingInfoRequestMsg msg) {
        TbProtoQueueMsg<IntegrationApiRequestMsg> protoMsg =
                new TbProtoQueueMsg<>(UUID.randomUUID(), IntegrationApiRequestMsg.newBuilder().setGetAllQueueRoutingInfoRequest(msg).build());
        try {
            TbProtoQueueMsg<IntegrationApiResponseMsg> response = apiTemplate.send(protoMsg).get(1, TimeUnit.MINUTES);
            return response.getValue().getGetQueueRoutingInfoResponseList();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendEventData(TenantId tenantId, EntityId entityId, TbIntegrationEventProto data, IntegrationCallback<Void> callback) {
        pushToTbCore(tenantId, entityId, entityId.getId(), ToCoreIntegrationMsg.newBuilder().setEventProto(data).build(), callback);
    }

    @Override
    public void sendTsData(TenantId tenantId, EntityId entityId, TbIntegrationTsDataProto tsData, IntegrationCallback<Void> callback) {
        pushToTbCore(tenantId, entityId, entityId.getId(), ToCoreIntegrationMsg.newBuilder().setTsDataProto(tsData).build(), callback);
    }

    public <T> void sendUplinkData(Integration integration, IntegrationInfoProto proto, T data,
                                   BiFunction<ToCoreIntegrationMsg.Builder, T, ToCoreIntegrationMsg> messageConstructor, boolean addIntegration,
                                   IntegrationCallback<Void> callback) {
        var builder = ToCoreIntegrationMsg.newBuilder();
        if (addIntegration) {
            builder.setIntegration(proto);
        }
        var msg = messageConstructor.apply(builder, data);
        pushToTbCore(integration.getTenantId(), integration.getId(), integration.getId().getId(), msg, callback);
    }

    private void pushToTbCore(TenantId tenantId, EntityId partitionKey, UUID messageKey, ToCoreIntegrationMsg msg, IntegrationCallback<Void> callback) {
        var producer = producerProvider.getTbCoreIntegrationMsgProducer();
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, partitionKey).withTopic(producer.getDefaultTopic());
        if (log.isTraceEnabled()) {
            log.trace("[{}][{}] Pushing to topic {} message {}", tenantId, partitionKey, tpi.getFullTopicName(), msg);
        }
        tbCoreProducerStats.incrementTotal();
        StatsTbQueueCallback wrappedCallback = new StatsTbQueueCallback(
                callback != null ? new IntegrationTbQueueCallback(callbackExecutor, callback) : null, tbCoreProducerStats);
        producer.send(tpi, new TbProtoQueueMsg<>(messageKey, msg), wrappedCallback);
    }

    private void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.PostTelemetryMsg msg, IntegrationCallback<Void> callback) {
        TenantId tenantId = TenantId.fromUUID(new UUID(sessionInfo.getTenantIdMSB(), sessionInfo.getTenantIdLSB()));
        DeviceId deviceId = new DeviceId(new UUID(sessionInfo.getDeviceIdMSB(), sessionInfo.getDeviceIdLSB()));
        processTelemetry(tenantId, getCustomerId(sessionInfo), sessionInfo.getDeviceName(), sessionInfo.getDeviceType(),
                DEVICE_NAME_META, DEVICE_TYPE_META, msg,
                (json, metaData, msgType, cb) -> sendToRuleEngine(tenantId, deviceId, sessionInfo, json, metaData, msgType, cb),
                callback);
    }

    private void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.PostAttributeMsg msg, IntegrationCallback<Void> callback) {
        TenantId tenantId = TenantId.fromUUID(new UUID(sessionInfo.getTenantIdMSB(), sessionInfo.getTenantIdLSB()));
        DeviceId deviceId = new DeviceId(new UUID(sessionInfo.getDeviceIdMSB(), sessionInfo.getDeviceIdLSB()));
        processAttributes(tenantId, getCustomerId(sessionInfo), sessionInfo.getDeviceName(), sessionInfo.getDeviceType(),
                DEVICE_NAME_META, DEVICE_TYPE_META, msg,
                (json, metaData, msgType, cb) -> sendToRuleEngine(tenantId, deviceId, sessionInfo, json, metaData, msgType, cb),
                callback);
    }

    private void process(AssetCacheInfo asset, TransportProtos.PostTelemetryMsg msg, IntegrationId integrationId, IntegrationCallback<Void> callback) {
        processTelemetry(asset.tenantId(), asset.customerId(), asset.name(), asset.type(),
                ASSET_NAME_META, ASSET_TYPE_META, msg,
                (json, metaData, msgType, cb) -> sendToRuleEngine(asset.tenantId(),
                        new AssetId(asset.id()), asset.type(), asset.assetProfileId(), asset.customerId(), json, metaData, msgType, integrationId, cb),
                callback);
    }

    private void process(AssetCacheInfo asset, TransportProtos.PostAttributeMsg msg, IntegrationId integrationId, IntegrationCallback<Void> callback) {
        processAttributes(asset.tenantId(), asset.customerId(), asset.name(), asset.type(),
                ASSET_NAME_META, ASSET_TYPE_META, msg,
                (json, metaData, msgType, cb) -> sendToRuleEngine(asset.tenantId(),
                        new AssetId(asset.id()), asset.type(), asset.assetProfileId(), asset.customerId(), json, metaData, msgType, integrationId, cb),
                callback);
    }

    private void processTelemetry(TenantId tenantId, CustomerId customerId, String entityName, String entityType,
                                  String nameMetaKey, String typeMetaKey, TransportProtos.PostTelemetryMsg msg,
                                  TbMsgSender sender, IntegrationCallback<Void> callback) {
        if (msg.getTsKvListCount() == 0) {
            callback.onSuccess(null);
            return;
        }
        int dataPoints = 0;
        for (TransportProtos.TsKvListProto tsKv : msg.getTsKvListList()) {
            dataPoints += tsKv.getKvCount();
        }
        MsgPackCallback packCallback = new MsgPackCallback(msg.getTsKvListCount(),
                new ApiStatsProxyCallback<>(tenantId, customerId, dataPoints, callback));
        for (TransportProtos.TsKvListProto tsKv : msg.getTsKvListList()) {
            TbMsgMetaData metaData = new TbMsgMetaData();
            metaData.putValue(nameMetaKey, entityName);
            metaData.putValue(typeMetaKey, entityType);
            metaData.putValue(TS_META, tsKv.getTs() + "");
            JsonObject json = JsonUtils.getJsonObject(tsKv.getKvList());
            sender.send(json, metaData, TbMsgType.POST_TELEMETRY_REQUEST, packCallback);
        }
    }

    private void processAttributes(TenantId tenantId, CustomerId customerId, String entityName, String entityType, String nameMetaKey, String typeMetaKey,
                                   TransportProtos.PostAttributeMsg msg, TbMsgSender sender, IntegrationCallback<Void> callback) {
        JsonObject json = JsonUtils.getJsonObject(msg.getKvList());
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue(nameMetaKey, entityName);
        metaData.putValue(typeMetaKey, entityType);
        sender.send(json, metaData, TbMsgType.POST_ATTRIBUTES_REQUEST,
                new MsgPackCallback(1, new ApiStatsProxyCallback<>(tenantId, customerId, msg.getKvList().size(), callback)));
    }

    private void sendToRuleEngine(TenantId tenantId, AssetId assetId, String type, AssetProfileId assetProfileId, CustomerId customerId, JsonObject json,
                                  TbMsgMetaData metaData, TbMsgType msgType, IntegrationId integrationId, TbQueueCallback callback) {
        ListenableFuture<AssetProfileCacheInfo> future = assetProfileCache.getProfile(assetProfileId.getId(), EntityCacheKey.builder().tenantId(tenantId).name(type).build());
        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(AssetProfileCacheInfo assetProfile) {
                dispatchToRuleEngine(tenantId, assetId, customerId, assetProfile.defaultRuleChainId(),
                        assetProfile.defaultQueueName(), json, metaData, msgType, callback);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[{}] Failed to fetch asset profile {} to send into rule engine for integration {}", tenantId, type, integrationId, t);
                callback.onFailure(t);
            }
        }, callbackExecutor);
    }

    void sendToRuleEngine(TenantId tenantId, DeviceId deviceId, TransportProtos.SessionInfoProto sessionInfo, JsonObject json,
                          TbMsgMetaData metaData, TbMsgType msgType, TbQueueCallback callback) {
        DeviceProfileId deviceProfileId = new DeviceProfileId(new UUID(sessionInfo.getDeviceProfileIdMSB(), sessionInfo.getDeviceProfileIdLSB()));
        ListenableFuture<DeviceProfileCacheInfo> future = deviceProfileCache.getProfile(deviceProfileId.getId(), EntityCacheKey.builder().tenantId(tenantId).name(sessionInfo.getDeviceType()).build());
        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(DeviceProfileCacheInfo deviceProfile) {
                dispatchToRuleEngine(tenantId, deviceId, getCustomerId(sessionInfo), deviceProfile.defaultRuleChainId(),
                        deviceProfile.defaultQueueName(), json, metaData, msgType, callback);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[{}] Failed to fetch device profile {} to send into rule engine for integration {}", tenantId, sessionInfo.getDeviceType(),
                        new IntegrationId(new UUID(sessionInfo.getSessionIdMSB(), sessionInfo.getSessionIdLSB())), t);
                callback.onFailure(t);
            }
        }, callbackExecutor);
    }

    private void dispatchToRuleEngine(TenantId tenantId, EntityId originator, CustomerId customerId,
                                      RuleChainId ruleChainId, String queueName, JsonObject json,
                                      TbMsgMetaData metaData, TbMsgType msgType, TbQueueCallback callback) {
        log.debug("[{}] Send msg to rule engine for {} {} using queue {} and ruleChain {}", tenantId, originator.getEntityType(), originator, queueName, ruleChainId);
        TbMsg tbMsg = TbMsg.newMsg()
                .queueName(queueName)
                .type(msgType)
                .originator(originator)
                .customerId(customerId)
                .metaData(metaData)
                .data(gson.toJson(json))
                .ruleChainId(ruleChainId)
                .build();
        ruleEngineProducerStats.incrementTotal();
        ruleEngineProducerService.sendToRuleEngine(producerProvider.getRuleEngineMsgProducer(), tenantId, tbMsg, callback);
    }

    private CustomerId getCustomerId(TransportProtos.SessionInfoProto sessionInfo) {
        if (sessionInfo.getCustomerIdMSB() != 0 && sessionInfo.getCustomerIdLSB() != 0) {
            return new CustomerId(new UUID(sessionInfo.getCustomerIdMSB(), sessionInfo.getCustomerIdLSB()));
        }
        return null;
    }

    @FunctionalInterface
    private interface TbMsgSender {
        void send(JsonObject json, TbMsgMetaData metaData, TbMsgType msgType, TbQueueCallback callback);
    }

    private class MsgPackCallback implements TbQueueCallback {

        private final AtomicInteger msgCount;
        private final AtomicBoolean completed = new AtomicBoolean(false);
        private final IntegrationCallback<Void> callback;

        public MsgPackCallback(Integer msgCount, IntegrationCallback<Void> callback) {
            this.msgCount = new AtomicInteger(msgCount);
            this.callback = callback;
        }

        @Override
        public void onSuccess(TbQueueMsgMetadata metadata) {
            if (msgCount.decrementAndGet() <= 0 && completed.compareAndSet(false, true) && callback != null) {
                submit(() -> callback.onSuccess(null), null);
            }
        }

        @Override
        public void onFailure(Throwable t) {
            if (completed.compareAndSet(false, true) && callback != null) {
                submit(() -> callback.onError(t), t);
            }
        }

        private void submit(Runnable task, Throwable originalError) {
            try {
                callbackExecutor.submit(task);
            } catch (Exception e) {
                log.warn("Failed to submit callback", e);
                callback.onError(originalError != null ? originalError : e);
            }
        }

    }

    private class ApiStatsProxyCallback<T> implements IntegrationCallback<T> {

        private final TenantId tenantId;
        private final CustomerId customerId;
        private final int dataPoints;
        private final IntegrationCallback<T> callback;

        public ApiStatsProxyCallback(TenantId tenantId, CustomerId customerId, int dataPoints, IntegrationCallback<T> callback) {
            this.tenantId = tenantId;
            this.customerId = customerId;
            this.dataPoints = dataPoints;
            this.callback = callback;
        }

        @Override
        public void onSuccess(T msg) {
            try {
                apiUsageReportClient.report(tenantId, customerId, ApiUsageRecordKey.TRANSPORT_MSG_COUNT, 1);
                apiUsageReportClient.report(tenantId, customerId, ApiUsageRecordKey.TRANSPORT_DP_COUNT, dataPoints);
            } finally {
                if (callback != null) {
                    callback.onSuccess(msg);
                }
            }
        }

        @Override
        public void onError(Throwable e) {
            if (callback != null) {
                callback.onError(e);
            }
        }

    }

    private static class CompositeCallback implements IntegrationCallback<Void> {

        private final AtomicInteger pendingCount;
        private final IntegrationCallback<Void> callback;
        private final AtomicBoolean completed = new AtomicBoolean(false);

        public CompositeCallback(int operationCount, IntegrationCallback<Void> callback) {
            this.pendingCount = new AtomicInteger(operationCount);
            this.callback = callback;
        }

        @Override
        public void onSuccess(Void msg) {
            if (pendingCount.decrementAndGet() <= 0 && completed.compareAndSet(false, true) && callback != null) {
                callback.onSuccess(null);
            }
        }

        @Override
        public void onError(Throwable e) {
            if (completed.compareAndSet(false, true) && callback != null) {
                callback.onError(e);
            }
        }

    }

}
