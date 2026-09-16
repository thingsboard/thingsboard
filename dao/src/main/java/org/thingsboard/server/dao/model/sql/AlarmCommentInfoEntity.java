// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.alarm.AlarmCommentInfo;

@Data
@EqualsAndHashCode(callSuper = true)
public class AlarmCommentInfoEntity extends AbstractAlarmCommentEntity<AlarmCommentInfo> {

    private String firstName;
    private String lastName;

    private String email;

    public AlarmCommentInfoEntity() {
        super();
    }

    public AlarmCommentInfoEntity(AlarmCommentEntity alarmCommentEntity) {
        super(alarmCommentEntity);
    }

    public AlarmCommentInfoEntity(AlarmCommentEntity alarmCommentEntity, String firstName, String lastName, String email) {
        super(alarmCommentEntity);
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    @Override
    public AlarmCommentInfo toData() {
        return new AlarmCommentInfo(super.toAlarmComment(), this.firstName, this.lastName, this.email);
    }
}
