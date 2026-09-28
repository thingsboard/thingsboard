// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent;

import org.thingsboard.server.common.data.id.AgentId;

public class AgentSessionNotFoundException extends Exception {

    public AgentSessionNotFoundException(AgentId agentId) {
        super("Couldn't find agent session by id: " + agentId);
    }
}
