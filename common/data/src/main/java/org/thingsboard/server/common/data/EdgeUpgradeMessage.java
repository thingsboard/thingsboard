// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
@Schema
public class EdgeUpgradeMessage implements Serializable {

    private static final long serialVersionUID = 2872965507642822989L;

    @Schema(description = "Mapping for upgrade versions and upgrade strategy (next ver).")
    private final Map<String, EdgeUpgradeInfo> edgeVersions;
}
