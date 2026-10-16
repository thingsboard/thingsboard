// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.event;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.UUID;

@ToString
@EqualsAndHashCode(callSuper = true)
public class CalculatedFieldDebugEvent extends Event {

    private static final long serialVersionUID = -7091690784759639853L;

    @Builder
    private CalculatedFieldDebugEvent(TenantId tenantId, UUID entityId, String serviceId, UUID id, long ts,
                                      CalculatedFieldId calculatedFieldId, EntityId eventEntity, UUID msgId,
                                      String msgType, String arguments, String result, String error) {
        super(tenantId, entityId, serviceId, id, ts);
        this.calculatedFieldId = calculatedFieldId;
        this.eventEntity = eventEntity;
        this.msgId = msgId;
        this.msgType = msgType;
        this.arguments = arguments;
        this.result = result;
        this.error = error;
    }

    @Getter
    private final CalculatedFieldId calculatedFieldId;
    @Getter
    private final EntityId eventEntity;
    @Getter
    private final UUID msgId;
    @Getter
    private final String msgType;
    @Getter
    @Setter
    private String arguments;
    @Getter
    @Setter
    private String result;
    @Getter
    @Setter
    private String error;

    @Override
    public EventType getType() {
        return EventType.DEBUG_CALCULATED_FIELD;
    }

    @Override
    public EventInfo toInfo(EntityType entityType) {
        EventInfo eventInfo = super.toInfo(entityType);
        var json = (ObjectNode) eventInfo.getBody();
        json.put("calculatedFieldId", calculatedFieldId.toString());
        if (eventEntity != null) {
            json.put("entityId", eventEntity.getId().toString())
                    .put("entityType", eventEntity.getEntityType().name());
        }
        if (msgId != null) {
            json.put("msgId", msgId.toString());
        }
        putNotNull(json, "msgType", msgType);
        putNotNull(json, "arguments", arguments);
        putNotNull(json, "result", result);
        putNotNull(json, "error", error);
        return eventInfo;
    }

}
