/**
 * Copyright © 2016-2024 The Thingsboard Authors
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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TenantProfileType;

@Schema
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class DefaultTenantProfileConfiguration implements TenantProfileConfiguration {

    private static final long serialVersionUID = -7134932690332578595L;

    private long maxDevices;
    private long maxAssets;
    private long maxCustomers;
    private long maxUsers;
    private long maxDashboards;
    private long maxRuleChains;
    private long maxResourcesInBytes;
    private long maxOtaPackagesInBytes;
    private long maxResourceSize;

    @Schema(example = "1000:1,20000:60")
    private String transportTenantMsgRateLimit;
    @Schema(example = "1000:1,20000:60")
    private String transportTenantTelemetryMsgRateLimit;
    @Schema(example = "1000:1,20000:60")
    private String transportTenantTelemetryDataPointsRateLimit;
    @Schema(example = "20:1,600:60")
    private String transportDeviceMsgRateLimit;
    @Schema(example = "20:1,600:60")
    private String transportDeviceTelemetryMsgRateLimit;
    @Schema(example = "20:1,600:60")
    private String transportDeviceTelemetryDataPointsRateLimit;
    @Schema(example = "20:1,600:60")
    private String transportGatewayMsgRateLimit;
    @Schema(example = "20:1,600:60")
    private String transportGatewayTelemetryMsgRateLimit;
    @Schema(example = "20:1,600:60")
    private String transportGatewayTelemetryDataPointsRateLimit;
    @Schema(example = "20:1,600:60")
    private String transportGatewayDeviceMsgRateLimit;
    @Schema(example = "20:1,600:60")
    private String transportGatewayDeviceTelemetryMsgRateLimit;
    @Schema(example = "20:1,600:60")
    private String transportGatewayDeviceTelemetryDataPointsRateLimit;

    @Schema(example = "20:1,600:60")
    private String tenantEntityExportRateLimit;
    @Schema(example = "20:1,600:60")
    private String tenantEntityImportRateLimit;
    @Schema(example = "20:1,600:60")
    private String tenantNotificationRequestsRateLimit;
    @Schema(example = "20:1,600:60")
    private String tenantNotificationRequestsPerRuleRateLimit;

    @Schema(example = "10000000")
    private long maxTransportMessages;
    @Schema(example = "10000000")
    private long maxTransportDataPoints;
    @Schema(example = "4000000")
    private long maxREExecutions;
    @Schema(example = "5000000")
    private long maxJSExecutions;
    @Schema(example = "5000000")
    private long maxTbelExecutions;
    @Schema(example = "0")
    private long maxDPStorageDays;
    @Schema(example = "50")
    private int maxRuleNodeExecutionsPerMessage;
    @Schema(example = "0")
    private long maxEmails;
    @Schema(example = "true")
    private Boolean smsEnabled;
    @Schema(example = "0")
    private long maxSms;
    @Schema(example = "1000")
    private long maxCreatedAlarms;

    private String tenantServerRestLimitsConfiguration;
    private String customerServerRestLimitsConfiguration;

    private int maxWsSessionsPerTenant;
    private int maxWsSessionsPerCustomer;
    private int maxWsSessionsPerRegularUser;
    private int maxWsSessionsPerPublicUser;
    private int wsMsgQueueLimitPerSession;
    private long maxWsSubscriptionsPerTenant;
    private long maxWsSubscriptionsPerCustomer;
    private long maxWsSubscriptionsPerRegularUser;
    private long maxWsSubscriptionsPerPublicUser;
    private String wsUpdatesPerSessionRateLimit;

    private String cassandraQueryTenantRateLimitsConfiguration;

    private String edgeEventRateLimits;
    private String edgeEventRateLimitsPerEdge;
    private String edgeUplinkMessagesRateLimits;
    private String edgeUplinkMessagesRateLimitsPerEdge;

    private int defaultStorageTtlDays;
    private int alarmsTtlDays;
    private int rpcTtlDays;
    private int queueStatsTtlDays;
    private int ruleEngineExceptionsTtlDays;

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
            case TBEL_EXEC_COUNT:
                return maxTbelExecutions;
            case RE_EXEC_COUNT:
                return maxREExecutions;
            case STORAGE_DP_COUNT:
                return maxDPStorageDays;
            case EMAIL_EXEC_COUNT:
                return maxEmails;
            case SMS_EXEC_COUNT:
                return maxSms;
            case CREATED_ALARMS_COUNT:
                return maxCreatedAlarms;
        }
        return 0L;
    }

    @Override
    public boolean getProfileFeatureEnabled(ApiUsageRecordKey key) {
        switch (key) {
            case SMS_EXEC_COUNT:
                return smsEnabled == null || Boolean.TRUE.equals(smsEnabled);
            default:
                return true;
        }
    }

    @Override
    public long getWarnThreshold(ApiUsageRecordKey key) {
        return (long) (getProfileThreshold(key) * (warnThreshold > 0.0 ? warnThreshold : 0.8));
    }

    public long getEntitiesLimit(EntityType entityType) {
        switch (entityType) {
            case DEVICE:
                return maxDevices;
            case ASSET:
                return maxAssets;
            case CUSTOMER:
                return maxCustomers;
            case USER:
                return maxUsers;
            case DASHBOARD:
                return maxDashboards;
            case RULE_CHAIN:
                return maxRuleChains;
            default:
                return 0;
        }
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
