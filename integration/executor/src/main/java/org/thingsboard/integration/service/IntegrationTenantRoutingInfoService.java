// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.queue.discovery.TenantRoutingInfo;
import org.thingsboard.server.queue.discovery.TenantRoutingInfoService;
import org.thingsboard.server.service.cache.IntegrationExecutorTenantProfileCache;

@Slf4j
@Service
public class IntegrationTenantRoutingInfoService implements TenantRoutingInfoService {

    private final IntegrationExecutorTenantProfileCache tenantProfileCache;

    public IntegrationTenantRoutingInfoService(@Lazy IntegrationExecutorTenantProfileCache tenantProfileCache) {
        this.tenantProfileCache = tenantProfileCache;
    }

    @Override
    public TenantRoutingInfo getRoutingInfo(TenantId tenantId) {
        TenantProfile profile = tenantProfileCache.get(tenantId);
        if (profile == null) {
            log.warn("Tenant profile not found for tenant [{}], using default routing info", tenantId);
            return new TenantRoutingInfo(tenantId, null, false);
        }
        return new TenantRoutingInfo(tenantId, profile.getId(), profile.isIsolatedTbRuleEngine());
    }

}
