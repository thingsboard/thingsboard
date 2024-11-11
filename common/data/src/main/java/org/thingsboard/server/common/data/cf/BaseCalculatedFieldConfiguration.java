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
package org.thingsboard.server.common.data.cf;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
public abstract class BaseCalculatedFieldConfiguration implements CalculatedFieldConfiguration {

    @JsonIgnore
    private final ObjectMapper mapper = new ObjectMapper();

    protected Map<String, Argument> arguments;
    protected SimpleCalculatedFieldConfiguration.Output output;

    public BaseCalculatedFieldConfiguration() {
    }

    public BaseCalculatedFieldConfiguration(JsonNode config, EntityType entityType, UUID entityId) {
        BaseCalculatedFieldConfiguration calculatedFieldConfig = toCalculatedFieldConfig(config, entityType, entityId);
        this.arguments = calculatedFieldConfig.getArguments();
        this.output =  calculatedFieldConfig.getOutput();
    }

    @Override
    public List<EntityId> getReferencedEntities() {
        return arguments.values().stream()
                .map(SimpleCalculatedFieldConfiguration.Argument::getEntityId)
                .collect(Collectors.toList());
    }

    @Override
    public CalculatedFiledLinkConfiguration getReferencedEntityConfig(EntityId entityId) {
        CalculatedFiledLinkConfiguration linkConfiguration = new CalculatedFiledLinkConfiguration();
        arguments.values().stream()
                .filter(argument -> argument.getEntityId().equals(entityId))
                .forEach(argument -> {
                    switch (argument.getType()) {
                        case "ATTRIBUTES":
                            linkConfiguration.getAttributes().add(argument.getKey());
                            break;
                        case "TIME_SERIES":
                            linkConfiguration.getTimeSeries().add(argument.getKey());
                            break;
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
            argumentNode.put("type", argument.getType());
            argumentNode.put("defaultValue", argument.getDefaultValue());
        });

        if (output != null) {
            ObjectNode outputNode = configNode.putObject("output");
            outputNode.put("type", output.getType());
            outputNode.put("expression", output.getExpression());
        }

        return configNode;
    }

    @Data
    public static class Argument {
        private EntityId entityId;
        private String key;
        private String type;
        private String defaultValue;
    }

    @Data
    public static class Output {
        private String type;
        private String expression;
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
                argument.setType(argumentNode.get("type").asText());
                argument.setDefaultValue(argumentNode.get("defaultValue").asText());
                arguments.put(key, argument);
            });
        }
        this.setArguments(arguments);

        JsonNode outputNode = config.get("output");
        if (outputNode != null) {
            Output output = new Output();
            output.setType(outputNode.get("type").asText());
            output.setExpression(outputNode.get("expression").asText());
            this.setOutput(output);
        }

        return this;
    }

}
