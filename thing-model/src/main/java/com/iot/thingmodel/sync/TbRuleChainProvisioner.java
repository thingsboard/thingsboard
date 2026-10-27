package com.iot.thingmodel.sync;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iot.thingmodel.config.ThingModelProperties;
import com.iot.thingmodel.tb.TbSyncException;
import com.iot.thingmodel.tb.ThingsboardClientProvider;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.thingsboard.client.ThingsboardClient;
import org.thingsboard.client.model.NodeConnectionInfo;
import org.thingsboard.client.model.PageDataRuleChain;
import org.thingsboard.client.model.RuleChain;
import org.thingsboard.client.model.RuleChainId;
import org.thingsboard.client.model.RuleChainMetaData;
import org.thingsboard.client.model.RuleChainType;
import org.thingsboard.client.model.RuleNode;
import org.thingsboard.client.model.RuleNodeId;

import static com.iot.thingmodel.common.Constants.RuleChain.*;
import static com.iot.thingmodel.common.Constants.Kafka.*;

@Component
public class TbRuleChainProvisioner {

    private final ThingsboardClientProvider clientProvider;
    private final ThingModelProperties properties;
    private final ObjectMapper objectMapper;
    private final String scriptTemplate;
    private final String normalizeScript;

    public TbRuleChainProvisioner(
            ThingsboardClientProvider clientProvider,
            ThingModelProperties properties,
            ObjectMapper objectMapper) {
        this.clientProvider = clientProvider;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.scriptTemplate = loadScript(SCRIPT_RESOURCE);
        this.normalizeScript = loadScript(NORMALIZE_SCRIPT_RESOURCE);
    }

    public UUID ensure(TbSyncRequest request) {
        if (!properties.getRuleChain().isEnabled()) {
            return null;
        }
        ThingsboardClient client = clientProvider.get(request.tenantId());
        String name = request.productId();
        RuleChain existingChain = findByName(client, name);
        boolean existed = existingChain != null;
        RuleChain chain = existingChain;

        if (chain == null) {
            String legacyName = LEGACY_NAME_PREFIX + request.productId() + LEGACY_NAME_SUFFIX;
            RuleChain legacyChain = findByName(client, legacyName);
            if (legacyChain != null) {
                legacyChain.setName(name);
                chain = client.saveRuleChain(legacyChain);
                existed = true;
            }
        }
        if (chain == null) {
            chain = new RuleChain();
            chain.setName(name);
            chain.setType(RuleChainType.CORE);
            chain.setRoot(false);
            chain.setDebugMode(false);
            chain = client.saveRuleChain(chain);
        }
        if (chain == null || chain.getId() == null) {
            throw new TbSyncException("ThingsBoard 未返回规则链 ID", 500, null);
        }

        UUID chainId = chain.getId().getId();
        RuleChainMetaData metadata = buildMetadata(chainId, request);
        if (existed) {
            applyExistingNodeIds(metadata, client.getRuleChainMetaData(chainId.toString()));
        }
        client.saveRuleChainMetaData(metadata, false);
        return chainId;
    }

    private RuleChain findByName(ThingsboardClient client, String name) {
        for (int page = 0; page < 10; page++) {
            PageDataRuleChain result = client.getRuleChains(100, page, "CORE", name, "name", "ASC");
            if (result == null || result.getData() == null) {
                return null;
            }
            for (RuleChain chain : result.getData()) {
                if (name.equals(chain.getName())) {
                    return chain;
                }
            }
            if (!Boolean.TRUE.equals(result.getHasNext())) {
                return null;
            }
        }
        return null;
    }

