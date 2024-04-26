/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.msa.rule.node;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.common.data.rule.NodeConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.DisableUIListeners;
import org.thingsboard.server.msa.TestProperties;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityDataUpdate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.fail;
import static org.thingsboard.server.msa.prototypes.DevicePrototypes.defaultDevicePrototype;

@DisableUIListeners
@Slf4j
public class MqttNodeTest extends AbstractContainerTest {

    private static final String TOPIC = "tb/mqtt/device";

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
        RuleChainId defaultRuleChainId = getDefaultRuleChainId();

        createRootRuleChainWithTestNode("MqttRuleNodeTestMetadata.json", "org.thingsboard.rule.engine.mqtt.TbMqttNode", 2);

        DeviceCredentials deviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        List<String> expectedKeys = Arrays.asList("booleanKey", "stringKey", "doubleKey", "longKey");

        SingleEntityFilter filter = new SingleEntityFilter();
        filter.setSingleEntity(device.getId());

        long now = System.currentTimeMillis();

        EntityDataUpdate entityDataUpdate = getWsClient().subscribeTsUpdate(expectedKeys, now, TimeUnit.SECONDS.toMillis(1), filter);
        assertThat(entityDataUpdate.getData().getData().size()).isEqualTo(1);
        Map<String, TsValue[]> timeseries = entityDataUpdate.getData().getData().get(0).getTimeseries();
        assertThat(timeseries.keySet()).containsOnlyOnceElementsOf(expectedKeys);

        getWsClient().registerWaitForUpdate();

        MqttMessageListener messageListener = new MqttMessageListener();
        MqttClient responseClient = new MqttClient(TestProperties.getMqttBrokerUrl(), StringUtils.randomAlphanumeric(10), new MemoryPersistence());
        responseClient.connect();
        responseClient.subscribe(TOPIC, messageListener);

        MqttClient mqttClient = new MqttClient("tcp://localhost:1883", StringUtils.randomAlphanumeric(10), new MemoryPersistence());
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setUserName(deviceCredentials.getCredentialsId());
        mqttClient.connect(mqttConnectOptions);
        mqttClient.publish("v1/devices/me/telemetry", new MqttMessage(createPayload().toString().getBytes()));

        String updateString = getWsClient().waitForUpdate(3000, true);
        EntityDataUpdate update = JacksonUtil.fromString(updateString, EntityDataUpdate.class);
        assertThat(update).isNotNull();
        assertThat(update.getUpdate()).isNotNull();
        assertThat(update.getUpdate().size()).isEqualTo(1);
        Map<String, TsValue[]> actualLatestTelemetry = update.getUpdate().get(0).getTimeseries();

        log.info("Received telemetry: {}", actualLatestTelemetry);

        assertThat(actualLatestTelemetry.keySet()).containsOnlyOnceElementsOf(expectedKeys);
        assertThat(actualLatestTelemetry.get("booleanKey")[0].getValue()).isEqualTo(Boolean.TRUE.toString());
        assertThat(actualLatestTelemetry.get("stringKey")[0].getValue()).isEqualTo("value1");
        assertThat(actualLatestTelemetry.get("doubleKey")[0].getValue()).isEqualTo(Double.toString(42.0));
        assertThat(actualLatestTelemetry.get("longKey")[0].getValue()).isEqualTo(Long.toString(73));

        Awaitility
                .await()
                .alias("Get integration events")
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> messageListener.getEvents().size() > 0);

        BlockingQueue<MqttEvent> events = messageListener.getEvents();
        JsonNode actual = JacksonUtil.toJsonNode(Objects.requireNonNull(events.poll()).message);

        assertThat(actual.get("stringKey").asText()).isEqualTo("value1");
        assertThat(actual.get("booleanKey").asBoolean()).isEqualTo(Boolean.TRUE);
        assertThat(actual.get("doubleKey").asDouble()).isEqualTo(42.0);
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

    protected RuleChainId createRootRuleChainWithTestNode(String ruleChainMetadataFile, String ruleNodeType, int eventsCount) throws Exception {
        RuleChain newRuleChain = new RuleChain();
        newRuleChain.setName("testRuleChain");
        RuleChain ruleChain = testRestClient.postRuleChain(newRuleChain);

        JsonNode configuration = JacksonUtil.OBJECT_MAPPER.readTree(this.getClass().getClassLoader().getResourceAsStream(ruleChainMetadataFile));
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
                                            "true".equals(eventInfo.getBody().get("success").asText()))
                            .collect(Collectors.toList());

                    return eventInfos.size() == eventsCount;
                });

        return ruleChain.getId();
    }
}
