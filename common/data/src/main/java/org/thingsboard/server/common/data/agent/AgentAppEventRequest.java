// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.agent.step.state.AgentAppStepState;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.Map;
import java.util.UUID;

@Schema(description = "Request payload for creating an agent application event (install, update, upgrade, restart, delete, etc.).")
@Data
public class AgentAppEventRequest {

    @Schema(description = "Action to perform against the agent application.")
    private AgentAppEventActionType actionType;

    @Schema(description = "Agent application payload supplied by the caller. "
            + "Used to carry name/config changes for UPDATE and the target state for INSTALL/UPGRADE.")
    private AgentApplication application;

    @Schema(description = "Per-step input overrides keyed by step id (e.g. pullImages flag, backup volume selection).")
    private Map<UUID, AgentAppStepState> stepInputs;

    @Schema(description = "INSTALL-action optional related entity (Edge or Gateway Device) to link to the application in the "
            + "same operation. When set, the application is created and the relation is assigned atomically. "
            + "Ignored for non-INSTALL actions.")
    private EntityId relatedEntityId;

    @Schema(description = "Optional bulk-action correlation id. "
            + "When the same value is used across multiple application events, duplicates are deduplicated server-side.")
    private UUID bulkActionId;

    @Schema(description = "UPDATE-action flag for profile-managed apps. When true, the compose is not re-resolved from the "
            + "(possibly upgraded) profile — only the credentials carried by `application.config` are applied. "
            + "Ignored for non-UPDATE actions and for non-profile-managed apps.")
    private boolean skipProfileRefetch;
}
