/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.tenant.TenantProfileService;

@Component
class RateLimitsUpdater extends PaginatedUpdater<String, TenantProfile> {

    @Value("#{ environment.getProperty('TB_SERVER_REST_LIMITS_TENANT_ENABLED') ?: environment.getProperty('server.rest.limits.tenant.enabled') ?: 'false' }")
    boolean tenantServerRestLimitsEnabled;
    @Value("#{ environment.getProperty('TB_SERVER_REST_LIMITS_TENANT_CONFIGURATION') ?: environment.getProperty('server.rest.limits.tenant.configuration') ?: '100:1,2000:60' }")
    String tenantServerRestLimitsConfiguration;
    @Value("#{ environment.getProperty('TB_SERVER_REST_LIMITS_CUSTOMER_ENABLED') ?: environment.getProperty('server.rest.limits.customer.enabled') ?: 'false' }")
    boolean customerServerRestLimitsEnabled;
    @Value("#{ environment.getProperty('TB_SERVER_REST_LIMITS_CUSTOMER_CONFIGURATION') ?: environment.getProperty('server.rest.limits.customer.configuration') ?: '50:1,1000:60' }")
    String customerServerRestLimitsConfiguration;

    @Value("#{ environment.getProperty('TB_SERVER_WS_TENANT_RATE_LIMITS_MAX_SESSIONS_PER_TENANT') ?: environment.getProperty('server.ws.limits.max_sessions_per_tenant') ?: '0' }")
    private int maxWsSessionsPerTenant;
    @Value("#{ environment.getProperty('TB_SERVER_WS_TENANT_RATE_LIMITS_MAX_SESSIONS_PER_CUSTOMER') ?: environment.getProperty('server.ws.limits.max_sessions_per_customer') ?: '0' }")
    private int maxWsSessionsPerCustomer;
    @Value("#{ environment.getProperty('TB_SERVER_WS_TENANT_RATE_LIMITS_MAX_SESSIONS_PER_REGULAR_USER') ?: environment.getProperty('server.ws.limits.max_sessions_per_regular_user') ?: '0' }")
    private int maxWsSessionsPerRegularUser;
    @Value("#{ environment.getProperty('TB_SERVER_WS_TENANT_RATE_LIMITS_MAX_SESSIONS_PER_PUBLIC_USER') ?: environment.getProperty('server.ws.limits.max_sessions_per_public_user') ?: '0' }")
    private int maxWsSessionsPerPublicUser;
    @Value("#{ environment.getProperty('TB_SERVER_WS_TENANT_RATE_LIMITS_MAX_QUEUE_PER_WS_SESSION') ?: environment.getProperty('server.ws.limits.max_queue_per_ws_session') ?: '500' }")
    private int wsMsgQueueLimitPerSession;
    @Value("#{ environment.getProperty('TB_SERVER_WS_TENANT_RATE_LIMITS_MAX_SUBSCRIPTIONS_PER_TENANT') ?: environment.getProperty('server.ws.limits.max_subscriptions_per_tenant') ?: '0' }")
    private long maxWsSubscriptionsPerTenant;
    @Value("#{ environment.getProperty('TB_SERVER_WS_TENANT_RATE_LIMITS_MAX_SUBSCRIPTIONS_PER_CUSTOMER') ?: environment.getProperty('server.ws.limits.max_subscriptions_per_customer') ?: '0' }")
    private long maxWsSubscriptionsPerCustomer;
    @Value("#{ environment.getProperty('TB_SERVER_WS_TENANT_RATE_LIMITS_MAX_SUBSCRIPTIONS_PER_REGULAR_USER') ?: environment.getProperty('server.ws.limits.max_subscriptions_per_regular_user') ?: '0' }")
    private long maxWsSubscriptionsPerRegularUser;
    @Value("#{ environment.getProperty('TB_SERVER_WS_TENANT_RATE_LIMITS_MAX_SUBSCRIPTIONS_PER_PUBLIC_USER') ?: environment.getProperty('server.ws.limits.max_subscriptions_per_public_user') ?: '0' }")
    private long maxWsSubscriptionsPerPublicUser;
    @Value("#{ environment.getProperty('TB_SERVER_WS_TENANT_RATE_LIMITS_MAX_UPDATES_PER_SESSION') ?: environment.getProperty('server.ws.limits.max_updates_per_session') ?: '300:1,3000:60' }")
    private String wsUpdatesPerSessionRateLimit;

    @Value("#{ environment.getProperty('CASSANDRA_QUERY_TENANT_RATE_LIMITS_ENABLED') ?: environment.getProperty('cassandra.query.tenant_rate_limits.enabled') ?: 'false' }")
    private boolean cassandraQueryTenantRateLimitsEnabled;
    @Value("#{ environment.getProperty('CASSANDRA_QUERY_TENANT_RATE_LIMITS_CONFIGURATION') ?: environment.getProperty('cassandra.query.tenant_rate_limits.configuration') ?: '1000:1,30000:60' }")
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
        var profileConfiguration = tenantProfile.getDefaultProfileConfiguration();

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
