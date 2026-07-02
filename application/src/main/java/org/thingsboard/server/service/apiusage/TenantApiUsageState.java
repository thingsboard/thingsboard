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

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.util.Pair;
import org.thingsboard.server.common.data.ApiFeature;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class TenantApiUsageState extends BaseApiUsageState {
    @Getter
    @Setter
    private TenantProfileId tenantProfileId;
    @Getter
    @Setter
    private TenantProfileData tenantProfileData;

    public TenantApiUsageState(TenantProfile tenantProfile, ApiUsageState apiUsageState) {
        super(apiUsageState);
        this.tenantProfileId = tenantProfile.getId();
        this.tenantProfileData = tenantProfile.getProfileData();
    }

    public TenantApiUsageState(ApiUsageState apiUsageState) {
        super(apiUsageState);
    }

    public long getProfileThreshold(ApiUsageRecordKey key) {
        return tenantProfileData.getConfiguration().getProfileThreshold(key);
    }

    public boolean getProfileFeatureEnabled(ApiUsageRecordKey key) {
        return tenantProfileData.getConfiguration().getProfileFeatureEnabled(key);
    }

    public long getProfileWarnThreshold(ApiUsageRecordKey key) {
        return tenantProfileData.getConfiguration().getWarnThreshold(key);
    }

    /**
     * Daily quota = monthlyQuota * peakDays / daysInBillingMonth.
     * Returns 0 (unlimited) when the monthly quota is unlimited.
     */
    public long getDailyThreshold(ApiUsageRecordKey key) {
        long monthly = getProfileThreshold(key);
        if (monthly == 0) return 0;
        int peakDays = ((DefaultTenantProfileConfiguration) tenantProfileData.getConfiguration()).getDailyPeakDays();
        long monthMs = getNextCycleTs() - getCurrentCycleTs();
        if (monthMs <= 0) return monthly;
        return Math.max(1L, monthly * peakDays * TimeUnit.DAYS.toMillis(1) / monthMs);
    }

    public long getDailyWarnThreshold(ApiUsageRecordKey key) {
        long daily = getDailyThreshold(key);
        if (daily == 0) return 0;
        double warnFraction = ((DefaultTenantProfileConfiguration) tenantProfileData.getConfiguration()).getWarnThreshold();
        return (long) (daily * (warnFraction > 0.0 ? warnFraction : 0.8));
    }

    private Pair<ApiFeature, ApiUsageStateValue> checkStateUpdatedDueToThreshold(ApiFeature feature) {
        ApiUsageStateValue featureValue = ApiUsageStateValue.ENABLED;
        for (ApiUsageRecordKey recordKey : ApiUsageRecordKey.getKeys(feature)) {
            long value = get(recordKey);
            long dailyValue = getDaily(recordKey);
            boolean featureEnabled = getProfileFeatureEnabled(recordKey);
            ApiUsageStateValue tmpValue;
            if (featureEnabled) {
                long threshold = getProfileThreshold(recordKey);
                long warnThreshold = getProfileWarnThreshold(recordKey);
                long dailyThreshold = getDailyThreshold(recordKey);
                long dailyWarnThreshold = getDailyWarnThreshold(recordKey);
                if (threshold == 0) {
                    tmpValue = ApiUsageStateValue.ENABLED;
                } else if (value >= threshold || (dailyThreshold > 0 && dailyValue >= dailyThreshold)) {
                    tmpValue = ApiUsageStateValue.DISABLED;
                } else if (value >= warnThreshold || (dailyThreshold > 0 && dailyValue >= dailyWarnThreshold)) {
                    tmpValue = ApiUsageStateValue.WARNING;
                } else {
                    tmpValue = ApiUsageStateValue.ENABLED;
                }
            } else {
                tmpValue = ApiUsageStateValue.DISABLED;
            }
            featureValue = ApiUsageStateValue.toMoreRestricted(featureValue, tmpValue);
        }
        return setFeatureValue(feature, featureValue) ? Pair.of(feature, featureValue) : null;
    }


    public Map<ApiFeature, ApiUsageStateValue> checkStateUpdatedDueToThresholds() {
        return checkStateUpdatedDueToThreshold(new HashSet<>(Arrays.asList(ApiFeature.values())));
    }

    public Map<ApiFeature, ApiUsageStateValue> checkStateUpdatedDueToThreshold(Set<ApiFeature> features) {
        Map<ApiFeature, ApiUsageStateValue> result = new HashMap<>();
        for (ApiFeature feature : features) {
            Pair<ApiFeature, ApiUsageStateValue> tmp = checkStateUpdatedDueToThreshold(feature);
            if (tmp != null) {
                result.put(tmp.getFirst(), tmp.getSecond());
            }
        }
        return result;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.TENANT;
    }

}
