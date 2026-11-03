// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

/**
 * Created by ashvayka on 11.05.17.
 */
public class EntityIdDeserializer extends JsonDeserializer<EntityId> {

    @Override
    public EntityId deserialize(JsonParser jsonParser, DeserializationContext ctx) throws IOException, JsonProcessingException {
        ObjectCodec oc = jsonParser.getCodec();
        ObjectNode node = oc.readTree(jsonParser);
        if (node.has("entityType") && node.has("id")) {
            return EntityIdFactory.getByTypeAndId(node.get("entityType").asText(), node.get("id").asText());
        } else {
            throw new IOException("Missing entityType or id!");
        }
    }

}
