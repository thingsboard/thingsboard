// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
    TrendzSettings trendzSettings;
    String nullsOrderStrategy;
    boolean edqsEnabled;
    String iotHubBaseUrl;
}
