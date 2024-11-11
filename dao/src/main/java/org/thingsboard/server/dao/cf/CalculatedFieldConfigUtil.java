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
package org.thingsboard.server.dao.cf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.cf.BaseCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.SimpleCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CalculatedFieldConfigUtil {

//    public static CalculatedFieldConfiguration toCalculatedFieldConfig(JsonNode config, EntityType entityType, UUID entityId) {
//        if (config == null) {
//            return null;
//        }
//        try {
//            CalculatedFieldConfiguration calculatedFieldConfig = new BaseCalculatedFieldConfiguration();
//            Map<String, Argument> arguments = new HashMap<>();
//
//            JsonNode argumentsNode = config.get("arguments");
//            if (argumentsNode != null && argumentsNode.isObject()) {
//                argumentsNode.fields().forEachRemaining(entry -> {
//                    String key = entry.getKey();
//                    JsonNode argumentNode = entry.getValue();
//
//                    CalculatedFieldConfig.Argument argument = new CalculatedFieldConfig.Argument();
//                    if (argumentNode.has("entityType") && argumentNode.has("entityId")) {
//                        String referencedEntityType = argumentNode.get("entityType").asText();
//                        UUID referencedEntityId = UUID.fromString(argumentNode.get("entityId").asText());
//                        argument.setEntityId(EntityIdFactory.getByTypeAndUuid(referencedEntityType, referencedEntityId));
//                    } else {
//                        argument.setEntityId(EntityIdFactory.getByTypeAndUuid(entityType, entityId));
//                    }
//                    argument.setKey(argumentNode.get("key").asText());
//                    argument.setType(argumentNode.get("type").asText());
//
//                    if (argumentNode.has("defaultValue")) {
//                        argument.setDefaultValue(argumentNode.get("defaultValue").asInt());
//                    }
//
//                    arguments.put(key, argument);
//                });
//            }
//            calculatedFieldConfig.setArguments(arguments);
//
//            JsonNode outputNode = config.get("output");
//            if (outputNode != null) {
//                CalculatedFieldConfig.Output output = new CalculatedFieldConfig.Output();
//                output.setType(outputNode.get("type").asText());
//                output.setExpression(outputNode.get("expression").asText());
//                calculatedFieldConfig.setOutput(output);
//            }
//
//            return calculatedFieldConfig;
//
//        } catch (Exception e) {
//            throw new IllegalArgumentException("Failed to convert JsonNode to CalculatedFieldConfig", e);
//        }
//    }
//
//    public static JsonNode calculatedFieldConfigToJson(CalculatedFieldConfiguration calculatedFieldConfig, EntityType entityType, UUID entityId) {
//        if (calculatedFieldConfig == null) {
//            return null;
//        }
//        try {
//            ObjectNode configNode = JacksonUtil.newObjectNode();
//
//            ObjectNode argumentsNode = configNode.putObject("arguments");
//            calculatedFieldConfig.getArguments().forEach((key, argument) -> {
//                ObjectNode argumentNode = argumentsNode.putObject(key);
//                EntityId referencedEntityId = argument.getEntityId();
//                if (referencedEntityId != null) {
//                    argumentNode.put("entityType", referencedEntityId.getEntityType().name());
//                    argumentNode.put("entityId", referencedEntityId.getId().toString());
//                } else {
//                    argumentNode.put("entityType", entityType.name());
//                    argumentNode.put("entityId", entityId.toString());
//                }
//                argumentNode.put("key", argument.getKey());
//                argumentNode.put("type", argument.getType());
//                argumentNode.put("defaultValue", argument.getDefaultValue());
//            });
//
//            if (calculatedFieldConfig.getOutput() != null) {
//                ObjectNode outputNode = configNode.putObject("output");
//                outputNode.put("type", calculatedFieldConfig.getOutput().getType());
//                outputNode.put("expression", calculatedFieldConfig.getOutput().getExpression());
//            }
//
//            return configNode;
//
//        } catch (Exception e) {
//            throw new IllegalArgumentException("Failed to convert CalculatedFieldConfig to JsonNode", e);
//        }
//    }

}
