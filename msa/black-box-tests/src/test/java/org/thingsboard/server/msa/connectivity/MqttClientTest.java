/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.msa.connectivity;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.JsonObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.*;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.thingsboard.mqtt.MqttClient;
import org.thingsboard.mqtt.MqttClientConfig;
import org.thingsboard.mqtt.MqttHandler;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.rule.NodeConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.WsClient;
import org.thingsboard.server.msa.mapper.AttributesResponse;
import org.thingsboard.server.msa.mapper.WsTelemetryResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
public class MqttClientTest extends AbstractContainerTest {

    @Test
    public void telemetryUpload() throws Exception {
        restClient.login("tenant@thingsboard.org", "tenant");
        Device device = createDevice("mqtt_");
        DeviceCredentials deviceCredentials = restClient.getCredentials(device.getId());

        WsClient wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        MqttClient mqttClient = getMqttClient(deviceCredentials, null);
        mqttClient.publish("v1/devices/me/telemetry", Unpooled.wrappedBuffer(createPayload().toString().getBytes())).get();
        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        log.info("Received telemetry: {}", actualLatestTelemetry);
        wsClient.closeBlocking();

        Assert.assertEquals(4, actualLatestTelemetry.getData().size());
        Assert.assertEquals(Sets.newHashSet("booleanKey", "stringKey", "doubleKey", "longKey"),
                actualLatestTelemetry.getLatestValues().keySet());

        Assert.assertTrue(verify(actualLatestTelemetry, "booleanKey", Boolean.TRUE.toString()));
        Assert.assertTrue(verify(actualLatestTelemetry, "stringKey", "value1"));
        Assert.assertTrue(verify(actualLatestTelemetry, "doubleKey", Double.toString(42.0)));
        Assert.assertTrue(verify(actualLatestTelemetry, "longKey", Long.toString(73)));

        restClient.getRestTemplate().delete(HTTPS_URL + "/api/device/" + device.getId());
    }

    @Test
    public void telemetryUploadWithTs() throws Exception {
        long ts = 1451649600512L;

        restClient.login("tenant@thingsboard.org", "tenant");
        Device device = createDevice("mqtt_");
        DeviceCredentials deviceCredentials = restClient.getCredentials(device.getId());

        WsClient wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        MqttClient mqttClient = getMqttClient(deviceCredentials, null);
        mqttClient.publish("v1/devices/me/telemetry", Unpooled.wrappedBuffer(createPayload(ts).toString().getBytes())).get();
        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        log.info("Received telemetry: {}", actualLatestTelemetry);
        wsClient.closeBlocking();

        Assert.assertEquals(4, actualLatestTelemetry.getData().size());
        Assert.assertEquals(getExpectedLatestValues(ts), actualLatestTelemetry.getLatestValues());

        Assert.assertTrue(verify(actualLatestTelemetry, "booleanKey", ts, Boolean.TRUE.toString()));
        Assert.assertTrue(verify(actualLatestTelemetry, "stringKey", ts, "value1"));
        Assert.assertTrue(verify(actualLatestTelemetry, "doubleKey", ts, Double.toString(42.0)));
        Assert.assertTrue(verify(actualLatestTelemetry, "longKey", ts, Long.toString(73)));

        restClient.getRestTemplate().delete(HTTPS_URL + "/api/device/" + device.getId());
    }

