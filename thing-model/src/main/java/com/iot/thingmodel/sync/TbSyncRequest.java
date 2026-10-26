package com.iot.thingmodel.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.iot.thingmodel.tsl.CompactSchema;
import com.iot.thingmodel.tsl.TslDefinition;
import java.util.UUID;

public record TbSyncRequest(
        UUID tenantId,
        String productId,
        String name,
        String model,
        String categoryId,
        String tslVersion,
        JsonNode rawTsl,
        TslDefinition definition,
        CompactSchema.CompactResult compact) {
}
