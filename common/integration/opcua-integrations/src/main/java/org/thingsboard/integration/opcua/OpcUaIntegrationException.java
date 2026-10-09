// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.opcua;

import java.util.Optional;

public class OpcUaIntegrationException extends Exception {

    private final OpcUaNode node;

    public OpcUaIntegrationException(OpcUaNode node, String message, Exception e) {
        super(message, e.getCause());
        this.node = node;
    }

    public OpcUaIntegrationException(String message, Exception e) {
        super(message, e.getCause());
        this.node = null;
    }

    public OpcUaIntegrationException(String message) {
        super(message);
        this.node = null;
    }

    public Optional<OpcUaNode> getNode() {
        return Optional.ofNullable(node);
    }
}
