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
import org.thingsboard.server.common.data.notification.info.EntitiesLimitNotificationInfo;
import org.thingsboard.server.common.data.notification.info.RuleOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.EntitiesLimitNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;
import org.thingsboard.server.dao.notification.trigger.EntitiesLimitTrigger;
import org.thingsboard.server.dao.tenant.TenantService;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

@Service
@RequiredArgsConstructor
public class EntitiesLimitTriggerProcessor implements NotificationRuleTriggerProcessor<EntitiesLimitTrigger, EntitiesLimitNotificationRuleTriggerConfig> {

    private final TenantService tenantService;

    @Override
    public boolean matchesFilter(EntitiesLimitTrigger trigger, EntitiesLimitNotificationRuleTriggerConfig triggerConfig) {
        if (isNotEmpty(triggerConfig.getEntityTypes()) && !triggerConfig.getEntityTypes().contains(trigger.getEntityType())) {
            return false;
        }
        return (int) (trigger.getLimit() * triggerConfig.getThreshold()) == trigger.getCurrentCount(); // strict comparing not to send notification on each new entity
    }

    @Override
    public RuleOriginatedNotificationInfo constructNotificationInfo(EntitiesLimitTrigger trigger) {
        return EntitiesLimitNotificationInfo.builder()
                .entityType(trigger.getEntityType())
                .currentCount(trigger.getCurrentCount())
                .limit(trigger.getLimit())
                .percents((int) (((float)trigger.getCurrentCount() / trigger.getLimit()) * 100))
                .tenantId(trigger.getTenantId())
                .tenantName(tenantService.findTenantById(trigger.getTenantId()).getName())
                .build();
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.ENTITIES_LIMIT;
    }

}
