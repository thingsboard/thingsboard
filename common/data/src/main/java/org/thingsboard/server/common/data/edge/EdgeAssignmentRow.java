// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.edge;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;

@Schema
@Data
public class EdgeAssignmentRow {
    private EntityId id;
    private String name;
    @Schema(description = "Entity groups only: type of the group members")
    private EntityType groupType;
    private EntityId ownerId;
    private String ownerName;
    @Schema(description = "Rule chains only: true when this chain is the root")
    private boolean root;
    @Schema(description = "Edge view only: PROFILE when the edge's profile lists this row, EDGE otherwise")
    private EdgeAssignmentSource source;
}
