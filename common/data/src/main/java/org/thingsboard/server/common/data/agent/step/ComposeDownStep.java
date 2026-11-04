// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent.step;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.agent.step.state.ComposeDownStepState;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ComposeDownStep extends ComposeServicesStep<ComposeDownStepState> {

    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    private ComposeDownStepState state;

    @Override
    public AgentAppStepType getType() {
        return AgentAppStepType.COMPOSE_DOWN;
    }

}
