// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;

import java.io.IOException;

/**
 * Created by ashvayka on 01.06.18.
 */
@Data
@AllArgsConstructor
public class EntityFieldsData {

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        SimpleModule entityFieldsModule = new SimpleModule("EntityFieldsModule", new Version(1, 0, 0, null, null, null));
        entityFieldsModule.addSerializer(EntityId.class, new EntityIdFieldSerializer());
        mapper.disable(MapperFeature.USE_ANNOTATIONS);
        mapper.registerModule(entityFieldsModule);
    }

    private ObjectNode fieldsData;

    public EntityFieldsData(BaseData data) {
        fieldsData = mapper.valueToTree(data);
    }

    public String getFieldValue(String field) {
        return getFieldValue(field, false);
    }

    public String getFieldValue(String field, boolean ignoreNullStrings) {
        String[] fieldsTree = field.split("\\.");
        JsonNode current = fieldsData;
        for (String key : fieldsTree) {
            if (current.has(key)) {
                current = current.get(key);
            } else {
                current = null;
                break;
            }
        }
        if (current == null) {
            return null;
        }
        if (current.isNull() && ignoreNullStrings) {
            return null;
        }
        if (current.isValueNode()) {
            String textValue = current.asText();
            if (StringUtils.isEmpty(textValue) && ignoreNullStrings) {
                return null;
            }
            return textValue;
        }
        try {
            return mapper.writeValueAsString(current);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static class EntityIdFieldSerializer extends JsonSerializer<EntityId> {

        @Override
        public void serialize(EntityId value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeObject(value.getId());
        }
    }

}

