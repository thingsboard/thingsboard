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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.id.AlarmCommentId;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.ALARM_COMMENT_ALARM_ID;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_COMMENT_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_COMMENT_COMMENT;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_COMMENT_CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_COMMENT_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_COMMENT_TYPE;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ALARM_COMMENT_COLUMN_FAMILY_NAME)

public class AlarmCommentEntity extends BaseSqlEntity<AlarmComment> implements BaseEntity<AlarmComment> {

    @Column(name = ALARM_COMMENT_TENANT_ID_PROPERTY, columnDefinition = "uuid")
    private UUID tenantId;

    @Column(name = ALARM_COMMENT_CUSTOMER_ID_PROPERTY, columnDefinition = "uuid")
    private UUID customerId;
    @Column(name = ALARM_COMMENT_ALARM_ID, columnDefinition = "uuid")
    private UUID alarmId;

    @Column(name = ModelConstants.ALARM_COMMENT_USER_ID)
    private UUID userId;

    @Column(name = ALARM_COMMENT_TYPE)
    private String type;

    @Type(type = "json")
    @Column(name = ALARM_COMMENT_COMMENT)
    private JsonNode comment;

    public AlarmCommentEntity() {
        super();
    }

    public AlarmCommentEntity(AlarmComment alarmComment) {
        if (alarmComment.getId() != null) {
            this.setUuid(alarmComment.getUuidId());
        }
        if (alarmComment.getTenantId() != null) {
            this.tenantId = alarmComment.getTenantId().getId();
        }
        if (alarmComment.getCustomerId() != null) {
            this.customerId = alarmComment.getCustomerId().getId();
        }
        this.setCreatedTime(alarmComment.getCreatedTime());
        this.setCreatedTime(alarmComment.getCreatedTime());
        if (alarmComment.getAlarmId() != null) {
            this.alarmId = alarmComment.getAlarmId().getId();
        }
        if (alarmComment.getUserId() != null) {
            this.userId = alarmComment.getUserId().getId();
        }
        if (alarmComment.getType() != null) {
            this.type = alarmComment.getType();
        }
        this.setComment(alarmComment.getComment());
    }

    @Override
    public AlarmComment toData() {
        AlarmComment alarmComment = new AlarmComment(new AlarmCommentId(id));
        alarmComment.setCreatedTime(createdTime);
        alarmComment.setAlarmId(new AlarmId(alarmId));
        if (userId != null) {
            alarmComment.setUserId(new UserId(userId));
        }
        if (tenantId != null) {
            alarmComment.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (customerId != null) {
            alarmComment.setCustomerId(new CustomerId(customerId));
        }
        alarmComment.setType(type);
        alarmComment.setComment(comment);
        return alarmComment;
    }

}
