// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.rule;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.id.RuleChainId;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by igor on 3/13/18.
 */
@Schema
@Data
public class RuleChainMetaData implements HasVersion {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "JSON object with Rule Chain Id.", accessMode = Schema.AccessMode.READ_ONLY)
    private RuleChainId ruleChainId;

    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "Version of the Rule Chain")
    private Long version;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Index of the first rule node in the 'nodes' list")
    private Integer firstNodeIndex;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "List of rule node JSON objects")
    private List<RuleNode> nodes;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "List of JSON objects that represent connections between rule nodes")
    private List<NodeConnectionInfo> connections;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "List of JSON objects that represent connections between rule nodes and other rule chains.")
    private List<RuleChainConnectionInfo> ruleChainConnections;

    public void addConnectionInfo(int fromIndex, int toIndex, String type) {
        NodeConnectionInfo connectionInfo = new NodeConnectionInfo();
        connectionInfo.setFromIndex(fromIndex);
        connectionInfo.setToIndex(toIndex);
        connectionInfo.setType(type);
        if (connections == null) {
            connections = new ArrayList<>();
        }
        connections.add(connectionInfo);
    }
}
