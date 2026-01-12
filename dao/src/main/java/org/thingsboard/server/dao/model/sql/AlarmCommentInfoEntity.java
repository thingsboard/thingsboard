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
