package com.iot.thingmodel.product;

import java.time.Instant;

public record ProductItem(
        String tenantId,
        String productId,
        String name,
        String model,
        String categoryId,
        String tslVersion,
        String status,
        long propertyCount,
        long eventCount,
        long actionCount,
        String tbProfileId,
        String tbAssetId,
        String tbRuleChainId,
        Instant updatedAt) {
}
