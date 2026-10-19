// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile.lwm2m;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
@Schema
public class TelemetryMappingConfiguration implements Serializable {

    private static final long serialVersionUID = -7594999741305410419L;

    @Schema(description = "Map of LwM2M resource paths to telemetry key names")
    private Map<String, String> keyName;
    @Schema(description = "Set of resources to observe")
    private Set<String> observe;
    @Schema(description = "Set of attribute keys")
    private Set<String> attribute;
    @Schema(description = "Set of telemetry keys")
    private Set<String> telemetry;
    @Schema(description = "Map of resource paths to specific LwM2M object attributes")
    private Map<String, ObjectAttributes> attributeLwm2m;
    private Boolean initAttrTelAsObsStrategy;
    @Schema(description = "Observation strategy for telemetry")
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
