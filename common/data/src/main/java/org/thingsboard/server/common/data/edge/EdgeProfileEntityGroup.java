// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.edge;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityGroupId;

import java.io.Serial;
import java.io.Serializable;

@Schema
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EdgeProfileEntityGroup implements Serializable {

    @Serial
    private static final long serialVersionUID = 1170768270939859559L;

    @Schema(description = "Entity type of the group members: DEVICE, ASSET, ENTITY_VIEW, DASHBOARD or USER")
    private EntityType groupType;
    @Schema(description = "Id of the entity group")
    private EntityGroupId entityGroupId;

}
