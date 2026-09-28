// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.install;

import jakarta.servlet.http.HttpServletRequest;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentInstructions;
import org.thingsboard.server.common.data.agent.AgentProfile;

public interface AgentInstallInstructionsService {

    AgentInstructions getInstallInstructions(Agent agent, String method, HttpServletRequest request);

    AgentInstructions getProvisionInstructions(AgentProfile profile, String method, HttpServletRequest request);

}
