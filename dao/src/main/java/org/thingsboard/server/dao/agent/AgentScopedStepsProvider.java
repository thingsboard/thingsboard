// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent;

import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;

import java.util.List;

public interface AgentScopedStepsProvider {

    AgentAppEventActionType actionType();

    List<AgentAppStep> steps();
}
