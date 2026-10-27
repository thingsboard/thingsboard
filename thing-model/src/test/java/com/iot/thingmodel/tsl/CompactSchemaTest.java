package com.iot.thingmodel.tsl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.List;
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
        assertEquals("1.0", node.path("tslVersion").asText());
        assertEquals(7, node.path("keys").size());
        assertEquals(0, node.path("events").size());
        assertEquals("int", node.at("/keys/rssi/t").asText());
        assertEquals(-100.0, node.at("/keys/rssi/min").asDouble());
        assertEquals(0.0, node.at("/keys/rssi/max").asDouble());
        assertFalse(node.at("/keys/rssi").has("name"));
    }

    @Test
    void includesEventDefinitions() throws Exception {
        TslDefinition definition = parser.parse(load("event-tsl.json"));

        CompactSchema.CompactResult result = compactSchema.generate(definition, false);

        JsonNode node = result.node();
        assertEquals("EVTPRODUCT01", node.path("productId").asText());
        assertEquals("alert", node.at("/events/overSpeed/type").asText());
        assertEquals("float", node.at("/events/overSpeed/params/speed/t").asText());
        assertEquals(300.0, node.at("/events/overSpeed/params/speed/max").asDouble());
        assertEquals("int", node.at("/events/overSpeed/params/limit/t").asText());
        assertEquals(200.0, node.at("/events/overSpeed/params/limit/max").asDouble());
    }

    @Test
    void keepsRequiredFlag() throws Exception {
        TslDefinition definition = parser.parse(load("valid-tsl.json"));

        CompactSchema.CompactResult result = compactSchema.generate(definition, true);

        assertEquals(true, result.node().at("/keys/accX/req").asBoolean());
        assertEquals(true, result.node().path("strictUnknown").asBoolean());
    }

    @Test
    void keepsMappingUnitRangeAndStructType() {
        JsonNode mapping = objectMapper.createObjectNode().put("0", "auto").put("1", "manual");
        TslDefine enumDefine = new TslDefine(
                "enum", 0.0, 10.0, 1.0, 2.0, "mode", null, mapping);
        TslProperty enumProperty = new TslProperty(
                "mode", "Mode", null, "rw", true, enumDefine, null);
        TslDefine structDefine = new TslDefine(
                "struct", null, null, null, null, null,
                objectMapper.createObjectNode().put("field", "int"), null);
        TslProperty structProperty = new TslProperty(
                "cfg", "Config", null, "r", false, structDefine, null);
        TslDefinition definition = new TslDefinition(
                "1.0", "P1", "C1", List.of(enumProperty, structProperty),
                List.of(), List.of(), null);

        JsonNode node = compactSchema.generate(definition, false).node();

        assertEquals("enum", node.at("/keys/mode/t").asText());
        assertEquals("auto", node.at("/keys/mode/map/0").asText());
        assertEquals("manual", node.at("/keys/mode/map/1").asText());
        assertEquals(0.0, node.at("/keys/mode/min").asDouble());
        assertEquals(10.0, node.at("/keys/mode/max").asDouble());
        assertEquals(1.0, node.at("/keys/mode/start").asDouble());
        assertEquals(2.0, node.at("/keys/mode/step").asDouble());
        assertEquals("mode", node.at("/keys/mode/unit").asText());
        assertEquals(true, node.at("/keys/mode/req").asBoolean());
        assertEquals("struct", node.at("/keys/cfg/t").asText());
    }

    @Test
    void keepsEventParameterMetadata() {
        JsonNode mapping = objectMapper.createObjectNode().put("0", "auto").put("1", "manual");
        TslDefine define = new TslDefine(
                "enum", 0.0, 2.0, 0.0, 1.0, "mode", null, mapping);
        TslEvent event = new TslEvent(
                "e1", "Event", null, "info",
                List.of(new TslParam("mode", "Mode", null, define)));
        TslDefinition definition = new TslDefinition(
                "1.0", "P1", "C1", List.of(), List.of(event), List.of(), null);

        JsonNode node = compactSchema.generate(definition, false).node();

        assertEquals("enum", node.at("/events/e1/params/mode/t").asText());
        assertEquals("auto", node.at("/events/e1/params/mode/map/0").asText());
        assertEquals(0.0, node.at("/events/e1/params/mode/min").asDouble());
        assertEquals(2.0, node.at("/events/e1/params/mode/max").asDouble());
        assertEquals(0.0, node.at("/events/e1/params/mode/start").asDouble());
        assertEquals(1.0, node.at("/events/e1/params/mode/step").asDouble());
        assertEquals("mode", node.at("/events/e1/params/mode/unit").asText());
    }

    @Test
    void usesUnknownTypeAndReadModeWhenDefineIsMissing() {
        TslProperty property = new TslProperty(
                "p1", "P1", null, null, false, null, null);
        TslDefinition definition = new TslDefinition(
                "1.0", "P1", "C1", List.of(property), List.of(), List.of(), null);

        JsonNode node = compactSchema.generate(definition, false).node();

        assertEquals("unknown", node.at("/keys/p1/t").asText());
        assertEquals("r", node.at("/keys/p1/mode").asText());
    }

    private JsonNode load(String name) throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/tsl/" + name)) {
            return objectMapper.readTree(input);
        }
    }
}