    @Test
    public void publishAttributeUpdateToServer() throws Exception {
        restClient.login("tenant@thingsboard.org", "tenant");
        Device device = createDevice("mqtt_");
        DeviceCredentials deviceCredentials = restClient.getCredentials(device.getId());

        WsClient wsClient = subscribeToWebSocket(device.getId(), "CLIENT_SCOPE", CmdsType.ATTR_SUB_CMDS);
        MqttMessageListener listener = new MqttMessageListener();
        MqttClient mqttClient = getMqttClient(deviceCredentials, listener);
        JsonObject clientAttributes = new JsonObject();
        clientAttributes.addProperty("attr1", "value1");
        clientAttributes.addProperty("attr2", true);
        clientAttributes.addProperty("attr3", 42.0);
        clientAttributes.addProperty("attr4", 73);
        mqttClient.publish("v1/devices/me/attributes", Unpooled.wrappedBuffer(clientAttributes.toString().getBytes())).get();
        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        log.info("Received telemetry: {}", actualLatestTelemetry);
        wsClient.closeBlocking();

        Assert.assertEquals(4, actualLatestTelemetry.getData().size());
        Assert.assertEquals(Sets.newHashSet("attr1", "attr2", "attr3", "attr4"),
                actualLatestTelemetry.getLatestValues().keySet());

        Assert.assertTrue(verify(actualLatestTelemetry, "attr1", "value1"));
        Assert.assertTrue(verify(actualLatestTelemetry, "attr2", Boolean.TRUE.toString()));
        Assert.assertTrue(verify(actualLatestTelemetry, "attr3", Double.toString(42.0)));
        Assert.assertTrue(verify(actualLatestTelemetry, "attr4", Long.toString(73)));

        restClient.getRestTemplate().delete(HTTPS_URL + "/api/device/" + device.getId());
    }

    @Test
    public void requestAttributeValuesFromServer() throws Exception {
        restClient.login("tenant@thingsboard.org", "tenant");
        Device device = createDevice("mqtt_");
        DeviceCredentials deviceCredentials = restClient.getCredentials(device.getId());

        WsClient wsClient = subscribeToWebSocket(device.getId(), "CLIENT_SCOPE", CmdsType.ATTR_SUB_CMDS);
        MqttMessageListener listener = new MqttMessageListener();
        MqttClient mqttClient = getMqttClient(deviceCredentials, listener);

        // Add a new client attribute
        JsonObject clientAttributes = new JsonObject();
        String clientAttributeValue = RandomStringUtils.randomAlphanumeric(8);
        clientAttributes.addProperty("clientAttr", clientAttributeValue);
        mqttClient.publish("v1/devices/me/attributes", Unpooled.wrappedBuffer(clientAttributes.toString().getBytes())).get();

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        log.info("Received ws telemetry: {}", actualLatestTelemetry);
        wsClient.closeBlocking();

        Assert.assertEquals(1, actualLatestTelemetry.getData().size());
        Assert.assertEquals(Sets.newHashSet("clientAttr"),
                actualLatestTelemetry.getLatestValues().keySet());

        Assert.assertTrue(verify(actualLatestTelemetry, "clientAttr", clientAttributeValue));

        // Add a new shared attribute
        JsonObject sharedAttributes = new JsonObject();
        String sharedAttributeValue = RandomStringUtils.randomAlphanumeric(8);
        sharedAttributes.addProperty("sharedAttr", sharedAttributeValue);
        ResponseEntity sharedAttributesResponse = restClient.getRestTemplate()
                .postForEntity(HTTPS_URL + "/api/plugins/telemetry/DEVICE/{deviceId}/SHARED_SCOPE",
                        mapper.readTree(sharedAttributes.toString()), ResponseEntity.class,
                        device.getId());
        Assert.assertTrue(sharedAttributesResponse.getStatusCode().is2xxSuccessful());

        // Subscribe to attributes response
        mqttClient.on("v1/devices/me/attributes/response/+", listener, MqttQoS.AT_LEAST_ONCE).get();

        // Wait until subscription is processed
        TimeUnit.SECONDS.sleep(3);

        // Request attributes
        JsonObject request = new JsonObject();
        request.addProperty("clientKeys", "clientAttr");
        request.addProperty("sharedKeys", "sharedAttr");
        mqttClient.publish("v1/devices/me/attributes/request/" + new Random().nextInt(100), Unpooled.wrappedBuffer(request.toString().getBytes())).get();
        MqttEvent event = listener.getEvents().poll(10, TimeUnit.SECONDS);
        AttributesResponse attributes = mapper.readValue(Objects.requireNonNull(event).getMessage(), AttributesResponse.class);
        log.info("Received telemetry: {}", attributes);

        Assert.assertEquals(1, attributes.getClient().size());
        Assert.assertEquals(clientAttributeValue, attributes.getClient().get("clientAttr"));

        Assert.assertEquals(1, attributes.getShared().size());
        Assert.assertEquals(sharedAttributeValue, attributes.getShared().get("sharedAttr"));

        restClient.getRestTemplate().delete(HTTPS_URL + "/api/device/" + device.getId());
    }