    private RuleChainMetaData buildMetadata(UUID chainId, TbSyncRequest request) {
        String validationScript = renderValidationScript(request);
        List<NodeSpec> specs = List.of(
                new NodeSpec(NODE_KEY_MSG_TYPE_SWITCH, MSG_TYPE_SWITCH,
                        NODE_NAME_MSG_TYPE_SWITCH, 0, 2, msgTypeSwitchConfig()),
                new NodeSpec(NODE_KEY_SAVE_TIMESERIES, SAVE_TIMESERIES,
                        NODE_NAME_SAVE_TIMESERIES, 4, 0, saveTimeseriesConfig()),
                new NodeSpec(NODE_KEY_SAVE_CLIENT_ATTRIBUTES, SAVE_CLIENT_ATTRIBUTES,
                        NODE_NAME_SAVE_CLIENT_ATTRIBUTES, 1, 1, saveClientAttributesConfig()),
                new NodeSpec(NODE_KEY_LOG_RPC_FROM_DEVICE, LOG,
                        NODE_NAME_LOG_RPC_FROM_DEVICE, 1, 2, logConfig()),
                new NodeSpec(NODE_KEY_LOG_OTHER, LOG,
                        NODE_NAME_LOG_OTHER, 1, 3, logConfig()),
                new NodeSpec(NODE_KEY_SEND_RPC, SEND_RPC,
                        NODE_NAME_SEND_RPC, 1, 4, sendRpcConfig()),
                new NodeSpec(NODE_KEY_GET_ATTRIBUTES, GET_ATTRIBUTES,
                        NODE_NAME_GET_ATTRIBUTES, 1, 0, getAttributesConfig()),
                new NodeSpec(NODE_KEY_TRANSFORM, TRANSFORM_SCRIPT,
                        NODE_NAME_TRANSFORM, 5, 1, normalizeConfig()),
                new NodeSpec(NODE_KEY_SWITCH, JS_SWITCH,
                        NODE_NAME_SWITCH, 6, 1, jsSwitchConfig()),
                new NodeSpec(NODE_KEY_KAFKA_TELEMETRY, KAFKA,
                        NODE_NAME_KAFKA_TELEMETRY, 7, 0,
                        kafkaConfig(properties.getRuleChain().getTopicTelemetry())),
                new NodeSpec(NODE_KEY_KAFKA_EVENT, KAFKA,
                        NODE_NAME_KAFKA_EVENT, 7, 1,
                        kafkaConfig(properties.getRuleChain().getTopicEvent())),
                new NodeSpec(NODE_KEY_KAFKA_INVALID, KAFKA,
                        NODE_NAME_KAFKA_INVALID, 7, 2,
                        kafkaConfig(properties.getRuleChain().getTopicInvalid())),
                new NodeSpec(NODE_KEY_VALIDATE, TRANSFORM_SCRIPT,
                        NODE_NAME_VALIDATE, 2, 0, validationConfig(validationScript)),
                new NodeSpec(NODE_KEY_VALIDATION_SWITCH, JS_SWITCH,
                        NODE_NAME_VALIDATION_SWITCH, 3, 0, validationSwitchConfig()));
        List<RuleNode> nodes = specs.stream().map(spec -> node(chainId, spec)).toList();

        List<NodeConnectionInfo> connections = List.of(
                connection(NODE_MSG_TYPE_SWITCH, NODE_GET_ATTRIBUTES, CONNECTION_POST_TELEMETRY),
                connection(NODE_MSG_TYPE_SWITCH, NODE_SAVE_CLIENT_ATTRIBUTES, CONNECTION_POST_ATTRIBUTES),
                connection(NODE_MSG_TYPE_SWITCH, NODE_LOG_RPC_FROM_DEVICE, CONNECTION_RPC_FROM_DEVICE),
                connection(NODE_MSG_TYPE_SWITCH, NODE_SEND_RPC, CONNECTION_RPC_TO_DEVICE),
                connection(NODE_MSG_TYPE_SWITCH, NODE_LOG_OTHER, CONNECTION_OTHER),
                connection(NODE_GET_ATTRIBUTES, NODE_VALIDATE, CONNECTION_SUCCESS),
                connection(NODE_VALIDATE, NODE_VALIDATION_SWITCH, CONNECTION_SUCCESS),
                connection(NODE_VALIDATION_SWITCH, NODE_SAVE_TIMESERIES, CONNECTION_VALID),
                connection(NODE_VALIDATION_SWITCH, NODE_TRANSFORM, CONNECTION_INVALID),
                connection(NODE_SAVE_TIMESERIES, NODE_TRANSFORM, CONNECTION_SUCCESS),
                connection(NODE_TRANSFORM, NODE_SWITCH, CONNECTION_SUCCESS),
                connection(NODE_SWITCH, NODE_KAFKA_TELEMETRY, CONNECTION_TELEMETRY),
                connection(NODE_SWITCH, NODE_KAFKA_EVENT, CONNECTION_EVENT),
                connection(NODE_SWITCH, NODE_KAFKA_INVALID, CONNECTION_INVALID));

        RuleChainMetaData metadata = new RuleChainMetaData(new RuleChainId().id(chainId));
        metadata.setFirstNodeIndex(FIRST_NODE_INDEX);
        metadata.setNodes(nodes);
        metadata.setConnections(connections);
        metadata.setRuleChainConnections(List.of());
        metadata.setNotes(List.of());
        return metadata;
    }

