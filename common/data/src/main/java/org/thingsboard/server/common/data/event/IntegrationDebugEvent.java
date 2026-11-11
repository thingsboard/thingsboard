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
public class IntegrationDebugEvent extends Event {

    @Builder
    public IntegrationDebugEvent(TenantId tenantId, UUID entityId, String serviceId, UUID id, long ts, String eventType, String messageType, String message, String status, String error) {
        super(tenantId, entityId, serviceId, id, ts);
        this.eventType = eventType;
        this.messageType = messageType;
        this.message = message;
        this.status = status;
        this.error = error;
    }

    @Getter
    private final String eventType;
    @Getter
    private final String messageType;
    @Getter
    @Setter
    private String message;
    @Getter
    @Setter
    private String status;
    @Getter
    @Setter
    private String error;

    @Override
    public EventType getType() {
        return EventType.DEBUG_INTEGRATION;
    }

    @Override
    public EventInfo toInfo(EntityType entityType) {
        EventInfo eventInfo = super.toInfo(entityType);
        var json = (ObjectNode) eventInfo.getBody();
        json.put("type", eventType);
        putNotNull(json, "messageType", messageType);
        putNotNull(json, "message", message);
        putNotNull(json, "status", status);
        putNotNull(json, "error", error);
        return eventInfo;
    }

}
