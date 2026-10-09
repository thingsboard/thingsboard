// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.trendz;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TrendzUsage(
        @JsonProperty("used") boolean used,

        @JsonProperty("anomalyUsage") Entity anomalyUsage,
        @JsonProperty("predictionUsage") Entity predictionUsage,
        @JsonProperty("calculationUsage") Entity calculationUsage,
        @JsonProperty("viewUsage") SimpleEntity viewUsage,
        @JsonProperty("metricUsage") SimpleEntity metricUsage,
        @JsonProperty("chatUsage") SimpleEntity chatUsage
) {
    public record Entity(
            @JsonProperty("used") boolean used,
            @JsonProperty("activeCount") long activeCount,
            @JsonProperty("totalCount") long totalCount
    ) { }

    public record SimpleEntity(
            @JsonProperty("used") boolean used,
            @JsonProperty("totalCount") long totalCount
    ) { }
}
