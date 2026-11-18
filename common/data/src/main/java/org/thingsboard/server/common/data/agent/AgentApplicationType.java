// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent;

import lombok.Getter;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.agent.config.GatewayEnvSchema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public enum AgentApplicationType {
    // GENERIC has no version graph; its single template is materialized and registered under this defaultVersion.
    GENERIC(null, "default", null, Collections.emptyList()),
    // the leading "(?:[^/\s]+/)*" accepts optional registry/namespace segments, so an image pulled through a
    // private mirror still matches: patterns are applied with Matcher.matches() over the whole image reference;
    // edge accepts both the CE-style "tb-edge" repo and the legacy "tb-edge-pe" one
    EDGE("(?:[^/\\s]+/)*thingsboard/tb-edge(?:-pe)?:.+", null, EntityType.EDGE,
            List.of("CLOUD_ROUTING_KEY", "CLOUD_ROUTING_SECRET")),
    GATEWAY("(?:[^/\\s]+/)*thingsboard/tb-gateway:.+", null, EntityType.DEVICE,
            gatewayCredentialEnvKeys());

    @Getter
    private final Pattern mainImagePattern;
    @Getter
    private final String defaultVersion;
    @Getter
    private final EntityType relatedEntityType;
    @Getter
    private final List<String> credentialEnvKeys;

    AgentApplicationType(String mainImageRegex, String defaultVersion, EntityType relatedEntityType,
                         List<String> credentialEnvKeys) {
        this.mainImagePattern = mainImageRegex != null ? Pattern.compile(mainImageRegex) : null;
        this.defaultVersion = defaultVersion;
        this.relatedEntityType = relatedEntityType;
        this.credentialEnvKeys = credentialEnvKeys;
    }

    public boolean hasRelatedEntityType() {
        return relatedEntityType != null;
    }

    // credential env keys of every gateway env schema (modern TB_GW_* and legacy unprefixed),
    // so credential-aware paths (equalsIgnoringCreds, profile config resolution) cover both
    private static List<String> gatewayCredentialEnvKeys() {
        List<String> keys = new ArrayList<>();
        for (GatewayEnvSchema schema : GatewayEnvSchema.values()) {
            if (schema.getSecurityTypeKey() != null) {
                keys.add(schema.getSecurityTypeKey());
            }
            keys.addAll(schema.credentialKeys());
        }
        return List.copyOf(keys);
    }
}
