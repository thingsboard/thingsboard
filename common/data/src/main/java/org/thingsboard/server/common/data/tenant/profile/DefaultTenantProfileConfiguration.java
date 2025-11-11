/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TenantProfileType;
import org.thingsboard.server.common.data.validation.RateLimit;

import java.io.Serial;

@Schema
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class DefaultTenantProfileConfiguration implements TenantProfileConfiguration {

    @Serial
    private static final long serialVersionUID = -7134932690332578595L;

    private long maxDevices;
    private long maxAssets;
    private long maxCustomers;
    private long maxUsers;
    private long maxDashboards;
    private long maxRuleChains;
    private long maxEdges;
    private long maxResourcesInBytes;
    private long maxOtaPackagesInBytes;
    private long maxResourceSize;

    @Schema(example = "1000:1,20000:60")
    @RateLimit(fieldName = "Transport tenant messages")
    private String transportTenantMsgRateLimit;
    @Schema(example = "1000:1,20000:60")
    @RateLimit(fieldName = "Transport tenant telemetry messages")
    private String transportTenantTelemetryMsgRateLimit;
    @Schema(example = "1000:1,20000:60")
    @RateLimit(fieldName = "Transport tenant telemetry data points")
    private String transportTenantTelemetryDataPointsRateLimit;
    @Schema(example = "20:1,600:60")
    @RateLimit(fieldName = "Transport device messages")
    private String transportDeviceMsgRateLimit;
    @Schema(example = "20:1,600:60")
    @RateLimit(fieldName = "Transport device telemetry messages")
    private String transportDeviceTelemetryMsgRateLimit;
    @Schema(example = "20:1,600:60")
    @RateLimit(fieldName = "Transport device telemetry data points")
    private String transportDeviceTelemetryDataPointsRateLimit;
    @Schema(example = "20:1,600:60")
    @RateLimit(fieldName = "Transport gateway messages")
    private String transportGatewayMsgRateLimit;
    @Schema(example = "20:1,600:60")
    @RateLimit(fieldName = "Transport gateway telemetry messages")
    private String transportGatewayTelemetryMsgRateLimit;
    @Schema(example = "20:1,600:60")
    @RateLimit(fieldName = "Transport gateway telemetry data points")
    private String transportGatewayTelemetryDataPointsRateLimit;
    @Schema(example = "20:1,600:60")
    @RateLimit(fieldName = "Transport gateway device messages")
    private String transportGatewayDeviceMsgRateLimit;
    @Schema(example = "20:1,600:60")
    @RateLimit(fieldName = "Transport gateway device telemetry messages")
    private String transportGatewayDeviceTelemetryMsgRateLimit;
    @Schema(example = "20:1,600:60")
    @RateLimit(fieldName = "Transport gateway device telemetry data points")
    private String transportGatewayDeviceTelemetryDataPointsRateLimit;

    @Schema(example = "20:1,600:60")
    @RateLimit(fieldName = "Entity version creation")
    private String tenantEntityExportRateLimit;
    @Schema(example = "20:1,600:60")
    @RateLimit(fieldName = "Entity version load")
    private String tenantEntityImportRateLimit;
    @Schema(example = "20:1,600:60")
    @RateLimit(fieldName = "Notification requests")
    private String tenantNotificationRequestsRateLimit;
    @Schema(example = "20:1,600:60")
    @RateLimit(fieldName = "Notification requests per notification rule")
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
    @Schema(example = "15")
    private int maxDebugModeDurationMinutes;
    @Schema(example = "0")
    private long maxEmails;
    @Schema(example = "true")
    private Boolean smsEnabled;
    @Schema(example = "0")
    private long maxSms;
    @Schema(example = "1000")
    private long maxCreatedAlarms;

    @RateLimit(fieldName = "REST requests for tenant")
    private String tenantServerRestLimitsConfiguration;
    @RateLimit(fieldName = "REST requests for customer")
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
    @RateLimit(fieldName = "WS updates per session")
    private String wsUpdatesPerSessionRateLimit;

    @RateLimit(fieldName = "Rest API and WS telemetry Cassandra read queries")
    private String cassandraReadQueryTenantCoreRateLimits;
    @RateLimit(fieldName = "Rest API Cassandra write queries")
    private String cassandraWriteQueryTenantCoreRateLimits;

    @RateLimit(fieldName = "Rule Engine telemetry Cassandra read queries")
    private String cassandraReadQueryTenantRuleEngineRateLimits;
    @RateLimit(fieldName = "Rule Engine telemetry Cassandra write queries")
    private String cassandraWriteQueryTenantRuleEngineRateLimits;

    @RateLimit(fieldName = "Edge events")
    private String edgeEventRateLimits;
    @RateLimit(fieldName = "Edge events per edge")
    private String edgeEventRateLimitsPerEdge;
    @RateLimit(fieldName = "Edge uplink messages")
    private String edgeUplinkMessagesRateLimits;
    @RateLimit(fieldName = "Edge uplink messages per edge")
    private String edgeUplinkMessagesRateLimitsPerEdge;

    private int defaultStorageTtlDays;
    private int alarmsTtlDays;
    private int rpcTtlDays;
    private int queueStatsTtlDays;
    private int ruleEngineExceptionsTtlDays;

    private double warnThreshold;

    @Schema(example = "5")
    private long maxCalculatedFieldsPerEntity = 5;
    @Schema(example = "10")
    private long maxArgumentsPerCF = 10;
    @Schema(example = "60")
    private int minAllowedScheduledUpdateIntervalInSecForCF = 60;
    @Schema(example = "10")
    private int maxRelationLevelPerCfArgument = 10;
    @Schema(example = "100")
    private int maxRelatedEntitiesToReturnPerCfArgument = 100;
    @Builder.Default
    @Min(value = 1, message = "must be at least 1")
    @Schema(example = "1000")
    private long maxDataPointsPerRollingArg = 1000;
    @Schema(example = "32")
    private long maxStateSizeInKBytes = 32;
    @Schema(example = "2")
    private long maxSingleValueArgumentSizeInKBytes = 2;
    @Schema(example = "60")
    private long minAllowedDeduplicationIntervalInSecForCF = 60;
    @Schema(example = "60")
    private long minAllowedAggregationIntervalInSecForCF = 60;

    @Override
    public long getProfileThreshold(ApiUsageRecordKey key) {
        return switch (key) {
            case TRANSPORT_MSG_COUNT -> maxTransportMessages;
            case TRANSPORT_DP_COUNT -> maxTransportDataPoints;
            case JS_EXEC_COUNT -> maxJSExecutions;
            case TBEL_EXEC_COUNT -> maxTbelExecutions;
            case RE_EXEC_COUNT -> maxREExecutions;
            case STORAGE_DP_COUNT -> maxDPStorageDays;
            case EMAIL_EXEC_COUNT -> maxEmails;
            case SMS_EXEC_COUNT -> maxSms;
            case CREATED_ALARMS_COUNT -> maxCreatedAlarms;
            default -> 0L;
        };
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
        return switch (entityType) {
            case DEVICE -> maxDevices;
            case ASSET -> maxAssets;
            case CUSTOMER -> maxCustomers;
            case USER -> maxUsers;
            case DASHBOARD -> maxDashboards;
            case RULE_CHAIN -> maxRuleChains;
            case EDGE -> maxEdges;
            default -> 0;
        };
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
