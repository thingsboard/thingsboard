package com.iot.thingmodel.product;

import com.iot.thingmodel.common.Issue;
import java.util.List;

public record ProductResponse(
        String productId,
        String tslVersion,
        String tbProfileId,
        String tbAssetId,
        String tbRuleChainId,
        int propertyCount,
        int eventCount,
        int actionCount,
        int schemaBytes,
        String status,
        List<Issue> warnings) {

    public ProductResponse {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
