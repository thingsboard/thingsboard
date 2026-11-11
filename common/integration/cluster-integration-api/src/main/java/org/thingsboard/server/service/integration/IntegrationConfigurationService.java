// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
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
import org.thingsboard.server.gen.integration.IntegrationInfoProto;
import org.thingsboard.server.service.data.EntityUplinkData;

import java.util.List;
import java.util.UUID;

public interface IntegrationConfigurationService {

    List<Integration> getActiveIntegrationList(IntegrationType type, boolean remote);

    Integration getIntegration(TenantId tenantId, IntegrationId integrationId);

    Integration getIntegration(TenantId tenantId, String routingKey);

    Converter getConverter(TenantId tenantId, ConverterId converterId);

    TenantProfile getTenantProfile(TenantId tenantId);

    // Used only for Caches that marked @TbIntegrationExecutorComponent
    default ListenableFuture<DeviceProfileCacheInfo> getDeviceProfile(TenantId tenantId, String name) {
        return Futures.immediateFuture(null);
    }

    default ListenableFuture<AssetProfileCacheInfo> getAssetProfile(TenantId tenantId, String name) {
        return Futures.immediateFuture(null);
    }

    default ListenableFuture<DeviceCacheInfo> getDevice(TenantId tenantId, IntegrationInfoProto integration, EntityUplinkData data) {
        return Futures.immediateFuture(null);
    }

    default ListenableFuture<AssetCacheInfo> getAsset(TenantId tenantId, IntegrationInfoProto integration, EntityUplinkData data) {
        return Futures.immediateFuture(null);
    }

    // Used for startup preloading - returns lightweight cache info objects in batches
    default List<DeviceProfileCacheInfo> getAllDeviceProfileCacheInfos(UUID lastId, int batchSize) {
        return List.of();
    }

    default List<AssetProfileCacheInfo> getAllAssetProfileCacheInfos(UUID lastId, int batchSize) {
        return List.of();
    }

    default List<DeviceCacheInfo> getDeviceCacheInfosByRelation(UUID lastId, int batchSize) {
        return List.of();
    }

    default List<AssetCacheInfo> getAssetCacheInfosByRelation(UUID lastId, int batchSize) {
        return List.of();
    }

}
