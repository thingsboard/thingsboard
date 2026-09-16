// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