    private void applyExistingNodeIds(RuleChainMetaData metadata, RuleChainMetaData existing) {
        if (existing == null || existing.getNodes() == null) {
            return;
        }
        Map<UUID, RuleNode> existingNodes = new HashMap<>();
        for (RuleNode node : existing.getNodes()) {
            if (node.getExternalId() != null && node.getExternalId().getId() != null) {
                existingNodes.put(node.getExternalId().getId(), node);
            }
        }
        for (RuleNode node : metadata.getNodes()) {
            RuleNode existingNode = existingNodes.get(node.getExternalId().getId());
            if (existingNode == null) {
                continue;
            }
            if (existingNode.getId() != null) {
                node.setId(existingNode.getId());
            }
            if (hasLayout(existingNode.getAdditionalInfo())) {
                node.setAdditionalInfo(existingNode.getAdditionalInfo().deepCopy());
            }
        }
    }

    private boolean hasLayout(JsonNode additionalInfo) {
        return additionalInfo != null
                && additionalInfo.hasNonNull(CONFIG_LAYOUT_X)
                && additionalInfo.hasNonNull(CONFIG_LAYOUT_Y);
    }

    private RuleNode node(UUID chainId, NodeSpec spec) {
        RuleNode node = new RuleNode();
        node.setExternalId(new RuleNodeId().id(externalId(chainId, spec.key())));
        node.setType(spec.type());
        node.setName(spec.name());
        node.setConfiguration(spec.configuration());
        node.setConfigurationVersion(CONFIGURATION_VERSION);
        node.setDebugMode(false);
        ObjectNode layout = objectMapper.createObjectNode();
        layout.put(CONFIG_LAYOUT_X, LAYOUT_START_X + spec.column() * LAYOUT_COLUMN_GAP);
        layout.put(CONFIG_LAYOUT_Y, LAYOUT_START_Y + spec.row() * LAYOUT_ROW_GAP);
        node.setAdditionalInfo(layout);
        return node;
    }

    private NodeConnectionInfo connection(int from, int to, String type) {
        NodeConnectionInfo connection = new NodeConnectionInfo();
        connection.setFromIndex(from);
        connection.setToIndex(to);
        connection.setType(type);
        return connection;
    }

    private UUID externalId(UUID chainId, String key) {
        return UUID.nameUUIDFromBytes(
                (chainId + NODE_ID_SEPARATOR + key).getBytes(StandardCharsets.UTF_8));
    }

    private JsonNode msgTypeSwitchConfig() {
        ObjectNode node = objectMapper.createObjectNode();
        node.put(CONFIG_VERSION, MSG_TYPE_SWITCH_VERSION);
        return node;
    }

    private JsonNode saveTimeseriesConfig() {
        ObjectNode node = objectMapper.createObjectNode();
        node.put(CONFIG_DEFAULT_TTL, TIMESERIES_DEFAULT_TTL);
        node.put(CONFIG_USE_SERVER_TS, false);
        node.set(CONFIG_PROCESSING_SETTINGS, processingSettings());
        return node;
    }

    private JsonNode saveClientAttributesConfig() {
        ObjectNode node = objectMapper.createObjectNode();
        node.set(CONFIG_PROCESSING_SETTINGS, processingSettings());
        node.put(CONFIG_SCOPE, SCOPE_CLIENT);
        node.put(CONFIG_NOTIFY_DEVICE, false);
        node.put(CONFIG_SEND_ATTRIBUTES_UPDATED_NOTIFICATION, false);
        node.put(CONFIG_UPDATE_ATTRIBUTES_ONLY_ON_VALUE_CHANGE, true);
        return node;
    }

    private ObjectNode processingSettings() {
        ObjectNode settings = objectMapper.createObjectNode();
        settings.put(CONFIG_PROCESSING_TYPE, PROCESSING_TYPE_ON_EVERY_MESSAGE);
        return settings;
    }

    private JsonNode logConfig() {
        ObjectNode node = objectMapper.createObjectNode();
        node.put(CONFIG_SCRIPT_LANG, SCRIPT_LANG_TBEL);
        node.put(CONFIG_JS_SCRIPT, LOG_SCRIPT);
        node.put(CONFIG_TBEL_SCRIPT, LOG_SCRIPT);
        return node;
    }

