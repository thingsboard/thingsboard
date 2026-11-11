// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.chart;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class SimpleValueSourceDeserializer extends JsonDeserializer<SimpleValueSourceConfig> {

    @Override
    public SimpleValueSourceConfig deserialize(JsonParser jsonParser, DeserializationContext ctx) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        if (node == null || node.isNull()) {
            return null;
        }

        if (node.isNumber()) {
            SimpleValueSourceConfig cfg = new SimpleValueSourceConfig();
            cfg.setType(ValueSourceType.constant);
            cfg.setValue(node.doubleValue());
            return cfg;
        }

        if (node.isObject()) {
            ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
            return mapper.treeToValue(node, SimpleValueSourceConfig.class);
        }

        throw new IOException("Wrong value source format!");
    }
}
