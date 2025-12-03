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
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.thingsboard.server.common.data.trendz.TrendzSettings;

import java.util.List;

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
    boolean mobileQrEnabled;
    int maxDebugModeDurationMinutes;
    String ruleChainDebugPerTenantLimitsConfiguration;
    String calculatedFieldDebugPerTenantLimitsConfiguration;
    long maxArgumentsPerCF;
    long maxDataPointsPerRollingArg;
    int minAllowedScheduledUpdateIntervalInSecForCF;
    int maxRelationLevelPerCfArgument;
    long minAllowedDeduplicationIntervalInSecForCF;
    long minAllowedAggregationIntervalInSecForCF;
    long intermediateAggregationIntervalInSecForCF;
    TrendzSettings trendzSettings;
}
