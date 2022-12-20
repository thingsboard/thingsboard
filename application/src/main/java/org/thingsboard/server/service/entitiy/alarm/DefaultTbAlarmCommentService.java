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
package org.thingsboard.server.service.entitiy.alarm;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

@Service
@AllArgsConstructor
public class DefaultTbAlarmCommentService extends AbstractTbEntityService implements TbAlarmCommentService{
    @Override
    public AlarmComment saveAlarmComment(Alarm alarm, AlarmComment alarmComment, User user) throws ThingsboardException {
        ActionType actionType = alarmComment.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        UserId userId = user.getId();
        alarmComment.setUserId(userId);
        try {
            AlarmComment savedAlarmComment = checkNotNull(alarmCommentService.createOrUpdateAlarmComment(alarm.getTenantId(), alarmComment).getAlarmComment());
            notificationEntityService.notifyCreateOrUpdateAlarmComment(alarm, savedAlarmComment, actionType, user);
            return savedAlarmComment;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(alarm.getTenantId(), emptyId(EntityType.ALARM_COMMENT), alarmComment, actionType, user, e);
            throw e;
        }
    }
    @Override
    public Boolean deleteAlarmComment(Alarm alarm, AlarmComment alarmComment, User user) {
        notificationEntityService.notifyDeleteAlarmComment(alarm, alarmComment, user);
        return alarmCommentService.deleteAlarmComment(alarm.getTenantId(), alarmComment.getId()).isSuccessful();
    }
}
