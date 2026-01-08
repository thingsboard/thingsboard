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

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.ApiFeature;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.tools.SchedulerUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BaseApiUsageState {
    private final Map<ApiUsageRecordKey, Long> currentCycleValues = new ConcurrentHashMap<>();
    private final Map<ApiUsageRecordKey, Long> currentHourValues = new ConcurrentHashMap<>();

    private final Map<ApiUsageRecordKey, Map<String, Long>> lastGaugesByServiceId = new HashMap<>();
    private final Map<ApiUsageRecordKey, Long> gaugesReportCycles = new HashMap<>();

    @Getter
    @Setter
    private ApiUsageState apiUsageState;
    @Getter
    private volatile long currentCycleTs;
    @Getter
    private volatile long nextCycleTs;
    @Getter
    private volatile long currentHourTs;

    @Setter
    private long gaugeReportInterval;

    public BaseApiUsageState(ApiUsageState apiUsageState) {
        this.apiUsageState = apiUsageState;
        this.currentCycleTs = SchedulerUtils.getStartOfCurrentMonth();
        this.nextCycleTs = SchedulerUtils.getStartOfNextMonth();
        this.currentHourTs = SchedulerUtils.getStartOfCurrentHour();
    }

    public StatsCalculationResult calculate(ApiUsageRecordKey key, long value, String serviceId) {
        long currentValue = get(key);
        long currentHourlyValue = getHourly(key);

        StatsCalculationResult result;
        if (key.isCounter()) {
            result = StatsCalculationResult.builder()
                    .newValue(currentValue + value).valueChanged(true)
                    .newHourlyValue(currentHourlyValue + value).hourlyValueChanged(true)
                    .build();
        } else {
            Long newGaugeValue = calculateGauge(key, value, serviceId);
            long newValue = newGaugeValue != null ? newGaugeValue : currentValue;
            long newHourlyValue = newGaugeValue != null ? Math.max(newGaugeValue, currentHourlyValue) : currentHourlyValue;
            result = StatsCalculationResult.builder()
                    .newValue(newValue).valueChanged(newValue != currentValue || !currentCycleValues.containsKey(key))
                    .newHourlyValue(newHourlyValue).hourlyValueChanged(newHourlyValue != currentHourlyValue || !currentHourValues.containsKey(key))
                    .build();
        }

        set(key, result.getNewValue());
        setHourly(key, result.getNewHourlyValue());
        return result;
    }

    private Long calculateGauge(ApiUsageRecordKey key, long value, String serviceId) {
        Map<String, Long> lastByServiceId = lastGaugesByServiceId.computeIfAbsent(key, k -> {
            gaugesReportCycles.put(key, System.currentTimeMillis());
            return new HashMap<>();
        });
        lastByServiceId.put(serviceId, value);

        Long gaugeReportCycle = gaugesReportCycles.get(key);
        if (gaugeReportCycle <= System.currentTimeMillis() - gaugeReportInterval) {
            long newValue = lastByServiceId.values().stream().mapToLong(Long::longValue).sum();
            lastGaugesByServiceId.remove(key);
            gaugesReportCycles.remove(key);
            return newValue;
        } else {
            return null;
        }
    }

    public void set(ApiUsageRecordKey key, Long value) {
        currentCycleValues.put(key, value);
    }

    public long get(ApiUsageRecordKey key) {
        return currentCycleValues.getOrDefault(key, 0L);
    }

    public void setHourly(ApiUsageRecordKey key, Long value) {
        currentHourValues.put(key, value);
    }

    public long getHourly(ApiUsageRecordKey key) {
        return currentHourValues.getOrDefault(key, 0L);
    }

    public void setHour(long currentHourTs) {
        this.currentHourTs = currentHourTs;
        currentHourValues.clear();
        lastGaugesByServiceId.clear();
        gaugesReportCycles.clear();
    }

    public void setCycles(long currentCycleTs, long nextCycleTs) {
        this.currentCycleTs = currentCycleTs;
        this.nextCycleTs = nextCycleTs;
        currentCycleValues.clear();
    }

    public void onRepartitionEvent() {
        lastGaugesByServiceId.clear();
        gaugesReportCycles.clear();
    }

    public ApiUsageStateValue getFeatureValue(ApiFeature feature) {
        switch (feature) {
            case TRANSPORT:
                return apiUsageState.getTransportState();
            case RE:
                return apiUsageState.getReExecState();
            case DB:
                return apiUsageState.getDbStorageState();
            case JS:
                return apiUsageState.getJsExecState();
            case TBEL:
                return apiUsageState.getTbelExecState();
            case EMAIL:
                return apiUsageState.getEmailExecState();
            case SMS:
                return apiUsageState.getSmsExecState();
            case ALARM:
                return apiUsageState.getAlarmExecState();
            default:
                return ApiUsageStateValue.ENABLED;
        }
    }

    public boolean setFeatureValue(ApiFeature feature, ApiUsageStateValue value) {
        ApiUsageStateValue currentValue = getFeatureValue(feature);
        switch (feature) {
            case TRANSPORT:
                apiUsageState.setTransportState(value);
                break;
            case RE:
                apiUsageState.setReExecState(value);
                break;
            case DB:
                apiUsageState.setDbStorageState(value);
                break;
            case JS:
                apiUsageState.setJsExecState(value);
                break;
            case TBEL:
                apiUsageState.setTbelExecState(value);
                break;
            case EMAIL:
                apiUsageState.setEmailExecState(value);
                break;
            case SMS:
                apiUsageState.setSmsExecState(value);
                break;
            case ALARM:
                apiUsageState.setAlarmExecState(value);
                break;
        }
        return !currentValue.equals(value);
    }

    public abstract EntityType getEntityType();

    public TenantId getTenantId() {
        return getApiUsageState().getTenantId();
    }

    public EntityId getEntityId() {
        return getApiUsageState().getEntityId();
    }

    @Override
    public String toString() {
        return "BaseApiUsageState{" +
                "apiUsageState=" + apiUsageState +
                ", currentCycleTs=" + currentCycleTs +
                ", nextCycleTs=" + nextCycleTs +
                ", currentHourTs=" + currentHourTs +
                '}';
    }

    @Data
    @Builder
    public static class StatsCalculationResult {
        private final long newValue;
        private final boolean valueChanged;
        private final long newHourlyValue;
        private final boolean hourlyValueChanged;
    }

}
