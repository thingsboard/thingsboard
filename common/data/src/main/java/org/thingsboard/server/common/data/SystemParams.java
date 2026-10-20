// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.thingsboard.server.common.data.trendz.TrendzSettings;

import java.util.List;
import java.util.Set;

@Data
public class SystemParams {
    boolean userTokenAccessEnabled;
    List<String> allowedDashboardIds;
    boolean edgesSupportEnabled;
    boolean hasRepository;
    boolean tbelEnabled;
    boolean persistDeviceStateToTelemetry;
    JsonNode userSettings;
    long maxDatapointsLimit;
    long maxResourceSize;
    boolean whiteLabelingAllowed;
    boolean customerWhiteLabelingAllowed;
    Set<String> availableLocales;
    boolean mobileQrEnabled;
    int maxDebugModeDurationMinutes;
    String ruleChainDebugPerTenantLimitsConfiguration;
    String integrationDebugPerTenantLimitsConfiguration;
    String converterDebugPerTenantLimitsConfiguration;
    String calculatedFieldDebugPerTenantLimitsConfiguration;
    long maxArgumentsPerCF;
    long maxDataPointsPerRollingArg;
    int minAllowedScheduledUpdateIntervalInSecForCF;
    int maxRelationLevelPerCfArgument;
    int maxRelatedEntitiesToReturnPerCfArgument;
    long minAllowedDeduplicationIntervalInSecForCF;
    long minAllowedAggregationIntervalInSecForCF;
    long intermediateAggregationIntervalInSecForCF;
    boolean aiEnabled;
    TrendzSettings trendzSettings;
    boolean allowKeyFiltersOrConditions;
    String nullsOrderStrategy;
    boolean edqsEnabled;
    String iotHubBaseUrl;
    int licenseVersion;
    boolean edgeEnabled;
    boolean trendzEnabled;
    boolean integrationsEnabled;
    boolean schedulerEnabled;
    boolean reportingEnabled;
    boolean isCommunityGrantLicense;
}
