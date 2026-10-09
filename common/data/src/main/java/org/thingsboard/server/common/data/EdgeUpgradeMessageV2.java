// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@Schema
public class EdgeUpgradeMessageV2 implements Serializable {

    private static final long serialVersionUID = -6647214126993298281L;

    @Schema(description = "Mapping of edge version to the list of available upgrade options (next ver + strategy).")
    private final Map<String, List<EdgeUpgradeInfo>> edgeVersions;
}
