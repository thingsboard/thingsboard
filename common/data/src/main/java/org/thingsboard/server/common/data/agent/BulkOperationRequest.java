// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent;

import lombok.Data;
import org.thingsboard.server.common.data.agent.step.state.AgentAppStepState;

import java.util.Map;
import java.util.UUID;

@Data
public class BulkOperationRequest {

    private AgentAppEventActionType actionType;
    private Map<UUID, AgentAppStepState> stepInputs;
}
