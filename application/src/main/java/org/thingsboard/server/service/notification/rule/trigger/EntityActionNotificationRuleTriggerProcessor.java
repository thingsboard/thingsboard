/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.notification.info.EntityActionNotificationInfo;
import org.thingsboard.server.common.data.notification.info.NotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.EntityActionNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.UUID;

@Service
public class EntityActionNotificationRuleTriggerProcessor implements NotificationRuleTriggerProcessor<TbMsg, EntityActionNotificationRuleTriggerConfig> {

    @Override
    public boolean matchesFilter(TbMsg ruleEngineMsg, EntityActionNotificationRuleTriggerConfig triggerConfig) {
        String msgType = ruleEngineMsg.getType();
        if (msgType.equals(DataConstants.ENTITY_CREATED)) {
            if (!triggerConfig.isCreated()) {
                return false;
            }
        } else if (msgType.equals(DataConstants.ENTITY_UPDATED)) {
            if (!triggerConfig.isUpdated()) {
                return false;
            }
        } else if (msgType.equals(DataConstants.ENTITY_DELETED)) {
            if (!triggerConfig.isDeleted()) {
                return false;
            }
        } else {
            return false;
        }
        return triggerConfig.getEntityType() == null || ruleEngineMsg.getOriginator().getEntityType() == triggerConfig.getEntityType();
    }

    @Override
    public NotificationInfo constructNotificationInfo(TbMsg ruleEngineMsg, EntityActionNotificationRuleTriggerConfig triggerConfig) {
        EntityId entityId = ruleEngineMsg.getOriginator();
        String msgType = ruleEngineMsg.getType();
        ActionType actionType = msgType.equals(DataConstants.ENTITY_CREATED) ? ActionType.ADDED :
                                msgType.equals(DataConstants.ENTITY_UPDATED) ? ActionType.UPDATED :
                                msgType.equals(DataConstants.ENTITY_DELETED) ? ActionType.DELETED : null;
        return EntityActionNotificationInfo.builder()
                .entityType(entityId.getEntityType())
                .entityId(entityId.getId())
                .entityName(ruleEngineMsg.getMetaData().getValue("entityName"))
                .actionType(actionType)
                .originatorUserId(UUID.fromString(ruleEngineMsg.getMetaData().getValue("userId")))
                .originatorUserName(ruleEngineMsg.getMetaData().getValue("userName"))
                .entityCustomerId(ruleEngineMsg.getCustomerId())
                .build();
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.ENTITY_ACTION;
    }

}
