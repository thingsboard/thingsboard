/**
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.install.update;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.tenant.TenantProfileService;

@Component
class RateLimitsUpdater extends PaginatedUpdater<String, TenantProfile> {

    @Value("${server.rest.limits.tenant.enabled}")
    boolean tenantServerRestLimitsEnabled;
    @Value("${server.rest.limits.tenant.configuration}")
    String tenantServerRestLimitsConfiguration;
    @Value("${server.rest.limits.customer.enabled}")
    boolean customerServerRestLimitsEnabled;
    @Value("${server.rest.limits.customer.configuration}")
    String customerServerRestLimitsConfiguration;

    @Value("${server.ws.limits.max_sessions_per_tenant}")
    private int maxWsSessionsPerTenant;
    @Value("${server.ws.limits.max_sessions_per_customer}")
    private int maxWsSessionsPerCustomer;
    @Value("${server.ws.limits.max_sessions_per_regular_user}")
    private int maxWsSessionsPerRegularUser;
    @Value("${server.ws.limits.max_sessions_per_public_user}")
    private int maxWsSessionsPerPublicUser;
    @Value("${server.ws.limits.max_queue_per_ws_session}")
    private int wsMsgQueueLimitPerSession;
    @Value("${server.ws.limits.max_subscriptions_per_tenant}")
    private long maxWsSubscriptionsPerTenant;
    @Value("${server.ws.limits.max_subscriptions_per_customer}")
    private long maxWsSubscriptionsPerCustomer;
    @Value("${server.ws.limits.max_subscriptions_per_regular_user}")
    private long maxWsSubscriptionsPerRegularUser;
    @Value("${server.ws.limits.max_subscriptions_per_public_user}")
    private long maxWsSubscriptionsPerPublicUser;
    @Value("${server.ws.limits.max_updates_per_session}")
    private String wsUpdatesPerSessionRateLimit;

    @Value("${cassandra.query.tenant_rate_limits.enabled}")
    private boolean cassandraQueryTenantRateLimitsEnabled;
    @Value("${cassandra.query.tenant_rate_limits.configuration}")
    private String cassandraQueryTenantRateLimitsConfiguration;

    @Autowired
    private TenantProfileService tenantProfileService;

    @Override
    protected boolean forceReportTotal() {
        return true;
    }

    @Override
    protected String getName() {
        return "Rate limits updater";
    }

    @Override
    protected PageData<TenantProfile> findEntities(String id, PageLink pageLink) {
        return tenantProfileService.findTenantProfiles(TenantId.SYS_TENANT_ID, pageLink);
    }

    @Override
    protected void updateEntity(TenantProfile tenantProfile) {
        var profileConfiguration = tenantProfile.getDefaultTenantProfileConfiguration();

        if (tenantServerRestLimitsEnabled && StringUtils.isNotEmpty(tenantServerRestLimitsConfiguration)) {
            profileConfiguration.setTenantServerRestLimitsConfiguration(tenantServerRestLimitsConfiguration);
        }
        if (customerServerRestLimitsEnabled && StringUtils.isNotEmpty(customerServerRestLimitsConfiguration)) {
            profileConfiguration.setCustomerServerRestLimitsConfiguration(customerServerRestLimitsConfiguration);
        }

        profileConfiguration.setMaxWsSessionsPerTenant(maxWsSessionsPerTenant);
        profileConfiguration.setMaxWsSessionsPerCustomer(maxWsSessionsPerCustomer);
        profileConfiguration.setMaxWsSessionsPerPublicUser(maxWsSessionsPerPublicUser);
        profileConfiguration.setMaxWsSessionsPerRegularUser(maxWsSessionsPerRegularUser);
        profileConfiguration.setMaxWsSubscriptionsPerTenant(maxWsSubscriptionsPerTenant);
        profileConfiguration.setMaxWsSubscriptionsPerCustomer(maxWsSubscriptionsPerCustomer);
        profileConfiguration.setMaxWsSubscriptionsPerPublicUser(maxWsSubscriptionsPerPublicUser);
        profileConfiguration.setMaxWsSubscriptionsPerRegularUser(maxWsSubscriptionsPerRegularUser);
        profileConfiguration.setWsMsgQueueLimitPerSession(wsMsgQueueLimitPerSession);
        if (StringUtils.isNotEmpty(wsUpdatesPerSessionRateLimit)) {
            profileConfiguration.setWsUpdatesPerSessionRateLimit(wsUpdatesPerSessionRateLimit);
        }

        if (cassandraQueryTenantRateLimitsEnabled && StringUtils.isNotEmpty(cassandraQueryTenantRateLimitsConfiguration)) {
            profileConfiguration.setCassandraQueryTenantRateLimitsConfiguration(cassandraQueryTenantRateLimitsConfiguration);
        }

        tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile);
    }

}
