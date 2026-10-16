// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.service.validator;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.alarm.AlarmCommentDao;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;

@Component
@AllArgsConstructor
public class AlarmCommentDataValidator extends DataValidator<AlarmComment> {

    private final AlarmCommentDao alarmCommentDao;

    @Override
    protected void validateDataImpl(TenantId tenantId, AlarmComment alarmComment) {
        if (alarmComment.getComment() == null) {
            throw new DataValidationException("Alarm comment should be specified!");
        }
        if (alarmComment.getAlarmId() == null) {
            throw new DataValidationException("Alarm id should be specified!");
        }
    }

    @Override
    protected AlarmComment validateUpdate(TenantId tenantId, AlarmComment alarmComment) {
        AlarmComment oldAlarmComment = null;
        if (alarmComment.getId() != null) {
            oldAlarmComment = alarmCommentDao.findAlarmCommentById(tenantId, alarmComment.getId().getId());
            if (oldAlarmComment == null) {
                throw new DataValidationException("Can't update non existing alarm comment!");
            }
            if (oldAlarmComment.getType() == AlarmCommentType.SYSTEM) {
                throw new DataValidationException("System alarm comment can't be updated!");
            }
        }
        return oldAlarmComment;
    }
}
