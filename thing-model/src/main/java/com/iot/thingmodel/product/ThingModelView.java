package com.iot.thingmodel.product;

import com.fasterxml.jackson.databind.JsonNode;

public record ThingModelView(
        String productId,
        String version,
        String status,
        boolean strictUnknown,
        JsonNode tsl) {
}
