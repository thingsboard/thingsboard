// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationInfo;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;
import org.thingsboard.server.common.stats.MessagesStats;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsType;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.cache.CacheExecutorService;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.secret.SecretConfigurationService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.gen.integration.ConverterRequestProto;
import org.thingsboard.server.gen.integration.EntityRequestProto;
import org.thingsboard.server.gen.integration.GetAllProfilesRequestProto;
import org.thingsboard.server.gen.integration.GetAllProfilesResponseProto;
import org.thingsboard.server.gen.integration.GetEntitiesByRelationRequestProto;
import org.thingsboard.server.gen.integration.GetEntitiesByRelationResponseProto;
import org.thingsboard.server.gen.integration.IntegrationApiRequestMsg;
import org.thingsboard.server.gen.integration.IntegrationApiResponseMsg;
import org.thingsboard.server.gen.integration.IntegrationInfoListRequestProto;
import org.thingsboard.server.gen.integration.IntegrationInfoListResponseProto;
import org.thingsboard.server.gen.integration.IntegrationInfoProto;
import org.thingsboard.server.gen.integration.IntegrationRequestProto;
import org.thingsboard.server.gen.integration.ProfileRequestProto;
import org.thingsboard.server.gen.integration.TenantProfileRequestProto;
import org.thingsboard.server.gen.integration.ToCoreIntegrationMsg;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.TbQueueResponseTemplate;
import org.thingsboard.server.queue.common.DefaultTbQueueResponseTemplate;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.provider.TbCoreQueueFactory;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultTbCoreIntegrationApiService implements TbCoreIntegrationApiService {

    private final TbCoreQueueFactory tbCoreQueueFactory;
    private final StatsFactory statsFactory;
    private final IntegrationService integrationService;
    private final ConverterService converterService;
    private final TbTenantProfileCache tenantProfileCache;
    private final TbDeviceProfileCache deviceProfileCache;
    private final TbAssetProfileCache assetProfileCache;
    private final PlatformIntegrationService platformIntegrationService;
    private final SecretConfigurationService secretConfigurationService;
    private final QueueService queueService;
    private final CacheExecutorService cacheExecutorService;
    private final DeviceProfileService deviceProfileService;
    private final AssetProfileService assetProfileService;
    private final DeviceService deviceService;
    private final AssetService assetService;

    @Value("${queue.integration_api.max_pending_requests:10000}")
    private int maxPendingRequests;
    @Value("${queue.integration_api.max_requests_timeout:10000}")
    private long requestTimeout;
    @Value("${queue.integration_api.request_poll_interval:25}")
    private int responsePollDuration;
    @Value("${queue.integration_api.max_callback_threads:10}")
    private int maxCallbackThreads;

    private ExecutorService integrationCallbackExecutor;
    private TbQueueResponseTemplate<TbProtoQueueMsg<IntegrationApiRequestMsg>,
            TbProtoQueueMsg<IntegrationApiResponseMsg>> integrationApiTemplate;

    @PostConstruct
    public void init() {
        this.integrationCallbackExecutor = ThingsBoardExecutors.newWorkStealingPool(maxCallbackThreads, getClass());
        TbQueueProducer<TbProtoQueueMsg<IntegrationApiResponseMsg>> producer = tbCoreQueueFactory.createIntegrationApiResponseProducer();
        TbQueueConsumer<TbProtoQueueMsg<IntegrationApiRequestMsg>> consumer = tbCoreQueueFactory.createIntegrationApiRequestConsumer();

        String key = StatsType.INTEGRATION.getName();
        MessagesStats queueStats = statsFactory.createMessagesStats(key);

        DefaultTbQueueResponseTemplate.DefaultTbQueueResponseTemplateBuilder
                <TbProtoQueueMsg<IntegrationApiRequestMsg>, TbProtoQueueMsg<IntegrationApiResponseMsg>> builder = DefaultTbQueueResponseTemplate.builder();
        builder.requestTemplate(consumer);
        builder.responseTemplate(producer);
        builder.maxPendingRequests(maxPendingRequests);
        builder.requestTimeout(requestTimeout);
        builder.pollInterval(responsePollDuration);
        builder.executor(integrationCallbackExecutor);
        builder.stats(queueStats);
        integrationApiTemplate = builder.build();
    }

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        log.info("Received application ready event. Starting polling for events.");
        integrationApiTemplate.subscribe();
        integrationApiTemplate.launch(this);
    }

    @PreDestroy
    public void destroy() {
        if (integrationApiTemplate != null) {
            integrationApiTemplate.stop();
        }
        if (integrationCallbackExecutor != null) {
            integrationCallbackExecutor.shutdownNow();
        }
    }

    @Override
    public ListenableFuture<TbProtoQueueMsg<IntegrationApiResponseMsg>> handle(TbProtoQueueMsg<IntegrationApiRequestMsg> tbProtoQueueMsg) {
        var integrationApiRequest = tbProtoQueueMsg.getValue();

        ListenableFuture<IntegrationApiResponseMsg> result;

        if (integrationApiRequest.hasIntegrationListRequest()) {
            result = handleListRequest(integrationApiRequest.getIntegrationListRequest());
        } else if (integrationApiRequest.hasIntegrationRequest()) {
            result = handleIntegrationRequest(integrationApiRequest.getIntegrationRequest());
        } else if (integrationApiRequest.hasConverterRequest()) {
            result = handleConverterRequest(integrationApiRequest.getConverterRequest());
        } else if (integrationApiRequest.hasTenantProfileRequest()) {
            result = handleTenantProfileRequest(integrationApiRequest.getTenantProfileRequest());
        } else if (integrationApiRequest.hasProfileRequest()) {
            result = handleProfileRequestByEntityType(integrationApiRequest.getProfileRequest());
        } else if (integrationApiRequest.hasEntityRequest()) {
            result = handleEntityRequestByEntityType(integrationApiRequest.getEntityRequest());
        } else if (integrationApiRequest.hasGetAllQueueRoutingInfoRequest()) {
            result = handleAllQueueRoutingInfoRequest();
        } else if (integrationApiRequest.hasGetAllProfilesRequest()) {
            result = handleGetAllProfilesRequest(integrationApiRequest.getGetAllProfilesRequest());
        } else if (integrationApiRequest.hasGetEntitiesByRelationRequest()) {
            result = handleGetEntitiesByRelationRequest(integrationApiRequest.getGetEntitiesByRelationRequest());
        } else {
            throw new RuntimeException("Not Implemented!");
        }

        return Futures.transform(result,
                value -> new TbProtoQueueMsg<>(tbProtoQueueMsg.getKey(), value, tbProtoQueueMsg.getHeaders()),
                MoreExecutors.directExecutor());
    }

    @Override
    public void handle(Collection<TbProtoQueueMsg<ToCoreIntegrationMsg>> msgs, TbCallback callback) {
        List<Pair<TbProtoQueueMsg<ToCoreIntegrationMsg>, ListenableFuture<Runnable>>> futures = new ArrayList<>(msgs.size());
        for (TbProtoQueueMsg<ToCoreIntegrationMsg> msg : msgs) {
            try {
                // TODO: improve the retry strategy.
                ListenableFuture<Runnable> future = cacheExecutorService.executeAsync(() -> this.handle(msg, TbCallback.EMPTY));
                futures.add(Pair.of(msg, future));
            } catch (Throwable e) {
                log.debug("Failed to process integration msg: {}", msg, e);
            }
        }
        for (var future : futures) {
            try {
                future.getSecond().get(20, TimeUnit.SECONDS).run();
            } catch (Throwable e) {
                log.debug("Failed to process integration msg: {}", future.getFirst(), e);
            }
        }

        callback.onSuccess();
    }

    Runnable handle(TbProtoQueueMsg<ToCoreIntegrationMsg> envelope, TbCallback callback) {
        var msg = envelope.getValue();
        if (msg.hasIntegration()) {
            var info = ProtoUtils.fromProto(msg.getIntegration());
            if (msg.hasDeviceUplinkProto()) {
                return platformIntegrationService.processUplinkData(info, msg.getDeviceUplinkProto(), new IntegrationApiCallback(callback));
            } else if (msg.hasAssetUplinkProto()) {
                return platformIntegrationService.processUplinkData(info, msg.getAssetUplinkProto(), new IntegrationApiCallback(callback));
            } else if (msg.hasEntityViewDataProto()) {
                return platformIntegrationService.processUplinkData(info, msg.getEntityViewDataProto(), new IntegrationApiCallback(callback));
            } else if (msg.hasCustomTbMsgProto()) {
                return () -> platformIntegrationService.processUplinkData(info, TbMsg.fromProto(null, msg.getCustomTbMsgProto(), TbMsgCallback.EMPTY), new IntegrationApiCallback(callback));
            } else {
                callback.onFailure(new RuntimeException("Empty or not supported ToCoreIntegrationMsg!"));
            }
        } else if (msg.hasEventProto()) {
            return () -> platformIntegrationService.processUplinkData(msg.getEventProto(), new IntegrationApiCallback(callback));
        } else if (msg.hasTsDataProto()) {
            return () -> platformIntegrationService.processUplinkData(msg.getTsDataProto(), new IntegrationApiCallback(callback));
        } else if (msg.hasActivityProto()) {
            return () -> platformIntegrationService.processDeviceActivityData(msg.getActivityProto(), new IntegrationApiCallback(callback));
        } else {
            callback.onFailure(new IllegalArgumentException("Unsupported integration msg!"));
        }
        return () -> {};
    }

    private ListenableFuture<IntegrationApiResponseMsg> handleConverterRequest(ConverterRequestProto request) {
        var converterId = new ConverterId(new UUID(request.getConverterIdMSB(), request.getConverterIdLSB()));
        var tenantId = TenantId.fromUUID(new UUID(request.getTenantIdMSB(), request.getTenantIdLSB()));
        var future = converterService.findConverterByIdAsync(tenantId, converterId);

        return Futures.transform(future, converter -> IntegrationApiResponseMsg.newBuilder()
                .setConverterResponse(ProtoUtils.toProto(converter)).build(), MoreExecutors.directExecutor());
    }

    private ListenableFuture<IntegrationApiResponseMsg> handleIntegrationRequest(IntegrationRequestProto request) {
        var tenantId = TenantId.fromUUID(new UUID(request.getTenantIdMSB(), request.getTenantIdLSB()));
        ListenableFuture<Integration> future;
        if (request.getIntegrationIdMSB() != 0 || request.getIntegrationIdLSB() != 0) {
            var integrationId = new IntegrationId(new UUID(request.getIntegrationIdMSB(), request.getIntegrationIdLSB()));
            future = integrationService.findIntegrationByIdAsync(tenantId, integrationId);
        } else if (StringUtils.isNotEmpty(request.getRoutingKey())) {
            future = Futures.transform(Futures.immediateFuture(integrationService.findIntegrationByRoutingKey(tenantId, request.getRoutingKey())), opt -> opt.orElse(null), MoreExecutors.directExecutor());
        } else {
            future = Futures.immediateFailedFuture(new RuntimeException("Invalid request parameters!"));
        }

        return Futures.transform(future, integration -> {
            var builder = IntegrationApiResponseMsg.newBuilder();
            if (integration != null) {
                Integration copy = new Integration(integration);
                secretConfigurationService.replaceSecretUsages(tenantId, copy.getConfiguration());
                builder.setIntegrationResponse(ProtoUtils.toProto(copy));
            }
            return builder.build();
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<IntegrationApiResponseMsg> handleProfileRequestByEntityType(ProfileRequestProto profileRequestProto) {
        return switch (profileRequestProto.getEntityType()) {
            case DEVICE_PROFILE -> handleDeviceProfileRequest(profileRequestProto);
            case ASSET_PROFILE -> handleAssetProfileRequest(profileRequestProto);
            default -> throw new UnsupportedOperationException("Unsupported entity type for profile request: " + profileRequestProto.getEntityType());
        };
    }

    private ListenableFuture<IntegrationApiResponseMsg> handleEntityRequestByEntityType(EntityRequestProto entityRequestProto) {
        return switch (entityRequestProto.getEntityType()) {
            case DEVICE -> handleDeviceRequest(entityRequestProto);
            case ASSET -> handleAssetRequest(entityRequestProto);
            default -> throw new UnsupportedOperationException("Unsupported entity type for entity request: " + entityRequestProto.getEntityType());
        };
    }

    private ListenableFuture<IntegrationApiResponseMsg> handleListRequest(IntegrationInfoListRequestProto request) {
        IntegrationType integrationType = IntegrationType.valueOf(request.getType());
        // Reads plain integration rows instead of the integration info view — this response only carries the
        // IntegrationInfoProto scalars, so the view's extra attribute_kv join to compute status would be wasted work.
        List<Integration> data = integrationService.findAllCoreIntegrations(integrationType, false, request.getEnabled());

        List<IntegrationInfoProto> integrationInfoList = data.stream()
                .map(ProtoUtils::toIntegrationInfoProto)
                .collect(Collectors.toList());

        return Futures.immediateFuture(IntegrationApiResponseMsg.newBuilder().setIntegrationListResponse(
                IntegrationInfoListResponseProto.newBuilder().addAllIntegrationInfoList(integrationInfoList).build()
        ).build());
    }

    private ListenableFuture<IntegrationApiResponseMsg> handleTenantProfileRequest(TenantProfileRequestProto request) {
        TenantId tenantId = TenantId.fromUUID(new UUID(request.getTenantIdMSB(), request.getTenantIdLSB()));
        TenantProfile tenantProfile = tenantProfileCache.get(tenantId);
        return Futures.immediateFuture(IntegrationApiResponseMsg.newBuilder()
                .setTenantProfileResponse(ProtoUtils.toProto(tenantProfile))
                .build());
    }

    private ListenableFuture<IntegrationApiResponseMsg> handleDeviceProfileRequest(ProfileRequestProto request) {
        try {
            TenantId tenantId = TenantId.fromUUID(new UUID(request.getTenantIdMSB(), request.getTenantIdLSB()));
            DeviceProfile deviceProfile = deviceProfileCache.findOrCreateDeviceProfile(tenantId, request.getName());
            return Futures.immediateFuture(IntegrationApiResponseMsg.newBuilder()
                    .setDeviceProfileResponse(ProtoUtils.toCacheProto(deviceProfile))
                    .build());
        } catch (Exception e) {
            log.debug("Failed to process device profile request: name={}", request.getName(), e);
            return Futures.immediateFailedFuture(e);
        }
    }

    private ListenableFuture<IntegrationApiResponseMsg> handleAssetProfileRequest(ProfileRequestProto request) {
        try {
            TenantId tenantId = TenantId.fromUUID(new UUID(request.getTenantIdMSB(), request.getTenantIdLSB()));
            AssetProfile assetProfile = assetProfileCache.findOrCreateAssetProfile(tenantId, request.getName());
            return Futures.immediateFuture(IntegrationApiResponseMsg.newBuilder()
                    .setAssetProfileResponse(ProtoUtils.toCacheProto(assetProfile))
                    .build());
        } catch (Exception e) {
            log.debug("Failed to process asset profile request: name={}", request.getName(), e);
            return Futures.immediateFailedFuture(e);
        }
    }

    private ListenableFuture<IntegrationApiResponseMsg> handleDeviceRequest(EntityRequestProto request) {
        try {
            IntegrationInfo integration = ProtoUtils.fromProto(request.getIntegration());
            Device device = platformIntegrationService.processGetOrCreateDevice(integration, request.getName(), request.getType(), request.getLabel(), request.getCustomerName(), request.getGroupName());
            return Futures.immediateFuture(IntegrationApiResponseMsg.newBuilder()
                    .setDeviceResponse(ProtoUtils.toCacheProto(device))
                    .build());
        } catch (Exception e) {
            log.debug("Failed to process device request: name={}, type={}", request.getName(), request.getType(), e);
            return Futures.immediateFailedFuture(e);
        }
    }

    private ListenableFuture<IntegrationApiResponseMsg> handleAssetRequest(EntityRequestProto request) {
        try {
            IntegrationInfo integration = ProtoUtils.fromProto(request.getIntegration());
            Asset asset = platformIntegrationService.processGetOrCreateAsset(integration, request.getName(), request.getType(), request.getLabel(), request.getCustomerName(), request.getGroupName());
            return Futures.immediateFuture(IntegrationApiResponseMsg.newBuilder()
                    .setAssetResponse(ProtoUtils.toCacheProto(asset))
                    .build());
        } catch (Exception e) {
            log.debug("Failed to process asset request: name={}, type={}", request.getName(), request.getType(), e);
            return Futures.immediateFailedFuture(e);
        }
    }

    private ListenableFuture<IntegrationApiResponseMsg> handleAllQueueRoutingInfoRequest() {
        return queuesToIntegrationApiResponseMsg(queueService.findAllQueues());
    }

    private ListenableFuture<IntegrationApiResponseMsg> queuesToIntegrationApiResponseMsg(List<Queue> queues) {
        return Futures.immediateFuture(IntegrationApiResponseMsg.newBuilder()
                .addAllGetQueueRoutingInfoResponse(queues.stream()
                        .map(queue -> TransportProtos.GetQueueRoutingInfoResponseMsg.newBuilder()
                                .setTenantIdMSB(queue.getTenantId().getId().getMostSignificantBits())
                                .setTenantIdLSB(queue.getTenantId().getId().getLeastSignificantBits())
                                .setQueueIdMSB(queue.getId().getId().getMostSignificantBits())
                                .setQueueIdLSB(queue.getId().getId().getLeastSignificantBits())
                                .setQueueName(queue.getName())
                                .setQueueTopic(queue.getTopic())
                                .setPartitions(queue.getPartitions())
                                .setDuplicateMsgToAllPartitions(queue.isDuplicateMsgToAllPartitions())
                                .build()).toList()).build());
    }

    private ListenableFuture<IntegrationApiResponseMsg> handleGetAllProfilesRequest(GetAllProfilesRequestProto request) {
        UUID fromId = new UUID(request.getIdMSB(), request.getIdLSB());
        int batchSize = request.getBatchSize();
        GetAllProfilesResponseProto.Builder responseBuilder = GetAllProfilesResponseProto.newBuilder();

        switch (request.getEntityType()) {
            case DEVICE_PROFILE -> {
                var deviceProfiles = deviceProfileService.findDeviceProfileCacheInfos(fromId, batchSize);
                deviceProfiles.forEach(profile -> responseBuilder.addDeviceProfiles(ProtoUtils.toCacheProto(profile)));
            }
            case ASSET_PROFILE -> {
                var assetProfiles = assetProfileService.findAssetProfileCacheInfos(fromId, batchSize);
                assetProfiles.forEach(profile -> responseBuilder.addAssetProfiles(ProtoUtils.toCacheProto(profile)));
            }
            default -> throw new UnsupportedOperationException("Unsupported entity type for profile request: " + request.getEntityType());
        }

        return Futures.immediateFuture(IntegrationApiResponseMsg.newBuilder()
                .setGetAllProfilesResponse(responseBuilder.build())
                .build());
    }

    private ListenableFuture<IntegrationApiResponseMsg> handleGetEntitiesByRelationRequest(GetEntitiesByRelationRequestProto request) {
        UUID fromId = new UUID(request.getIdMSB(), request.getIdLSB());
        int batchSize = request.getBatchSize();
        GetEntitiesByRelationResponseProto.Builder responseBuilder = GetEntitiesByRelationResponseProto.newBuilder();

        switch (request.getEntityType()) {
            case DEVICE -> {
                var devices = deviceService.findDeviceCacheInfosByRelationType(request.getRelationType(), fromId, batchSize);
                devices.forEach(device -> responseBuilder.addDevices(ProtoUtils.toCacheProto(device)));
            }
            case ASSET -> {
                var assets = assetService.findAssetCacheInfosByRelationType(request.getRelationType(), fromId, batchSize);
                assets.forEach(asset -> responseBuilder.addAssets(ProtoUtils.toCacheProto(asset)));
            }
            default -> throw new UnsupportedOperationException("Unsupported entity type for relation request: " + request.getEntityType());
        }

        return Futures.immediateFuture(IntegrationApiResponseMsg.newBuilder()
                .setGetEntitiesByRelationResponse(responseBuilder.build())
                .build());
    }

    // Error details are propagated via ERROR_MESSAGE_HEADER in queue message headers
    // by DefaultTbQueueResponseTemplate.sendErrorResponse(), not in the response body.
    @Override
    public TbProtoQueueMsg<IntegrationApiResponseMsg> constructErrorResponseMsg(TbProtoQueueMsg<IntegrationApiRequestMsg> request, Throwable e) {
        return new TbProtoQueueMsg<>(request.getKey(), IntegrationApiResponseMsg.newBuilder().build(), request.getHeaders());
    }

}
