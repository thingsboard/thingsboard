// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
