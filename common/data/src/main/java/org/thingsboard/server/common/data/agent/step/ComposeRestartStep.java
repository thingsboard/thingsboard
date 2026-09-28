// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent.step;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import static org.thingsboard.server.common.data.agent.step.AgentAppStepType.COMPOSE_RESTART;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ComposeRestartStep extends AgentAppStep {

    @Override
    public AgentAppStepType getType() {
        return COMPOSE_RESTART;
    }
}
