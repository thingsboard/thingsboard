// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockerComposeUtilsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern EDGE_PATTERN = Pattern.compile("thingsboard/tb-edge:.+");
    private static final List<String> EDGE_CRED_KEYS = List.of("CLOUD_ROUTING_KEY", "CLOUD_ROUTING_SECRET");

    @Test
    void equalsIgnoringEnvKeys_returnsTrue_whenOnlyIgnoredKeysDiffer() {
        ObjectNode a = createEdgeCompose("rk-1", "secret-1");
        ObjectNode b = createEdgeCompose("rk-2", "secret-2");
        assertTrue(DockerComposeUtils.equalsIgnoringEnvKeys(a, b, EDGE_PATTERN, EDGE_CRED_KEYS));
    }

    @Test
    void equalsIgnoringEnvKeys_returnsFalse_whenNonIgnoredEnvKeyDiffers() {
        ObjectNode a = createEdgeCompose("rk-1", "secret-1");
        ObjectNode b = createEdgeCompose("rk-1", "secret-1");
        ((ObjectNode) b.get("services").get("mytbedge").get("environment")).put("CLOUD_RPC_HOST", "other-host");
        assertFalse(DockerComposeUtils.equalsIgnoringEnvKeys(a, b, EDGE_PATTERN, EDGE_CRED_KEYS));
    }

    @Test
    void equalsIgnoringEnvKeys_returnsFalse_whenServiceImageDiffers() {
        ObjectNode a = createEdgeCompose("rk-1", "secret-1");
        ObjectNode b = createEdgeCompose("rk-1", "secret-1");
        ((ObjectNode) b.get("services").get("mytbedge")).put("image", "thingsboard/tb-edge:9.9.9");
        assertFalse(DockerComposeUtils.equalsIgnoringEnvKeys(a, b, EDGE_PATTERN, EDGE_CRED_KEYS));
    }

    @Test
    void equalsIgnoringEnvKeys_doesNotMutateInputs() {
        ObjectNode a = createEdgeCompose("rk-1", "secret-1");
        ObjectNode b = createEdgeCompose("rk-2", "secret-2");
        DockerComposeUtils.equalsIgnoringEnvKeys(a, b, EDGE_PATTERN, EDGE_CRED_KEYS);

        assertTrue(a.get("services").get("mytbedge").get("environment").has("CLOUD_ROUTING_KEY"));
        assertTrue(b.get("services").get("mytbedge").get("environment").has("CLOUD_ROUTING_SECRET"));
    }

    @Test
    void equalsIgnoringEnvKeys_fallsBackToPlainEquality_whenPatternIsNull() {
        ObjectNode a = createEdgeCompose("rk-1", "secret-1");
        ObjectNode b = createEdgeCompose("rk-1", "secret-1");
        assertTrue(DockerComposeUtils.equalsIgnoringEnvKeys(a, b, null, EDGE_CRED_KEYS));

        ObjectNode c = createEdgeCompose("rk-1", "secret-1");
        ObjectNode d = createEdgeCompose("rk-2", "secret-1");
        assertFalse(DockerComposeUtils.equalsIgnoringEnvKeys(c, d, null, EDGE_CRED_KEYS));
    }

    @Test
    void equalsIgnoringEnvKeys_fallsBackToPlainEquality_whenKeyListIsEmpty() {
        ObjectNode a = createEdgeCompose("rk-1", "secret-1");
        ObjectNode b = createEdgeCompose("rk-2", "secret-1");
        assertFalse(DockerComposeUtils.equalsIgnoringEnvKeys(a, b, EDGE_PATTERN, List.of()));
    }

    @Test
    void equalsIgnoringEnvKeys_bothNull_returnsTrue() {
        assertTrue(DockerComposeUtils.equalsIgnoringEnvKeys(null, null, EDGE_PATTERN, EDGE_CRED_KEYS));
    }

    @Test
    void equalsIgnoringEnvKeys_oneNull_returnsFalse() {
        ObjectNode a = createEdgeCompose("rk-1", "secret-1");
        assertFalse(DockerComposeUtils.equalsIgnoringEnvKeys(a, null, EDGE_PATTERN, EDGE_CRED_KEYS));
    }

    // --- findServiceEnvironment ---

    @Test
    void findServiceEnvironment_returnsMapForm() {
        ObjectNode compose = createEdgeCompose("rk-1", "secret-1");
        JsonNode env = DockerComposeUtils.findServiceEnvironment(compose, EDGE_PATTERN);
        assertNotNull(env);
        assertTrue(env.isObject());
        assertEquals("rk-1", env.get("CLOUD_ROUTING_KEY").asText());
    }

    @Test
    void findServiceEnvironment_returnsListForm() {
        ObjectNode compose = createEdgeComposeListEnv("rk-1", "secret-1");
        JsonNode env = DockerComposeUtils.findServiceEnvironment(compose, EDGE_PATTERN);
        assertNotNull(env);
        assertTrue(env.isArray());
    }

    @Test
    void findServiceEnvironment_returnsNull_whenNoMatchingService() {
        ObjectNode compose = createEdgeCompose("rk-1", "secret-1");
        ((ObjectNode) compose.get("services").get("mytbedge")).put("image", "postgres:15");
        assertNull(DockerComposeUtils.findServiceEnvironment(compose, EDGE_PATTERN));
    }

    @Test
    void findServiceEnvironment_returnsNull_whenEnvBlockMissing() {
        ObjectNode compose = createEdgeCompose("rk-1", "secret-1");
        ((ObjectNode) compose.get("services").get("mytbedge")).remove("environment");
        assertNull(DockerComposeUtils.findServiceEnvironment(compose, EDGE_PATTERN));
    }

    // --- getEnvVariable ---

    @Test
    void getEnvVariable_mapForm_returnsValue() {
        ObjectNode compose = createEdgeCompose("rk-1", "secret-1");
        assertEquals("rk-1", DockerComposeUtils.getEnvVariable(compose, EDGE_PATTERN, "CLOUD_ROUTING_KEY"));
    }

    @Test
    void getEnvVariable_mapForm_returnsNull_whenKeyMissing() {
        ObjectNode compose = createEdgeCompose("rk-1", "secret-1");
        assertNull(DockerComposeUtils.getEnvVariable(compose, EDGE_PATTERN, "MISSING_KEY"));
    }

    @Test
    void getEnvVariable_listForm_returnsValue() {
        ObjectNode compose = createEdgeComposeListEnv("rk-1", "secret-1");
        assertEquals("rk-1", DockerComposeUtils.getEnvVariable(compose, EDGE_PATTERN, "CLOUD_ROUTING_KEY"));
        assertEquals("secret-1", DockerComposeUtils.getEnvVariable(compose, EDGE_PATTERN, "CLOUD_ROUTING_SECRET"));
    }

    @Test
    void getEnvVariable_listForm_returnsNull_whenKeyMissing() {
        ObjectNode compose = createEdgeComposeListEnv("rk-1", "secret-1");
        assertNull(DockerComposeUtils.getEnvVariable(compose, EDGE_PATTERN, "MISSING_KEY"));
    }

    @Test
    void getEnvVariable_listForm_returnsEmptyString_whenKeyHasNoValue() {
        // Entries may be bare keys ("KEY" with no "=") — those inherit from the host env.
        // Compose treats that as value = empty from the compose file's perspective.
        ArrayNode env = MAPPER.createArrayNode();
        env.add("BARE_KEY");
        env.add("KEY_WITH_EQ=some-value");
        ObjectNode compose = composeWithEnv(env);
        assertEquals("", DockerComposeUtils.getEnvVariable(compose, EDGE_PATTERN, "BARE_KEY"));
        assertEquals("some-value", DockerComposeUtils.getEnvVariable(compose, EDGE_PATTERN, "KEY_WITH_EQ"));
    }

    @Test
    void getEnvVariable_listForm_valueMayContainEqualsSign() {
        ArrayNode env = MAPPER.createArrayNode();
        env.add("DATABASE_URL=jdbc:postgresql://host:5432/db?a=b&c=d");
        ObjectNode compose = composeWithEnv(env);
        assertEquals("jdbc:postgresql://host:5432/db?a=b&c=d",
                DockerComposeUtils.getEnvVariable(compose, EDGE_PATTERN, "DATABASE_URL"));
    }

    // --- setEnvVariables ---

    @Test
    void setEnvVariables_mapForm_overwritesExistingOnly() {
        ObjectNode compose = createEdgeCompose("rk-1", "secret-1");
        Map<String, String> updates = new LinkedHashMap<>();
        updates.put("CLOUD_ROUTING_KEY", "rk-2");
        updates.put("NEW_UNRELATED_KEY", "never-added");
        DockerComposeUtils.setEnvVariables(compose, EDGE_PATTERN, updates);

        assertEquals("rk-2", DockerComposeUtils.getEnvVariable(compose, EDGE_PATTERN, "CLOUD_ROUTING_KEY"));
        assertFalse(DockerComposeUtils.envHasKey(
                DockerComposeUtils.findServiceEnvironment(compose, EDGE_PATTERN), "NEW_UNRELATED_KEY"));
    }

    @Test
    void setEnvVariables_listForm_overwritesExistingEntryInPlace() {
        ObjectNode compose = createEdgeComposeListEnv("rk-1", "secret-1");
        ArrayNode env = (ArrayNode) DockerComposeUtils.findServiceEnvironment(compose, EDGE_PATTERN);
        int originalSize = env.size();

        DockerComposeUtils.setEnvVariables(compose, EDGE_PATTERN,
                Map.of("CLOUD_ROUTING_KEY", "rk-2", "CLOUD_ROUTING_SECRET", "secret-2"));

        assertEquals(originalSize, env.size());
        assertEquals("rk-2", DockerComposeUtils.getEnvVariable(compose, EDGE_PATTERN, "CLOUD_ROUTING_KEY"));
        assertEquals("secret-2", DockerComposeUtils.getEnvVariable(compose, EDGE_PATTERN, "CLOUD_ROUTING_SECRET"));
    }

    @Test
    void setEnvVariables_listForm_doesNotAddMissingKey() {
        ObjectNode compose = createEdgeComposeListEnv("rk-1", "secret-1");
        int originalSize = ((ArrayNode) DockerComposeUtils.findServiceEnvironment(compose, EDGE_PATTERN)).size();

        DockerComposeUtils.setEnvVariables(compose, EDGE_PATTERN, Map.of("BRAND_NEW_KEY", "v"));

        assertEquals(originalSize,
                ((ArrayNode) DockerComposeUtils.findServiceEnvironment(compose, EDGE_PATTERN)).size());
        assertNull(DockerComposeUtils.getEnvVariable(compose, EDGE_PATTERN, "BRAND_NEW_KEY"));
    }

    @Test
    void setEnvVariables_noOp_whenEmptyUpdatesMap() {
        ObjectNode compose = createEdgeCompose("rk-1", "secret-1");
        DockerComposeUtils.setEnvVariables(compose, EDGE_PATTERN, Map.of());
        assertEquals("rk-1", DockerComposeUtils.getEnvVariable(compose, EDGE_PATTERN, "CLOUD_ROUTING_KEY"));
    }

    @Test
    void setEnvVariables_noOp_whenNoMatchingService() {
        ObjectNode compose = createEdgeCompose("rk-1", "secret-1");
        DockerComposeUtils.setEnvVariables(compose, Pattern.compile("no-such-image"),
                Map.of("CLOUD_ROUTING_KEY", "rk-2"));
        assertEquals("rk-1", DockerComposeUtils.getEnvVariable(compose, EDGE_PATTERN, "CLOUD_ROUTING_KEY"));
    }

    // --- upsertEnvVariables ---

    @Test
    void upsertEnvVariables_mapForm_overwritesExistingAndAddsMissing() {
        ObjectNode compose = createEdgeCompose("rk-1", "secret-1");
        Map<String, String> updates = new LinkedHashMap<>();
        updates.put("CLOUD_ROUTING_KEY", "rk-2");
        updates.put("BRAND_NEW_KEY", "added");
        DockerComposeUtils.upsertEnvVariables(compose, EDGE_PATTERN, updates);

        assertEquals("rk-2", DockerComposeUtils.getEnvVariable(compose, EDGE_PATTERN, "CLOUD_ROUTING_KEY"));
        assertEquals("added", DockerComposeUtils.getEnvVariable(compose, EDGE_PATTERN, "BRAND_NEW_KEY"));
    }

    @Test
    void upsertEnvVariables_listForm_overwritesExistingEntryInPlace() {
        ObjectNode compose = createEdgeComposeListEnv("rk-1", "secret-1");
        ArrayNode env = (ArrayNode) DockerComposeUtils.findServiceEnvironment(compose, EDGE_PATTERN);
        int originalSize = env.size();

        DockerComposeUtils.upsertEnvVariables(compose, EDGE_PATTERN,
                Map.of("CLOUD_ROUTING_KEY", "rk-2", "CLOUD_ROUTING_SECRET", "secret-2"));

        assertEquals(originalSize, env.size());
        assertEquals("rk-2", DockerComposeUtils.getEnvVariable(compose, EDGE_PATTERN, "CLOUD_ROUTING_KEY"));
        assertEquals("secret-2", DockerComposeUtils.getEnvVariable(compose, EDGE_PATTERN, "CLOUD_ROUTING_SECRET"));
    }

    @Test
    void upsertEnvVariables_listForm_appendsMissingKey() {
        ObjectNode compose = createEdgeComposeListEnv("rk-1", "secret-1");
        ArrayNode env = (ArrayNode) DockerComposeUtils.findServiceEnvironment(compose, EDGE_PATTERN);
        int originalSize = env.size();

        DockerComposeUtils.upsertEnvVariables(compose, EDGE_PATTERN, Map.of("BRAND_NEW_KEY", "added"));

        assertEquals(originalSize + 1, env.size());
        assertEquals("BRAND_NEW_KEY=added", env.get(env.size() - 1).asText());
        assertEquals("added", DockerComposeUtils.getEnvVariable(compose, EDGE_PATTERN, "BRAND_NEW_KEY"));
    }

    @Test
    void upsertEnvVariables_listForm_overwritesBareKeyEntry() {
        ArrayNode env = MAPPER.createArrayNode();
        env.add("CLOUD_ROUTING_KEY");
        ObjectNode compose = composeWithEnv(env);

        DockerComposeUtils.upsertEnvVariables(compose, EDGE_PATTERN, Map.of("CLOUD_ROUTING_KEY", "rk-1"));

        assertEquals(1, env.size());
        assertEquals("rk-1", DockerComposeUtils.getEnvVariable(compose, EDGE_PATTERN, "CLOUD_ROUTING_KEY"));
    }

    @Test
    void upsertEnvVariables_noOp_whenEmptyUpdatesMap() {
        ObjectNode compose = createEdgeCompose("rk-1", "secret-1");
        DockerComposeUtils.upsertEnvVariables(compose, EDGE_PATTERN, Map.of());
        assertEquals(4, DockerComposeUtils.findServiceEnvironment(compose, EDGE_PATTERN).size());
        assertEquals("rk-1", DockerComposeUtils.getEnvVariable(compose, EDGE_PATTERN, "CLOUD_ROUTING_KEY"));
    }

    @Test
    void upsertEnvVariables_noOp_whenNoMatchingService() {
        ObjectNode compose = createEdgeCompose("rk-1", "secret-1");
        DockerComposeUtils.upsertEnvVariables(compose, Pattern.compile("no-such-image"),
                Map.of("BRAND_NEW_KEY", "added"));
        assertNull(DockerComposeUtils.getEnvVariable(compose, EDGE_PATTERN, "BRAND_NEW_KEY"));
    }

    @Test
    void upsertEnvVariables_noOp_whenEnvBlockMissing() {
        ObjectNode compose = createEdgeCompose("rk-1", "secret-1");
        ObjectNode service = (ObjectNode) compose.get("services").get("mytbedge");
        service.remove("environment");

        DockerComposeUtils.upsertEnvVariables(compose, EDGE_PATTERN, Map.of("BRAND_NEW_KEY", "added"));

        assertFalse(service.has("environment"));
    }

    // --- getMainImage / setMainImage (upgrade image bump) ---

    @Test
    void getMainImage_returnsImageOfMatchingService() {
        ObjectNode compose = createEdgeCompose("rk-1", "secret-1");
        assertEquals("thingsboard/tb-edge:3.8.0", DockerComposeUtils.getMainImage(compose, EDGE_PATTERN));
    }

    @Test
    void getMainImage_returnsNull_whenNoMatchingService() {
        ObjectNode compose = createEdgeCompose("rk-1", "secret-1");
        ((ObjectNode) compose.get("services").get("mytbedge")).put("image", "postgres:15");
        assertNull(DockerComposeUtils.getMainImage(compose, EDGE_PATTERN));
    }

    @Test
    void getMainImage_returnsNull_whenComposeNull() {
        assertNull(DockerComposeUtils.getMainImage(null, EDGE_PATTERN));
    }

    @Test
    void setMainImage_bumpsImageOfMatchingService() {
        ObjectNode compose = createEdgeCompose("rk-1", "secret-1");
        DockerComposeUtils.setMainImage(compose, EDGE_PATTERN, "thingsboard/tb-edge:4.0.0");
        assertEquals("thingsboard/tb-edge:4.0.0", DockerComposeUtils.getMainImage(compose, EDGE_PATTERN));
    }

    @Test
    void setMainImage_preservesEnvAndOtherKeys() {
        ObjectNode compose = createEdgeCompose("rk-1", "secret-1");
        DockerComposeUtils.setMainImage(compose, EDGE_PATTERN, "thingsboard/tb-edge:4.0.0");
        // Only the image changes; credentials/host env survive the bump.
        assertEquals("rk-1", DockerComposeUtils.getEnvVariable(compose, EDGE_PATTERN, "CLOUD_ROUTING_KEY"));
        assertEquals("tb.cloud", DockerComposeUtils.getEnvVariable(compose, EDGE_PATTERN, "CLOUD_RPC_HOST"));
    }

    @Test
    void setMainImage_noOp_whenNoMatchingService() {
        ObjectNode compose = createEdgeCompose("rk-1", "secret-1");
        DockerComposeUtils.setMainImage(compose, Pattern.compile("no-such-image"), "thingsboard/tb-edge:4.0.0");
        assertEquals("thingsboard/tb-edge:3.8.0", DockerComposeUtils.getMainImage(compose, EDGE_PATTERN));
    }

    // --- envHasKey / envGet ---

    @Test
    void envHasKey_handlesBothForms() {
        ObjectNode mapCompose = createEdgeCompose("rk-1", "secret-1");
        ObjectNode listCompose = createEdgeComposeListEnv("rk-1", "secret-1");

        assertTrue(DockerComposeUtils.envHasKey(
                DockerComposeUtils.findServiceEnvironment(mapCompose, EDGE_PATTERN), "CLOUD_ROUTING_KEY"));
        assertTrue(DockerComposeUtils.envHasKey(
                DockerComposeUtils.findServiceEnvironment(listCompose, EDGE_PATTERN), "CLOUD_ROUTING_KEY"));

        assertFalse(DockerComposeUtils.envHasKey(
                DockerComposeUtils.findServiceEnvironment(mapCompose, EDGE_PATTERN), "MISSING"));
        assertFalse(DockerComposeUtils.envHasKey(
                DockerComposeUtils.findServiceEnvironment(listCompose, EDGE_PATTERN), "MISSING"));

        assertFalse(DockerComposeUtils.envHasKey(null, "CLOUD_ROUTING_KEY"));
    }

    @Test
    void envGet_handlesBothForms() {
        JsonNode mapEnv = DockerComposeUtils.findServiceEnvironment(createEdgeCompose("rk-1", "secret-1"), EDGE_PATTERN);
        JsonNode listEnv = DockerComposeUtils.findServiceEnvironment(createEdgeComposeListEnv("rk-2", "secret-2"), EDGE_PATTERN);

        assertEquals("rk-1", DockerComposeUtils.envGet(mapEnv, "CLOUD_ROUTING_KEY"));
        assertEquals("rk-2", DockerComposeUtils.envGet(listEnv, "CLOUD_ROUTING_KEY"));
        assertNull(DockerComposeUtils.envGet(mapEnv, "MISSING"));
        assertNull(DockerComposeUtils.envGet(listEnv, "MISSING"));
        assertNull(DockerComposeUtils.envGet(null, "CLOUD_ROUTING_KEY"));
    }

    // --- equalsIgnoringEnvKeys — list form ---

    @Test
    void equalsIgnoringEnvKeys_listForm_ignoresDifferencesInSpecifiedKeys() {
        ObjectNode a = createEdgeComposeListEnv("rk-1", "secret-1");
        ObjectNode b = createEdgeComposeListEnv("rk-2", "secret-2");
        assertTrue(DockerComposeUtils.equalsIgnoringEnvKeys(a, b, EDGE_PATTERN, EDGE_CRED_KEYS));
    }

    @Test
    void equalsIgnoringEnvKeys_mixedForms_treatedAsDifferent() {
        // A map and a list with the same logical contents are still different
        // JSON structures — callers that want to compare across shapes must
        // normalise first. This documents the current behaviour.
        ObjectNode mapCompose = createEdgeCompose("rk-1", "secret-1");
        ObjectNode listCompose = createEdgeComposeListEnv("rk-1", "secret-1");
        assertFalse(DockerComposeUtils.equalsIgnoringEnvKeys(mapCompose, listCompose, EDGE_PATTERN, EDGE_CRED_KEYS));
    }

    // --- fixtures ---

    private ObjectNode createEdgeCompose(String routingKey, String secret) {
        ObjectNode env = MAPPER.createObjectNode();
        env.put("CLOUD_ROUTING_KEY", routingKey);
        env.put("CLOUD_ROUTING_SECRET", secret);
        env.put("CLOUD_RPC_HOST", "tb.cloud");
        env.put("CLOUD_RPC_PORT", "7070");
        return composeWithEnv(env);
    }

    private ObjectNode createEdgeComposeListEnv(String routingKey, String secret) {
        ArrayNode env = MAPPER.createArrayNode();
        env.add("CLOUD_ROUTING_KEY=" + routingKey);
        env.add("CLOUD_ROUTING_SECRET=" + secret);
        env.add("CLOUD_RPC_HOST=tb.cloud");
        env.add("CLOUD_RPC_PORT=7070");
        return composeWithEnv(env);
    }

    private ObjectNode composeWithEnv(JsonNode envNode) {
        ObjectNode service = MAPPER.createObjectNode();
        service.put("image", "thingsboard/tb-edge:3.8.0");
        service.set("environment", envNode);

        ObjectNode services = MAPPER.createObjectNode();
        services.set("mytbedge", service);

        ObjectNode compose = MAPPER.createObjectNode();
        compose.set("services", services);
        return compose;
    }
}
