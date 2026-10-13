// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.service.api;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.AssetCacheInfo;
import org.thingsboard.server.common.data.AssetProfileCacheInfo;
import org.thingsboard.server.common.data.DeviceCacheInfo;
import org.thingsboard.server.common.data.DeviceProfileCacheInfo;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.gen.integration.ConverterRequestProto;
import org.thingsboard.server.gen.integration.EntityRequestProto;
import org.thingsboard.server.gen.integration.GetAllProfilesRequestProto;
import org.thingsboard.server.gen.integration.GetEntitiesByRelationRequestProto;
import org.thingsboard.server.gen.integration.IntegrationApiRequestMsg;
import org.thingsboard.server.gen.integration.IntegrationApiResponseMsg;
import org.thingsboard.server.gen.integration.IntegrationInfoListRequestProto;
import org.thingsboard.server.gen.integration.IntegrationInfoProto;
import org.thingsboard.server.gen.integration.IntegrationRequestProto;
import org.thingsboard.server.gen.integration.ProfileRequestProto;
import org.thingsboard.server.gen.integration.TenantProfileRequestProto;
import org.thingsboard.server.gen.transport.TransportProtos.EntityTypeProto;
import org.thingsboard.server.queue.TbQueueRequestTemplate;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.service.data.EntityUplinkData;
import org.thingsboard.server.service.integration.IntegrationConfigurationService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RequiredArgsConstructor
@Service
@Slf4j
public class TbIntegrationExecutorIntegrationConfigurationService implements IntegrationConfigurationService {

    private final TbQueueRequestTemplate<TbProtoQueueMsg<IntegrationApiRequestMsg>, TbProtoQueueMsg<IntegrationApiResponseMsg>> apiTemplate;
    private final ExecutorService callbackExecutor = ThingsBoardExecutors.newWorkStealingPool(4, "integration-api-callback");

    @PostConstruct
    public void init() {
        apiTemplate.init();
    }

    @PreDestroy
    public void destroy() {
        if (apiTemplate != null) {
            apiTemplate.stop();
        }
        callbackExecutor.shutdownNow();
    }

