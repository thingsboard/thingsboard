// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent.step.state;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.thingsboard.server.common.data.agent.step.AgentAppStepType;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.exception.DataValidationException;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class RollBackStepState extends AgentAppStepState {

    public static final String FAILED_EVENT_ID = "failedEventId";
    public static final String FAILED_COMMAND_ID = "failedCommandId";

    private StepField<AgentAppEventId> failedEventId;

    public RollBackStepState(AgentAppEventId failedEventId) {
        this.failedEventId = new StepField<>(failedEventId, false);
    }

    @Override
    public AgentAppStepType getType() {
        return AgentAppStepType.ROLLBACK;
    }

    @Override
    public void validate() throws DataValidationException {
        if (failedEventId == null || failedEventId.getValue() == null) {
            throw new DataValidationException("Rollback event must have RollBackStepState with failedEventId!");
        }
    }

    @Override
    protected @NonNull Map<String, StepField<?>> fields() {
        return Collections.singletonMap(FAILED_EVENT_ID, failedEventId);
    }

    @Override
    public Map<String, String> getCommandMetadata(@Nullable AgentAppStepState overlay) {
        AgentAppEventId id = effectiveValue(FAILED_EVENT_ID, overlay);
        if (id == null) {
            return Collections.emptyMap();
        }
        return Map.of(FAILED_COMMAND_ID, id.getId().toString());
    }

}
