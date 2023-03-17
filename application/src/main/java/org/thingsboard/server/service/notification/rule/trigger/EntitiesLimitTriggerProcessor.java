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

import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.notification.info.EntitiesLimitNotificationInfo;
import org.thingsboard.server.common.data.notification.info.NotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.EntitiesLimitNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;
import org.thingsboard.server.service.notification.rule.trigger.EntitiesLimitTriggerProcessor.EntitiesLimitTriggerObject;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

@Service
public class EntitiesLimitTriggerProcessor implements NotificationRuleTriggerProcessor<EntitiesLimitTriggerObject, EntitiesLimitNotificationRuleTriggerConfig> {

    @Override
    public boolean matchesFilter(EntitiesLimitTriggerObject triggerObject, EntitiesLimitNotificationRuleTriggerConfig triggerConfig) {
        if (isNotEmpty(triggerConfig.getEntityTypes()) && !triggerConfig.getEntityTypes().contains(triggerObject.getEntityType())) {
            return false;
        }
        return ((float) triggerObject.getCurrentCount() / triggerObject.getLimit()) >= triggerConfig.getThreshold();
    }

    @Override
    public NotificationInfo constructNotificationInfo(EntitiesLimitTriggerObject triggerObject, EntitiesLimitNotificationRuleTriggerConfig triggerConfig) {
        return EntitiesLimitNotificationInfo.builder()
                .entityType(triggerObject.getEntityType())
                .threshold((int) (triggerConfig.getThreshold() * 100))
                .build();
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.ENTITIES_LIMIT;
    }

    @Data
    @Builder
    public static class EntitiesLimitTriggerObject {
        private final EntityType entityType;
        private final long limit;
        private final long currentCount;
    }

}
