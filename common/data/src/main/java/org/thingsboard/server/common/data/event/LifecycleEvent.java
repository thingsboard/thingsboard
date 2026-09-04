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
import org.thingsboard.server.common.data.id.TenantId;

import java.util.UUID;

@ToString
@EqualsAndHashCode(callSuper = true)
public class LifecycleEvent extends Event {

    private static final long serialVersionUID = -3247420461850911549L;

    @Builder
    private LifecycleEvent(TenantId tenantId, UUID entityId, String serviceId,
                           UUID id, long ts,
                           String lcEventType, boolean success, String error) {
        super(tenantId, entityId, serviceId, id, ts);
        this.lcEventType = lcEventType;
        this.success = success;
        this.error = error;
    }

    @Getter
    private final String lcEventType;
    @Getter
    private final boolean success;
    @Getter
    @Setter
    private String error;

    @Override
    public EventType getType() {
        return EventType.LC_EVENT;
    }

    @Override
    public EventInfo toInfo(EntityType entityType) {
        EventInfo eventInfo = super.toInfo(entityType);
        var json = (ObjectNode) eventInfo.getBody();
        json.put("event", lcEventType)
                .put("success", success);
        if (error != null) {
            json.put("error", error);
        }
        return eventInfo;
    }

}
