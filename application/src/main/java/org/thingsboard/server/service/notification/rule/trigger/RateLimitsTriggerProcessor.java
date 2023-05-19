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
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.info.RateLimitsNotificationInfo;
import org.thingsboard.server.common.data.notification.info.RuleOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.notification.rule.trigger.RateLimitsNotificationRuleTriggerConfig;
import org.thingsboard.server.common.msg.notification.trigger.RateLimitsTrigger;
import org.thingsboard.server.dao.tenant.TenantService;

@Service
@RequiredArgsConstructor
public class RateLimitsTriggerProcessor implements NotificationRuleTriggerProcessor<RateLimitsTrigger, RateLimitsNotificationRuleTriggerConfig> {

    private final TenantService tenantService;

    @Override
    public boolean matchesFilter(RateLimitsTrigger trigger, RateLimitsNotificationRuleTriggerConfig triggerConfig) {
        return trigger.getLimitLevel() != null;
    }

    @Override
    public RuleOriginatedNotificationInfo constructNotificationInfo(RateLimitsTrigger trigger) {
        return RateLimitsNotificationInfo.builder()
                .tenantId(trigger.getTenantId())
                .tenantName(tenantService.findTenantById(trigger.getTenantId()).getName())
                .api(trigger.getApi())
                .limitLevel(trigger.getLimitLevel() instanceof TenantId ? null : trigger.getLimitLevel())
                .limitLevelEntityName(trigger.getLimitLevelEntityName())
                .build();
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.RATE_LIMITS;
    }

}
