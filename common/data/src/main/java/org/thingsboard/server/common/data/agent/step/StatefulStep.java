// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent.step;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.step.state.AgentAppStepState;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

public abstract class StatefulStep<T extends AgentAppStepState> extends AgentAppStep {

    /**
     * Template-defined state. Each field carries a value plus a {@code userChoice} flag (see {@link AgentAppStepState}).
     * Fields with {@code userChoice == true} are rendered for user input and required; the validator enforces a
     * corresponding stepInput in the event. Fields with {@code userChoice == false} are applied silently.
     * <p>
     * In command metadata resolution the resolvedState (from user stepInputs) is used when present; otherwise the
     * step's own template state values are the fallback.
     */
    public abstract @Nullable T getState();

    @Override
    @JsonIgnore
    public Map<String, String> getCommandMetadata(AgentApplication application, @Nullable AgentAppStepState resolvedState) {
        T base = getState();
        if (base != null) {
            return base.getCommandMetadata(resolvedState);
        }
        return resolvedState != null ? resolvedState.getCommandMetadata(null) : Collections.emptyMap();
    }
}
