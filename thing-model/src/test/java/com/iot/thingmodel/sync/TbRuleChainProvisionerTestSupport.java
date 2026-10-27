package com.iot.thingmodel.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.thingmodel.common.Constants;
import com.iot.thingmodel.config.ThingModelProperties;
import com.iot.thingmodel.support.TbClientStubs;
import com.iot.thingmodel.support.TslFixtures;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.thingsboard.client.ThingsboardClient;
import org.thingsboard.client.model.NodeConnectionInfo;
import org.thingsboard.client.model.PageDataRuleChain;
import org.thingsboard.client.model.RuleChain;
import org.thingsboard.client.model.RuleChainId;
import org.thingsboard.client.model.RuleChainMetaData;
import org.thingsboard.client.model.RuleNode;
import org.thingsboard.client.model.RuleNodeId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

abstract class TbRuleChainProvisionerTestSupport {

    protected static final String PRODUCT_ID = TslFixtures.PRODUCT_ID;
    protected static final String PRODUCT_NAME = TslFixtures.PRODUCT_NAME;
    protected static final String VERSION = TslFixtures.VERSION;
    protected static final String CUSTOM_TELEMETRY_TOPIC = "custom.telemetry";
    protected static final String CUSTOM_EVENT_TOPIC = "custom.event";
    protected static final String CUSTOM_INVALID_TOPIC = "custom.invalid";
    protected static final String CUSTOM_KEY_PATTERN = "$[productId]";

    protected final ObjectMapper objectMapper = new ObjectMapper();

    protected ThingModelProperties properties() {
        ThingModelProperties properties = new ThingModelProperties();
        properties.getRuleChain().setKafkaBootstrap(
                "test-kafka-" + UUID.randomUUID() + ":9092");
        return properties;
    }

    protected TbSyncRequest request(UUID tenantId) {
        return TslFixtures.syncRequest(objectMapper, tenantId);
    }

    protected String chainName() {
        return PRODUCT_ID;
    }

    protected static void stubByName(
            ThingsboardClient client, String name, PageDataRuleChain page) {
        TbClientStubs.stubRuleChainsByName(client, name, page);
    }

    protected RuleChain existingChain(UUID chainId) {
        RuleChain chain = new RuleChain();
        chain.setId(new RuleChainId().id(chainId));
        chain.setName(chainName());
        chain.setType(org.thingsboard.client.model.RuleChainType.CORE);
        chain.setRoot(false);
        return chain;
    }

    protected RuleNode existingNode(UUID chainId, String key, UUID nodeId) {
        RuleNode node = new RuleNode();
        node.setId(new RuleNodeId().id(nodeId));
        node.setExternalId(new RuleNodeId().id(externalId(chainId, key)));
        return node;
    }

    protected RuleChainMetaData captureMetadata(ThingsboardClient client) {
        org.mockito.ArgumentCaptor<RuleChainMetaData> captor =
                org.mockito.ArgumentCaptor.forClass(RuleChainMetaData.class);
        verify(client).saveRuleChainMetaData(captor.capture(), eq(false));
        return captor.getValue();
    }

    protected void assertExternalId(UUID chainId, String key, RuleNode node) {
        assertEquals(externalId(chainId, key), node.getExternalId().getId());
    }

