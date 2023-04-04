/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.service.notification.rule.trigger;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.notification.info.ApiUsageLimitNotificationInfo;
import org.thingsboard.server.common.data.notification.info.RuleOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.ApiUsageLimitNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;
import org.thingsboard.server.common.msg.notification.trigger.ApiUsageLimitTrigger;
import org.thingsboard.server.dao.tenant.TenantService;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

@Service
@RequiredArgsConstructor
public class ApiUsageLimitTriggerProcessor implements NotificationRuleTriggerProcessor<ApiUsageLimitTrigger, ApiUsageLimitNotificationRuleTriggerConfig> {

    private final TenantService tenantService;

    @Override
    public boolean matchesFilter(ApiUsageLimitTrigger trigger, ApiUsageLimitNotificationRuleTriggerConfig triggerConfig) {
        return (isEmpty(triggerConfig.getApiFeatures()) || triggerConfig.getApiFeatures().contains(trigger.getState().getApiFeature())) &&
                (isEmpty(triggerConfig.getNotifyOn()) || triggerConfig.getNotifyOn().contains(trigger.getStatus()));
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
