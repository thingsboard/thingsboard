// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.service.context;

import io.netty.channel.EventLoopGroup;
import org.thingsboard.integration.api.IntegrationRateLimitService;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.integration.downlink.DownlinkCacheService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

public interface TbIntegrationExecutorContextComponent {

    /*
        We assume that the device is already cached during processing of the uplink.
     */
    Device findCachedDeviceByTenantIdAndName(TenantId tenantId, String deviceName);

    DownlinkCacheService getDownlinkCacheService();

    EventLoopGroup getEventLoopGroup();

    ScheduledExecutorService getScheduledExecutorService();

    ExecutorService getCallBackExecutorService();

    ExecutorService getGeneralExecutorService();

    IntegrationRateLimitService getRateLimitService();
}
