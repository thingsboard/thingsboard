/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.common.data.cf.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.cf.CalculatedFieldLinkConfiguration;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
public abstract class BaseCalculatedFieldConfiguration implements CalculatedFieldConfiguration {

    @JsonIgnore
    private final ObjectMapper mapper = new ObjectMapper();

    protected Map<String, Argument> arguments;
    protected String expression;
    protected Output output;

    public BaseCalculatedFieldConfiguration() {
    }

    public BaseCalculatedFieldConfiguration(JsonNode config, EntityType entityType, UUID entityId) {
        BaseCalculatedFieldConfiguration calculatedFieldConfig = toCalculatedFieldConfig(config, entityType, entityId);
        this.arguments = calculatedFieldConfig.getArguments();
        this.expression = calculatedFieldConfig.getExpression();
        this.output = calculatedFieldConfig.getOutput();
    }

    @Override
    public List<EntityId> getReferencedEntities() {
        return arguments.values().stream()
                .map(Argument::getEntityId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public CalculatedFieldLinkConfiguration getReferencedEntityConfig(EntityId entityId) {
        CalculatedFieldLinkConfiguration linkConfiguration = new CalculatedFieldLinkConfiguration();

        arguments.entrySet().stream()
                .filter(entry -> entry.getValue().getEntityId().equals(entityId))
                .forEach(entry -> {
                    Argument argument = entry.getValue();
                    String argumentKey = entry.getKey();

                    switch (argument.getType()) {
                        case ATTRIBUTE -> {
                            switch (argument.getScope()) {
                                case CLIENT_SCOPE -> linkConfiguration.getClientAttributes().put(entry.getKey(), argument.getKey());
                                case SERVER_SCOPE -> linkConfiguration.getServerAttributes().put(entry.getKey(), argument.getKey());
                                case SHARED_SCOPE -> linkConfiguration.getSharedAttributes().put(entry.getKey(), argument.getKey());
                            }
                        }
                        case TS_LATEST, TS_ROLLING ->
                                linkConfiguration.getTimeSeries().put(argumentKey, argument.getKey());
                    }
                });

        return linkConfiguration;
    }

    @Override
    public JsonNode calculatedFieldConfigToJson(EntityType entityType, UUID entityId) {
        ObjectNode configNode = mapper.createObjectNode();

        ObjectNode argumentsNode = configNode.putObject("arguments");
        arguments.forEach((key, argument) -> {
            ObjectNode argumentNode = argumentsNode.putObject(key);
            EntityId referencedEntityId = argument.getEntityId();
            if (referencedEntityId != null) {
                argumentNode.put("entityType", referencedEntityId.getEntityType().name());
                argumentNode.put("entityId", referencedEntityId.getId().toString());
            } else {
                argumentNode.put("entityType", entityType.name());
                argumentNode.put("entityId", entityId.toString());
            }
            argumentNode.put("key", argument.getKey());
            argumentNode.put("type", String.valueOf(argument.getType()));
            argumentNode.put("scope", String.valueOf(argument.getScope()));
            argumentNode.put("defaultValue", argument.getDefaultValue());
            argumentNode.put("limit", String.valueOf(argument.getLimit()));
            argumentNode.put("timeWindow", String.valueOf(argument.getTimeWindow()));
        });

        if (expression != null) {
            configNode.put("expression", expression);
        }

        if (output != null) {
            ObjectNode outputNode = configNode.putObject("output");
            outputNode.put("name", output.getName());
            outputNode.put("type", String.valueOf(output.getType()));
            if (output.getScope() != null) {
                outputNode.put("scope", String.valueOf(output.getScope()));
            }
        }

        return configNode;
    }

    private BaseCalculatedFieldConfiguration toCalculatedFieldConfig(JsonNode config, EntityType entityType, UUID entityId) {
        if (config == null || !config.isObject()) {
            return null;
        }

        Map<String, Argument> arguments = new HashMap<>();
        JsonNode argumentsNode = config.get("arguments");
        if (argumentsNode != null && argumentsNode.isObject()) {
            argumentsNode.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode argumentNode = entry.getValue();
                Argument argument = new Argument();
                if (argumentNode.hasNonNull("entityType") && argumentNode.hasNonNull("entityId")) {
                    String referencedEntityType = argumentNode.get("entityType").asText();
                    UUID referencedEntityId = UUID.fromString(argumentNode.get("entityId").asText());
                    argument.setEntityId(EntityIdFactory.getByTypeAndUuid(referencedEntityType, referencedEntityId));
                } else {
                    argument.setEntityId(EntityIdFactory.getByTypeAndUuid(entityType, entityId));
                }
                argument.setKey(argumentNode.get("key").asText());
                JsonNode type = argumentNode.get("type");
                if (type != null && !type.isNull() && !type.asText().equals("null")) {
                    argument.setType(ArgumentType.valueOf(type.asText()));
                }
                JsonNode scope = argumentNode.get("scope");
                if (scope != null && !scope.isNull() && !scope.asText().equals("null")) {
                    argument.setScope(AttributeScope.valueOf(scope.asText()));
                }
                if (argumentNode.hasNonNull("defaultValue")) {
                    argument.setDefaultValue(argumentNode.get("defaultValue").asText());
                }
                if (argumentNode.hasNonNull("limit")) {
                    argument.setLimit(argumentNode.get("limit").asInt());
                }
                if (argumentNode.hasNonNull("timeWindow")) {
                    argument.setTimeWindow(argumentNode.get("timeWindow").asInt());
                }
                arguments.put(key, argument);
            });
        }
        this.setArguments(arguments);

        JsonNode expressionNode = config.get("expression");
        if (expressionNode != null && expressionNode.isTextual()) {
            this.setExpression(expressionNode.asText());
        }

        JsonNode outputNode = config.get("output");
        if (outputNode != null) {
            Output output = new Output();
            output.setName(outputNode.get("name").asText());
            JsonNode type = outputNode.get("type");
            if (type != null && !type.isNull() && !type.asText().equals("null")) {
                output.setType(OutputType.valueOf(type.asText()));
            }
            JsonNode scope = outputNode.get("scope");
            if (scope != null && !scope.isNull() && !scope.asText().equals("null")) {
                output.setScope(AttributeScope.valueOf(scope.asText()));
            }
            this.setOutput(output);
        }

        return this;
    }

}
