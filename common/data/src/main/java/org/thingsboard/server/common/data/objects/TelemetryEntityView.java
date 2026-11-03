// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.objects;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Victor Basanets on 9/05/2017.
 */
@Schema
@Data
@NoArgsConstructor
public class TelemetryEntityView implements Serializable {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "List of time-series data keys to expose", example = "[\"temperature\", \"humidity\"]")
    private List<String> timeseries;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "JSON object with attributes to expose")
    private AttributesEntityView attributes;

    public TelemetryEntityView(List<String> timeseries, AttributesEntityView attributes) {

        this.timeseries = new ArrayList<>(timeseries);
        this.attributes = attributes;
    }

    public TelemetryEntityView(TelemetryEntityView obj) {
        this(obj.getTimeseries(), obj.getAttributes());
    }
}
