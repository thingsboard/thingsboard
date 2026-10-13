// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.EntityType;

import java.util.UUID;

@Schema(allOf = EntityId.class)
public class SchedulerEventId extends UUIDBased implements EntityId {

    private static final long serialVersionUID = 1L;

    @JsonCreator
    public SchedulerEventId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static SchedulerEventId fromString(String schedulerEventId) {
        return new SchedulerEventId(UUID.fromString(schedulerEventId));
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, accessMode = Schema.AccessMode.READ_ONLY, description = "string", example = "SCHEDULER_EVENT", allowableValues = "SCHEDULER_EVENT")
    @Override
    public EntityType getEntityType() {
        return EntityType.SCHEDULER_EVENT;
    }
}
