package com.iot.thingmodel.db;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.JSONB;

public final class JsonbCodec {

    private JsonbCodec() {
    }

    public static JSONB toJsonb(ObjectMapper objectMapper, Object value) {
        if (value == null) {
            return null;
        }
        try {
            return JSONB.valueOf(objectMapper.writeValueAsString(value));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize JSON", ex);
        }
    }

    public static JsonNode toJsonNode(ObjectMapper objectMapper, JSONB value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readTree(value.data());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse stored JSON", ex);
        }
    }
}
