/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.rule.engine.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonBooleanSchema;
import dev.langchain4j.model.chat.request.json.JsonEnumSchema;
import dev.langchain4j.model.chat.request.json.JsonIntegerSchema;
import dev.langchain4j.model.chat.request.json.JsonNullSchema;
import dev.langchain4j.model.chat.request.json.JsonNumberSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts a Jackson {@link ObjectNode} JSON Schema into a Langchain4j {@link JsonSchema} model.
 */
final class Langchain4jJsonSchemaAdapter {

    private Langchain4jJsonSchemaAdapter() {
        throw new AssertionError("Can't instantiate utility class");
    }

    /**
     * Creates a Langchain4j {@link JsonSchema} from the given root JSON Schema node.
     *
     * @param rootSchemaNode a valid JSON Schema as a Jackson {@link ObjectNode}
     * @return the corresponding Langchain4j {@link JsonSchema}
     */
    public static JsonSchema fromObjectNode(ObjectNode rootSchemaNode) {
        return JsonSchema.builder()
                .name(rootSchemaNode.get("title").textValue())
                .rootElement(parse(rootSchemaNode))
                .build();
    }

    private static JsonSchemaElement parse(JsonNode schemaNode) {
        String description = schemaNode.hasNonNull("description") ? schemaNode.get("description").textValue() : null;

        if (schemaNode.has("enum")) { // enum schemas can be defined without 'type'
            return parseEnum(schemaNode).description(description).build();
        }

        String type = schemaNode.get("type").textValue();

        return switch (type) {
            case "string" -> JsonStringSchema.builder().description(description).build();
            case "integer" -> JsonIntegerSchema.builder().description(description).build();
            case "boolean" -> JsonBooleanSchema.builder().description(description).build();
            case "number" -> JsonNumberSchema.builder().description(description).build();
            case "null" -> new JsonNullSchema();
            case "object" -> parseObject(schemaNode).description(description).build();
            case "array" -> parseArray(schemaNode).description(description).build();
            default -> throw new IllegalArgumentException("Unsupported JSON Schema type: " + type);
        };
    }

    private static JsonEnumSchema.Builder parseEnum(JsonNode enumSchema) {
        var builder = new JsonEnumSchema.Builder();

        List<String> enumValues = new ArrayList<>();
        for (JsonNode element : enumSchema.get("enum")) {
            if (!element.isTextual()) {
                throw new IllegalArgumentException("Expected each 'enum' element to be a string, but found: " + element.getNodeType());
            }
            enumValues.add(element.textValue());
        }
        builder.enumValues(enumValues);

        return builder;
    }

    private static JsonObjectSchema.Builder parseObject(JsonNode objectSchema) {
        var builder = new JsonObjectSchema.Builder();

        JsonNode propertiesNode = objectSchema.get("properties");
        if (propertiesNode != null) {
            propertiesNode.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                builder.addProperty(key, parse(value));
            });
        }

        List<String> required = new ArrayList<>();
        JsonNode requiredNode = objectSchema.get("required");
        if (requiredNode != null) {
            for (JsonNode value : requiredNode) {
                required.add(value.textValue());
            }
        }
        builder.required(required);

        boolean additionalProperties = true; // default value if 'additionalProperties' is not set
        JsonNode additionalPropertiesNode = objectSchema.get("additionalProperties");
        if (additionalPropertiesNode != null) {
            if (!additionalPropertiesNode.isBoolean()) {
                throw new IllegalArgumentException("Expected 'additionalProperties' to be a boolean, but found: " + additionalPropertiesNode.getNodeType());
            }
            additionalProperties = additionalPropertiesNode.booleanValue();
        }
        builder.additionalProperties(additionalProperties);

        return builder;
    }

    private static JsonArraySchema.Builder parseArray(JsonNode arraySchema) {
        var builder = new JsonArraySchema.Builder();

        if (arraySchema.hasNonNull("items")) {
            builder.items(parse(arraySchema.get("items")));
        }

        return builder;
    }

}
