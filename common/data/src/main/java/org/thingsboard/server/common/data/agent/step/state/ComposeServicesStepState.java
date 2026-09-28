// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent.step.state;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NonNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public abstract class ComposeServicesStepState extends AgentAppStepState {

    public static final String SERVICES_IMAGE_REGEX_PATTERNS = "serviceImageRegexPatterns";

    @JsonProperty(SERVICES_IMAGE_REGEX_PATTERNS)
    private StepField<List<String>> servicesImagesRegexPatterns;

    @JsonIgnore
    @Override
    protected final @NonNull Map<String, StepField<?>> fields() {
        Map<String, StepField<?>> fields = new LinkedHashMap<>();
        fields.put(SERVICES_IMAGE_REGEX_PATTERNS, servicesImagesRegexPatterns);
        fields.putAll(ownFields());
        return fields;
    }

    @JsonIgnore
    protected abstract Map<String, StepField<?>> ownFields();
}
