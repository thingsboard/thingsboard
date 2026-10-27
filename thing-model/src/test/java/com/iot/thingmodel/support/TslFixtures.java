package com.iot.thingmodel.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.thingmodel.sync.TbSyncRequest;
import com.iot.thingmodel.tsl.CompactSchema;
import com.iot.thingmodel.tsl.TslDefinition;
import com.iot.thingmodel.tsl.TslParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.UUID;

public final class TslFixtures {

    public static final String PRODUCT_ID = "9HYN9PJNOY";
    public static final String PRODUCT_NAME = "WiFi诊断终端";
    public static final String CATEGORY_ID = "1";
    public static final String VERSION = "1.0";

    private TslFixtures() {
    }

    public static JsonNode load(ObjectMapper objectMapper, String resourceName) {
        try (InputStream input = TslFixtures.class.getResourceAsStream("/tsl/" + resourceName)) {
            if (input == null) {
                throw new IllegalStateException("Missing /tsl/" + resourceName);
            }
            return objectMapper.readTree(input);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static JsonNode validTsl(ObjectMapper objectMapper) {
        return load(objectMapper, "valid-tsl.json");
    }

    public static JsonNode invalidTsl(ObjectMapper objectMapper) {
        return load(objectMapper, "invalid-tsl.json");
    }

    public static JsonNode eventTsl(ObjectMapper objectMapper) {
        return load(objectMapper, "event-tsl.json");
    }

    public static TslDefinition definition(ObjectMapper objectMapper, JsonNode raw) {
        return new TslParser(objectMapper).parse(raw);
    }

    public static TslDefinition validDefinition(ObjectMapper objectMapper) {
        return definition(objectMapper, validTsl(objectMapper));
    }

    public static CompactSchema.CompactResult compact(
            ObjectMapper objectMapper, TslDefinition definition, boolean strictUnknown) {
        return new CompactSchema(objectMapper).generate(definition, strictUnknown);
    }

    public static TbSyncRequest syncRequest(ObjectMapper objectMapper, UUID tenantId) {
        TslDefinition definition = validDefinition(objectMapper);
        return syncRequest(objectMapper, tenantId, definition.raw(), PRODUCT_ID,
                PRODUCT_NAME, PRODUCT_ID, CATEGORY_ID, VERSION, false);
    }

    public static TbSyncRequest syncRequest(
            ObjectMapper objectMapper,
            UUID tenantId,
            JsonNode rawTsl,
            String productId,
            String name,
            String model,
            String categoryId,
            String version,
            boolean strictUnknown) {
        TslDefinition definition = definition(objectMapper, rawTsl);
        CompactSchema.CompactResult compact = compact(objectMapper, definition, strictUnknown);
        return new TbSyncRequest(
                tenantId, productId, name, model, categoryId, version,
                definition.raw(), definition, compact);
    }
}
