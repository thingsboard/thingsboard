// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.notification.rule.trigger;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.notification.info.ApiUsageLimitNotificationInfo;
import org.thingsboard.server.common.data.notification.info.RuleOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.ApiUsageLimitTrigger;
import org.thingsboard.server.common.data.notification.rule.trigger.config.ApiUsageLimitNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;
import org.thingsboard.server.dao.tenant.TenantService;

import static org.thingsboard.server.common.data.util.CollectionsUtil.emptyOrContains;

@Service
@RequiredArgsConstructor
public class ApiUsageLimitTriggerProcessor implements NotificationRuleTriggerProcessor<ApiUsageLimitTrigger, ApiUsageLimitNotificationRuleTriggerConfig> {

    private final TenantService tenantService;

    @Override
    public boolean matchesFilter(ApiUsageLimitTrigger trigger, ApiUsageLimitNotificationRuleTriggerConfig triggerConfig) {
        return emptyOrContains(triggerConfig.getApiFeatures(), trigger.getState().getApiFeature()) &&
                emptyOrContains(triggerConfig.getNotifyOn(), trigger.getStatus());
    }

    @Override
    public RuleOriginatedNotificationInfo constructNotificationInfo(ApiUsageLimitTrigger trigger) {
        return ApiUsageLimitNotificationInfo.builder()
                .feature(trigger.getState().getApiFeature())
                .recordKey(trigger.getState().getKey())
                .status(trigger.getStatus())
                .limit(trigger.getState().getThresholdAsString())
                .currentValue(trigger.getState().getValueAsString())
                .tenantId(trigger.getTenantId())
                .tenantName(tenantService.findTenantById(trigger.getTenantId()).getName())
                .build();
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.API_USAGE_LIMIT;
    }

}
