package com.iot.thingmodel.tsl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TslParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TslParser parser = new TslParser(objectMapper);

    @Test
    void parsesProfileVersionPropertiesEventsAndActions() throws Exception {
        TslDefinition definition = parser.parse(objectMapper.readTree("""
                {
                  "version": "1.2",
                  "profile": {"ProductId": "P1", "CategoryId": "C1"},
                  "properties": [{
                    "id": "p1",
                    "name": "P1",
                    "mode": "rw",
                    "required": true,
                    "define": {"type": "int", "min": 0, "max": 100},
                    "mapping": {"0": "A", "1": "B"}
                  }],
                  "events": [{
                    "id": "e1",
                    "name": "E1",
                    "type": "alert",
                    "params": [{"id": "speed", "name": "Speed", "define": {"type": "float"}}]
                  }],
                  "actions": [{
                    "id": "a1",
                    "name": "A1",
                    "input": [{"id": "x", "name": "X", "define": {"type": "string"}}],
                    "output": [{"id": "y", "name": "Y", "define": {"type": "int"}}]
                  }]
                }
                """));

        assertEquals("1.2", definition.version());
        assertEquals("P1", definition.productId());
        assertEquals("C1", definition.categoryId());
        assertEquals(1, definition.properties().size());
        assertEquals(1, definition.events().size());
        assertEquals(1, definition.actions().size());

        TslProperty property = definition.properties().get(0);
        assertEquals("p1", property.id());
        assertEquals("rw", property.mode());
        assertTrue(property.required());
        assertEquals("int", property.define().type());
        assertEquals(0.0, property.define().min());
        assertEquals(100.0, property.define().max());
        assertEquals("A", property.mapping().path("0").asText());

        TslEvent event = definition.events().get(0);
        assertEquals("e1", event.id());
        assertEquals("alert", event.type());
        assertEquals(1, event.params().size());
        assertEquals("speed", event.params().get(0).id());
        assertEquals("float", event.params().get(0).define().type());

        TslAction action = definition.actions().get(0);
        assertEquals("a1", action.id());
        assertEquals("x", action.input().get(0).id());
        assertEquals("y", action.output().get(0).id());
    }

    @Test
    void defaultsModeToRead() throws Exception {
        TslDefinition definition = parser.parse(objectMapper.readTree("""
                {
                  "version": "1.0",
                  "profile": {"ProductId": "P1"},
                  "properties": [{"id":"p1","name":"P1","define":{"type":"int"}}]
                }
                """));

        assertEquals("r", definition.properties().get(0).mode());
        assertFalse(definition.properties().get(0).required());
    }

    @Test
    void prefersPropertyMappingOverDefineMapping() throws Exception {
        TslDefinition definition = parser.parse(objectMapper.readTree("""
                {
                  "version": "1.0",
                  "profile": {"ProductId": "P1"},
                  "properties": [{
                    "id": "p1",
                    "name": "P1",
                    "define": {"type": "enum", "mapping": {"0": "Define"}},
                    "mapping": {"0": "Property"}
                  }]
                }
                """));

        assertEquals("Property",
                definition.properties().get(0).mapping().path("0").asText());
    }

    @Test
    void parsesTextualJson() {
        TslDefinition definition = parser.parse(new TextNode("""
                {"version":"1.0","profile":{"ProductId":"P1"},"properties":[]}
                """));

        assertEquals("P1", definition.productId());
        assertEquals("1.0", definition.version());
    }

    @Test
    void returnsEmptyDefinitionForInvalidTextualJson() {
        TslDefinition definition = parser.parse(new TextNode("{invalid"));

        assertNull(definition.productId());
        assertNotNull(definition.properties());
        assertTrue(definition.properties().isEmpty());
    }

    @Test
    void handlesNullAndEmptyObject() {
        TslDefinition nullDefinition = parser.parse(null);
        TslDefinition emptyDefinition = parser.parse(objectMapper.createObjectNode());

        assertNull(nullDefinition.productId());
        assertTrue(nullDefinition.properties().isEmpty());
        assertNull(emptyDefinition.version());
        assertTrue(emptyDefinition.events().isEmpty());
    }

    @Test
    void parsesNumericTextValues() throws Exception {
        TslDefinition definition = parser.parse(objectMapper.readTree("""
                {
                  "version": "1.0",
                  "profile": {"ProductId": "P1"},
                  "properties": [{
                    "id": "p1",
                    "name": "P1",
                    "define": {"type": "int", "min": "1.5", "max": "9.5"}
                  }]
                }
                """));

        assertEquals(1.5, definition.properties().get(0).define().min());
        assertEquals(9.5, definition.properties().get(0).define().max());
    }
}
