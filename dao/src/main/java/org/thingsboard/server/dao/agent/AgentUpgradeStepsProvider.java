// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.agent.step.AgentFinalizeStep;
import org.thingsboard.server.common.data.agent.step.AgentPrepareStep;

import java.util.List;

@Component
public class AgentUpgradeStepsProvider implements AgentScopedStepsProvider {

    @Override
    public AgentAppEventActionType actionType() {
        return AgentAppEventActionType.AGENT_UPGRADE;
    }

    @Override
    public List<AgentAppStep> steps() {
        AgentPrepareStep prepare = new AgentPrepareStep();
        prepare.setId(AgentPrepareStep.STEP_ID);
        prepare.setNextId(AgentFinalizeStep.STEP_ID);
        prepare.setTitle("Prepare agent upgrade");
        AgentFinalizeStep finalize = new AgentFinalizeStep();
        finalize.setId(AgentFinalizeStep.STEP_ID);
        finalize.setTitle("Finalize agent upgrade");
        return List.of(prepare, finalize);
    }
}
