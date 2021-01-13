package org.thingsboard.server.common.data.query;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

public class EntityKeyDeserializer extends JsonDeserializer<EntityKey> {

    @Override
    public EntityKey deserialize(JsonParser jsonParser, DeserializationContext ctx) throws IOException, JsonProcessingException {
        ObjectCodec oc = jsonParser.getCodec();
        ObjectNode node = oc.readTree(jsonParser);
        if (node.has("dataConversion")) {
            return new EntityKey(EntityKeyType.valueOf(node.get("type").asText()), node.get("key").asText(), node.get("dataConversion").asBoolean());
        } else {
            return new EntityKey(node.get("key").asText());
        }
    }

}