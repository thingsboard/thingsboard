// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.cache;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.AssetCacheInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.integration.IntegrationInfoProto;
import org.thingsboard.server.queue.util.TbIntegrationExecutorComponent;
import org.thingsboard.server.service.data.EntityUplinkData;
import org.thingsboard.server.service.integration.IntegrationConfigurationService;

@Slf4j
@Service
@RequiredArgsConstructor
@TbIntegrationExecutorComponent
public class DefaultIntegrationExecutorAssetCache extends AbstractIntegrationExecutorCache<AssetCacheInfo> implements IntegrationExecutorAssetCache {

    private final IntegrationConfigurationService integrationConfigurationService;

    @Override
    protected ListenableFuture<AssetCacheInfo> fetchEntity(TenantId tenantId, IntegrationInfoProto proto, EntityUplinkData data) {
        return integrationConfigurationService.getAsset(tenantId, proto, data);
    }

}
