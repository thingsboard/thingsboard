// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.cache;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DeviceProfileCacheInfo;
import org.thingsboard.server.queue.util.TbIntegrationExecutorComponent;
import org.thingsboard.server.service.data.EntityCacheKey;
import org.thingsboard.server.service.integration.IntegrationConfigurationService;

@Slf4j
@Service
@RequiredArgsConstructor
@TbIntegrationExecutorComponent
public class DefaultIntegrationExecutorDeviceProfileCache extends AbstractIntegrationExecutorCache<DeviceProfileCacheInfo> implements IntegrationExecutorDeviceProfileCache {

    private final IntegrationConfigurationService integrationConfigurationService;

    @Override
    protected ListenableFuture<DeviceProfileCacheInfo> fetchProfile(EntityCacheKey key) {
        return integrationConfigurationService.getDeviceProfile(key.tenantId(), key.name());
    }

    @Override
    protected boolean isProfileCache() {
        return true;
    }

}
