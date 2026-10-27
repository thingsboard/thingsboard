// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent;

import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.gen.agent.v1.ServerToAgent;
import org.thingsboard.server.gen.transport.TransportProtos.LogStreamRequestProto;

public interface AgentRpcService {

    boolean push(AgentId agentId, ServerToAgent msg) throws AgentSessionNotFoundException;
    void processLogStreamRequest(LogStreamRequestProto req);
    void stopLogStream(AgentId agentId, String projectName, String unitIdentifier);
}
