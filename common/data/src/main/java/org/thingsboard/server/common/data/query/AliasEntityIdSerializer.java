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
package org.thingsboard.server.common.data.query;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.UUID;

public class AliasEntityIdSerializer extends JsonSerializer<AliasEntityId> {
    @Override
    public void serialize(AliasEntityId value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        String entityType;
        if (value.isAliasEntityId()) {
            entityType = value.getAliasEntityType().name();
        } else {
            entityType = value.getEntityType().name();
        }
        gen.writeStringField("entityType", entityType);
        UUID id = null;
        if (value.getId() != null) {
            id = value.getId();
        } else if (value.defaultEntityId() != null) {
            id = value.defaultEntityId().getId();
        }
        if (id != null) {
            gen.writeStringField("id", id.toString());
        }
        gen.writeEndObject();
    }
}
