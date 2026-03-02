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
package org.thingsboard.server.common.data.device.profile.lwm2m;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
public class TelemetryMappingConfiguration implements Serializable {

    private static final long serialVersionUID = -7594999741305410419L;

    private Map<String, String> keyName;
    private Set<String> observe;
    private Set<String> attribute;
    private Set<String> telemetry;
    private Map<String, ObjectAttributes> attributeLwm2m;
    private Boolean initAttrTelAsObsStrategy;
    private TelemetryObserveStrategy observeStrategy;

    @JsonCreator
    public TelemetryMappingConfiguration(
            @JsonProperty("keyName") Map<String, String> keyName,
            @JsonProperty("observe") Set<String> observe,
            @JsonProperty("attribute") Set<String> attribute,
            @JsonProperty("telemetry") Set<String> telemetry,
            @JsonProperty("attributeLwm2m") Map<String, ObjectAttributes> attributeLwm2m,
            @JsonProperty("initAttrTelAsObsStrategy") Boolean initAttrTelAsObsStrategy,
            @JsonProperty("observeStrategy") TelemetryObserveStrategy observeStrategy) {

        this.keyName = keyName != null ? keyName : Collections.emptyMap();
        this.observe = observe != null ? observe : Collections.emptySet();
        this.attribute = attribute != null ? attribute : Collections.emptySet();
        this.telemetry = telemetry != null ? telemetry : Collections.emptySet();
        this.attributeLwm2m = attributeLwm2m != null ? attributeLwm2m : Collections.emptyMap();
        this.initAttrTelAsObsStrategy = initAttrTelAsObsStrategy != null ? initAttrTelAsObsStrategy : false;
        this.observeStrategy = observeStrategy != null ? observeStrategy : TelemetryObserveStrategy.SINGLE;
    }
}
