// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.event;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.UUID;

@ToString
@EqualsAndHashCode(callSuper = true)
public class RawDataEvent extends Event {

    @Builder
    public RawDataEvent(TenantId tenantId, UUID entityId, String serviceId, UUID id, long ts, String uuid, String messageType, String message) {
        super(tenantId, entityId, serviceId, id, ts);
        this.uuid = uuid;
        this.messageType = messageType;
        this.message = message;
    }

    @Getter @Setter
    private String uuid;
    @Getter @Setter
    private String messageType;
    @Getter @Setter
    private String message;

    @Override
    public EventType getType() {
        return EventType.RAW_DATA;
    }

    @Override
    public EventInfo toInfo(EntityType entityType) {
        EventInfo eventInfo = super.toInfo(entityType);
        eventInfo.setUid(uuid);
        var json = (ObjectNode) eventInfo.getBody();
        putNotNull(json, "uuid", uuid);
        putNotNull(json, "messageType", messageType);
        putNotNull(json, "message", message);
        return eventInfo;
    }

}
