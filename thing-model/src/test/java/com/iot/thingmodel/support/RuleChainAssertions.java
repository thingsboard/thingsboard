package com.iot.thingmodel.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.iot.thingmodel.common.Constants;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class RuleChainAssertions {

    public static final int EXPECTED_NODES = 14;
    public static final int EXPECTED_CONNECTIONS = 14;

    private RuleChainAssertions() {
    }

    public static void assertMetadata(
            JsonNode metadata, String productId, String... scriptFragments) {
        JsonNode nodes = metadata.path("nodes");
        assertEquals(EXPECTED_NODES, nodes.size());
        assertEquals(EXPECTED_CONNECTIONS, metadata.path("connections").size());

        Map<String, JsonNode> byName = new HashMap<>();
        nodes.forEach(node -> byName.put(node.path("name").asText(), node));

        assertTrue(byName.containsKey(Constants.RuleChain.NODE_NAME_VALIDATE));
        assertTrue(byName.containsKey(Constants.RuleChain.NODE_NAME_VALIDATION_SWITCH));
        assertTrue(byName.containsKey(Constants.RuleChain.NODE_NAME_TRANSFORM));

        String validationScript = byName.get(Constants.RuleChain.NODE_NAME_VALIDATE)
                .path("configuration")
                .path(Constants.RuleChain.CONFIG_JS_SCRIPT)
                .asText();
        assertTrue(validationScript.contains(productId));
        for (String fragment : scriptFragments) {
            assertTrue(validationScript.contains(fragment));
        }

        String normalizeScript = byName.get(Constants.RuleChain.NODE_NAME_TRANSFORM)
                .path("configuration")
                .path(Constants.RuleChain.CONFIG_JS_SCRIPT)
                .asText();
        assertTrue(normalizeScript.contains("tsl_normalized"));
        assertTrue(normalizeScript.contains("JSON.parse(metadata.tsl_normalized)"));
        assertFalse(normalizeScript.contains("out.msg = metadata.tsl_normalized;"));

        assertKafkaTopic(byName.get(Constants.RuleChain.NODE_NAME_KAFKA_TELEMETRY),
                Constants.Defaults.KAFKA_TOPIC_TELEMETRY);
        assertKafkaTopic(byName.get(Constants.RuleChain.NODE_NAME_KAFKA_EVENT),
                Constants.Defaults.KAFKA_TOPIC_EVENT);
        assertKafkaTopic(byName.get(Constants.RuleChain.NODE_NAME_KAFKA_INVALID),
                Constants.Defaults.KAFKA_TOPIC_INVALID);

        assertValidationConnections(metadata);
    }

    private static void assertValidationConnections(JsonNode metadata) {
        int validationIndex = nodeIndex(
                metadata.path("nodes"), Constants.RuleChain.NODE_NAME_VALIDATION_SWITCH);
        int saveIndex = nodeIndex(
                metadata.path("nodes"), Constants.RuleChain.NODE_NAME_SAVE_TIMESERIES);
        int normalizeIndex = nodeIndex(
                metadata.path("nodes"), Constants.RuleChain.NODE_NAME_TRANSFORM);

        int valid = 0;
        int invalid = 0;
        int wrongRoute = 0;
        for (JsonNode connection : metadata.path("connections")) {
            if (connection.path("fromIndex").asInt() != validationIndex) {
                continue;
            }
            String type = connection.path("type").asText();
            int toIndex = connection.path("toIndex").asInt();
            if (Constants.RuleChain.CONNECTION_VALID.equals(type) && toIndex == saveIndex) {
                valid++;
            } else if (Constants.RuleChain.CONNECTION_INVALID.equals(type)
                    && toIndex == normalizeIndex) {
                invalid++;
            } else if (Constants.RuleChain.CONNECTION_TELEMETRY.equals(type)
                    || Constants.RuleChain.CONNECTION_EVENT.equals(type)) {
                wrongRoute++;
            }
        }
        assertEquals(1, valid);
        assertEquals(1, invalid);
        assertEquals(0, wrongRoute);
    }

    private static int nodeIndex(JsonNode nodes, String name) {
        for (int i = 0; i < nodes.size(); i++) {
            if (name.equals(nodes.get(i).path("name").asText())) {
                return i;
            }
        }
        throw new AssertionError("Node not found: " + name);
    }

    private static void assertKafkaTopic(JsonNode node, String topic) {
        assertNotNull(node);
        assertEquals(topic, node.path("configuration")
                .path(Constants.RuleChain.CONFIG_TOPIC_PATTERN).asText());
    }
}
