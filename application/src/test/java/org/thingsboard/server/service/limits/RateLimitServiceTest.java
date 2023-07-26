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
package org.thingsboard.server.service.limits;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.util.limits.DefaultRateLimitService;
import org.thingsboard.server.dao.util.limits.RateLimitService;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RateLimitServiceTest {

    private RateLimitService rateLimitService;
    private TbTenantProfileCache tenantProfileCache;
    private TenantId tenantId;

    @Before
    public void beforeEach() {
        tenantProfileCache = Mockito.mock(TbTenantProfileCache.class);
        rateLimitService = new DefaultRateLimitService(tenantProfileCache, mock(NotificationRuleProcessor.class), 60, 100);
        tenantId = new TenantId(UUID.randomUUID());
    }

    @Test
    public void testRateLimits() {
        int max = 2;
        String rateLimit = max + ":600";
        DefaultTenantProfileConfiguration profileConfiguration = new DefaultTenantProfileConfiguration();
        profileConfiguration.setTenantEntityExportRateLimit(rateLimit);
        profileConfiguration.setTenantEntityImportRateLimit(rateLimit);
        profileConfiguration.setTenantNotificationRequestsRateLimit(rateLimit);
        profileConfiguration.setTenantNotificationRequestsPerRuleRateLimit(rateLimit);
        profileConfiguration.setTenantServerRestLimitsConfiguration(rateLimit);
        profileConfiguration.setCustomerServerRestLimitsConfiguration(rateLimit);
        profileConfiguration.setWsUpdatesPerSessionRateLimit(rateLimit);
        profileConfiguration.setCassandraQueryTenantRateLimitsConfiguration(rateLimit);
        updateTenantProfileConfiguration(profileConfiguration);

        for (LimitedApi limitedApi : List.of(
                LimitedApi.ENTITY_EXPORT,
                LimitedApi.ENTITY_IMPORT,
                LimitedApi.NOTIFICATION_REQUESTS,
                LimitedApi.REST_REQUESTS_PER_CUSTOMER,
                LimitedApi.CASSANDRA_QUERIES
        )) {
            testRateLimits(limitedApi, max, tenantId);
        }

        CustomerId customerId = new CustomerId(UUID.randomUUID());
        testRateLimits(LimitedApi.REST_REQUESTS_PER_CUSTOMER, max, customerId);

        NotificationRuleId notificationRuleId = new NotificationRuleId(UUID.randomUUID());
        testRateLimits(LimitedApi.NOTIFICATION_REQUESTS_PER_RULE, max, notificationRuleId);

        String wsSessionId = UUID.randomUUID().toString();
        testRateLimits(LimitedApi.WS_UPDATES_PER_SESSION, max, wsSessionId);
    }

    private void testRateLimits(LimitedApi limitedApi, int max, Object level) {
        for (int i = 1; i <= max; i++) {
            boolean success = rateLimitService.checkRateLimit(limitedApi, tenantId, level);
            assertTrue(success);
        }
        boolean success = rateLimitService.checkRateLimit(limitedApi, tenantId, level);
        assertFalse(success);
    }

    private void updateTenantProfileConfiguration(DefaultTenantProfileConfiguration profileConfiguration) {
        reset(tenantProfileCache);
        TenantProfile tenantProfile = new TenantProfile();
        TenantProfileData profileData = new TenantProfileData();
        profileData.setConfiguration(profileConfiguration);
        tenantProfile.setProfileData(profileData);
        when(tenantProfileCache.get(eq(tenantId))).thenReturn(tenantProfile);
    }

}
