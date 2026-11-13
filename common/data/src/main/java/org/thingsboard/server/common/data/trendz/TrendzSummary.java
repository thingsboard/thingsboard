// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.trendz;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

public record TrendzSummary(
        @JsonProperty("metricSummaryItems") List<Object> metricSummaryItems,
        @JsonProperty("anomalyModelSummaryItems") List<Object> anomalyModelSummaryItems,
        @JsonProperty("calculationFieldSummaryItems") List<Object> calculationFieldSummaryItems,
        @JsonProperty("predictionModelSummaryItems") List<Object> predictionModelSummaryItems,
        @JsonProperty("viewSummaryItems") List<Object> viewSummaryItems,
        @JsonProperty("aiSummaryItems") List<Object> aiSummaryItems
) implements Serializable { }
