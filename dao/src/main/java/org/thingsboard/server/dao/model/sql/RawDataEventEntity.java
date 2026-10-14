// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.event.RawDataEvent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseEntity;

import static org.thingsboard.server.dao.model.ModelConstants.EVENT_MESSAGE_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_MESSAGE_TYPE_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_UUID_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.RAW_DATA_EVENT_TABLE_NAME;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = RAW_DATA_EVENT_TABLE_NAME)
@NoArgsConstructor
public class RawDataEventEntity extends EventEntity<RawDataEvent> implements BaseEntity<RawDataEvent> {

    @Column(name = EVENT_UUID_COLUMN_NAME)
    private String eventUuid;
    @Column(name = EVENT_MESSAGE_TYPE_COLUMN_NAME)
    private String messageType;
    @Column(name = EVENT_MESSAGE_COLUMN_NAME)
    private String message;


    public RawDataEventEntity(RawDataEvent event) {
        super(event);
        this.eventUuid = event.getUuid();
        this.messageType = event.getMessageType();
        this.message = event.getMessage();
    }

    @Override
    public RawDataEvent toData() {
        return RawDataEvent.builder()
                .tenantId(TenantId.fromUUID(tenantId))
                .entityId(entityId)
                .serviceId(serviceId)
                .id(id)
                .ts(ts)
                .uuid(eventUuid)
                .messageType(messageType)
                .message(message)
                .build();
    }

}