    private JsonNode sendRpcConfig() {
        ObjectNode node = objectMapper.createObjectNode();
        node.put(CONFIG_TIMEOUT_IN_SECONDS, RPC_TIMEOUT_SECONDS);
        return node;
    }

    private JsonNode getAttributesConfig() {
        ObjectNode node = objectMapper.createObjectNode();
        node.put(CONFIG_FETCH_TO, FETCH_TO_METADATA);
        node.set(CONFIG_CLIENT_ATTRIBUTE_NAMES, objectMapper.createArrayNode());
        node.set(CONFIG_SHARED_ATTRIBUTE_NAMES, objectMapper.createArrayNode());
        ArrayNode serverAttributes = node.putArray(CONFIG_SERVER_ATTRIBUTE_NAMES);
        SERVER_ATTRIBUTE_NAMES.forEach(serverAttributes::add);
        node.set(CONFIG_LATEST_TS_KEY_NAMES, objectMapper.createArrayNode());
        node.put(CONFIG_TELL_FAILURE_IF_ABSENT, false);
        node.put(CONFIG_GET_LATEST_VALUE_WITH_TS, false);
        return node;
    }

    private JsonNode validationConfig(String script) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put(CONFIG_SCRIPT_LANG, SCRIPT_LANG_JS);
        node.put(CONFIG_JS_SCRIPT, script);
        node.put(CONFIG_TBEL_SCRIPT, script);
        return node;
    }

    private JsonNode normalizeConfig() {
        ObjectNode node = objectMapper.createObjectNode();
        node.put(CONFIG_SCRIPT_LANG, SCRIPT_LANG_JS);
        node.put(CONFIG_JS_SCRIPT, normalizeScript);
        node.put(CONFIG_TBEL_SCRIPT, normalizeScript);
        return node;
    }

    private JsonNode jsSwitchConfig() {
        ObjectNode node = objectMapper.createObjectNode();
        node.put(CONFIG_SCRIPT_LANG, SCRIPT_LANG_JS);
        node.put(CONFIG_JS_SCRIPT, JS_KIND_SCRIPT);
        node.put(CONFIG_TBEL_SCRIPT, JS_KIND_SCRIPT);
        return node;
    }

    private JsonNode validationSwitchConfig() {
        ObjectNode node = objectMapper.createObjectNode();
        node.put(CONFIG_SCRIPT_LANG, SCRIPT_LANG_JS);
        node.put(CONFIG_JS_SCRIPT, JS_VALIDATION_SCRIPT);
        node.put(CONFIG_TBEL_SCRIPT, JS_VALIDATION_SCRIPT);
        return node;
    }

    private JsonNode kafkaConfig(String topic) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put(CONFIG_TOPIC_PATTERN, topic);
        node.put(CONFIG_KEY_PATTERN, properties.getRuleChain().getKeyPattern());
        node.put(CONFIG_BOOTSTRAP_SERVERS, properties.getRuleChain().getKafkaBootstrap());
        node.put(CONFIG_RETRIES, RETRIES);
        node.put(CONFIG_BATCH_SIZE, BATCH_SIZE);
        node.put(CONFIG_LINGER, LINGER);
        node.put(CONFIG_BUFFER_MEMORY, BUFFER_MEMORY);
        node.put(CONFIG_ACKS, ACKS_ALL);
        node.set(CONFIG_OTHER_PROPERTIES, objectMapper.createObjectNode());
        node.put(CONFIG_ADD_METADATA_AS_HEADERS, false);
        node.put(CONFIG_HEADERS_CHARSET, HEADERS_CHARSET_UTF8);
        return node;
    }

    private record NodeSpec(
            String key,
            String type,
            String name,
            int column,
            int row,
            JsonNode configuration) {
    }

    private String renderValidationScript(TbSyncRequest request) {
        try {
            String schemaLiteral = objectMapper.writeValueAsString(request.compact().json());
            return scriptTemplate.replace(SCRIPT_SCHEMA_PLACEHOLDER, schemaLiteral);
        } catch (JsonProcessingException ex) {
            throw new TbSyncException("物模型 schema 序列化失败", 500, null);
        }
    }

    private String loadScript(String resource) {
        try (InputStream input = getClass().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("Missing " + resource);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load rule chain script", ex);
        }
    }
}
