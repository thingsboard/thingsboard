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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.event.CalculatedFieldDebugEvent;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseEntity;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.CALCULATED_FIELD_DEBUG_EVENT_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_CALCULATED_FIELD_ARGUMENTS_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_CALCULATED_FIELD_ID_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_CALCULATED_FIELD_RESULT_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_ENTITY_ID_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_ENTITY_TYPE_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_ERROR_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_MSG_ID_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_MSG_TYPE_COLUMN_NAME;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = CALCULATED_FIELD_DEBUG_EVENT_TABLE_NAME)
@NoArgsConstructor
public class CalculatedFieldDebugEventEntity extends EventEntity<CalculatedFieldDebugEvent> implements BaseEntity<CalculatedFieldDebugEvent> {

    @Column(name = EVENT_CALCULATED_FIELD_ID_COLUMN_NAME)
    private UUID calculatedFieldId;
    @Column(name = EVENT_ENTITY_ID_COLUMN_NAME)
    private UUID eventEntityId;
    @Column(name = EVENT_ENTITY_TYPE_COLUMN_NAME)
    private String eventEntityType;
    @Column(name = EVENT_MSG_ID_COLUMN_NAME)
    private UUID msgId;
    @Column(name = EVENT_MSG_TYPE_COLUMN_NAME)
    private String msgType;
    @Column(name = EVENT_CALCULATED_FIELD_ARGUMENTS_COLUMN_NAME)
    private String arguments;
    @Column(name = EVENT_CALCULATED_FIELD_RESULT_COLUMN_NAME)
    private String result;
    @Column(name = EVENT_ERROR_COLUMN_NAME)
    private String error;

    public CalculatedFieldDebugEventEntity(CalculatedFieldDebugEvent event) {
        super(event);
        if (event.getCalculatedFieldId() != null) {
            this.calculatedFieldId = event.getCalculatedFieldId().getId();
        }
        if (event.getEventEntity() != null) {
            this.eventEntityId = event.getEventEntity().getId();
            this.eventEntityType = event.getEventEntity().getEntityType().name();
        }
        this.msgId = event.getMsgId();
        this.msgType = event.getMsgType();
        this.arguments = event.getArguments();
        this.result = event.getResult();
        this.error = event.getError();
    }

    @Override
    public CalculatedFieldDebugEvent toData() {
        var builder = CalculatedFieldDebugEvent.builder()
                .id(id)
                .tenantId(TenantId.fromUUID(tenantId))
                .ts(ts)
                .serviceId(serviceId)
                .entityId(entityId)
                .msgId(msgId)
                .msgType(msgType)
                .arguments(arguments)
                .result(result)
                .error(error);
        if (calculatedFieldId != null) {
            builder.calculatedFieldId(new CalculatedFieldId(calculatedFieldId));
        }
        if (eventEntityId != null) {
            builder.eventEntity(EntityIdFactory.getByTypeAndUuid(eventEntityType, eventEntityId));
        }
        return builder.build();
    }

}
