/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.gateway.validator;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.Map;

public class TrackingDeserializer<T> extends StdDeserializer<T> {

    private final Class<T> targetClass;
    private final FieldLocationTrackingParser fieldLocationTrackingParser;

    public TrackingDeserializer(Class<T> targetClass, FieldLocationTrackingParser fieldLocationTrackingParser) {
        super(targetClass);
        this.targetClass = targetClass;
        this.fieldLocationTrackingParser = fieldLocationTrackingParser;
    }

    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonParser trackingParser = new FieldLocationTrackingParser(p);
        return mapper.readValue(trackingParser, targetClass);
    }

    public Map<String, JsonLocation> getFieldLocations() {
        return fieldLocationTrackingParser.getFieldLocations();
    }
}
