/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
        if (node.has("type") && node.has("key")) {
            if (node.has("dataConversion")) {
                return new EntityKey(EntityKeyType.valueOf(node.get("type").asText()), node.get("key").asText(), node.get("dataConversion").asBoolean());
            } else {
                return new EntityKey(EntityKeyType.valueOf(node.get("type").asText()), node.get("key").asText());
            }
        } else {
            throw new IOException("Missing type or key!");
        }
    }

}