// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.EntityType;

import java.util.UUID;

public class QueueStatsId extends UUIDBased implements EntityId {

    private static final long serialVersionUID = 1L;

    @JsonCreator
    public QueueStatsId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static QueueStatsId fromString(String queueId) {
        return new QueueStatsId(UUID.fromString(queueId));
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "string", example = "QUEUE_STATS", allowableValues = "QUEUE_STATS")
    @Override
    public EntityType getEntityType() {
        return EntityType.QUEUE_STATS;
    }
}
