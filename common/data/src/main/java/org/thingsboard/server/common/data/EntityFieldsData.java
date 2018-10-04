/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.*;
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
        if (current != null) {
            if (current.isValueNode()) {
                return current.asText();
            } else {
                try {
                    return mapper.writeValueAsString(current);
                } catch (JsonProcessingException e) {
                    return null;
                }
            }
        } else {
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

