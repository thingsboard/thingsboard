// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

/**
 * Patch payload for {@code AgentAppEventService#updateStatus(...)}.
 * Any field left {@code null} is preserved in the database via COALESCE,
 * so callers only set what they want to change.
 */
@Value
@Builder
public class AgentAppEventStatusUpdate {

    AgentProcessingStatus processingStatus;
    UUID currentStepId;
    String currentActivity;
    String errorMessage;

}
