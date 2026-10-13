// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.edge;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.EntityInfo;

import java.util.List;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class EdgeInfo extends Edge {

    @Valid
    @Schema(description = "Owner name", accessMode = Schema.AccessMode.READ_ONLY)
    private String ownerName;

    @Valid
    @Schema(description = "Groups", accessMode = Schema.AccessMode.READ_ONLY)
    private List<EntityInfo> groups;

    public EdgeInfo() {
        super();
    }

    public EdgeInfo(Edge edge, String ownerName, List<EntityInfo> groups) {
        super(edge);
        this.ownerName = ownerName;
        this.groups = groups;
    }
}
