// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1

package org.thingsboard.server.msa.rule.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.SecretType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.rule.NodeConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.DisableUIListeners;
import org.thingsboard.server.msa.TestProperties;
import org.thingsboard.server.msa.WsClient;
import org.thingsboard.server.msa.mapper.WsTelemetryResponse;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.fail;
import static org.thingsboard.server.msa.prototypes.DevicePrototypes.defaultDevicePrototype;

@DisableUIListeners
@Slf4j
public class MqttNodeTest extends AbstractContainerTest {

    private static final String TOPIC = "tb/mqtt/device";
    private static final String CONTAINER_MQTT_URL = "tcp://localhost:1883";

    private Device device;

    @BeforeMethod
    public void setUp() {
        testRestClient.login("tenant@thingsboard.org", "tenant");
        device = testRestClient.postDevice("", defaultDevicePrototype("mqtt_"));
    }

    @AfterMethod
    public void tearDown() {
        testRestClient.deleteDeviceIfExists(device.getId());
    }

    @Test
    public void telemetryUpload() throws Exception {
        String password = "pass";
        Secret secret = createSecret(password);
        String formattedSecret = toSecretPlaceholder(secret.getName(), secret.getType());

        RuleChainId defaultRuleChainId = getDefaultRuleChainId();

        DeviceCredentials deviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        createRootRuleChainWithTestNode("MqttRuleNodeTestMetadata.json", "org.thingsboard.rule.engine.mqtt.TbMqttNode", 2,
                deviceCredentials.getCredentialsId(), formattedSecret);

        WsClient wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);

        MqttMessageListener messageListener = new MqttMessageListener();
        MqttClient responseClient = new MqttClient(TestProperties.getMqttBrokerUrl(), StringUtils.randomAlphanumeric(10), new MemoryPersistence());
        responseClient.connect();
        responseClient.subscribe(TOPIC, messageListener);

        MqttClient mqttClient = new MqttClient(CONTAINER_MQTT_URL, StringUtils.randomAlphanumeric(10), new MemoryPersistence());
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setUserName(deviceCredentials.getCredentialsId());
        mqttConnectOptions.setPassword(password.toCharArray());
        mqttClient.connect(mqttConnectOptions);
        mqttClient.publish("v1/devices/me/telemetry", new MqttMessage(createPayload().toString().getBytes()));

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        log.info("Received telemetry: {}", actualLatestTelemetry);
        wsClient.closeBlocking();

        assertThat(actualLatestTelemetry.getData()).hasSize(4);
        assertThat(actualLatestTelemetry.getLatestValues().keySet()).containsOnlyOnceElementsOf(Arrays.asList("booleanKey", "stringKey", "doubleKey", "longKey"));

        assertThat(actualLatestTelemetry.getDataValuesByKey("booleanKey").get(1)).isEqualTo(Boolean.TRUE.toString());
        assertThat(actualLatestTelemetry.getDataValuesByKey("stringKey").get(1)).isEqualTo("value1");
        assertThat(actualLatestTelemetry.getDataValuesByKey("doubleKey").get(1)).isEqualTo(Double.toString(42.6));
        assertThat(actualLatestTelemetry.getDataValuesByKey("longKey").get(1)).isEqualTo(Long.toString(73));

