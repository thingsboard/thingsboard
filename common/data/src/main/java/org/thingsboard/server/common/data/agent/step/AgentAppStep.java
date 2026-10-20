// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent.step;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.step.state.AgentAppStepState;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes({
        @JsonSubTypes.Type(name = "COMPOSE_TEMPLATE", value = ComposeTypeChoiceStep.class),
        @JsonSubTypes.Type(name = "COMPOSE", value = ComposeStep.class),
        @JsonSubTypes.Type(name = "COMPOSE_START", value = ComposeStartStep.class),
        @JsonSubTypes.Type(name = "COMPOSE_DOWN", value = ComposeDownStep.class),
        @JsonSubTypes.Type(name = "COMPOSE_RESTART", value = ComposeRestartStep.class),
        @JsonSubTypes.Type(name = "ROLLBACK", value = RollBackStep.class),
        @JsonSubTypes.Type(name = "BACKUP_VOLUME", value = BackupVolumesStep.class),
        @JsonSubTypes.Type(name = "BACKUP_VOLUME_REMOVE", value = BackupVolumesRemoveStep.class),
        @JsonSubTypes.Type(name = "RUN_JOB", value = RunJobStep.class),
        @JsonSubTypes.Type(name = "AGENT_PREPARE", value = AgentPrepareStep.class),
        @JsonSubTypes.Type(name = "AGENT_FINALIZE", value = AgentFinalizeStep.class),
})
@Data
@NoArgsConstructor
public abstract class AgentAppStep {

    protected UUID id;
    protected UUID nextId;
    protected String title;
    protected boolean templateOnly;
    /**
     * Optional name of a boolean variable in the materialization context (see {@code TemplateVarSubstitutor}).
     * When set, the step is included only if that variable resolves to {@code true}; otherwise the materializer
     * drops the step and re-stitches the {@code nextId} chain. {@code null} means the step is always included.
     */
    protected String condition;

    public AgentAppStep(UUID nextId, UUID id, String title, boolean templateOnly) {
        this.nextId = nextId;
        this.id = id;
        this.title = title;
        this.templateOnly = templateOnly;
    }

    public abstract AgentAppStepType getType();

    public boolean isStateful() {
        return this instanceof StatefulStep<?> ss && ss.getState() != null;
    }

    @JsonIgnore
    public Map<String, String> getCommandMetadata(AgentApplication application, @Nullable AgentAppStepState resolvedState) {
        return Collections.emptyMap();
    }
}
