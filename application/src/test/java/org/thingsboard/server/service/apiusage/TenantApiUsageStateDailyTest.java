/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.apiusage;

import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.ApiUsageStateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.msg.tools.SchedulerUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

public class TenantApiUsageStateDailyTest {

    private static final long MONTHLY = 10_000_000L;
    private static final double WARN_FRACTION = 0.8;
    private static final int PEAK_DAYS = 3;

    private TenantProfile tenantProfile;
    private long currentCycleTs;
    private long nextCycleTs;

    @Before
    public void setUp() {
        currentCycleTs = SchedulerUtils.getStartOfCurrentMonth();
        nextCycleTs = SchedulerUtils.getStartOfNextMonth();

        DefaultTenantProfileConfiguration config = DefaultTenantProfileConfiguration.builder()
                .maxTransportMessages(MONTHLY)
                .warnThreshold(WARN_FRACTION)
                .dailyPeakDays(PEAK_DAYS)
                .build();
        TenantProfileData profileData = new TenantProfileData();
        profileData.setConfiguration(config);
        tenantProfile = new TenantProfile();
        tenantProfile.setProfileData(profileData);
    }

    private TenantApiUsageState createState() {
        ApiUsageState apiUsageState = new ApiUsageState(new ApiUsageStateId(UUID.randomUUID()));
        apiUsageState.setTenantId(TenantId.fromUUID(UUID.randomUUID()));
        apiUsageState.setEntityId(TenantId.fromUUID(UUID.randomUUID()));
        return new TenantApiUsageState(tenantProfile, apiUsageState);
    }

    @Test
    public void testDailyThresholdIsProportionalToMonthly() {
        TenantApiUsageState state = createState();

        long daily = state.getDailyThreshold(ApiUsageRecordKey.TRANSPORT_MSG_COUNT);
        long monthMs = nextCycleTs - currentCycleTs;
        long expected = MONTHLY * PEAK_DAYS * TimeUnit.DAYS.toMillis(1) / monthMs;

        assertThat(daily).isEqualTo(expected);
        // Roughly 3/30 of monthly for a 30-day month
        double ratio = (double) daily / MONTHLY;
        assertThat(ratio).isCloseTo((double) PEAK_DAYS / 30, within(0.01));
    }

    @Test
    public void testDailyThresholdIsLessThanMonthly() {
        TenantApiUsageState state = createState();
        assertThat(state.getDailyThreshold(ApiUsageRecordKey.TRANSPORT_MSG_COUNT))
                .isLessThan(MONTHLY);
    }

    @Test
    public void testUnlimitedMonthlyGivesUnlimitedDaily() {
        DefaultTenantProfileConfiguration config = DefaultTenantProfileConfiguration.builder()
                .maxTransportMessages(0L)
                .build();
        TenantProfileData profileData = new TenantProfileData();
        profileData.setConfiguration(config);
        tenantProfile.setProfileData(profileData);

        TenantApiUsageState state = createState();
        assertThat(state.getDailyThreshold(ApiUsageRecordKey.TRANSPORT_MSG_COUNT)).isZero();
    }

    @Test
    public void testDailyWarnThresholdAppliesWarnFraction() {
        TenantApiUsageState state = createState();

        long daily = state.getDailyThreshold(ApiUsageRecordKey.TRANSPORT_MSG_COUNT);
        long dailyWarn = state.getDailyWarnThreshold(ApiUsageRecordKey.TRANSPORT_MSG_COUNT);

        assertThat((double) dailyWarn / daily).isCloseTo(WARN_FRACTION, within(0.001));
    }

    @Test
    public void testMonthlyThresholdUnchanged() {
        TenantApiUsageState state = createState();
        assertThat(state.getProfileThreshold(ApiUsageRecordKey.TRANSPORT_MSG_COUNT)).isEqualTo(MONTHLY);
    }

    @Test
    public void testDailyThresholdRespectsPeakDaysConfig() {
        // peakDays=1 → daily ≈ 1/30 monthly; peakDays=6 → daily ≈ 6/30 monthly
        for (int days : new int[]{1, 3, 6}) {
            DefaultTenantProfileConfiguration config = DefaultTenantProfileConfiguration.builder()
                    .maxTransportMessages(MONTHLY)
                    .warnThreshold(WARN_FRACTION)
                    .dailyPeakDays(days)
                    .build();
            TenantProfileData profileData = new TenantProfileData();
            profileData.setConfiguration(config);
            tenantProfile.setProfileData(profileData);

            TenantApiUsageState state = createState();
            double ratio = (double) state.getDailyThreshold(ApiUsageRecordKey.TRANSPORT_MSG_COUNT) / MONTHLY;
            assertThat(ratio).isCloseTo((double) days / 30, within(0.01));
        }
    }
}
