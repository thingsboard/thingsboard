// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.rule;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Created by ashvayka on 21.03.18.
 */
@Schema
@Data
public class NodeConnectionInfo {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Index of rule node in the 'nodes' array of the RuleChainMetaData. Indicates the 'from' part of the connection.")
    private int fromIndex;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Index of rule node in the 'nodes' array of the RuleChainMetaData. Indicates the 'to' part of the connection.")
    private int toIndex;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Type of the relation. Typically indicated the result of processing by the 'from' rule node. For example, 'Success' or 'Failure'")
    private String type;
}