    @Test
    public void subscribeToAttributeUpdatesFromServer() throws Exception {
        restClient.login("tenant@thingsboard.org", "tenant");
        Device device = createDevice("mqtt_");
        DeviceCredentials deviceCredentials = restClient.getCredentials(device.getId());

        MqttMessageListener listener = new MqttMessageListener();
        MqttClient mqttClient = getMqttClient(deviceCredentials, listener);
        mqttClient.on("v1/devices/me/attributes", listener, MqttQoS.AT_LEAST_ONCE).get();

        // Wait until subscription is processed
        TimeUnit.SECONDS.sleep(3);

        String sharedAttributeName = "sharedAttr";

        // Add a new shared attribute
        JsonObject sharedAttributes = new JsonObject();
        String sharedAttributeValue = RandomStringUtils.randomAlphanumeric(8);
        sharedAttributes.addProperty(sharedAttributeName, sharedAttributeValue);
        ResponseEntity sharedAttributesResponse = restClient.getRestTemplate()
                .postForEntity(HTTPS_URL + "/api/plugins/telemetry/DEVICE/{deviceId}/SHARED_SCOPE",
                        mapper.readTree(sharedAttributes.toString()), ResponseEntity.class,
                        device.getId());
        Assert.assertTrue(sharedAttributesResponse.getStatusCode().is2xxSuccessful());

        MqttEvent event = listener.getEvents().poll(10, TimeUnit.SECONDS);
        Assert.assertEquals(sharedAttributeValue,
                mapper.readValue(Objects.requireNonNull(event).getMessage(), JsonNode.class).get(sharedAttributeName).asText());

        // Update the shared attribute value
        JsonObject updatedSharedAttributes = new JsonObject();
        String updatedSharedAttributeValue = RandomStringUtils.randomAlphanumeric(8);
        updatedSharedAttributes.addProperty(sharedAttributeName, updatedSharedAttributeValue);
        ResponseEntity updatedSharedAttributesResponse = restClient.getRestTemplate()
                .postForEntity(HTTPS_URL + "/api/plugins/telemetry/DEVICE/{deviceId}/SHARED_SCOPE",
                        mapper.readTree(updatedSharedAttributes.toString()), ResponseEntity.class,
                        device.getId());
        Assert.assertTrue(updatedSharedAttributesResponse.getStatusCode().is2xxSuccessful());

        event = listener.getEvents().poll(10, TimeUnit.SECONDS);
        Assert.assertEquals(updatedSharedAttributeValue,
                mapper.readValue(Objects.requireNonNull(event).getMessage(), JsonNode.class).get(sharedAttributeName).asText());

        restClient.getRestTemplate().delete(HTTPS_URL + "/api/device/" + device.getId());
    }

