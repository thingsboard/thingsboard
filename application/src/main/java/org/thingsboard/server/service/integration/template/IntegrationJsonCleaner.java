// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.integration.IntegrationType;

import java.util.Set;

/**
 * Stateless cleaner for an Integration entity JSON tree, applied before annotation-driven
 * tokenization at IoT Hub package export time.
 */
@Component
public class IntegrationJsonCleaner {

    static final String BASE_URL_PLACEHOLDER = "${baseUrl}";
    static final String ROUTING_KEY_PLACEHOLDER = "${routingKey}";

    private static final Set<String> FIELDS_TO_REMOVE = Set.of(
            "id", "tenantId", "createdTime",
            "defaultConverterId", "downlinkConverterId",
            "routingKey", "secret",
            "externalId",
            "debugSettings"
    );

    /**
     * Returns a fresh JsonNode with the cleanup rules applied; does not mutate input.
     *
     * <p>For HTTP-style integrations the persisted {@code baseUrl} and {@code httpEndpoint}
     * fields embed the source platform's host and the source integration's routingKey,
     * neither of which is portable. They are rewritten to placeholders the install dialog
     * resolves at install time: {@link #BASE_URL_PLACEHOLDER} (current platform, https
     * preferred) and {@link #ROUTING_KEY_PLACEHOLDER} (the freshly generated routingKey).
     */
    public JsonNode clean(IntegrationType type, JsonNode source) {
        ObjectNode out = source.deepCopy();
        FIELDS_TO_REMOVE.forEach(out::remove);
        out.put("enabled", true);
        out.put("debugMode", true);
        rewriteHttpEndpointFields(type, out);
        return out;
    }

    private void rewriteHttpEndpointFields(IntegrationType type, ObjectNode out) {
        JsonNode cfgNode = out.get("configuration");
        if (!(cfgNode instanceof ObjectNode cfg)) {
            return;
        }
        rewriteOn(cfg, type);
        JsonNode clientNode = cfg.get("clientConfiguration");
        if (clientNode instanceof ObjectNode client) {
            rewriteOn(client, type);
        }
    }

    private void rewriteOn(ObjectNode target, IntegrationType type) {
        if (target.has("baseUrl")) {
            target.put("baseUrl", BASE_URL_PLACEHOLDER);
        }
        if (target.has("httpEndpoint")) {
            target.put("httpEndpoint", BASE_URL_PLACEHOLDER
                    + "/api/v1/integrations/" + type.name().toLowerCase()
                    + "/" + ROUTING_KEY_PLACEHOLDER);
        }
    }
}
