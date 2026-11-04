// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.opcua;

import lombok.Data;
import lombok.ToString;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.thingsboard.server.common.data.StringUtils;

/**
 * Created by Valerii Sosliuk on 4/27/2018.
 */
@Data
@ToString(exclude = "parent")
public class OpcUaNode {

    private final NodeId nodeId;
    private final OpcUaNode parent;
    private final String name;
    private final String fqn;

    public OpcUaNode(NodeId nodeId, String name) {
        this(null, nodeId, name);
    }

    public OpcUaNode(OpcUaNode parent, NodeId nodeId, String name) {
        this.parent = parent;
        this.nodeId = nodeId;
        this.name = name;
        this.fqn = ((parent != null && !StringUtils.isEmpty(parent.getFqn())) ? parent.getFqn() + "." : "") + name;
    }

}
