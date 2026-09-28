// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent.step;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ComposeTypeChoiceStep extends AgentAppStep {

    private Map<String, JsonNode> composeTemplates;

    public ComposeTypeChoiceStep(UUID id, UUID nextId, String title) {
        super(nextId, id, title, true);
    }

    @Override
    public AgentAppStepType getType() {
        return AgentAppStepType.COMPOSE_TEMPLATE;
    }

    public Optional<JsonNode> getTemplateByType(String composeType) {
        if (composeTemplates == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(composeTemplates.get(composeType));
    }

    @JsonIgnore
    public Set<String> getComposeTypes() {
        return composeTemplates == null ? Collections.emptySet() : composeTemplates.keySet();
    }
}
