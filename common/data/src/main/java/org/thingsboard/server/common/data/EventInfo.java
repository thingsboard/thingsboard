// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EventId;
import org.thingsboard.server.common.data.id.TenantId;

/**
 * @author Andrew Shvayka
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema
public class EventInfo extends BaseData<EventId> {

    @Schema(description = "JSON object with Tenant Id.", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;
    @Schema(description = "Event type", example = "STATS")
    private String type;
    @Schema(description = "string", example = "784f394c-42b6-435a-983c-b7beff2784f9")
    private String uid;
    @Schema(description = "JSON object with Entity Id for which event is created.", accessMode = Schema.AccessMode.READ_ONLY)
    private EntityId entityId;
    @Schema(description = "Event body.",implementation = com.fasterxml.jackson.databind.JsonNode.class)
    private transient JsonNode body;

    public EventInfo() {
        super();
    }

    public EventInfo(EventId id) {
        super(id);
    }

    public EventInfo(EventInfo event) {
        super(event);
    }

    @Schema(description = "Timestamp of the event creation, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }
}