    @Override
    public List<Integration> getActiveIntegrationList(IntegrationType type, boolean remote) {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                var request = IntegrationInfoListRequestProto.newBuilder().setEnabled(true).setRemote(remote).setType(type.name()).build();
                var response =
                        apiTemplate.send(new TbProtoQueueMsg<>(UUID.randomUUID(), IntegrationApiRequestMsg.newBuilder().setIntegrationListRequest(request).build()));
                return Futures.transform(response, this::parseListFromProto, callbackExecutor).get(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while fetching integration list", e);
            } catch (Exception e) {
                log.warn("Failed to receive the list of integrations. Going to retry.", e);
            }
        }
        throw new RuntimeException("Interrupted while fetching integration list");
    }

    @Override
    public Integration getIntegration(TenantId tenantId, IntegrationId integrationId) {
        var request = IntegrationRequestProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setIntegrationIdMSB(integrationId.getId().getMostSignificantBits())
                .setIntegrationIdLSB(integrationId.getId().getLeastSignificantBits())
                .build();
        var response =
                apiTemplate.send(new TbProtoQueueMsg<>(UUID.randomUUID(), IntegrationApiRequestMsg.newBuilder().setIntegrationRequest(request).build()));
        return getBlocking(Futures.transform(response, this::parseIntegrationFromProto, callbackExecutor));
    }

    @Override
    public Integration getIntegration(TenantId tenantId, String routingKey) {
        var request = IntegrationRequestProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setRoutingKey(routingKey)
                .build();
        var response =
                apiTemplate.send(new TbProtoQueueMsg<>(UUID.randomUUID(), IntegrationApiRequestMsg.newBuilder().setIntegrationRequest(request).build()));
        return getBlocking(Futures.transform(response, this::parseIntegrationFromProto, callbackExecutor));
    }

    @Override
    public Converter getConverter(TenantId tenantId, ConverterId converterId) {
        var request = ConverterRequestProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setConverterIdMSB(converterId.getId().getMostSignificantBits())
                .setConverterIdLSB(converterId.getId().getLeastSignificantBits())
                .build();
        var response =
                apiTemplate.send(new TbProtoQueueMsg<>(UUID.randomUUID(), IntegrationApiRequestMsg.newBuilder().setConverterRequest(request).build()));
        return getBlocking(Futures.transform(response, this::parseConverterFromProto, callbackExecutor));
    }

    @Override
    public TenantProfile getTenantProfile(TenantId tenantId) {
        var response = apiTemplate.send(new TbProtoQueueMsg<>(UUID.randomUUID(), IntegrationApiRequestMsg.newBuilder()
                .setTenantProfileRequest(TenantProfileRequestProto.newBuilder()
                        .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                        .setTenantIdLSB(tenantId.getId().getLeastSignificantBits()))
                .build()));
        return getBlocking(Futures.transform(response, msg -> ProtoUtils.fromProto(msg.getValue().getTenantProfileResponse()), callbackExecutor));
    }

    @Override
    public ListenableFuture<DeviceProfileCacheInfo> getDeviceProfile(TenantId tenantId, String name) {
        var response = apiTemplate.send(new TbProtoQueueMsg<>(UUID.randomUUID(), IntegrationApiRequestMsg.newBuilder()
                .setProfileRequest(ProfileRequestProto.newBuilder()
                        .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                        .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                        .setName(name)
                        .setEntityType(EntityTypeProto.DEVICE_PROFILE))
                .build()));
        return Futures.transform(response, msg -> ProtoUtils.fromCacheProto(msg.getValue().getDeviceProfileResponse()), callbackExecutor);
    }

    @Override
    public ListenableFuture<AssetProfileCacheInfo> getAssetProfile(TenantId tenantId, String name) {
        var response = apiTemplate.send(new TbProtoQueueMsg<>(UUID.randomUUID(), IntegrationApiRequestMsg.newBuilder()
                .setProfileRequest(ProfileRequestProto.newBuilder()
                        .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                        .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                        .setName(name)
                        .setEntityType(EntityTypeProto.ASSET_PROFILE))
                .build()));
        return Futures.transform(response, msg -> ProtoUtils.fromCacheProto(msg.getValue().getAssetProfileResponse()), callbackExecutor);
    }

    @Override
    public ListenableFuture<DeviceCacheInfo> getDevice(TenantId tenantId, IntegrationInfoProto integration, EntityUplinkData data) {
        var response = apiTemplate.send(new TbProtoQueueMsg<>(UUID.randomUUID(), IntegrationApiRequestMsg.newBuilder()
                .setEntityRequest(EntityRequestProto.newBuilder()
                        .setName(data.getName())
                        .setType(data.getType())
                        .setLabel(data.getLabel())
                        .setCustomerName(data.getCustomerName())
                        .setGroupName(data.getGroupName())
                        .setEntityType(EntityTypeProto.DEVICE)
                        .setIntegration(integration))
                .build()));
        return Futures.transform(response, msg -> ProtoUtils.fromCacheProto(msg.getValue().getDeviceResponse()), callbackExecutor);
    }

    @Override
    public ListenableFuture<AssetCacheInfo> getAsset(TenantId tenantId, IntegrationInfoProto integration, EntityUplinkData data) {
        var response = apiTemplate.send(new TbProtoQueueMsg<>(UUID.randomUUID(), IntegrationApiRequestMsg.newBuilder()
                .setEntityRequest(EntityRequestProto.newBuilder()
                        .setName(data.getName())
                        .setType(data.getType())
                        .setLabel(data.getLabel())
                        .setCustomerName(data.getCustomerName())
                        .setGroupName(data.getGroupName())
                        .setEntityType(EntityTypeProto.ASSET)
                        .setIntegration(integration))
                .build()));
        return Futures.transform(response, msg -> ProtoUtils.fromCacheProto(msg.getValue().getAssetResponse()), callbackExecutor);
    }

    private List<Integration> parseListFromProto(TbProtoQueueMsg<IntegrationApiResponseMsg> proto) {
        var result = new ArrayList<Integration>();

        var response = proto.getValue().getIntegrationListResponse().getIntegrationInfoListList();

        for (var integrationInfoProto : response) {
            result.add(ProtoUtils.fromProtoToIntegration(integrationInfoProto));
        }

        return result;
    }

    private Integration parseIntegrationFromProto(TbProtoQueueMsg<IntegrationApiResponseMsg> proto) {
        var responseProto = proto.getValue();
        if (responseProto.hasIntegrationResponse()) {
            return ProtoUtils.fromProto(responseProto.getIntegrationResponse());
        }
        return null;
    }

    private Converter parseConverterFromProto(TbProtoQueueMsg<IntegrationApiResponseMsg> proto) {
        var responseProto = proto.getValue();
        return ProtoUtils.fromProto(responseProto.getConverterResponse());
    }

    @Override
    public List<DeviceProfileCacheInfo> getAllDeviceProfileCacheInfos(UUID lastId, int batchSize) {
        UUID fromId = lastId != null ? lastId : UUID.fromString("00000000-0000-0000-0000-000000000000");
        var request = GetAllProfilesRequestProto.newBuilder()
                .setEntityType(EntityTypeProto.DEVICE_PROFILE)
                .setIdMSB(fromId.getMostSignificantBits())
                .setIdLSB(fromId.getLeastSignificantBits())
                .setBatchSize(batchSize)
                .build();

        var response = apiTemplate.send(new TbProtoQueueMsg<>(UUID.randomUUID(), IntegrationApiRequestMsg.newBuilder()
                .setGetAllProfilesRequest(request)
                .build()));
        return getBlocking(Futures.transform(response, this::parseDeviceProfileListFromProto, callbackExecutor));
    }

    @Override
    public List<AssetProfileCacheInfo> getAllAssetProfileCacheInfos(UUID lastId, int batchSize) {
        UUID fromId = lastId != null ? lastId : UUID.fromString("00000000-0000-0000-0000-000000000000");
        var request = GetAllProfilesRequestProto.newBuilder()
                .setEntityType(EntityTypeProto.ASSET_PROFILE)
                .setIdMSB(fromId.getMostSignificantBits())
                .setIdLSB(fromId.getLeastSignificantBits())
                .setBatchSize(batchSize)
                .build();

        var response = apiTemplate.send(new TbProtoQueueMsg<>(UUID.randomUUID(), IntegrationApiRequestMsg.newBuilder()
                .setGetAllProfilesRequest(request)
                .build()));
        return getBlocking(Futures.transform(response, this::parseAssetProfileListFromProto, callbackExecutor));
    }

    @Override
    public List<DeviceCacheInfo> getDeviceCacheInfosByRelation(UUID lastId, int batchSize) {
        UUID fromId = lastId != null ? lastId : UUID.fromString("00000000-0000-0000-0000-000000000000");
        var request = GetEntitiesByRelationRequestProto.newBuilder()
                .setEntityType(EntityTypeProto.DEVICE)
                .setRelationType(EntityRelation.INTEGRATION_TYPE)
                .setIdMSB(fromId.getMostSignificantBits())
                .setIdLSB(fromId.getLeastSignificantBits())
                .setBatchSize(batchSize)
                .build();

        var response = apiTemplate.send(new TbProtoQueueMsg<>(UUID.randomUUID(), IntegrationApiRequestMsg.newBuilder()
                .setGetEntitiesByRelationRequest(request)
                .build()));
        return getBlocking(Futures.transform(response, this::parseDeviceListFromProto, callbackExecutor));
    }

    @Override
    public List<AssetCacheInfo> getAssetCacheInfosByRelation(UUID lastId, int batchSize) {
        UUID fromId = lastId != null ? lastId : UUID.fromString("00000000-0000-0000-0000-000000000000");
        var request = GetEntitiesByRelationRequestProto.newBuilder()
                .setEntityType(EntityTypeProto.ASSET)
                .setRelationType(EntityRelation.INTEGRATION_TYPE)
                .setIdMSB(fromId.getMostSignificantBits())
                .setIdLSB(fromId.getLeastSignificantBits())
                .setBatchSize(batchSize)
                .build();

        var response = apiTemplate.send(new TbProtoQueueMsg<>(UUID.randomUUID(), IntegrationApiRequestMsg.newBuilder()
                .setGetEntitiesByRelationRequest(request)
                .build()));
        return getBlocking(Futures.transform(response, this::parseAssetListFromProto, callbackExecutor));
    }

    private List<DeviceProfileCacheInfo> parseDeviceProfileListFromProto(TbProtoQueueMsg<IntegrationApiResponseMsg> proto) {
        var result = new ArrayList<DeviceProfileCacheInfo>();
        var responseProto = proto.getValue().getGetAllProfilesResponse();
        for (var cacheInfoProto : responseProto.getDeviceProfilesList()) {
            result.add(ProtoUtils.fromCacheProto(cacheInfoProto));
        }
        return result;
    }

    private List<AssetProfileCacheInfo> parseAssetProfileListFromProto(TbProtoQueueMsg<IntegrationApiResponseMsg> proto) {
        var result = new ArrayList<AssetProfileCacheInfo>();
        var responseProto = proto.getValue().getGetAllProfilesResponse();
        for (var cacheInfoProto : responseProto.getAssetProfilesList()) {
            result.add(ProtoUtils.fromCacheProto(cacheInfoProto));
        }
        return result;
    }

    private List<DeviceCacheInfo> parseDeviceListFromProto(TbProtoQueueMsg<IntegrationApiResponseMsg> proto) {
        var result = new ArrayList<DeviceCacheInfo>();
        var responseProto = proto.getValue().getGetEntitiesByRelationResponse();
        for (var cacheInfoProto : responseProto.getDevicesList()) {
            result.add(ProtoUtils.fromCacheProto(cacheInfoProto));
        }
        return result;
    }

    private List<AssetCacheInfo> parseAssetListFromProto(TbProtoQueueMsg<IntegrationApiResponseMsg> proto) {
        var result = new ArrayList<AssetCacheInfo>();
        var responseProto = proto.getValue().getGetEntitiesByRelationResponse();
        for (var cacheInfoProto : responseProto.getAssetsList()) {
            result.add(ProtoUtils.fromCacheProto(cacheInfoProto));
        }
        return result;
    }

    private <T> T getBlocking(ListenableFuture<T> future) {
        try {
            return future.get(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

}
