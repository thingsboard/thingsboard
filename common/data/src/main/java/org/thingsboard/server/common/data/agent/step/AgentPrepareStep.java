// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent.step;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AgentPrepareStep extends AgentAppStep {

    public static final UUID STEP_ID = UUID.fromString("a9e17001-0000-4000-8000-000000000001");

    @Override
    public AgentAppStepType getType() {
        return AgentAppStepType.AGENT_PREPARE;
    }
}
