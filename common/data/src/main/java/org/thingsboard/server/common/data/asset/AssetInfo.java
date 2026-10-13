// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.asset;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.EntityInfo;

import java.util.List;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class AssetInfo extends Asset {

    @Valid
    @Schema(description = "Owner name", accessMode = Schema.AccessMode.READ_ONLY)
    private String ownerName;

    @Valid
    @Schema(description = "Groups", accessMode = Schema.AccessMode.READ_ONLY)
    private List<EntityInfo> groups;

    public AssetInfo() {
        super();
    }

    public AssetInfo(Asset asset, String ownerName, List<EntityInfo> groups) {
        super(asset);
        this.ownerName = ownerName;
        this.groups = groups;
    }
}
