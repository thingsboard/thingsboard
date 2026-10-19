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
public class ComposeStepState extends ComposeServicesStepState {

    public static final String PULL_IMAGES = "pullImages";

    private StepField<Boolean> pullImages;

    @Override
    public AgentAppStepType getType() {
        return AgentAppStepType.COMPOSE;
    }

    @Override
    public void validate() throws DataValidationException {}

    @Override
    protected Map<String, StepField<?>> ownFields() {
        return Collections.singletonMap(PULL_IMAGES, pullImages);
    }

    @Override
    public Map<String, String> getCommandMetadata(@Nullable AgentAppStepState overlay) {
        boolean pull = Boolean.TRUE.equals(effectiveValue(PULL_IMAGES, overlay));
        return Map.of(PULL_IMAGES, String.valueOf(pull));
    }
}
