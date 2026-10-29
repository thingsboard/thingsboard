// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.iot_hub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * The checksum of an entity as the server stores it, kept in the install state of a device package. A different
 * checksum later means someone changed the entity, even a widget resized. The fields the server sets, and the name,
 * which an update keeps, don't count.
 */
final class EntityChecksum {

    private static final Set<String> SKIPPED_KEYS = Set.of("id", "name", "title", "version", "createdTime", "tenantId",
            "customerId", "ownerId", "assignedCustomers", "externalId", "ruleChainId", "resources");

    private EntityChecksum() {
    }

    /** A rule chain is checked with its metadata: pass both. */
    static String of(Object... parts) {
        StringBuilder canonical = new StringBuilder();
        for (Object part : parts) {
            JsonNode node = part instanceof RuleChainMetaData metadata ? ruleChainContent(metadata) : JacksonUtil.valueToTree(part);
            appendCanonical(canonical, node, true);
            canonical.append('|');
        }
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * What a rule chain does, as the IoT Hub checksum of a rule chain item takes it: the nodes' type, name and
     * configuration, and the connections. Node ids, layout and debug settings change without the user changing
     * what the rule chain does, e.g. when it is saved again.
     */
    private static JsonNode ruleChainContent(RuleChainMetaData metadata) {
        ObjectNode content = JacksonUtil.newObjectNode();
        ArrayNode nodes = content.putArray("nodes");
        if (metadata.getNodes() != null) {
            for (RuleNode node : metadata.getNodes()) {
                ObjectNode n = nodes.addObject();
                n.put("type", node.getType());
                n.put("name", node.getName());
                n.set("configuration", node.getConfiguration());
                n.put("singletonMode", node.isSingletonMode());
                n.put("queueName", node.getQueueName());
            }
        }
        content.set("connections", JacksonUtil.valueToTree(metadata.getConnections()));
        content.set("ruleChainConnections", JacksonUtil.valueToTree(metadata.getRuleChainConnections()));
        content.set("firstNodeIndex", JacksonUtil.valueToTree(metadata.getFirstNodeIndex()));
        return content;
    }

    /** JSON with the keys sorted, so the same entity always gives the same text. */
    private static void appendCanonical(StringBuilder out, JsonNode node, boolean topLevel) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            out.append("null");
        } else if (node.isObject()) {
            List<String> keys = new ArrayList<>();
            for (Iterator<String> it = node.fieldNames(); it.hasNext(); ) {
                String key = it.next();
                if (!topLevel || !SKIPPED_KEYS.contains(key)) {
                    keys.add(key);
                }
            }
            keys.sort(null);
            out.append('{');
            for (int i = 0; i < keys.size(); i++) {
                if (i > 0) {
                    out.append(',');
                }
                out.append(JacksonUtil.toString(keys.get(i))).append(':');
                appendCanonical(out, node.get(keys.get(i)), false);
            }
            out.append('}');
        } else if (node.isArray()) {
            out.append('[');
            for (int i = 0; i < node.size(); i++) {
                if (i > 0) {
                    out.append(',');
                }
                appendCanonical(out, node.get(i), false);
            }
            out.append(']');
        } else {
            out.append(node.toString());
        }
    }

}
