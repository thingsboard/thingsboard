/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.msg.tools.SchedulerUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TenantApiUsageState {

    private final Map<ApiUsageRecordKey, Long> currentCycleValues = new ConcurrentHashMap<>();
    private final Map<ApiUsageRecordKey, Long> currentHourValues = new ConcurrentHashMap<>();

    @Getter
    @Setter
    private TenantProfileId tenantProfileId;
    @Getter
    @Setter
    private TenantProfileData tenantProfileData;
    @Getter
    private final ApiUsageState apiUsageState;
    @Getter
    private volatile long currentCycleTs;
    @Getter
    private volatile long nextCycleTs;
    @Getter
    private volatile long currentHourTs;

    public TenantApiUsageState(TenantProfile tenantProfile, ApiUsageState apiUsageState) {
        this.tenantProfileId = tenantProfile.getId();
        this.tenantProfileData = tenantProfile.getProfileData();
        this.apiUsageState = apiUsageState;
        this.currentCycleTs = SchedulerUtils.getStartOfCurrentMonth();
        this.nextCycleTs = SchedulerUtils.getStartOfNextMonth();
        this.currentHourTs = SchedulerUtils.getStartOfCurrentHour();
    }

    public void put(ApiUsageRecordKey key, Long value) {
        currentCycleValues.put(key, value);
    }

    public void putHourly(ApiUsageRecordKey key, Long value) {
        currentHourValues.put(key, value);
    }

    public long add(ApiUsageRecordKey key, long value) {
        long result = currentCycleValues.getOrDefault(key, 0L) + value;
        currentCycleValues.put(key, result);
        return result;
    }

    public long get(ApiUsageRecordKey key) {
        return currentCycleValues.getOrDefault(key, 0L);
    }

    public long addToHourly(ApiUsageRecordKey key, long value) {
        long result = currentHourValues.getOrDefault(key, 0L) + value;
        currentHourValues.put(key, result);
        return result;
    }

    public void setHour(long currentHourTs) {
        this.currentHourTs = currentHourTs;
        for (ApiUsageRecordKey key : ApiUsageRecordKey.values()) {
            currentHourValues.put(key, 0L);
        }
    }

    public void setCycles(long currentCycleTs, long nextCycleTs) {
        this.currentCycleTs = currentCycleTs;
        this.nextCycleTs = nextCycleTs;
        for (ApiUsageRecordKey key : ApiUsageRecordKey.values()) {
            currentCycleValues.put(key, 0L);
        }
    }

    public long getProfileThreshold(ApiUsageRecordKey key) {
        Object threshold = tenantProfileData.getProperties().get(key.name());
        if (threshold != null) {
            if (threshold instanceof String) {
                return Long.parseLong((String) threshold);
            } else if (threshold instanceof Long) {
                return (Long) threshold;
            } else if (threshold instanceof Integer) {
                return (Integer) threshold;
            }
        }
        return 0L;
    }

    public TenantId getTenantId() {
        return apiUsageState.getTenantId();
    }

    public EntityId getEntityId() {
        return apiUsageState.getEntityId();
    }

    public boolean isTransportEnabled() {
        return apiUsageState.isTransportEnabled();
    }

    public boolean isDbStorageEnabled() {
        return apiUsageState.isDbStorageEnabled();
    }

    public boolean isRuleEngineEnabled() {
        return apiUsageState.isReExecEnabled();
    }

    public boolean isJsExecEnabled() {
        return apiUsageState.isJsExecEnabled();
    }

    public void setTransportEnabled(boolean transportEnabled) {
        apiUsageState.setTransportEnabled(transportEnabled);
    }

    public void setDbStorageEnabled(boolean dbStorageEnabled) {
        apiUsageState.setDbStorageEnabled(dbStorageEnabled);
    }

    public void setRuleEngineEnabled(boolean ruleEngineEnabled) {
        apiUsageState.setReExecEnabled(ruleEngineEnabled);
    }

    public void setJsExecEnabled(boolean jsExecEnabled) {
        apiUsageState.setJsExecEnabled(jsExecEnabled);
    }

    public boolean isFeatureEnabled(ApiUsageRecordKey recordKey) {
        switch (recordKey) {
            case MSG_COUNT:
            case DP_TRANSPORT_COUNT:
                return isTransportEnabled();
            case RE_EXEC_COUNT:
                return isRuleEngineEnabled();
            case DP_STORAGE_COUNT:
                return isDbStorageEnabled();
            case JS_EXEC_COUNT:
                return isJsExecEnabled();
            default:
                return true;
        }
    }

    public ApiFeature setFeatureValue(ApiUsageRecordKey recordKey, boolean value) {
        ApiFeature feature = null;
        boolean currentValue = isFeatureEnabled(recordKey);
        switch (recordKey) {
            case MSG_COUNT:
            case DP_TRANSPORT_COUNT:
                feature = ApiFeature.TRANSPORT;
                setTransportEnabled(value);
                break;
            case RE_EXEC_COUNT:
                feature = ApiFeature.RE;
                setRuleEngineEnabled(value);
                break;
            case DP_STORAGE_COUNT:
                feature = ApiFeature.DB;
                setDbStorageEnabled(value);
                break;
            case JS_EXEC_COUNT:
                feature = ApiFeature.JS;
                setJsExecEnabled(value);
                break;
        }
        return currentValue == value ? null : feature;
    }

    public Map<ApiFeature, Boolean> checkStateUpdatedDueToThresholds() {
        Map<ApiFeature, Boolean> result = new HashMap<>();
        for (ApiUsageRecordKey key : ApiUsageRecordKey.values()) {
            Pair<ApiFeature, Boolean> featureUpdate = checkStateUpdatedDueToThreshold(key);
            if (featureUpdate != null) {
                result.put(featureUpdate.getFirst(), featureUpdate.getSecond());
            }
        }
        return result;
    }

    public Pair<ApiFeature, Boolean> checkStateUpdatedDueToThreshold(ApiUsageRecordKey recordKey) {
        long value = get(recordKey);
        long threshold = getProfileThreshold(recordKey);
        boolean featureValue = threshold == 0 || value < threshold;
        ApiFeature feature = setFeatureValue(recordKey, featureValue);
        return feature != null ? Pair.of(feature, featureValue) : null;
    }
}
