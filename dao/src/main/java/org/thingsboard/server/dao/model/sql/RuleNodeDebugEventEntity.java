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
import org.thingsboard.server.common.data.event.RuleNodeDebugEvent;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseEntity;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.EVENT_DATA_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_DATA_TYPE_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_ENTITY_ID_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_ENTITY_TYPE_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_ERROR_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_METADATA_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_MSG_ID_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_MSG_TYPE_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_RELATION_TYPE_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_TYPE_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.RULE_NODE_DEBUG_EVENT_TABLE_NAME;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = RULE_NODE_DEBUG_EVENT_TABLE_NAME)
@NoArgsConstructor
public class RuleNodeDebugEventEntity extends EventEntity<RuleNodeDebugEvent> implements BaseEntity<RuleNodeDebugEvent> {

    @Column(name = EVENT_TYPE_COLUMN_NAME)
    private String eventType;
    @Column(name = EVENT_ENTITY_ID_COLUMN_NAME)
    private UUID eventEntityId;
    @Column(name = EVENT_ENTITY_TYPE_COLUMN_NAME)
    private String eventEntityType;
    @Column(name = EVENT_MSG_ID_COLUMN_NAME)
    private UUID msgId;
    @Column(name = EVENT_MSG_TYPE_COLUMN_NAME)
    private String msgType;
    @Column(name = EVENT_DATA_TYPE_COLUMN_NAME)
    private String dataType;
    @Column(name = EVENT_RELATION_TYPE_COLUMN_NAME)
    private String relationType;
    @Column(name = EVENT_DATA_COLUMN_NAME)
    private String data;
    @Column(name = EVENT_METADATA_COLUMN_NAME)
    private String metadata;
    @Column(name = EVENT_ERROR_COLUMN_NAME)
    private String error;

    public RuleNodeDebugEventEntity(RuleNodeDebugEvent event) {
        super(event);
        this.eventType = event.getEventType();
        if (event.getEventEntity() != null) {
            this.eventEntityId = event.getEventEntity().getId();
            this.eventEntityType = event.getEventEntity().getEntityType().name();
        }
        this.msgId = event.getMsgId();
        this.msgType = event.getMsgType();
        this.dataType = event.getDataType();
        this.relationType = event.getRelationType();
        this.data = event.getData();
        this.metadata = event.getMetadata();
        this.error = event.getError();
    }

    @Override
    public RuleNodeDebugEvent toData() {
        var builder = RuleNodeDebugEvent.builder()
                .tenantId(TenantId.fromUUID(tenantId))
                .entityId(entityId)
                .serviceId(serviceId)
                .id(id)
                .ts(ts)
                .eventType(eventType)
                .msgId(msgId)
                .msgType(msgType)
                .dataType(dataType)
                .relationType(relationType)
                .data(data)
                .metadata(metadata)
                .error(error);
        if (eventEntityId != null) {
            builder.eventEntity(EntityIdFactory.getByTypeAndUuid(eventEntityType, eventEntityId));
        }
        return builder.build();
    }

}
