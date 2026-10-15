// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.service.context;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.integration.api.IntegrationRateLimitService;
import org.thingsboard.integration.api.IntegrationStatisticsService;
import org.thingsboard.server.cache.TbCacheValueWrapper;
import org.thingsboard.server.cache.VersionedTbCache;
import org.thingsboard.server.cache.device.DeviceCacheKey;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.integration.downlink.DownlinkCacheService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Data
@RequiredArgsConstructor
public class DefaultTbIntegrationExecutorContextComponent implements TbIntegrationExecutorContextComponent {

    private final DownlinkCacheService downlinkCacheService;
    private final VersionedTbCache<DeviceCacheKey, Device> deviceCache;
    private final IntegrationStatisticsService integrationStatisticsService;
    private final IntegrationRateLimitService rateLimitService;
    private EventLoopGroup eventLoopGroup;
    private ScheduledExecutorService scheduledExecutorService;
    private ExecutorService generalExecutorService;
    private ExecutorService callBackExecutorService;

    @PostConstruct
    public void init() {
        eventLoopGroup = new NioEventLoopGroup();
        scheduledExecutorService = ThingsBoardExecutors.newScheduledThreadPool(3, "integration-scheduled");
        generalExecutorService = ThingsBoardExecutors.newWorkStealingPool(20, "integration-general");
        callBackExecutorService = ThingsBoardExecutors.newWorkStealingPool(Math.max(2, Runtime.getRuntime().availableProcessors()), "integration-callback");
    }

    @PreDestroy
    public void destroy() {
        eventLoopGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdownNow();
        }
        if (generalExecutorService != null) {
            generalExecutorService.shutdownNow();
        }
        if (callBackExecutorService != null) {
            callBackExecutorService.shutdownNow();
        }
    }

    @Override
    public Device findCachedDeviceByTenantIdAndName(TenantId tenantId, String deviceName) {
        TbCacheValueWrapper<Device> cacheValue = deviceCache.get(new DeviceCacheKey(tenantId, deviceName));
        return cacheValue == null ? null : cacheValue.get();
    }

}
