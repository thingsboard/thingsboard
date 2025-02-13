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
package org.thingsboard.server.dao.util.mapping;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.AttributeConverter;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public abstract class AbstractEntityInfosConverter implements AttributeConverter<List<EntityInfo>, String> {

    protected abstract EntityType getEntityType();

    @Override
    public String convertToDatabaseColumn(List<EntityInfo> attribute) {
        throw new IllegalArgumentException("Not implemented!");
    }

    @Override
    public List<EntityInfo> convertToEntityAttribute(String s) {
        try {
            JsonNode node = JacksonUtil.fromBytes(s.getBytes(StandardCharsets.UTF_8));
            if (node.isArray()) {
                List<EntityInfo> entities = new ArrayList<>();
                for (int i = 0; i < node.size(); i++) {
                    JsonNode row = node.get(i);
                    UUID id = null;
                    String name = null;
                    JsonNode idNode = row.get("id");
                    JsonNode nameNode = row.get("name");
                    if (idNode != null && nameNode != null) {
                        try {
                            id = UUID.fromString(idNode.asText());
                        } catch (Exception ignored) {}
                        name = nameNode.asText();
                    }
                    if (id != null && name != null) {
                        entities.add(new EntityInfo(id, EntityType.WIDGETS_BUNDLE.name(), name));
                    }
                }
                return entities;
            } else {
                return Collections.emptyList();
            }
        } catch (Exception ex) {
            String exception = String.format("Failed to convert String to %s list: %s", getEntityType(), ex.getMessage());
            throw new RuntimeException(exception, ex);
        }
    }

}
