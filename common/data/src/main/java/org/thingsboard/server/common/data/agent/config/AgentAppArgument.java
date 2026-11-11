// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.validation.NoXss;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentAppArgument {

    @Schema(description = "Argument name, referenced in the compose as ${tb.<name>}.", example = "device_uuid")
    @NoXss
    private String name;

    @Schema(description = "Source entity the value is resolved from.")
    private AgentAppArgumentSource sourceType;

    @Schema(description = "Concrete source entity id. Required when sourceType references a specific entity " +
            "(DEVICE, ASSET, CUSTOMER, EDGE); ignored for the context-derived sources.")
    private EntityId sourceEntityId;

    @Schema(description = "Whether the value is read from an attribute or the latest telemetry.")
    private AgentAppArgumentValueType valueType;

    @Schema(description = "Attribute scope. Applicable only when valueType is ATTRIBUTE. Defaults to SERVER_SCOPE.")
    private AttributeScope scope;

    @Schema(description = "Attribute or latest telemetry key to read.", example = "cloud_endpoint")
    @NoXss
    private String key;

    @Schema(description = "Optional fallback value used when the source has no value for the key.")
    @NoXss
    private String defaultValue;

    @Schema(description = "How the resolved value is injected into the compose: STRING (quoted) or JSON (raw, " +
            "for arrays/objects/numbers when the placeholder is the whole value). Defaults to STRING.")
    private AgentAppArgumentFormat format;

    public AgentAppArgument(AgentAppArgument other) {
        this.name = other.name;
        this.sourceType = other.sourceType;
        this.sourceEntityId = other.sourceEntityId;
        this.valueType = other.valueType;
        this.scope = other.scope;
        this.key = other.key;
        this.defaultValue = other.defaultValue;
        this.format = other.format;
    }

    @JsonIgnore
    public boolean isJsonFormat() {
        return format == AgentAppArgumentFormat.JSON;
    }

    @JsonIgnore
    public AttributeScope getScopeOrDefault() {
        return scope != null ? scope : AttributeScope.SERVER_SCOPE;
    }

}
