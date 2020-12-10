/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.common.data.tenant.profile;

import lombok.Data;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.TenantProfileType;

@Data
public class DefaultTenantProfileConfiguration implements TenantProfileConfiguration {

    private long maxDevices;
    private long maxAssets;
    private long maxCustomers;
    private long maxUsers;
    private long maxDashboards;
    private long maxRuleChains;

    private String transportTenantMsgRateLimit;
    private String transportTenantTelemetryMsgRateLimit;
    private String transportTenantTelemetryDataPointsRateLimit;
    private String transportDeviceMsgRateLimit;
    private String transportDeviceTelemetryMsgRateLimit;
    private String transportDeviceTelemetryDataPointsRateLimit;

    private long maxTransportMessages;
    private long maxTransportDataPoints;
    private long maxREExecutions;
    private long maxJSExecutions;
    private long maxDPStorageDays;
    private int maxRuleNodeExecutionsPerMessage;
    private long maxEmails;
    private long maxSms;

    private int defaultStorageTtlDays;

    private double warnThreshold;

    @Override
    public long getProfileThreshold(ApiUsageRecordKey key) {
        switch (key) {
            case TRANSPORT_MSG_COUNT:
                return maxTransportMessages;
            case TRANSPORT_DP_COUNT:
                return maxTransportDataPoints;
            case JS_EXEC_COUNT:
                return maxJSExecutions;
            case RE_EXEC_COUNT:
                return maxREExecutions;
            case STORAGE_DP_COUNT:
                return maxDPStorageDays;
            case EMAIL_EXEC_COUNT:
                return maxEmails;
            case SMS_EXEC_COUNT:
                return maxSms;
        }
        return 0L;
    }

    @Override
    public long getWarnThreshold(ApiUsageRecordKey key) {
        return (long) (getProfileThreshold(key) * (warnThreshold > 0.0 ? warnThreshold : 0.8));
    }

    @Override
    public TenantProfileType getType() {
        return TenantProfileType.DEFAULT;
    }

    @Override
    public int getMaxRuleNodeExecsPerMessage() {
        return maxRuleNodeExecutionsPerMessage;
    }
}
