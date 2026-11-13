// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent.step.state;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.agent.step.AgentAppStepType;
import org.thingsboard.server.exception.DataValidationException;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ComposeDownStepState extends ComposeServicesStepState {

    public static final String REMOVE_VOLUMES = "removeVolumes";

    private StepField<Boolean> removeVolumes;

    @Override
    public AgentAppStepType getType() {
        return AgentAppStepType.COMPOSE_DOWN;
    }

    @Override
    public void validate() throws DataValidationException {}

    @Override
    protected Map<String, StepField<?>> ownFields() {
        return Collections.singletonMap(REMOVE_VOLUMES, removeVolumes);
    }

    @Override
    public Map<String, String> getCommandMetadata(@Nullable AgentAppStepState overlay) {
        boolean remove = Boolean.TRUE.equals(effectiveValue(REMOVE_VOLUMES, overlay));
        return Map.of(REMOVE_VOLUMES, String.valueOf(remove));
    }

}
