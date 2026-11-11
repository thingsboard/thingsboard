// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.converter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.google.gson.JsonElement;
import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.EntityType;

@Builder
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DedicatedUplinkData {
    private final EntityType entityType;
    private final String name;
    private final String profile;
    private final String label;
    private final String customer;
    private final String group;
    private final JsonElement telemetry;
    private final JsonElement attributes;

    @JsonRawValue
    public String getTelemetry() {
        return telemetry.toString();
    }

    @JsonRawValue
    public String getAttributes() {
        return attributes.toString();
    }

    @JsonIgnore
    public JsonElement getTelemetryJson() {
        return telemetry;
    }

    @JsonIgnore
    public JsonElement getAttributesJson() {
        return attributes;
    }
}
