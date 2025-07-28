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

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;

import java.io.IOException;
import java.util.UUID;

public class AliasEntityIdDeserializer extends JsonDeserializer<AliasEntityId> {

    @Override
    public AliasEntityId deserialize(JsonParser jsonParser, DeserializationContext ctx) throws IOException, JacksonException {
        ObjectCodec oc = jsonParser.getCodec();
        ObjectNode node = oc.readTree(jsonParser);
        if (node.has("entityType")) {
            String entityType = node.get("entityType").asText();
            try {
                EntityType.valueOf(entityType);
                if (!node.has("id")) {
                    throw new IOException("Missing entityType or id!");
                }
                EntityId entityId = EntityIdFactory.getByTypeAndId(node.get("entityType").asText(), node.get("id").asText());
                return new AliasEntityIdImpl(entityId);
            } catch (IllegalArgumentException e) {
                AliasEntityType aliasEntityType = AliasEntityType.valueOf(entityType);
                UUID id = null;
                if (node.has("id")) {
                    id = UUID.fromString(node.get("id").asText());
                }
                return new AliasEntityIdImpl(aliasEntityType, id);
            }
        } else {
            throw new IOException("Missing entityType!");
        }
    }
}