    protected void assertKafkaConfig(
            RuleNode node,
            String expectedTopic,
            ThingModelProperties properties,
            UUID chainId,
            String nodeKey) {
        assertNotNull(node);
        assertEquals(Constants.RuleChain.KAFKA, node.getType());
        assertEquals(expectedTopic,
                node.getConfiguration().path(Constants.RuleChain.CONFIG_TOPIC_PATTERN).asText());
        assertEquals(properties.getRuleChain().getKeyPattern(),
                node.getConfiguration().path(Constants.RuleChain.CONFIG_KEY_PATTERN).asText());
        assertEquals(properties.getRuleChain().getKafkaBootstrap(),
                node.getConfiguration().path(Constants.RuleChain.CONFIG_BOOTSTRAP_SERVERS).asText());
        assertEquals(Constants.Kafka.RETRIES,
                node.getConfiguration().path(Constants.RuleChain.CONFIG_RETRIES).asInt());
        assertEquals(Constants.Kafka.BATCH_SIZE,
                node.getConfiguration().path(Constants.RuleChain.CONFIG_BATCH_SIZE).asInt());
        assertEquals(Constants.Kafka.LINGER,
                node.getConfiguration().path(Constants.RuleChain.CONFIG_LINGER).asInt());
        assertEquals(Constants.Kafka.BUFFER_MEMORY,
                node.getConfiguration().path(Constants.RuleChain.CONFIG_BUFFER_MEMORY).asInt());
        assertEquals(Constants.Kafka.ACKS_ALL,
                node.getConfiguration().path(Constants.RuleChain.CONFIG_ACKS).asText());
        assertTrue(node.getConfiguration()
                .path(Constants.RuleChain.CONFIG_OTHER_PROPERTIES).isEmpty());
        assertFalse(node.getConfiguration()
                .path(Constants.RuleChain.CONFIG_ADD_METADATA_AS_HEADERS).asBoolean(true));
        assertEquals(Constants.Kafka.HEADERS_CHARSET_UTF8,
                node.getConfiguration().path(Constants.RuleChain.CONFIG_HEADERS_CHARSET).asText());
        assertExternalId(chainId, nodeKey, node);
    }

    protected void assertLayouts(RuleChainMetaData metadata) {
        Set<String> positions = new HashSet<>();
        for (RuleNode node : metadata.getNodes()) {
            JsonNode additionalInfo = node.getAdditionalInfo();
            assertNotNull(additionalInfo);
            assertTrue(additionalInfo.hasNonNull(Constants.RuleChain.CONFIG_LAYOUT_X));
            assertTrue(additionalInfo.hasNonNull(Constants.RuleChain.CONFIG_LAYOUT_Y));
            String position = additionalInfo.get(Constants.RuleChain.CONFIG_LAYOUT_X).asInt()
                    + ","
                    + additionalInfo.get(Constants.RuleChain.CONFIG_LAYOUT_Y).asInt();
            assertTrue(positions.add(position), "duplicate layout " + position);
        }
    }

    protected void assertLogConfig(RuleNode node) {
        assertNotNull(node);
        assertEquals(Constants.RuleChain.SCRIPT_LANG_TBEL,
                node.getConfiguration().path(Constants.RuleChain.CONFIG_SCRIPT_LANG).asText());
        assertEquals(Constants.RuleChain.LOG_SCRIPT,
                node.getConfiguration().path(Constants.RuleChain.CONFIG_JS_SCRIPT).asText());
        assertEquals(Constants.RuleChain.LOG_SCRIPT,
                node.getConfiguration().path(Constants.RuleChain.CONFIG_TBEL_SCRIPT).asText());
    }

    protected String connectionKey(NodeConnectionInfo connection) {
        return connection.getFromIndex() + "->" + connection.getToIndex() + ":" + connection.getType();
    }

    protected static UUID externalId(UUID chainId, String key) {
        return UUID.nameUUIDFromBytes(
                (chainId + Constants.RuleChain.NODE_ID_SEPARATOR + key)
                        .getBytes(StandardCharsets.UTF_8));
    }

    protected static PageDataRuleChain emptyPage(boolean hasNext) {
        return page(List.of(), hasNext);
    }

    protected static PageDataRuleChain page(RuleChain data, boolean hasNext) {
        return page(List.of(data), hasNext);
    }

    protected static PageDataRuleChain page(List<RuleChain> data, boolean hasNext) {
        PageDataRuleChain page = new PageDataRuleChain(null, null, hasNext);
        page.setData(data);
        return page;
    }

    protected String loadNormalizeScript() {
        try (java.io.InputStream input = getClass().getResourceAsStream(
                Constants.RuleChain.NORMALIZE_SCRIPT_RESOURCE)) {
            assertNotNull(input);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (java.io.IOException ex) {
            throw new java.io.UncheckedIOException(ex);
        }
    }
}
