// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.agent.step.state.AgentAppStepState;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Map;
import java.util.UUID;

@Schema
@EqualsAndHashCode(callSuper = true)
@ToString
@Getter
@Setter
public class AgentAppEvent extends BaseData<AgentAppEventId> implements HasId<AgentAppEventId>, HasTenantId, TenantEntity {

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT_APP_EVENT;
    }

    private TenantId tenantId;
    private AgentApplicationId applicationId;
    private AgentId agentId;
    private String applicationName;
    private AgentAppEventActionType actionType;
    private ProcessingStartStatus startStatus;
    private AgentProcessingStatus processingStatus;
    private UUID currentStepId;
    private String currentActivity;
    private String errorMessage;
    private long updatedTime;
    private Map<UUID, AgentAppStepState> stepStates;
    private UUID bulkActionId;
    private Map<String, String> resolvedArguments;

    private String winnerContainerId;
    private Long finalizeDeadlineTs;
    private Map<String, String> contextMetadata;

    public AgentAppEvent() {
        super();
    }

    public AgentAppEvent(AgentAppEventId id) {
        super(id);
    }

    public AgentAppEvent(AgentAppEvent event) {
        super(event);
        this.tenantId = event.getTenantId();
        this.applicationId = event.getApplicationId();
        this.agentId = event.getAgentId();
        this.applicationName = event.getApplicationName();
        this.actionType = event.getActionType();
        this.startStatus = event.getStartStatus();
        this.processingStatus = event.getProcessingStatus();
        this.currentStepId = event.getCurrentStepId();
        this.currentActivity = event.getCurrentActivity();
        this.errorMessage = event.getErrorMessage();
        this.updatedTime = event.getUpdatedTime();
        this.stepStates = event.getStepStates();
        this.bulkActionId = event.getBulkActionId();
        this.resolvedArguments = event.getResolvedArguments();
        this.winnerContainerId = event.getWinnerContainerId();
        this.finalizeDeadlineTs = event.getFinalizeDeadlineTs();
        this.contextMetadata = event.getContextMetadata();
    }

    @Schema(description = "JSON object with the Agent App Event Id.")
    @Override
    public AgentAppEventId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the event creation, in milliseconds", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    public boolean hasActionType(AgentAppEventActionType type) {
        return actionType != null && actionType == type;
    }
}
