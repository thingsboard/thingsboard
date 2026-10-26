package com.iot.thingmodel.tsl;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record TslDefinition(
        String version,
        String productId,
        String categoryId,
        List<TslProperty> properties,
        List<TslEvent> events,
        List<TslAction> actions,
        JsonNode raw) {
}
