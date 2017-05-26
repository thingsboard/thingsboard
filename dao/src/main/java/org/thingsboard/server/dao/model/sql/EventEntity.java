/**
 * Copyright © 2016-2017 The Thingsboard Authors
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

import com.datastax.driver.core.utils.UUIDs;

import javax.persistence.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.util.JsonBinaryType;

import static org.thingsboard.server.dao.model.ModelConstants.*;

import java.io.IOException;
import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Table(name = EVENT_COLUMN_FAMILY_NAME)
public class EventEntity implements BaseEntity<Event> {

    @Transient
    private static final long serialVersionUID = -5717830061727466727L;

    @Id
    @Column(name = ID_PROPERTY)
    private UUID id;

    @Column(name = EVENT_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = EVENT_ENTITY_TYPE_PROPERTY)
    private EntityType entityType;

    @Column(name = EVENT_ENTITY_ID_PROPERTY)
    private UUID entityId;

    @Column(name = EVENT_TYPE_PROPERTY)
    private String eventType;

    @Column(name = EVENT_UID_PROPERTY)
    private String eventUid;

    @Type(type = "jsonb")
    @Column(name = EVENT_BODY_PROPERTY, columnDefinition = "jsonb")
    private JsonNode body;

    public EventEntity(Event event) {
        if (event.getId() != null) {
            this.id = event.getId().getId();
        }
        if (event.getTenantId() != null) {
            this.tenantId = event.getTenantId().getId();
        }
        if (event.getEntityId() != null) {
            this.entityType = event.getEntityId().getEntityType();
            this.entityId = event.getEntityId().getId();
        }
        this.eventType = event.getType();
        this.eventUid = event.getUid();
        this.body = event.getBody();
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    public Event toData() {
        Event event = new Event(new EventId(id));
        event.setCreatedTime(UUIDs.unixTimestamp(id));
        event.setTenantId(new TenantId(tenantId));
        switch (entityType) {
            case TENANT:
                event.setEntityId(new TenantId(entityId));
                break;
            case DEVICE:
                event.setEntityId(new DeviceId(entityId));
                break;
            case CUSTOMER:
                event.setEntityId(new CustomerId(entityId));
                break;
            case RULE:
                event.setEntityId(new RuleId(entityId));
                break;
            case PLUGIN:
                event.setEntityId(new PluginId(entityId));
                break;
        }
        event.setBody(body);
        event.setType(eventType);
        event.setUid(eventUid);
        return event;
    }
}
