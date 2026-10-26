package com.iot.thingmodel.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.thingmodel.common.Constants;
import com.iot.thingmodel.config.ThingModelProperties;
import com.iot.thingmodel.tb.TbSyncException;
import com.iot.thingmodel.tb.ThingsboardClientProvider;
import com.iot.thingmodel.tsl.CompactSchema;
import com.iot.thingmodel.tsl.TslDefinition;
import com.iot.thingmodel.tsl.TslParser;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.thingsboard.client.ThingsboardClient;
import org.thingsboard.client.model.NodeConnectionInfo;
import org.thingsboard.client.model.PageDataRuleChain;
import org.thingsboard.client.model.RuleChain;
import org.thingsboard.client.model.RuleChainId;
import org.thingsboard.client.model.RuleChainMetaData;
import org.thingsboard.client.model.RuleChainType;
import org.thingsboard.client.model.RuleNode;
import org.thingsboard.client.model.RuleNodeId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TbRuleChainProvisionerTest {

    private static final String PRODUCT_ID = "9HYN9PJNOY";
    private static final String PRODUCT_NAME = "WiFi诊断终端";
    private static final String VERSION = "1.0";
    private static final String CUSTOM_PREFIX = "custom_";
    private static final String CUSTOM_TELEMETRY_TOPIC = "custom.telemetry";
    private static final String CUSTOM_EVENT_TOPIC = "custom.event";
    private static final String CUSTOM_INVALID_TOPIC = "custom.invalid";
    private static final String CUSTOM_KEY_PATTERN = "$[productId]";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TslParser parser = new TslParser(objectMapper);
    private final CompactSchema compactSchema = new CompactSchema(objectMapper);

    @Test
    void createsRuleChainWithThermostatStructureAndKafka() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID chainId = UUID.randomUUID();
        ThingsboardClientProvider provider = mock(ThingsboardClientProvider.class);
        ThingsboardClient client = mock(ThingsboardClient.class);
        when(provider.get(tenantId)).thenReturn(client);
        stubByName(client, chainName(), emptyPage(false));

        RuleChain saved = new RuleChain();
        saved.setId(new RuleChainId().id(chainId));
        when(client.saveRuleChain(any(RuleChain.class))).thenReturn(saved);

        ThingModelProperties properties = properties();
        TbRuleChainProvisioner provisioner = new TbRuleChainProvisioner(provider, properties, objectMapper);

        UUID actual = provisioner.ensure(request(tenantId));

        assertEquals(chainId, actual);
        verify(client).saveRuleChain(any(RuleChain.class));
        verify(client, never()).getRuleChainMetaData(anyString());

        ArgumentCaptor<RuleChain> chainCaptor = ArgumentCaptor.forClass(RuleChain.class);
        verify(client).saveRuleChain(chainCaptor.capture());
        RuleChain created = chainCaptor.getValue();
        assertEquals(chainName(properties), created.getName());
        assertEquals(RuleChainType.CORE, created.getType());
        assertEquals(Boolean.FALSE, created.getRoot());
        assertFalse(Boolean.TRUE.equals(created.getDebugMode()));

        RuleChainMetaData metadata = captureMetadata(client);
        assertEquals(chainId, metadata.getRuleChainId().getId());
        assertEquals(Constants.RuleChain.FIRST_NODE_INDEX, metadata.getFirstNodeIndex());
        assertEquals(12, metadata.getNodes().size());
        assertEquals(11, metadata.getConnections().size());
        assertTrue(metadata.getRuleChainConnections().isEmpty());
        assertTrue(metadata.getNotes().isEmpty());
        assertLayouts(metadata);
        assertEquals(
                List.of(
                        Constants.RuleChain.NODE_NAME_MSG_TYPE_SWITCH,
                        Constants.RuleChain.NODE_NAME_SAVE_TIMESERIES,
                        Constants.RuleChain.NODE_NAME_SAVE_CLIENT_ATTRIBUTES,
                        Constants.RuleChain.NODE_NAME_LOG_RPC_FROM_DEVICE,
                        Constants.RuleChain.NODE_NAME_LOG_OTHER,
                        Constants.RuleChain.NODE_NAME_SEND_RPC,
                        Constants.RuleChain.NODE_NAME_GET_ATTRIBUTES,
                        Constants.RuleChain.NODE_NAME_TRANSFORM,
                        Constants.RuleChain.NODE_NAME_SWITCH,
                        Constants.RuleChain.NODE_NAME_KAFKA_TELEMETRY,
                        Constants.RuleChain.NODE_NAME_KAFKA_EVENT,
                        Constants.RuleChain.NODE_NAME_KAFKA_INVALID),
                metadata.getNodes().stream().map(RuleNode::getName).toList());
        assertEquals(
                List.of(
                        Constants.RuleChain.MSG_TYPE_SWITCH,
                        Constants.RuleChain.SAVE_TIMESERIES,
                        Constants.RuleChain.SAVE_CLIENT_ATTRIBUTES,
                        Constants.RuleChain.LOG,
                        Constants.RuleChain.LOG,
                        Constants.RuleChain.SEND_RPC,
                        Constants.RuleChain.GET_ATTRIBUTES,
                        Constants.RuleChain.TRANSFORM_SCRIPT,
                        Constants.RuleChain.JS_SWITCH,
                        Constants.RuleChain.KAFKA,
                        Constants.RuleChain.KAFKA,
                        Constants.RuleChain.KAFKA),
                metadata.getNodes().stream().map(RuleNode::getType).toList());

        Map<String, RuleNode> nodes = metadata.getNodes().stream()
                .collect(Collectors.toMap(RuleNode::getName, Function.identity()));
        for (RuleNode node : metadata.getNodes()) {
            assertNull(node.getId());
            assertNotNull(node.getExternalId());
            assertEquals(Constants.RuleChain.CONFIGURATION_VERSION, node.getConfigurationVersion());
            assertFalse(Boolean.TRUE.equals(node.getDebugMode()));
        }
        assertExternalId(chainId, Constants.RuleChain.NODE_KEY_MSG_TYPE_SWITCH,
                nodes.get(Constants.RuleChain.NODE_NAME_MSG_TYPE_SWITCH));
        assertExternalId(chainId, Constants.RuleChain.NODE_KEY_SAVE_TIMESERIES,
                nodes.get(Constants.RuleChain.NODE_NAME_SAVE_TIMESERIES));
        assertExternalId(chainId, Constants.RuleChain.NODE_KEY_SAVE_CLIENT_ATTRIBUTES,
                nodes.get(Constants.RuleChain.NODE_NAME_SAVE_CLIENT_ATTRIBUTES));
        assertExternalId(chainId, Constants.RuleChain.NODE_KEY_LOG_RPC_FROM_DEVICE,
                nodes.get(Constants.RuleChain.NODE_NAME_LOG_RPC_FROM_DEVICE));
        assertExternalId(chainId, Constants.RuleChain.NODE_KEY_LOG_OTHER,
                nodes.get(Constants.RuleChain.NODE_NAME_LOG_OTHER));
        assertExternalId(chainId, Constants.RuleChain.NODE_KEY_SEND_RPC,
                nodes.get(Constants.RuleChain.NODE_NAME_SEND_RPC));
        assertExternalId(chainId, Constants.RuleChain.NODE_KEY_GET_ATTRIBUTES,
                nodes.get(Constants.RuleChain.NODE_NAME_GET_ATTRIBUTES));
        assertExternalId(chainId, Constants.RuleChain.NODE_KEY_TRANSFORM,
                nodes.get(Constants.RuleChain.NODE_NAME_TRANSFORM));
        assertExternalId(chainId, Constants.RuleChain.NODE_KEY_SWITCH,
                nodes.get(Constants.RuleChain.NODE_NAME_SWITCH));
        assertExternalId(chainId, Constants.RuleChain.NODE_KEY_KAFKA_TELEMETRY,
                nodes.get(Constants.RuleChain.NODE_NAME_KAFKA_TELEMETRY));
        assertExternalId(chainId, Constants.RuleChain.NODE_KEY_KAFKA_EVENT,
                nodes.get(Constants.RuleChain.NODE_NAME_KAFKA_EVENT));
        assertExternalId(chainId, Constants.RuleChain.NODE_KEY_KAFKA_INVALID,
                nodes.get(Constants.RuleChain.NODE_NAME_KAFKA_INVALID));

        RuleNode msgSwitch = nodes.get(Constants.RuleChain.NODE_NAME_MSG_TYPE_SWITCH);
        assertEquals(Constants.RuleChain.MSG_TYPE_SWITCH_VERSION,
                msgSwitch.getConfiguration().path(Constants.RuleChain.CONFIG_VERSION).asInt());

        RuleNode saveTimeseries = nodes.get(Constants.RuleChain.NODE_NAME_SAVE_TIMESERIES);
        assertEquals(Constants.RuleChain.TIMESERIES_DEFAULT_TTL,
                saveTimeseries.getConfiguration().path(Constants.RuleChain.CONFIG_DEFAULT_TTL).asInt());
        assertFalse(saveTimeseries.getConfiguration()
                .path(Constants.RuleChain.CONFIG_USE_SERVER_TS).asBoolean(true));
        assertEquals(Constants.RuleChain.PROCESSING_TYPE_ON_EVERY_MESSAGE,
                saveTimeseries.getConfiguration()
                        .path(Constants.RuleChain.CONFIG_PROCESSING_SETTINGS)
                        .path(Constants.RuleChain.CONFIG_PROCESSING_TYPE).asText());

        RuleNode saveAttributes = nodes.get(Constants.RuleChain.NODE_NAME_SAVE_CLIENT_ATTRIBUTES);
        assertEquals(Constants.RuleChain.SCOPE_CLIENT,
                saveAttributes.getConfiguration().path(Constants.RuleChain.CONFIG_SCOPE).asText());
        assertFalse(saveAttributes.getConfiguration()
                .path(Constants.RuleChain.CONFIG_NOTIFY_DEVICE).asBoolean(true));
        assertFalse(saveAttributes.getConfiguration()
                .path(Constants.RuleChain.CONFIG_SEND_ATTRIBUTES_UPDATED_NOTIFICATION).asBoolean(true));
        assertTrue(saveAttributes.getConfiguration()
                .path(Constants.RuleChain.CONFIG_UPDATE_ATTRIBUTES_ONLY_ON_VALUE_CHANGE).asBoolean());

        assertLogConfig(nodes.get(Constants.RuleChain.NODE_NAME_LOG_RPC_FROM_DEVICE));
        assertLogConfig(nodes.get(Constants.RuleChain.NODE_NAME_LOG_OTHER));

        RuleNode rpc = nodes.get(Constants.RuleChain.NODE_NAME_SEND_RPC);
        assertEquals(Constants.RuleChain.RPC_TIMEOUT_SECONDS,
                rpc.getConfiguration().path(Constants.RuleChain.CONFIG_TIMEOUT_IN_SECONDS).asInt());

        RuleNode attributes = nodes.get(Constants.RuleChain.NODE_NAME_GET_ATTRIBUTES);
        assertEquals(Constants.RuleChain.FETCH_TO_METADATA,
                attributes.getConfiguration().path(Constants.RuleChain.CONFIG_FETCH_TO).asText());
        assertEquals(
                Constants.RuleChain.SERVER_ATTRIBUTE_NAMES,
                objectMapper.convertValue(
                        attributes.getConfiguration()
                                .path(Constants.RuleChain.CONFIG_SERVER_ATTRIBUTE_NAMES),
                        objectMapper.getTypeFactory()
                                .constructCollectionType(List.class, String.class)));
        assertTrue(attributes.getConfiguration()
                .path(Constants.RuleChain.CONFIG_CLIENT_ATTRIBUTE_NAMES).isEmpty());
        assertTrue(attributes.getConfiguration()
                .path(Constants.RuleChain.CONFIG_SHARED_ATTRIBUTE_NAMES).isEmpty());
        assertTrue(attributes.getConfiguration()
                .path(Constants.RuleChain.CONFIG_LATEST_TS_KEY_NAMES).isEmpty());
        assertFalse(attributes.getConfiguration()
                .path(Constants.RuleChain.CONFIG_TELL_FAILURE_IF_ABSENT).asBoolean(true));
        assertFalse(attributes.getConfiguration()
                .path(Constants.RuleChain.CONFIG_GET_LATEST_VALUE_WITH_TS).asBoolean(true));

        RuleNode transform = nodes.get(Constants.RuleChain.NODE_NAME_TRANSFORM);
        assertEquals(Constants.RuleChain.SCRIPT_LANG_JS,
                transform.getConfiguration().path(Constants.RuleChain.CONFIG_SCRIPT_LANG).asText());
        assertEquals(loadScript(),
                transform.getConfiguration().path(Constants.RuleChain.CONFIG_JS_SCRIPT).asText());
        assertEquals(loadScript(),
                transform.getConfiguration().path(Constants.RuleChain.CONFIG_TBEL_SCRIPT).asText());

        RuleNode route = nodes.get(Constants.RuleChain.NODE_NAME_SWITCH);
        assertEquals(Constants.RuleChain.SCRIPT_LANG_JS,
                route.getConfiguration().path(Constants.RuleChain.CONFIG_SCRIPT_LANG).asText());
        assertEquals(Constants.RuleChain.JS_KIND_SCRIPT,
                route.getConfiguration().path(Constants.RuleChain.CONFIG_JS_SCRIPT).asText());
        assertEquals(Constants.RuleChain.JS_KIND_SCRIPT,
                route.getConfiguration().path(Constants.RuleChain.CONFIG_TBEL_SCRIPT).asText());

        assertKafkaConfig(nodes.get(Constants.RuleChain.NODE_NAME_KAFKA_TELEMETRY),
                properties.getRuleChain().getTopicTelemetry(), properties, chainId,
                Constants.RuleChain.NODE_KEY_KAFKA_TELEMETRY);
        assertKafkaConfig(nodes.get(Constants.RuleChain.NODE_NAME_KAFKA_EVENT),
                properties.getRuleChain().getTopicEvent(), properties, chainId,
                Constants.RuleChain.NODE_KEY_KAFKA_EVENT);
        assertKafkaConfig(nodes.get(Constants.RuleChain.NODE_NAME_KAFKA_INVALID),
                properties.getRuleChain().getTopicInvalid(), properties, chainId,
                Constants.RuleChain.NODE_KEY_KAFKA_INVALID);

        assertEquals(
                List.of(
                        "0->1:" + Constants.RuleChain.CONNECTION_POST_TELEMETRY,
                        "0->2:" + Constants.RuleChain.CONNECTION_POST_ATTRIBUTES,
                        "0->3:" + Constants.RuleChain.CONNECTION_RPC_FROM_DEVICE,
                        "0->5:" + Constants.RuleChain.CONNECTION_RPC_TO_DEVICE,
                        "0->4:" + Constants.RuleChain.CONNECTION_OTHER,
                        "1->6:" + Constants.RuleChain.CONNECTION_SUCCESS,
                        "6->7:" + Constants.RuleChain.CONNECTION_SUCCESS,
                        "7->8:" + Constants.RuleChain.CONNECTION_SUCCESS,
                        "8->9:" + Constants.RuleChain.CONNECTION_TELEMETRY,
                        "8->10:" + Constants.RuleChain.CONNECTION_EVENT,
                        "8->11:" + Constants.RuleChain.CONNECTION_INVALID),
                metadata.getConnections().stream().map(this::connectionKey).toList());
    }

    @Test
    void usesConfiguredPrefixTopicsAndKeyPattern() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID chainId = UUID.randomUUID();
        ThingsboardClientProvider provider = mock(ThingsboardClientProvider.class);
        ThingsboardClient client = mock(ThingsboardClient.class);
        when(provider.get(tenantId)).thenReturn(client);

        ThingModelProperties properties = properties();
        properties.getRuleChain().setNamePrefix(CUSTOM_PREFIX);
        properties.getRuleChain().setTopicTelemetry(CUSTOM_TELEMETRY_TOPIC);
        properties.getRuleChain().setTopicEvent(CUSTOM_EVENT_TOPIC);
        properties.getRuleChain().setTopicInvalid(CUSTOM_INVALID_TOPIC);
        properties.getRuleChain().setKeyPattern(CUSTOM_KEY_PATTERN);
        stubByName(client, chainName(properties), emptyPage(false));

        RuleChain saved = new RuleChain();
        saved.setId(new RuleChainId().id(chainId));
        when(client.saveRuleChain(any(RuleChain.class))).thenReturn(saved);

        new TbRuleChainProvisioner(provider, properties, objectMapper).ensure(request(tenantId));

        ArgumentCaptor<RuleChain> chainCaptor = ArgumentCaptor.forClass(RuleChain.class);
        verify(client).saveRuleChain(chainCaptor.capture());
        assertEquals(CUSTOM_PREFIX + PRODUCT_ID + Constants.RuleChain.NAME_SUFFIX,
                chainCaptor.getValue().getName());

        RuleChainMetaData metadata = captureMetadata(client);
        Map<String, RuleNode> nodes = metadata.getNodes().stream()
                .collect(Collectors.toMap(RuleNode::getName, Function.identity()));
        assertKafkaConfig(nodes.get(Constants.RuleChain.NODE_NAME_KAFKA_TELEMETRY),
                CUSTOM_TELEMETRY_TOPIC, properties, chainId,
                Constants.RuleChain.NODE_KEY_KAFKA_TELEMETRY);
        assertKafkaConfig(nodes.get(Constants.RuleChain.NODE_NAME_KAFKA_EVENT),
                CUSTOM_EVENT_TOPIC, properties, chainId,
                Constants.RuleChain.NODE_KEY_KAFKA_EVENT);
        assertKafkaConfig(nodes.get(Constants.RuleChain.NODE_NAME_KAFKA_INVALID),
                CUSTOM_INVALID_TOPIC, properties, chainId,
                Constants.RuleChain.NODE_KEY_KAFKA_INVALID);
    }

    @Test
    void reusesExistingRuleChainAndPreservesNodeIds() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID chainId = UUID.randomUUID();
        UUID switchNodeId = UUID.randomUUID();
        UUID saveTimeseriesNodeId = UUID.randomUUID();
        ThingsboardClientProvider provider = mock(ThingsboardClientProvider.class);
        ThingsboardClient client = mock(ThingsboardClient.class);
        when(provider.get(tenantId)).thenReturn(client);
        stubByName(client, chainName(), page(existingChain(chainId), false));

        RuleNode existingSwitch = existingNode(
                chainId, Constants.RuleChain.NODE_KEY_MSG_TYPE_SWITCH, switchNodeId);
        existingSwitch.setAdditionalInfo(objectMapper.createObjectNode()
                .put(Constants.RuleChain.CONFIG_LAYOUT_X, 42)
                .put(Constants.RuleChain.CONFIG_LAYOUT_Y, 84));
        RuleNode existingSaveTimeseries = existingNode(
                chainId, Constants.RuleChain.NODE_KEY_SAVE_TIMESERIES, saveTimeseriesNodeId);
        existingSaveTimeseries.setAdditionalInfo(objectMapper.createObjectNode()
                .put(Constants.RuleChain.CONFIG_LAYOUT_X, 43)
                .put(Constants.RuleChain.CONFIG_LAYOUT_Y, 85));
        RuleChainMetaData existing = new RuleChainMetaData(new RuleChainId().id(chainId));
        existing.setNodes(List.of(existingSwitch, existingSaveTimeseries));
        when(client.getRuleChainMetaData(chainId.toString())).thenReturn(existing);

        UUID actual = new TbRuleChainProvisioner(provider, properties(), objectMapper)
                .ensure(request(tenantId));

        assertEquals(chainId, actual);
        verify(client, never()).saveRuleChain(any(RuleChain.class));

        RuleChainMetaData metadata = captureMetadata(client);
        Map<UUID, RuleNode> byExternalId = metadata.getNodes().stream()
                .collect(Collectors.toMap(node -> node.getExternalId().getId(), Function.identity()));
        assertEquals(switchNodeId, byExternalId.get(externalId(chainId,
                Constants.RuleChain.NODE_KEY_MSG_TYPE_SWITCH)).getId().getId());
        assertEquals(saveTimeseriesNodeId, byExternalId.get(externalId(chainId,
                Constants.RuleChain.NODE_KEY_SAVE_TIMESERIES)).getId().getId());
        assertNull(byExternalId.get(externalId(chainId,
                Constants.RuleChain.NODE_KEY_KAFKA_TELEMETRY)).getId());
        RuleNode savedSwitch = byExternalId.get(externalId(chainId,
                Constants.RuleChain.NODE_KEY_MSG_TYPE_SWITCH));
        assertEquals(42, savedSwitch.getAdditionalInfo()
                .path(Constants.RuleChain.CONFIG_LAYOUT_X).asInt());
        assertEquals(84, savedSwitch.getAdditionalInfo()
                .path(Constants.RuleChain.CONFIG_LAYOUT_Y).asInt());
        RuleNode savedKafka = byExternalId.get(externalId(chainId,
                Constants.RuleChain.NODE_KEY_KAFKA_TELEMETRY));
        assertEquals(
                Constants.RuleChain.LAYOUT_START_X
                        + 5 * Constants.RuleChain.LAYOUT_COLUMN_GAP,
                savedKafka.getAdditionalInfo()
                        .path(Constants.RuleChain.CONFIG_LAYOUT_X).asInt());
        assertEquals(Constants.RuleChain.LAYOUT_START_Y,
                savedKafka.getAdditionalInfo()
                        .path(Constants.RuleChain.CONFIG_LAYOUT_Y).asInt());
    }

    @Test
    void findsExistingChainAcrossPages() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID chainId = UUID.randomUUID();
        ThingsboardClientProvider provider = mock(ThingsboardClientProvider.class);
        ThingsboardClient client = mock(ThingsboardClient.class);
        when(provider.get(tenantId)).thenReturn(client);
        when(client.getRuleChains(
                        Constants.ThingsBoard.PAGE_SIZE,
                        0,
                        Constants.ThingsBoard.RULE_CHAIN_TYPE_CORE,
                        chainName(),
                        Constants.ThingsBoard.SORT_PROPERTY_NAME,
                        Constants.ThingsBoard.SORT_ORDER_ASC))
                .thenReturn(emptyPage(true));
        when(client.getRuleChains(
                        Constants.ThingsBoard.PAGE_SIZE,
                        1,
                        Constants.ThingsBoard.RULE_CHAIN_TYPE_CORE,
                        chainName(),
                        Constants.ThingsBoard.SORT_PROPERTY_NAME,
                        Constants.ThingsBoard.SORT_ORDER_ASC))
                .thenReturn(page(existingChain(chainId), false));

        UUID actual = new TbRuleChainProvisioner(provider, properties(), objectMapper)
                .ensure(request(tenantId));

        assertEquals(chainId, actual);
        verify(client, never()).saveRuleChain(any(RuleChain.class));
        verify(client).saveRuleChainMetaData(any(RuleChainMetaData.class), eq(false));
    }

    @Test
    void keepsExternalIdsStableAndUsesServerAssignedIds() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID chainId = UUID.randomUUID();
        ThingsboardClientProvider provider = mock(ThingsboardClientProvider.class);
        ThingsboardClient client = mock(ThingsboardClient.class);
        when(provider.get(tenantId)).thenReturn(client);
        when(client.getRuleChains(
                        Constants.ThingsBoard.PAGE_SIZE,
                        0,
                        Constants.ThingsBoard.RULE_CHAIN_TYPE_CORE,
                        chainName(),
                        Constants.ThingsBoard.SORT_PROPERTY_NAME,
                        Constants.ThingsBoard.SORT_ORDER_ASC))
                .thenReturn(emptyPage(false), page(existingChain(chainId), false));
        when(client.saveRuleChain(any(RuleChain.class))).thenReturn(existingChain(chainId));

        TbRuleChainProvisioner provisioner = new TbRuleChainProvisioner(provider, properties(), objectMapper);
        provisioner.ensure(request(tenantId));

        ArgumentCaptor<RuleChainMetaData> firstCaptor =
                ArgumentCaptor.forClass(RuleChainMetaData.class);
        verify(client).saveRuleChainMetaData(firstCaptor.capture(), eq(false));
        RuleChainMetaData first = firstCaptor.getValue();
        Map<UUID, UUID> assignedIds = first.getNodes().stream()
                .collect(Collectors.toMap(
                        node -> node.getExternalId().getId(),
                        node -> UUID.randomUUID()));
        RuleChainMetaData existing = new RuleChainMetaData(new RuleChainId().id(chainId));
        existing.setNodes(first.getNodes().stream().map(node -> {
            RuleNode saved = new RuleNode();
            saved.setId(new RuleNodeId().id(assignedIds.get(node.getExternalId().getId())));
            saved.setExternalId(node.getExternalId());
            return saved;
        }).toList());
        when(client.getRuleChainMetaData(chainId.toString())).thenReturn(existing);

        provisioner.ensure(request(tenantId));

        ArgumentCaptor<RuleChainMetaData> captor = ArgumentCaptor.forClass(RuleChainMetaData.class);
        verify(client, times(2)).saveRuleChainMetaData(captor.capture(), eq(false));
        RuleChainMetaData second = captor.getAllValues().get(1);
        Map<UUID, UUID> actualIds = second.getNodes().stream()
                .collect(Collectors.toMap(
                        node -> node.getExternalId().getId(),
                        node -> node.getId().getId()));
        assertEquals(assignedIds, actualIds);
        assertEquals(
                first.getNodes().stream().map(node -> node.getExternalId().getId()).toList(),
                second.getNodes().stream().map(node -> node.getExternalId().getId()).toList());
    }

    @Test
    void failsWhenCreateReturnsNull() throws Exception {
        UUID tenantId = UUID.randomUUID();
        ThingsboardClientProvider provider = mock(ThingsboardClientProvider.class);
        ThingsboardClient client = mock(ThingsboardClient.class);
        when(provider.get(tenantId)).thenReturn(client);
        stubByName(client, chainName(), emptyPage(false));
        when(client.saveRuleChain(any(RuleChain.class))).thenReturn(null);

        TbSyncException ex = assertThrows(TbSyncException.class,
                () -> new TbRuleChainProvisioner(provider, properties(), objectMapper)
                        .ensure(request(tenantId)));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getHttpCode());
        verify(client, never()).saveRuleChainMetaData(any(RuleChainMetaData.class), eq(false));
    }

    @Test
    void failsWhenCreatedChainHasNoId() throws Exception {
        UUID tenantId = UUID.randomUUID();
        ThingsboardClientProvider provider = mock(ThingsboardClientProvider.class);
        ThingsboardClient client = mock(ThingsboardClient.class);
        when(provider.get(tenantId)).thenReturn(client);
        stubByName(client, chainName(), emptyPage(false));
        when(client.saveRuleChain(any(RuleChain.class))).thenReturn(new RuleChain());

        TbSyncException ex = assertThrows(TbSyncException.class,
                () -> new TbRuleChainProvisioner(provider, properties(), objectMapper)
                        .ensure(request(tenantId)));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getHttpCode());
        verify(client, never()).saveRuleChainMetaData(any(RuleChainMetaData.class), eq(false));
    }

    @Test
    void skipsWhenDisabled() throws Exception {
        UUID tenantId = UUID.randomUUID();
        ThingsboardClientProvider provider = mock(ThingsboardClientProvider.class);
        ThingModelProperties properties = properties();
        properties.getRuleChain().setEnabled(false);

        UUID actual = new TbRuleChainProvisioner(provider, properties, objectMapper)
                .ensure(request(tenantId));

        assertNull(actual);
        verifyNoInteractions(provider);
    }

    private void assertLayouts(RuleChainMetaData metadata) {
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

    private void assertLogConfig(RuleNode node) {
        assertNotNull(node);
        assertEquals(Constants.RuleChain.SCRIPT_LANG_TBEL,
                node.getConfiguration().path(Constants.RuleChain.CONFIG_SCRIPT_LANG).asText());
        assertEquals(Constants.RuleChain.LOG_SCRIPT,
                node.getConfiguration().path(Constants.RuleChain.CONFIG_JS_SCRIPT).asText());
        assertEquals(Constants.RuleChain.LOG_SCRIPT,
                node.getConfiguration().path(Constants.RuleChain.CONFIG_TBEL_SCRIPT).asText());
    }

    private void assertKafkaConfig(
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

    private void assertExternalId(UUID chainId, String key, RuleNode node) {
        assertEquals(externalId(chainId, key), node.getExternalId().getId());
    }

    private RuleChainMetaData captureMetadata(ThingsboardClient client) {
        ArgumentCaptor<RuleChainMetaData> captor = ArgumentCaptor.forClass(RuleChainMetaData.class);
        verify(client).saveRuleChainMetaData(captor.capture(), eq(false));
        return captor.getValue();
    }

    private UUID externalId(UUID chainId, String key) {
        return UUID.nameUUIDFromBytes(
                (chainId + Constants.RuleChain.NODE_ID_SEPARATOR + key)
                        .getBytes(StandardCharsets.UTF_8));
    }

    private String connectionKey(NodeConnectionInfo connection) {
        return connection.getFromIndex() + "->" + connection.getToIndex() + ":" + connection.getType();
    }

    private RuleNode existingNode(UUID chainId, String key, UUID nodeId) {
        RuleNode node = new RuleNode();
        node.setId(new RuleNodeId().id(nodeId));
        node.setExternalId(new RuleNodeId().id(externalId(chainId, key)));
        return node;
    }

    private void stubByName(ThingsboardClient client, String name, PageDataRuleChain page) {
        when(client.getRuleChains(
                        Constants.ThingsBoard.PAGE_SIZE,
                        Constants.ThingsBoard.FIRST_PAGE,
                        Constants.ThingsBoard.RULE_CHAIN_TYPE_CORE,
                        name,
                        Constants.ThingsBoard.SORT_PROPERTY_NAME,
                        Constants.ThingsBoard.SORT_ORDER_ASC))
                .thenReturn(page);
    }

    private RuleChain existingChain(UUID chainId) {
        RuleChain chain = new RuleChain();
        chain.setId(new RuleChainId().id(chainId));
        chain.setName(chainName());
        chain.setType(RuleChainType.CORE);
        chain.setRoot(false);
        return chain;
    }

    private PageDataRuleChain emptyPage(boolean hasNext) {
        return page(List.of(), hasNext);
    }

    private PageDataRuleChain page(RuleChain data, boolean hasNext) {
        return page(List.of(data), hasNext);
    }

    private PageDataRuleChain page(List<RuleChain> data, boolean hasNext) {
        PageDataRuleChain page = new PageDataRuleChain(null, null, hasNext);
        page.setData(data);
        return page;
    }

    private String chainName() {
        return chainName(properties());
    }

    private String chainName(ThingModelProperties properties) {
        return properties.getRuleChain().getNamePrefix()
                + PRODUCT_ID
                + Constants.RuleChain.NAME_SUFFIX;
    }

    private ThingModelProperties properties() {
        ThingModelProperties properties = new ThingModelProperties();
        properties.getRuleChain().setKafkaBootstrap(
                "test-kafka-" + UUID.randomUUID() + ":9092");
        return properties;
    }

    private TbSyncRequest request(UUID tenantId) throws Exception {
        TslDefinition definition = parser.parse(load());
        CompactSchema.CompactResult compact = compactSchema.generate(definition, false);
        return new TbSyncRequest(
                tenantId,
                PRODUCT_ID,
                PRODUCT_NAME,
                PRODUCT_ID,
                "1",
                VERSION,
                definition.raw(),
                definition,
                compact);
    }

    private JsonNode load() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/tsl/valid-tsl.json")) {
            return objectMapper.readTree(input);
        }
    }

    private String loadScript() throws Exception {
        try (InputStream input = getClass().getResourceAsStream(Constants.RuleChain.SCRIPT_RESOURCE)) {
            assertNotNull(input);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
