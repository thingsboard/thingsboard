// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Collections;
import java.util.List;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class DashboardInfo extends Dashboard {

    @Valid
    @Schema(description = "Owner name", accessMode = Schema.AccessMode.READ_ONLY)
    private String ownerName;

    @Valid
    @Schema(description = "Groups", accessMode = Schema.AccessMode.READ_ONLY)
    private List<EntityInfo> groups;

    public DashboardInfo() {
        super();
    }

    public DashboardInfo(Dashboard dashboard) {
        super(dashboard);
        this.groups = Collections.emptyList();
    }

    public DashboardInfo(Dashboard dashboard, String ownerName, List<EntityInfo> groups) {
        super(dashboard);
        this.ownerName = ownerName;
        this.groups = groups;
    }

}
