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
package org.thingsboard.common.util.geo;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class PerimeterDefinitionDeserializer extends JsonDeserializer<PerimeterDefinition> {

    @Override
    public PerimeterDefinition deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
        ObjectCodec codec = p.getCodec();
        JsonNode node = codec.readTree(p);

        if (node.isObject()) {
            double latitude = node.get("latitude").asDouble();
            double longitude = node.get("longitude").asDouble();
            double radius = node.get("radius").asDouble();
            return new CirclePerimeterDefinition(latitude, longitude, radius);
        }
        if (node.isArray()) {
            ObjectMapper mapper = (ObjectMapper) p.getCodec();
            String polygonStrDefinition = mapper.writeValueAsString(node);
            return new PolygonPerimeterDefinition(polygonStrDefinition);
        }
        throw new IOException("Failed to deserialize PerimeterDefinition from node: " + node);
    }

}
