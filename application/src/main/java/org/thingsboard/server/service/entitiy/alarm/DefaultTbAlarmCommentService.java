// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.entitiy.alarm;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.dao.alarm.AlarmCommentService;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

@Service
@AllArgsConstructor
public class DefaultTbAlarmCommentService extends AbstractTbEntityService implements TbAlarmCommentService {

    @Autowired
    private AlarmCommentService alarmCommentService;

    @Override
    public AlarmComment saveAlarmComment(Alarm alarm, AlarmComment alarmComment, User user) throws ThingsboardException {
        ActionType actionType = alarmComment.getId() == null ? ActionType.ADDED_COMMENT : ActionType.UPDATED_COMMENT;
        if (user != null) {
            alarmComment.setUserId(user.getId());
        }
        try {
            AlarmComment savedAlarmComment = checkNotNull(alarmCommentService.createOrUpdateAlarmComment(alarm.getTenantId(), alarmComment));
            logEntityActionService.logEntityAction(alarm.getTenantId(), alarm.getId(), alarm, alarm.getCustomerId(), actionType, user, savedAlarmComment);

            return savedAlarmComment;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(alarm.getTenantId(), emptyId(EntityType.ALARM), alarm, actionType, user, e, alarmComment);
            throw e;
        }
    }

    @Override
    public void deleteAlarmComment(Alarm alarm, AlarmComment alarmComment, User user) throws ThingsboardException {
        if (alarmComment.getType() == AlarmCommentType.OTHER) {
            alarmComment.setType(AlarmCommentType.SYSTEM);
            alarmComment.setUserId(null);
            alarmComment.setComment(JacksonUtil.newObjectNode().put("text",
                    String.format("Comment was deleted by user %s",
                            (user.getFirstName() == null || user.getLastName() == null) ? user.getName() : user.getFirstName() + " " + user.getLastName())));
            AlarmComment savedAlarmComment = checkNotNull(alarmCommentService.saveAlarmComment(alarm.getTenantId(), alarmComment));
            logEntityActionService.logEntityAction(alarm.getTenantId(), alarm.getId(), alarm, alarm.getCustomerId(), ActionType.DELETED_COMMENT, user, savedAlarmComment);
        } else {
            throw new ThingsboardException("System comment could not be deleted", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

}
