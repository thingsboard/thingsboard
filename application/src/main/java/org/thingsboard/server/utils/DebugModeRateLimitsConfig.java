// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.utils;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class DebugModeRateLimitsConfig {

    @Value("${actors.rule.chain.debug_mode_rate_limits_per_tenant.enabled:true}")
    private boolean ruleChainDebugPerTenantLimitsEnabled;
    @Value("${actors.rule.chain.debug_mode_rate_limits_per_tenant.configuration:50000:3600}")
    private String ruleChainDebugPerTenantLimitsConfiguration;

    @Value("${actors.calculated_fields.debug_mode_rate_limits_per_tenant.enabled:true}")
    private boolean calculatedFieldDebugPerTenantLimitsEnabled;
    @Value("${actors.calculated_fields.debug_mode_rate_limits_per_tenant.configuration:50000:3600}")
    private String calculatedFieldDebugPerTenantLimitsConfiguration;

}
