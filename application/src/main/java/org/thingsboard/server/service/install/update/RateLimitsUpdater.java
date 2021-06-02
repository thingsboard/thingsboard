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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.tenant.TenantProfileService;

@Component
class RateLimitsUpdater extends PaginatedUpdater<String, TenantProfile> {
    @Value("${server.rest.limits.tenant.enabled}")
    boolean tenantServerRateLimitsEnabled;
    @Value("${server.rest.limits.customer.enabled}")
    boolean customerServerRateLimitsEnabled;
    @Value("${server.rest.limits.tenant.configuration}")
    String tenantServerRateLimitsConfiguration;
    @Value("${server.rest.limits.customer.configuration}")
    String customerServerRateLimitsConfiguration;

    @Value("${server.ws.limits.max_sessions_per_tenant}")
    private int wsLimitMaxSessionsPerTenant;
    @Value("${server.ws.limits.max_sessions_per_customer}")
    private int wsLimitMaxSessionsPerCustomer;
    @Value("${server.ws.limits.max_sessions_per_regular_user}")
    private int wsLimitMaxSessionsPerRegularUser;
    @Value("${server.ws.limits.max_sessions_per_public_user}")
    private int wsLimitMaxSessionsPerPublicUser;
    @Value("${server.ws.limits.max_queue_per_ws_session}")
    private int wsLimitQueuePerWsSession;
    @Value("${server.ws.limits.max_subscriptions_per_tenant}")
    private long wsLimitMaxSubscriptionsPerTenant;
    @Value("${server.ws.limits.max_subscriptions_per_customer}")
    private long wsLimitMaxSubscriptionsPerCustomer;
    @Value("${server.ws.limits.max_subscriptions_per_regular_user}")
    private long wsLimitMaxSubscriptionsPerRegularUser;
    @Value("${server.ws.limits.max_subscriptions_per_public_user}")
    private long wsLimitMaxSubscriptionsPerPublicUser;
    @Value("${server.ws.limits.max_updates_per_session}")
    private String wsLimitUpdatesPerSession;

    @Value("${cassandra.query.tenant_rate_limits.enabled}")
    private boolean cassandraLimitsIsEnabled;
    @Value("${cassandra.query.tenant_rate_limits.configuration}")
    private String cassandraTenantLimitsConfiguration;
    @Value("${cassandra.query.tenant_rate_limits.print_tenant_names}")
    private boolean printTenantNames;

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
        return tenantProfileService.findTenantProfiles(null, pageLink);
    }

    @Override
    protected void updateEntity(TenantProfile entity) {
        var profileConfiguration = entity.getDefaultTenantProfileConfiguration();
        if (profileConfiguration != null) {
            if (tenantServerRateLimitsEnabled && StringUtils.isNotEmpty(tenantServerRateLimitsConfiguration)) {
                profileConfiguration.setRateLimitsTenantConfiguration(tenantServerRateLimitsConfiguration);
            }
            if (customerServerRateLimitsEnabled && StringUtils.isNotEmpty(customerServerRateLimitsConfiguration)) {
                profileConfiguration.setRateLimitsCustomerConfiguration(customerServerRateLimitsConfiguration);
            }

            profileConfiguration.setWsLimitMaxSessionsPerTenant(wsLimitMaxSessionsPerTenant);
            profileConfiguration.setWsLimitMaxSessionsPerCustomer(wsLimitMaxSessionsPerCustomer);
            profileConfiguration.setWsLimitMaxSessionsPerPublicUser(wsLimitMaxSessionsPerPublicUser);
            profileConfiguration.setWsLimitMaxSessionsPerRegularUser(wsLimitMaxSessionsPerRegularUser);
            profileConfiguration.setWsLimitMaxSubscriptionsPerTenant(wsLimitMaxSubscriptionsPerTenant);
            profileConfiguration.setWsLimitMaxSubscriptionsPerCustomer(wsLimitMaxSubscriptionsPerCustomer);
            profileConfiguration.setWsLimitMaxSubscriptionsPerPublicUser(wsLimitMaxSubscriptionsPerPublicUser);
            profileConfiguration.setWsLimitMaxSubscriptionsPerRegularUser(wsLimitMaxSubscriptionsPerRegularUser);
            profileConfiguration.setWsLimitQueuePerWsSession(wsLimitQueuePerWsSession);

            if (StringUtils.isNotEmpty(wsLimitUpdatesPerSession)) {
                profileConfiguration.setWsLimitUpdatesPerSession(wsLimitUpdatesPerSession);
            }

            if (cassandraLimitsIsEnabled) {
                profileConfiguration.setCassandraTenantLimitsConfiguration(cassandraTenantLimitsConfiguration);
                profileConfiguration.setPrintTenantNames(printTenantNames);
            }

            tenantProfileService.saveTenantProfile(null, entity);
        }
    }
}
