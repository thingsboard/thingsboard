/**
 * Copyright © 2016-2026 The Thingsboard Authors
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

import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.notification.info.EntityActionNotificationInfo;
import org.thingsboard.server.common.data.notification.info.RuleOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.EntityActionTrigger;
import org.thingsboard.server.common.data.notification.rule.trigger.config.EntityActionNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;

import static org.thingsboard.server.common.data.util.CollectionsUtil.emptyOrContains;

@Service
public class EntityActionTriggerProcessor implements NotificationRuleTriggerProcessor<EntityActionTrigger, EntityActionNotificationRuleTriggerConfig> {

    @Override
    public boolean matchesFilter(EntityActionTrigger trigger, EntityActionNotificationRuleTriggerConfig triggerConfig) {
        return ((trigger.getActionType() == ActionType.ADDED && triggerConfig.isCreated())
                || (trigger.getActionType() == ActionType.UPDATED && triggerConfig.isUpdated())
                || (trigger.getActionType() == ActionType.DELETED && triggerConfig.isDeleted()))
                && emptyOrContains(triggerConfig.getEntityTypes(), trigger.getEntityId().getEntityType());
    }

    @Override
    public RuleOriginatedNotificationInfo constructNotificationInfo(EntityActionTrigger trigger) {
        return EntityActionNotificationInfo.builder()
                .entityId(trigger.getEntityId())
                .entityName(trigger.getEntity().getName())
                .actionType(trigger.getActionType())
                .userId(trigger.getUser().getUuidId())
                .userTitle(trigger.getUser().getTitle())
                .userEmail(trigger.getUser().getEmail())
                .userFirstName(trigger.getUser().getFirstName())
                .userLastName(trigger.getUser().getLastName())
                .entityCustomerId(trigger.getEntity() instanceof HasCustomerId ?
                        ((HasCustomerId) trigger.getEntity()).getCustomerId() :
                        trigger.getUser().getCustomerId())
                .build();
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.ENTITY_ACTION;
    }

}
