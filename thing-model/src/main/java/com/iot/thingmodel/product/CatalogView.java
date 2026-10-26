package com.iot.thingmodel.product;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record CatalogView(
        String productId,
        String version,
        List<PropertyView> properties,
        List<EventView> events,
        List<ActionView> actions) {

    public record PropertyView(
            String id,
            String name,
            String dataType,
            String mode,
            String unit,
            Double min,
            Double max,
            Double step,
            boolean required,
            JsonNode mapping) {
    }

    public record EventView(String id, String name, String type, JsonNode params) {
    }

    public record ActionView(String id, String name, JsonNode input, JsonNode output) {
    }
}
