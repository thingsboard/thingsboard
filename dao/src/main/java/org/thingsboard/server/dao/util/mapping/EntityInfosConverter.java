// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.util.mapping;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Converter
public class EntityInfosConverter implements AttributeConverter<List<EntityInfo>, String> {

    @Override
    public String convertToDatabaseColumn(List<EntityInfo> attribute) {
        throw new IllegalArgumentException("Not implemented!");
    }

    @Override
    public List<EntityInfo> convertToEntityAttribute(String s) {
        try {
            JsonNode node = JacksonUtil.fromBytes(s.getBytes(StandardCharsets.UTF_8));
            if (node.isArray()) {
                List<EntityInfo> groups = new ArrayList<>();
                for (int i = 0; i < node.size(); i++) {
                    JsonNode row = node.get(i);
                    UUID id = null;
                    String name = null;
                    JsonNode idNode = row.get("id");
                    JsonNode nameNode = row.get("name");
                    if (idNode != null && nameNode != null) {
                        try {
                            id = UUID.fromString(idNode.asText());
                        } catch (Exception ignored) {
                        }
                        name = nameNode.asText();
                    }
                    if (id != null && name != null) {
                        groups.add(new EntityInfo(id, EntityType.ENTITY_GROUP.name(), name));
                    }
                }
                return groups;
            } else {
                return Collections.emptyList();
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to convert String to Groups list: " + ex.getMessage(), ex);
        }
    }

}
