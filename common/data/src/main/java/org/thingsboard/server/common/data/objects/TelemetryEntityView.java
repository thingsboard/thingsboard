/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
