package com.iot.thingmodel.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jooq.JSONB;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonbCodecTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesAndParsesJsonb() {
        ObjectNode value = objectMapper.createObjectNode()
                .put("a", 1)
                .put("b", true);

        JSONB jsonb = JsonbCodec.toJsonb(objectMapper, value);

        assertEquals(value, JsonbCodec.toJsonNode(objectMapper, jsonb));
    }

    @Test
    void returnsNullForNullValues() {
        assertNull(JsonbCodec.toJsonb(objectMapper, null));
        assertNull(JsonbCodec.toJsonNode(objectMapper, null));
    }

    @Test
    void failsForInvalidStoredJson() {
        assertThrows(IllegalStateException.class,
                () -> JsonbCodec.toJsonNode(objectMapper, JSONB.valueOf("{invalid")));
    }
}