    @Test
    public void serverSideRpc() throws Exception {
        restClient.login("tenant@thingsboard.org", "tenant");
        Device device = createDevice("mqtt_");
        DeviceCredentials deviceCredentials = restClient.getCredentials(device.getId());

        MqttMessageListener listener = new MqttMessageListener();
        MqttClient mqttClient = getMqttClient(deviceCredentials, listener);
        mqttClient.on("v1/devices/me/rpc/request/+", listener, MqttQoS.AT_LEAST_ONCE).get();

        // Wait until subscription is processed
        TimeUnit.SECONDS.sleep(3);

        // Send an RPC from the server
        JsonObject serverRpcPayload = new JsonObject();
        serverRpcPayload.addProperty("method", "getValue");
        serverRpcPayload.addProperty("params", true);
        ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
        ListenableFuture<ResponseEntity> future = service.submit(() -> {
            try {
                return restClient.getRestTemplate()
                        .postForEntity(HTTPS_URL + "/api/plugins/rpc/twoway/{deviceId}",
                                mapper.readTree(serverRpcPayload.toString()), String.class,
                                device.getId());
            } catch (IOException e) {
                return ResponseEntity.badRequest().build();
            }
        });

        // Wait for RPC call from the server and send the response
        MqttEvent requestFromServer = listener.getEvents().poll(10, TimeUnit.SECONDS);

        Assert.assertEquals("{\"method\":\"getValue\",\"params\":true}", Objects.requireNonNull(requestFromServer).getMessage());

        Integer requestId = Integer.valueOf(Objects.requireNonNull(requestFromServer).getTopic().substring("v1/devices/me/rpc/request/".length()));
        JsonObject clientResponse = new JsonObject();
        clientResponse.addProperty("response", "someResponse");
        // Send a response to the server's RPC request
        mqttClient.publish("v1/devices/me/rpc/response/" + requestId, Unpooled.wrappedBuffer(clientResponse.toString().getBytes())).get();

        ResponseEntity serverResponse = future.get(5, TimeUnit.SECONDS);
        Assert.assertTrue(serverResponse.getStatusCode().is2xxSuccessful());
        Assert.assertEquals(clientResponse.toString(), serverResponse.getBody());

        restClient.getRestTemplate().delete(HTTPS_URL + "/api/device/" + device.getId());
    }

    @Test
    public void clientSideRpc() throws Exception {
        restClient.login("tenant@thingsboard.org", "tenant");
        Device device = createDevice("mqtt_");
        DeviceCredentials deviceCredentials = restClient.getCredentials(device.getId());

        MqttMessageListener listener = new MqttMessageListener();
        MqttClient mqttClient = getMqttClient(deviceCredentials, listener);
        mqttClient.on("v1/devices/me/rpc/request/+", listener, MqttQoS.AT_LEAST_ONCE).get();

        // Get the default rule chain id to make it root again after test finished
        RuleChainId defaultRuleChainId = getDefaultRuleChainId();

        // Create a new root rule chain
        RuleChainId ruleChainId = createRootRuleChainForRpcResponse();

        TimeUnit.SECONDS.sleep(3);
        // Send the request to the server
        JsonObject clientRequest = new JsonObject();
        clientRequest.addProperty("method", "getResponse");
        clientRequest.addProperty("params", true);
        Integer requestId = 42;
        mqttClient.publish("v1/devices/me/rpc/request/" + requestId, Unpooled.wrappedBuffer(clientRequest.toString().getBytes())).get();

        // Check the response from the server
        TimeUnit.SECONDS.sleep(1);
        MqttEvent responseFromServer = listener.getEvents().poll(1, TimeUnit.SECONDS);
        Integer responseId = Integer.valueOf(Objects.requireNonNull(responseFromServer).getTopic().substring("v1/devices/me/rpc/response/".length()));
        Assert.assertEquals(requestId, responseId);
        Assert.assertEquals("requestReceived", mapper.readTree(responseFromServer.getMessage()).get("response").asText());

        // Make the default rule chain a root again
        ResponseEntity<RuleChain> rootRuleChainResponse = restClient.getRestTemplate()
                .postForEntity(HTTPS_URL + "/api/ruleChain/{ruleChainId}/root",
                        null,
                        RuleChain.class,
                        defaultRuleChainId);
        Assert.assertTrue(rootRuleChainResponse.getStatusCode().is2xxSuccessful());

        // Delete the created rule chain
        restClient.getRestTemplate().delete(HTTPS_URL + "/api/ruleChain/{ruleChainId}", ruleChainId);
        restClient.getRestTemplate().delete(HTTPS_URL + "/api/device/" + device.getId());
    }

