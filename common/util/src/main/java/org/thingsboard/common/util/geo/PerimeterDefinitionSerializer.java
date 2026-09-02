/**
 * Copyright © 2016-2026 The Thingsboard Authors
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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.thingsboard.server.common.data.StringUtils;

import java.io.IOException;

public class PerimeterDefinitionSerializer extends JsonSerializer<PerimeterDefinition> {

    @Override
    public void serialize(PerimeterDefinition value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value instanceof CirclePerimeterDefinition c) {
            gen.writeStartObject();
            gen.writeNumberField("latitude", c.getLatitude());
            gen.writeNumberField("longitude", c.getLongitude());
            gen.writeNumberField("radius", c.getRadius());
            gen.writeEndObject();
            return;
        }
        if (value instanceof PolygonPerimeterDefinition p) {
            String raw = p.getPolygonDefinition();
            if (StringUtils.isBlank(raw)) {
                throw new IOException("Failed to serialize PolygonPerimeterDefinition with blank: " + value);
            }
            ObjectMapper mapper = (ObjectMapper) gen.getCodec();
            gen.writeTree(mapper.readTree(raw));
            return;
        }
        throw new IOException("Failed to serialize PerimeterDefinition from value: " + value);
    }
}
