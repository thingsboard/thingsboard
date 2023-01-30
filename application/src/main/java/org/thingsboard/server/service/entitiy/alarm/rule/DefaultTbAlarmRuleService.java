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
package org.thingsboard.server.service.entitiy.alarm.rule;

import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.AlarmRuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

@Service
public class DefaultTbAlarmRuleService extends AbstractTbEntityService implements TbAlarmRuleService {

    @Override
    public AlarmRule save(AlarmRule alarmRule, User user) throws Exception {
        ActionType actionType = alarmRule.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = alarmRule.getTenantId();
        try {
            AlarmRule savedAlarmRule = checkNotNull(alarmRuleService.saveAlarmRule(tenantId, alarmRule));
            autoCommit(user, savedAlarmRule.getId());
            notificationEntityService.notifyCreateOrUpdateEntity(tenantId, savedAlarmRule.getId(), savedAlarmRule, null, actionType, user);
            return savedAlarmRule;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ALARM_RULE), alarmRule, actionType, user, e);
            throw e;
        }
    }

    @Override
    public void delete(AlarmRule alarmRule, User user) {
        TenantId tenantId = alarmRule.getTenantId();
        AlarmRuleId alarmRuleId = alarmRule.getId();
        try {
            alarmRuleService.deleteAlarmRule(tenantId, alarmRuleId);
            notificationEntityService.notifyDeleteEntity(tenantId, alarmRuleId, alarmRule, null,
                    ActionType.DELETED, null, user, alarmRuleId.toString());
            tbClusterService.broadcastEntityStateChangeEvent(tenantId, alarmRuleId, ComponentLifecycleEvent.DELETED);
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ALARM_RULE), ActionType.DELETED,
                    user, e, alarmRuleId.toString());
            throw e;
        }
    }
}
