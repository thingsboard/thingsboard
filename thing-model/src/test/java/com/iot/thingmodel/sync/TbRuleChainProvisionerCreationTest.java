package com.iot.thingmodel.sync;

import com.iot.thingmodel.common.Constants;
import com.iot.thingmodel.config.ThingModelProperties;
import com.iot.thingmodel.support.TbClientStubs;
import com.iot.thingmodel.support.TslFixtures;
import com.iot.thingmodel.tb.ThingsboardClientProvider;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.client.ThingsboardClient;
import org.thingsboard.client.model.RuleChain;
import org.thingsboard.client.model.RuleChainMetaData;
import org.thingsboard.client.model.RuleChainType;
import org.thingsboard.client.model.RuleNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TbRuleChainProvisionerCreationTest extends TbRuleChainProvisionerTestSupport {

    @Test
    void createsRuleChainWithThermostatStructureAndKafka() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID chainId = UUID.randomUUID();
        ThingsboardClientProvider provider = mock(ThingsboardClientProvider.class);
        ThingsboardClient client = mock(ThingsboardClient.class);
        when(provider.get(tenantId)).thenReturn(client);
        TbClientStubs.stubNoRuleChains(client);
        TbClientStubs.stubRuleChainCreated(client, chainId);

        ThingModelProperties properties = properties();
        TbRuleChainProvisioner provisioner =
                new TbRuleChainProvisioner(provider, properties, objectMapper);

        UUID actual = provisioner.ensure(request(tenantId));

        assertEquals(chainId, actual);
        verify(client).saveRuleChain(any(RuleChain.class));
        verify(client, never()).getRuleChainMetaData(anyString());

        ArgumentCaptor<RuleChain> chainCaptor = ArgumentCaptor.forClass(RuleChain.class);
        verify(client).saveRuleChain(chainCaptor.capture());
        RuleChain created = chainCaptor.getValue();
        assertEquals(chainName(), created.getName());
        assertEquals(RuleChainType.CORE, created.getType());
        assertEquals(Boolean.FALSE, created.getRoot());
        assertFalse(Boolean.TRUE.equals(created.getDebugMode()));

        RuleChainMetaData metadata = captureMetadata(client);
        assertEquals(chainId, metadata.getRuleChainId().getId());
        assertEquals(Constants.RuleChain.FIRST_NODE_INDEX, metadata.getFirstNodeIndex());
        assertEquals(14, metadata.getNodes().size());
        assertEquals(14, metadata.getConnections().size());
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
                        Constants.RuleChain.NODE_NAME_KAFKA_INVALID,
                        Constants.RuleChain.NODE_NAME_VALIDATE,
                        Constants.RuleChain.NODE_NAME_VALIDATION_SWITCH),
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
                        Constants.RuleChain.KAFKA,
                        Constants.RuleChain.TRANSFORM_SCRIPT,
                        Constants.RuleChain.JS_SWITCH),
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
        assertExternalId(chainId, Constants.RuleChain.NODE_KEY_VALIDATE,
                nodes.get(Constants.RuleChain.NODE_NAME_VALIDATE));
        assertExternalId(chainId, Constants.RuleChain.NODE_KEY_VALIDATION_SWITCH,
                nodes.get(Constants.RuleChain.NODE_NAME_VALIDATION_SWITCH));

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
        assertEquals(loadNormalizeScript(),
                transform.getConfiguration().path(Constants.RuleChain.CONFIG_JS_SCRIPT).asText());
        assertEquals(loadNormalizeScript(),
                transform.getConfiguration().path(Constants.RuleChain.CONFIG_TBEL_SCRIPT).asText());

        RuleNode route = nodes.get(Constants.RuleChain.NODE_NAME_SWITCH);
        assertEquals(Constants.RuleChain.SCRIPT_LANG_JS,
                route.getConfiguration().path(Constants.RuleChain.CONFIG_SCRIPT_LANG).asText());
        assertEquals(Constants.RuleChain.JS_KIND_SCRIPT,
                route.getConfiguration().path(Constants.RuleChain.CONFIG_JS_SCRIPT).asText());
        assertEquals(Constants.RuleChain.JS_KIND_SCRIPT,
                route.getConfiguration().path(Constants.RuleChain.CONFIG_TBEL_SCRIPT).asText());

        RuleNode validate = nodes.get(Constants.RuleChain.NODE_NAME_VALIDATE);
        assertEquals(Constants.RuleChain.SCRIPT_LANG_JS,
                validate.getConfiguration().path(Constants.RuleChain.CONFIG_SCRIPT_LANG).asText());
        String validationScript = validate.getConfiguration()
                .path(Constants.RuleChain.CONFIG_JS_SCRIPT).asText();
        assertTrue(validationScript.contains(TslFixtures.PRODUCT_ID));
        assertTrue(validationScript.contains("accX"));
        assertFalse(validationScript.contains(Constants.RuleChain.SCRIPT_SCHEMA_PLACEHOLDER));
        assertEquals(validationScript,
                validate.getConfiguration().path(Constants.RuleChain.CONFIG_TBEL_SCRIPT).asText());

        RuleNode validationSwitch = nodes.get(Constants.RuleChain.NODE_NAME_VALIDATION_SWITCH);
        assertEquals(Constants.RuleChain.SCRIPT_LANG_JS,
                validationSwitch.getConfiguration()
                        .path(Constants.RuleChain.CONFIG_SCRIPT_LANG).asText());
        assertEquals(Constants.RuleChain.JS_VALIDATION_SCRIPT,
                validationSwitch.getConfiguration()
                        .path(Constants.RuleChain.CONFIG_JS_SCRIPT).asText());

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
                        "0->6:" + Constants.RuleChain.CONNECTION_POST_TELEMETRY,
                        "0->2:" + Constants.RuleChain.CONNECTION_POST_ATTRIBUTES,
                        "0->3:" + Constants.RuleChain.CONNECTION_RPC_FROM_DEVICE,
                        "0->5:" + Constants.RuleChain.CONNECTION_RPC_TO_DEVICE,
                        "0->4:" + Constants.RuleChain.CONNECTION_OTHER,
                        "6->12:" + Constants.RuleChain.CONNECTION_SUCCESS,
                        "12->13:" + Constants.RuleChain.CONNECTION_SUCCESS,
                        "13->1:" + Constants.RuleChain.CONNECTION_VALID,
                        "13->7:" + Constants.RuleChain.CONNECTION_INVALID,
                        "1->7:" + Constants.RuleChain.CONNECTION_SUCCESS,
                        "7->8:" + Constants.RuleChain.CONNECTION_SUCCESS,
                        "8->9:" + Constants.RuleChain.CONNECTION_TELEMETRY,
                        "8->10:" + Constants.RuleChain.CONNECTION_EVENT,
                        "8->11:" + Constants.RuleChain.CONNECTION_INVALID),
                metadata.getConnections().stream().map(this::connectionKey).toList());
    }

    @Test
    void embedsEventDefinitionsInValidationScript() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID chainId = UUID.randomUUID();
        ThingsboardClientProvider provider = mock(ThingsboardClientProvider.class);
        ThingsboardClient client = mock(ThingsboardClient.class);
        when(provider.get(tenantId)).thenReturn(client);
        TbClientStubs.stubNoRuleChains(client);
        TbClientStubs.stubRuleChainCreated(client, chainId);

        var eventTsl = TslFixtures.eventTsl(objectMapper);
        var request = TslFixtures.syncRequest(
                objectMapper, tenantId, eventTsl,
                "EVTPRODUCT01", "Event Product", "EVTPRODUCT01", "1", "1.0", false);

        new TbRuleChainProvisioner(provider, properties(), objectMapper).ensure(request);

        RuleChainMetaData metadata = captureMetadata(client);
        RuleNode validate = metadata.getNodes().stream()
                .filter(node -> Constants.RuleChain.NODE_NAME_VALIDATE.equals(node.getName()))
                .findFirst()
                .orElseThrow();
        String script = validate.getConfiguration()
                .path(Constants.RuleChain.CONFIG_JS_SCRIPT).asText();

        assertTrue(script.contains("EVTPRODUCT01"));
        assertTrue(script.contains("overSpeed"));
        assertTrue(script.contains("speed"));
        assertTrue(script.contains("limit"));
    }

    @Test
    void usesConfiguredTopicsAndKeyPattern() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID chainId = UUID.randomUUID();
        ThingsboardClientProvider provider = mock(ThingsboardClientProvider.class);
        ThingsboardClient client = mock(ThingsboardClient.class);
        when(provider.get(tenantId)).thenReturn(client);

        ThingModelProperties properties = properties();
        properties.getRuleChain().setTopicTelemetry(CUSTOM_TELEMETRY_TOPIC);
        properties.getRuleChain().setTopicEvent(CUSTOM_EVENT_TOPIC);
        properties.getRuleChain().setTopicInvalid(CUSTOM_INVALID_TOPIC);
        properties.getRuleChain().setKeyPattern(CUSTOM_KEY_PATTERN);
        TbClientStubs.stubNoRuleChains(client);
        TbClientStubs.stubRuleChainCreated(client, chainId);

        new TbRuleChainProvisioner(provider, properties, objectMapper).ensure(request(tenantId));

        ArgumentCaptor<RuleChain> chainCaptor = ArgumentCaptor.forClass(RuleChain.class);
        verify(client).saveRuleChain(chainCaptor.capture());
        assertEquals(PRODUCT_ID, chainCaptor.getValue().getName());

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
}
