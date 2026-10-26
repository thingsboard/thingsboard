package com.iot.thingmodel.product;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;

public record CreateProductCommand(
        UUID tenantId,
        String productId,
        String name,
        String model,
        String categoryId,
        String tslVersion,
        JsonNode tsl,
        boolean strictUnknown) {
}
