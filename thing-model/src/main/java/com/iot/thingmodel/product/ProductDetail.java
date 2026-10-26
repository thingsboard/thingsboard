package com.iot.thingmodel.product;

import java.time.Instant;
import java.util.List;

public record ProductDetail(
        ProductItem product,
        List<TslVersionView> versions,
        CatalogView catalog) {

    public ProductDetail {
        versions = versions == null ? List.of() : List.copyOf(versions);
    }

    public record TslVersionView(
            String version,
            String status,
            String checksum,
            Instant publishedAt,
            Instant createdAt) {
    }
}