        Awaitility
                .await()
                .alias("Get integration events")
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> !messageListener.getEvents().isEmpty());

        BlockingQueue<MqttEvent> events = messageListener.getEvents();
        JsonNode actual = JacksonUtil.toJsonNode(Objects.requireNonNull(events.poll()).message);

        assertThat(actual.get("stringKey").asText()).isEqualTo("value1");
        assertThat(actual.get("booleanKey").asBoolean()).isEqualTo(Boolean.TRUE);
        assertThat(actual.get("doubleKey").asDouble()).isEqualTo(42.6);
        assertThat(actual.get("longKey").asLong()).isEqualTo(73);

        testRestClient.setRootRuleChain(defaultRuleChainId);
    }

    @Data
    private class MqttMessageListener implements IMqttMessageListener {

        private final BlockingQueue<MqttEvent> events;

        private MqttMessageListener() {
            events = new ArrayBlockingQueue<>(100);
        }

        @Override
        public void messageArrived(String s, MqttMessage mqttMessage) {
            log.info("MQTT message [{}], topic [{}]", mqttMessage.toString(), s);
            events.add(new MqttEvent(s, mqttMessage.toString()));
        }

        public BlockingQueue<MqttEvent> getEvents() {
            return events;
        }

    }

    @Data
    private class MqttEvent {

        private final String topic;
        private final String message;

    }

    private RuleChainId getDefaultRuleChainId() {
        PageData<RuleChain> ruleChains = testRestClient.getRuleChains(new PageLink(40, 0));

        Optional<RuleChain> defaultRuleChain = ruleChains.getData()
                .stream()
                .filter(RuleChain::isRoot)
                .findFirst();
        if (!defaultRuleChain.isPresent()) {
            fail("Root rule chain wasn't found");
        }
        return defaultRuleChain.get().getId();
    }

    protected RuleChainId createRootRuleChainWithTestNode(String ruleChainMetadataFile, String ruleNodeType, int eventsCount, String username, String password) throws Exception {
        RuleChain newRuleChain = new RuleChain();
        newRuleChain.setName("testRuleChain");
        RuleChain ruleChain = testRestClient.postRuleChain(newRuleChain);

        JsonNode configuration = JacksonUtil.OBJECT_MAPPER.readTree(this.getClass().getClassLoader().getResourceAsStream(ruleChainMetadataFile));
        replaceCredentialsInConfiguration(configuration, username, password);
        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        ruleChainMetaData.setRuleChainId(ruleChain.getId());
        ruleChainMetaData.setFirstNodeIndex(configuration.get("firstNodeIndex").asInt());
        ruleChainMetaData.setNodes(Arrays.asList(JacksonUtil.OBJECT_MAPPER.treeToValue(configuration.get("nodes"), RuleNode[].class)));
        ruleChainMetaData.setConnections(Arrays.asList(JacksonUtil.OBJECT_MAPPER.treeToValue(configuration.get("connections"), NodeConnectionInfo[].class)));

        ruleChainMetaData = testRestClient.postRuleChainMetadata(ruleChainMetaData);

        testRestClient.setRootRuleChain(ruleChain.getId());

        RuleNode node = ruleChainMetaData.getNodes().stream().filter(ruleNode -> ruleNode.getType().equals(ruleNodeType)).findFirst().get();

        Awaitility
                .await()
                .alias("Get events from rule chain")
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    PageData<EventInfo> events = testRestClient.getEvents(node.getId(), EventType.LC_EVENT, ruleChain.getTenantId(), new TimePageLink(1024));
                    List<EventInfo> eventInfos = events.getData().stream().filter(eventInfo ->
                            "STARTED".equals(eventInfo.getBody().get("event").asText()) &&
                                    "true".equals(eventInfo.getBody().get("success").asText())).toList();

                    return eventInfos.size() == eventsCount;
                });

        return ruleChain.getId();
    }

    private void replaceCredentialsInConfiguration(JsonNode configuration, String username, String password) {
        JsonNode credentialsNode = configuration.path("nodes").get(0).get("configuration").path("credentials");
        if (credentialsNode.isObject()) {
            ((ObjectNode) credentialsNode).put("username", username);
            ((ObjectNode) credentialsNode).put("password", password);
        }
    }

    private Secret createSecret(String value) {
        Secret secret = new Secret();
        secret.setName("mqtt_node_secret");
        secret.setValue(value);
        secret.setType(SecretType.TEXT);
        return testRestClient.saveSecret(secret);
    }

    private String toSecretPlaceholder(String name, SecretType type) {
        return String.format("${secret:%s;type:%s}", name, type);
    }

}