    private RuleChainId createRootRuleChainForRpcResponse() throws Exception {
        RuleChain newRuleChain = new RuleChain();
        newRuleChain.setName("testRuleChain");
        ResponseEntity<RuleChain> ruleChainResponse = restClient.getRestTemplate()
                .postForEntity(HTTPS_URL + "/api/ruleChain",
                        newRuleChain,
                        RuleChain.class);
        Assert.assertTrue(ruleChainResponse.getStatusCode().is2xxSuccessful());
        RuleChain ruleChain = ruleChainResponse.getBody();

        JsonNode configuration = mapper.readTree(this.getClass().getClassLoader().getResourceAsStream("RpcResponseRuleChainMetadata.json"));
        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        ruleChainMetaData.setRuleChainId(ruleChain.getId());
        ruleChainMetaData.setFirstNodeIndex(configuration.get("firstNodeIndex").asInt());
        ruleChainMetaData.setNodes(Arrays.asList(mapper.treeToValue(configuration.get("nodes"), RuleNode[].class)));
        ruleChainMetaData.setConnections(Arrays.asList(mapper.treeToValue(configuration.get("connections"), NodeConnectionInfo[].class)));

        ResponseEntity<RuleChainMetaData> ruleChainMetadataResponse = restClient.getRestTemplate()
                .postForEntity(HTTPS_URL + "/api/ruleChain/metadata",
                        ruleChainMetaData,
                        RuleChainMetaData.class);
        Assert.assertTrue(ruleChainMetadataResponse.getStatusCode().is2xxSuccessful());

        // Set a new rule chain as root
        ResponseEntity<RuleChain> rootRuleChainResponse = restClient.getRestTemplate()
                .postForEntity(HTTPS_URL + "/api/ruleChain/{ruleChainId}/root",
                        null,
                        RuleChain.class,
                        ruleChain.getId());
        Assert.assertTrue(rootRuleChainResponse.getStatusCode().is2xxSuccessful());

        return ruleChain.getId();
    }

    private RuleChainId getDefaultRuleChainId() {
        ResponseEntity<PageData<RuleChain>> ruleChains = restClient.getRestTemplate().exchange(
                HTTPS_URL + "/api/ruleChains?pageSize=40&page=0&textSearch=",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<PageData<RuleChain>>() {
                });

        Optional<RuleChain> defaultRuleChain = ruleChains.getBody().getData()
                .stream()
                .filter(RuleChain::isRoot)
                .findFirst();
        if (!defaultRuleChain.isPresent()) {
            Assert.fail("Root rule chain wasn't found");
        }
        return defaultRuleChain.get().getId();
    }

    private MqttClient getMqttClient(DeviceCredentials deviceCredentials, MqttMessageListener listener) throws InterruptedException, ExecutionException {
        MqttClientConfig clientConfig = new MqttClientConfig();
        clientConfig.setClientId("MQTT client from test");
        clientConfig.setUsername(deviceCredentials.getCredentialsId());
        MqttClient mqttClient = MqttClient.create(clientConfig, listener);
        mqttClient.connect("localhost", 1883).get();
        return mqttClient;
    }

    @Data
    private class MqttMessageListener implements MqttHandler {
        private final BlockingQueue<MqttEvent> events;

        private MqttMessageListener() {
            events = new ArrayBlockingQueue<>(100);
        }

        @Override
        public void onMessage(String topic, ByteBuf message) {
            log.info("MQTT message [{}], topic [{}]", message.toString(StandardCharsets.UTF_8), topic);
            events.add(new MqttEvent(topic, message.toString(StandardCharsets.UTF_8)));
        }
    }

    @Data
    private class MqttEvent {
        private final String topic;
        private final String message;
    }
}
