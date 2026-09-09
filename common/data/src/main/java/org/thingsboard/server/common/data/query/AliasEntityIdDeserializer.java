// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
