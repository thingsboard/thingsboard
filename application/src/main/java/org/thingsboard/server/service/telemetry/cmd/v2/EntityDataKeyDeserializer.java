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
package org.thingsboard.server.service.telemetry.cmd.v2;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.io.IOException;

public class EntityDataKeyDeserializer extends JsonDeserializer<EntityDataKey> {

    @Override
    public EntityDataKey deserialize(JsonParser jsonParser, DeserializationContext ctx) throws IOException, JsonProcessingException {
        ObjectCodec oc = jsonParser.getCodec();
        TreeNode treeNode = oc.readTree(jsonParser);
        boolean isObject = treeNode.isObject();
        if (isObject) {
            ObjectNode node = (ObjectNode) treeNode;
            if (node.has("key") && node.has("dataConversion")) {
                return new EntityDataKey(node.get("key").asText(), node.get("dataConversion").asBoolean());
            } else {
                throw new IOException("Missing key or dataConversion!");
            }
        } else {
            return new EntityDataKey(((TextNode) treeNode).asText(""));
        }
    }

}