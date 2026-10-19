// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.chart;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class TimeSeriesChartStateSettings {

    private String label;
    private Double value;
    private TimeSeriesChartStateSourceType sourceType;

    @JsonProperty("sourceValue")
    private JsonNode sourceValue;

    private Double sourceRangeFrom;
    private Double sourceRangeTo;

    @JsonIgnore
    public boolean isValidState() {
        if (value == null || !Double.isFinite(value) || sourceType == null) {
            return false;
        }
        if (sourceType == TimeSeriesChartStateSourceType.constant) {
            return sourceValue != null && !sourceValue.isNull();
        }
        return true;
    }

    @JsonIgnore
    public String sourceValueAsString() {
        if (sourceValue != null) {
            return sourceValue.asText();
        } else {
            return null;
        }
    }

}
