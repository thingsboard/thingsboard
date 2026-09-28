// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLJsonPGObjectJsonbType;
import com.fasterxml.jackson.core.type.TypeReference;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.step.state.AgentAppStepState;
import org.thingsboard.server.common.data.agent.ProcessingStartStatus;
import org.thingsboard.server.common.data.agent.AgentProcessingStatus;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.Map;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.AGENT_APP_EVENT_TABLE_NAME)
public class AgentAppEventEntity extends BaseSqlEntity<AgentAppEvent> {

    @Column(name = ModelConstants.AGENT_APP_EVENT_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.AGENT_APP_EVENT_APPLICATION_ID_PROPERTY)
    private UUID applicationId;

    @Column(name = ModelConstants.AGENT_APP_EVENT_AGENT_ID_PROPERTY)
    private UUID agentId;

    @Column(name = ModelConstants.AGENT_APP_EVENT_APPLICATION_NAME_PROPERTY)
    private String applicationName;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.AGENT_APP_EVENT_ACTION_TYPE_PROPERTY)
    private AgentAppEventActionType actionType;

    @Column(name = ModelConstants.AGENT_APP_EVENT_AGENT_SCOPED_PROPERTY)
    private boolean agentScoped;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.AGENT_APP_EVENT_START_STATUS_PROPERTY)
    private ProcessingStartStatus startStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.AGENT_APP_EVENT_PROCESSING_STATUS_PROPERTY)
    private AgentProcessingStatus processingStatus;

    @Column(name = ModelConstants.AGENT_APP_EVENT_CURRENT_STEP_ID_PROPERTY)
    private UUID currentStepId;

    @Column(name = ModelConstants.AGENT_APP_EVENT_CURRENT_ACTIVITY_PROPERTY)
    private String currentActivity;

    @Column(name = ModelConstants.AGENT_APP_EVENT_ERROR_MESSAGE_PROPERTY)
    private String errorMessage;

    @Column(name = ModelConstants.AGENT_APP_EVENT_UPDATED_TIME_PROPERTY)
    private long updatedTime;

    @Convert(converter = JsonConverter.class)
    @JdbcType(PostgreSQLJsonPGObjectJsonbType.class)
    @Column(name = ModelConstants.AGENT_APP_EVENT_STEP_STATES_PROPERTY, columnDefinition = "jsonb")
    private JsonNode stepStates;

    @Column(name = ModelConstants.AGENT_APP_EVENT_BULK_ACTION_ID_PROPERTY)
    private UUID bulkActionId;

    @Convert(converter = JsonConverter.class)
    @JdbcType(PostgreSQLJsonPGObjectJsonbType.class)
    @Column(name = ModelConstants.AGENT_APP_EVENT_RESOLVED_ARGUMENTS_PROPERTY, columnDefinition = "jsonb")
    private JsonNode resolvedArguments;

    @Column(name = ModelConstants.AGENT_APP_EVENT_WINNER_CONTAINER_ID_PROPERTY)
    private String winnerContainerId;

    @Column(name = ModelConstants.AGENT_APP_EVENT_FINALIZE_DEADLINE_TS_PROPERTY)
    private Long finalizeDeadlineTs;

    @Convert(converter = JsonConverter.class)
    @JdbcType(PostgreSQLJsonPGObjectJsonbType.class)
    @Column(name = ModelConstants.AGENT_APP_EVENT_CONTEXT_METADATA_PROPERTY, columnDefinition = "jsonb")
    private JsonNode contextMetadata;

    public AgentAppEventEntity() {
        super();
    }

    public AgentAppEventEntity(AgentAppEvent event) {
        super(event);
        if (event.getTenantId() != null) {
            this.tenantId = event.getTenantId().getId();
        }
        if (event.getApplicationId() != null) {
            this.applicationId = event.getApplicationId().getId();
        }
        if (event.getAgentId() != null) {
            this.agentId = event.getAgentId().getId();
        }
        this.applicationName = event.getApplicationName();
        this.actionType = event.getActionType();
        this.agentScoped = event.getActionType() != null && event.getActionType().isAgentScoped();
        this.startStatus = event.getStartStatus();
        this.processingStatus = event.getProcessingStatus();
        this.currentStepId = event.getCurrentStepId();
        this.currentActivity = event.getCurrentActivity();
        this.errorMessage = event.getErrorMessage();
        this.updatedTime = event.getUpdatedTime();
        this.stepStates = JacksonUtil.convertValue(event.getStepStates(), JsonNode.class);
        this.bulkActionId = event.getBulkActionId();
        this.resolvedArguments = JacksonUtil.convertValue(event.getResolvedArguments(), JsonNode.class);
        this.winnerContainerId = event.getWinnerContainerId();
        this.finalizeDeadlineTs = event.getFinalizeDeadlineTs();
        this.contextMetadata = JacksonUtil.convertValue(event.getContextMetadata(), JsonNode.class);
    }

    public AgentAppEventEntity(AgentAppEventEntity entity) {
        super(entity);
        this.tenantId = entity.tenantId;
        this.applicationId = entity.applicationId;
        this.agentId = entity.agentId;
        this.applicationName = entity.applicationName;
        this.actionType = entity.actionType;
        this.agentScoped = entity.agentScoped;
        this.startStatus = entity.startStatus;
        this.processingStatus = entity.processingStatus;
        this.currentStepId = entity.currentStepId;
        this.currentActivity = entity.currentActivity;
        this.errorMessage = entity.errorMessage;
        this.updatedTime = entity.updatedTime;
        this.stepStates = entity.stepStates;
        this.bulkActionId = entity.bulkActionId;
        this.resolvedArguments = entity.resolvedArguments;
        this.winnerContainerId = entity.winnerContainerId;
        this.finalizeDeadlineTs = entity.finalizeDeadlineTs;
        this.contextMetadata = entity.contextMetadata;
    }

    @Override
    public AgentAppEvent toData() {
        AgentAppEvent event = new AgentAppEvent(new AgentAppEventId(id));
        event.setCreatedTime(createdTime);
        if (tenantId != null) {
            event.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (applicationId != null) {
            event.setApplicationId(new AgentApplicationId(applicationId));
        }
        if (agentId != null) {
            event.setAgentId(new AgentId(agentId));
        }
        event.setApplicationName(applicationName);
        event.setActionType(actionType);
        event.setStartStatus(startStatus);
        event.setProcessingStatus(processingStatus);
        event.setCurrentStepId(currentStepId);
        event.setCurrentActivity(currentActivity);
        event.setErrorMessage(errorMessage);
        event.setUpdatedTime(updatedTime);
        event.setStepStates(stepStates != null ? JacksonUtil.convertValue(stepStates, new TypeReference<Map<UUID, AgentAppStepState>>() {}) : null);
        event.setBulkActionId(bulkActionId);
        event.setResolvedArguments(resolvedArguments != null ? JacksonUtil.convertValue(resolvedArguments, new TypeReference<Map<String, String>>() {}) : null);
        event.setWinnerContainerId(winnerContainerId);
        event.setFinalizeDeadlineTs(finalizeDeadlineTs);
        event.setContextMetadata(contextMetadata != null ? JacksonUtil.convertValue(contextMetadata, new TypeReference<Map<String, String>>() {}) : null);
        return event;
    }
}
