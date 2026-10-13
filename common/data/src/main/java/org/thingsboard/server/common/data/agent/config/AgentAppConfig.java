// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes({
        @JsonSubTypes.Type(name = "DOCKER_COMPOSE", value = DockerComposeConfig.class)
})
@Data
@NoArgsConstructor
public abstract class AgentAppConfig {

    private static final Pattern ARGUMENT_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_]+");

    @Valid
    private List<AgentAppArgument> arguments;

    public abstract AgentAppConfigType getType();

    public abstract AgentAppConfig copy();

    public static AgentAppConfig forType(AgentAppConfigType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case DOCKER_COMPOSE -> new DockerComposeConfig();
        };
    }

    public abstract String getEdgeRoutingKey();

    public abstract void validate();

    public abstract void validateForProfile(AgentApplicationType appType);

    protected List<AgentAppArgument> copyArguments() {
        return arguments == null ? null
                : arguments.stream()
                  .map(AgentAppArgument::new)
                  .collect(Collectors.toCollection(ArrayList::new));
    }

    protected void validateArguments() {
        if (arguments == null || arguments.isEmpty()) {
            return;
        }
        Set<String> names = new HashSet<>();
        for (AgentAppArgument argument : arguments) {
            String name = argument.getName();
            if (name == null || name.isBlank()) {
                throw new DataValidationException("Custom argument name must be specified!");
            }
            if (!ARGUMENT_NAME_PATTERN.matcher(name).matches()) {
                throw new DataValidationException("Custom argument name '" + name
                        + "' must contain only letters, digits and underscores!");
            }
            if (!names.add(name)) {
                throw new DataValidationException("Duplicate custom argument name: " + name);
            }
            AgentAppArgumentSource sourceType = argument.getSourceType();
            if (sourceType == null) {
                throw new DataValidationException("Custom argument '" + name + "' source type must be specified!");
            }
            if (sourceType.isConcreteEntityRef()) {
                EntityId sourceEntityId = argument.getSourceEntityId();
                if (sourceEntityId == null) {
                    throw new DataValidationException("Custom argument '" + name + "' source entity id must be specified!");
                }
                if (sourceEntityId.getEntityType() != sourceType.getSearchEntityType()) {
                    throw new DataValidationException("Custom argument '" + name + "' source entity id type must be "
                            + sourceType.getSearchEntityType() + "!");
                }
            }
            if (argument.getValueType() == null) {
                throw new DataValidationException("Custom argument '" + name + "' value type must be specified!");
            }
            if (argument.getKey() == null || argument.getKey().isBlank()) {
                throw new DataValidationException("Custom argument '" + name + "' key must be specified!");
            }
        }
    }

    /**
     * Compare two configs for equality while ignoring the credential env vars
     * declared by the given app type. Used by service/validator paths that
     * allow rotating credentials independently of "real" config edits.
     */
    public static boolean equalsIgnoringCreds(AgentApplicationType appType, AgentAppConfig a, AgentAppConfig b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (!(a instanceof DockerComposeConfig aDc) || !(b instanceof DockerComposeConfig bDc)) {
            return Objects.equals(a, b);
        }
        if (!argumentsEqual(aDc.getArguments(), bDc.getArguments())) {
            return false;
        }
        if (appType == null) {
            return Objects.equals(aDc.getCompose(), bDc.getCompose());
        }
        return DockerComposeUtils.equalsIgnoringEnvKeys(
                aDc.getCompose(), bDc.getCompose(),
                appType.getMainImagePattern(), appType.getCredentialEnvKeys());
    }

    private static boolean argumentsEqual(List<AgentAppArgument> a, List<AgentAppArgument> b) {
        return normalizeArguments(a).equals(normalizeArguments(b));
    }

    private static List<AgentAppArgument> normalizeArguments(List<AgentAppArgument> arguments) {
        return arguments == null ? List.of() : arguments;
    }
}
