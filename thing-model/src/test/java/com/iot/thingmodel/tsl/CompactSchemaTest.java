package com.iot.thingmodel.tsl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CompactSchemaTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TslParser parser = new TslParser(objectMapper);
    private final CompactSchema compactSchema = new CompactSchema(objectMapper);

    @Test
    void generatesStableCompactSchema() throws Exception {
        TslDefinition definition = parser.parse(load("valid-tsl.json"));

        CompactSchema.CompactResult first = compactSchema.generate(definition, false);
        CompactSchema.CompactResult second = compactSchema.generate(definition, false);

        assertEquals(first.json(), second.json());
        JsonNode node = first.node();
        assertEquals("1.0", node.path("schemaVersion").asText());
        assertEquals("9HYN9PJNOY", node.path("productId").asText());
        assertEquals(7, node.path("keys").size());
        assertEquals("int", node.at("/keys/rssi/t").asText());
        assertEquals(-100.0, node.at("/keys/rssi/min").asDouble());
        assertEquals(0.0, node.at("/keys/rssi/max").asDouble());
        assertFalse(node.at("/keys/rssi").has("name"));
    }

    @Test
    void keepsRequiredFlag() throws Exception {
        TslDefinition definition = parser.parse(load("valid-tsl.json"));

        CompactSchema.CompactResult result = compactSchema.generate(definition, true);

        assertEquals(true, result.node().at("/keys/accX/req").asBoolean());
        assertEquals(true, result.node().path("strictUnknown").asBoolean());
    }

    private JsonNode load(String name) throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/tsl/" + name)) {
            return objectMapper.readTree(input);
        }
    }
}
