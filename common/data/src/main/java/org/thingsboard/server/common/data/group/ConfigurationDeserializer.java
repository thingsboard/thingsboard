// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.group;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class ConfigurationDeserializer extends JsonDeserializer {

    @Override
    public JsonNode deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
        try {
            JsonNode node = jsonParser.readValueAsTree();
            if (node.isNull()) {
                return null;
            }
            return node;
        } catch (IOException e) {
             log.trace("Failed to deserialize JSON content into equivalent tree model during creating EntityGroup!", e);
             throw new RuntimeException("Failed to deserialize JSON content into equivalent tree model during creating EntityGroup!", e);
        }
    }
}