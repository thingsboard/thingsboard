// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.install.update;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.tenant.TenantProfileService;

@Component
@Profile("install")
@RequiredArgsConstructor
public class IntegrationRateLimitsUpdater extends PaginatedUpdater<String, TenantProfile> {

    private final TenantProfileService tenantProfileService;

    @Value("#{ environment.getProperty('TB_INTEGRATION_RATE_LIMITS_ENABLED') ?: environment.getProperty('integrations.rate_limits.enabled') ?: 'false' }")
    protected boolean rateLimitsEnabled;
    @Value("#{ environment.getProperty('TB_INTEGRATION_RATE_LIMITS_TENANT') ?: environment.getProperty('integrations.rate_limits.tenant') ?: '1000:1,20000:60' }")
    protected String msgsPerTenantRateLimit;
    @Value("#{ environment.getProperty('TB_INTEGRATION_RATE_LIMITS_DEVICE') ?: environment.getProperty('integrations.rate_limits.device') ?: '10:1,300:60' }")
    protected String msgsPerDeviceRateLimit;

    @Override
    protected PageData<TenantProfile> findEntities(String id, PageLink pageLink) {
        if (!rateLimitsEnabled) {
            return PageData.emptyPageData();
        }
        return tenantProfileService.findTenantProfiles(TenantId.SYS_TENANT_ID, pageLink);
    }

    @Override
    protected void updateEntity(TenantProfile tenantProfile) {
        DefaultTenantProfileConfiguration profileConfiguration = tenantProfile.getDefaultProfileConfiguration();
        if (StringUtils.isNotEmpty(msgsPerTenantRateLimit)) {
            profileConfiguration.setIntegrationMsgsPerTenantRateLimit(msgsPerTenantRateLimit);
        }
        if (StringUtils.isNotEmpty(msgsPerDeviceRateLimit)) {
            profileConfiguration.setIntegrationMsgsPerDeviceRateLimit(msgsPerDeviceRateLimit);
        }
        tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile);
    }

    @Override
    protected String getName() {
        return "Integration rate limits updater";
    }

    @Override
    protected boolean forceReportTotal() {
        return true;
    }

}
