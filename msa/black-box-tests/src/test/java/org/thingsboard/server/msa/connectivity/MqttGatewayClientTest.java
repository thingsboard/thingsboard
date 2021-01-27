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
import com.google.gson.JsonParser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.thingsboard.mqtt.MqttClient;
import org.thingsboard.mqtt.MqttClientConfig;
import org.thingsboard.mqtt.MqttHandler;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.WsClient;
import org.thingsboard.server.msa.mapper.WsTelemetryResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MqttGatewayClientTest extends AbstractContainerTest {
    Device gatewayDevice;
    MqttClient mqttClient;
    Device createdDevice;
    MqttMessageListener listener;

    @Before
    public void createGateway() throws Exception {
        restClient.login("tenant@thingsboard.org", "tenant");
        this.gatewayDevice = createGatewayDevice();
        Optional<DeviceCredentials> gatewayDeviceCredentials = restClient.getDeviceCredentialsByDeviceId(gatewayDevice.getId());
        Assert.assertTrue(gatewayDeviceCredentials.isPresent());
        this.listener = new MqttMessageListener();
        this.mqttClient = getMqttClient(gatewayDeviceCredentials.get(), listener);
        this.createdDevice = createDeviceThroughGateway(mqttClient, gatewayDevice);
    }

    @After
    public void removeGateway() throws Exception {
        restClient.getRestTemplate().delete(HTTPS_URL + "/api/device/" + this.gatewayDevice.getId());
        restClient.getRestTemplate().delete(HTTPS_URL + "/api/device/" + this.createdDevice.getId());
        this.listener = null;
        this.mqttClient = null;
        this.createdDevice = null;
    }

    @Test
    public void telemetryUpload() throws Exception {
        WsClient wsClient = subscribeToWebSocket(createdDevice.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        mqttClient.publish("v1/gateway/telemetry", Unpooled.wrappedBuffer(createGatewayPayload(createdDevice.getName(), -1).toString().getBytes())).get();
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
    }

    @Test
    public void telemetryUploadWithTs() throws Exception {
        long ts = 1451649600512L;

        restClient.login("tenant@thingsboard.org", "tenant");
        WsClient wsClient = subscribeToWebSocket(createdDevice.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        mqttClient.publish("v1/gateway/telemetry", Unpooled.wrappedBuffer(createGatewayPayload(createdDevice.getName(), ts).toString().getBytes())).get();
        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        log.info("Received telemetry: {}", actualLatestTelemetry);
        wsClient.closeBlocking();

        Assert.assertEquals(4, actualLatestTelemetry.getData().size());
        Assert.assertEquals(getExpectedLatestValues(ts), actualLatestTelemetry.getLatestValues());

        Assert.assertTrue(verify(actualLatestTelemetry, "booleanKey", ts, Boolean.TRUE.toString()));
        Assert.assertTrue(verify(actualLatestTelemetry, "stringKey", ts, "value1"));
        Assert.assertTrue(verify(actualLatestTelemetry, "doubleKey", ts, Double.toString(42.0)));
        Assert.assertTrue(verify(actualLatestTelemetry, "longKey", ts, Long.toString(73)));
    }

    @Test
    public void publishAttributeUpdateToServer() throws Exception {
        Optional<DeviceCredentials> createdDeviceCredentials = restClient.getDeviceCredentialsByDeviceId(createdDevice.getId());
        Assert.assertTrue(createdDeviceCredentials.isPresent());
        WsClient wsClient = subscribeToWebSocket(createdDevice.getId(), "CLIENT_SCOPE", CmdsType.ATTR_SUB_CMDS);
        JsonObject clientAttributes = new JsonObject();
        clientAttributes.addProperty("attr1", "value1");
        clientAttributes.addProperty("attr2", true);
        clientAttributes.addProperty("attr3", 42.0);
        clientAttributes.addProperty("attr4", 73);
        JsonObject gatewayClientAttributes = new JsonObject();
        gatewayClientAttributes.add(createdDevice.getName(), clientAttributes);
        mqttClient.publish("v1/gateway/attributes", Unpooled.wrappedBuffer(gatewayClientAttributes.toString().getBytes())).get();
        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        log.info("Received attributes: {}", actualLatestTelemetry);
        wsClient.closeBlocking();

        Assert.assertEquals(4, actualLatestTelemetry.getData().size());
        Assert.assertEquals(Sets.newHashSet("attr1", "attr2", "attr3", "attr4"),
                actualLatestTelemetry.getLatestValues().keySet());

        Assert.assertTrue(verify(actualLatestTelemetry, "attr1", "value1"));
        Assert.assertTrue(verify(actualLatestTelemetry, "attr2", Boolean.TRUE.toString()));
        Assert.assertTrue(verify(actualLatestTelemetry, "attr3", Double.toString(42.0)));
        Assert.assertTrue(verify(actualLatestTelemetry, "attr4", Long.toString(73)));
    }

    @Test
    public void requestAttributeValuesFromServer() throws Exception {
        WsClient wsClient = subscribeToWebSocket(createdDevice.getId(), "CLIENT_SCOPE", CmdsType.ATTR_SUB_CMDS);
        // Add a new client attribute
        JsonObject clientAttributes = new JsonObject();
        String clientAttributeValue = RandomStringUtils.randomAlphanumeric(8);
        clientAttributes.addProperty("clientAttr", clientAttributeValue);

        JsonObject gatewayClientAttributes = new JsonObject();
        gatewayClientAttributes.add(createdDevice.getName(), clientAttributes);
        mqttClient.publish("v1/gateway/attributes", Unpooled.wrappedBuffer(gatewayClientAttributes.toString().getBytes())).get();

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

        // Subscribe for attribute update event
        mqttClient.on("v1/gateway/attributes", listener, MqttQoS.AT_LEAST_ONCE).get();

        ResponseEntity sharedAttributesResponse = restClient.getRestTemplate()
                .postForEntity(HTTPS_URL + "/api/plugins/telemetry/DEVICE/{deviceId}/SHARED_SCOPE",
                        mapper.readTree(sharedAttributes.toString()), ResponseEntity.class,
                        createdDevice.getId());
        Assert.assertTrue(sharedAttributesResponse.getStatusCode().is2xxSuccessful());
        MqttEvent sharedAttributeEvent = listener.getEvents().poll(10, TimeUnit.SECONDS);

        // Catch attribute update event
        Assert.assertNotNull(sharedAttributeEvent);
        Assert.assertEquals("v1/gateway/attributes", sharedAttributeEvent.getTopic());

        // Subscribe to attributes response
        mqttClient.on("v1/gateway/attributes/response", listener, MqttQoS.AT_LEAST_ONCE).get();

        // Wait until subscription is processed
        TimeUnit.SECONDS.sleep(3);

        checkAttribute(true, clientAttributeValue);
        checkAttribute(false, sharedAttributeValue);
    }

    @Test
    public void subscribeToAttributeUpdatesFromServer() throws Exception {
        mqttClient.on("v1/gateway/attributes", listener, MqttQoS.AT_LEAST_ONCE).get();
        // Wait until subscription is processed
        TimeUnit.SECONDS.sleep(3);
        String sharedAttributeName = "sharedAttr";
        // Add a new shared attribute

        JsonObject sharedAttributes = new JsonObject();
        String sharedAttributeValue = RandomStringUtils.randomAlphanumeric(8);
        sharedAttributes.addProperty(sharedAttributeName, sharedAttributeValue);

        JsonObject gatewaySharedAttributeValue = new JsonObject();
        gatewaySharedAttributeValue.addProperty("device", createdDevice.getName());
        gatewaySharedAttributeValue.add("data", sharedAttributes);

        ResponseEntity sharedAttributesResponse = restClient.getRestTemplate()
                .postForEntity(HTTPS_URL + "/api/plugins/telemetry/DEVICE/{deviceId}/SHARED_SCOPE",
                        mapper.readTree(sharedAttributes.toString()), ResponseEntity.class,
                        createdDevice.getId());
        Assert.assertTrue(sharedAttributesResponse.getStatusCode().is2xxSuccessful());

        MqttEvent event = listener.getEvents().poll(10, TimeUnit.SECONDS);
        Assert.assertEquals(sharedAttributeValue,
                mapper.readValue(Objects.requireNonNull(event).getMessage(), JsonNode.class).get("data").get(sharedAttributeName).asText());

        // Update the shared attribute value
        JsonObject updatedSharedAttributes = new JsonObject();
        String updatedSharedAttributeValue = RandomStringUtils.randomAlphanumeric(8);
        updatedSharedAttributes.addProperty(sharedAttributeName, updatedSharedAttributeValue);

        JsonObject gatewayUpdatedSharedAttributeValue = new JsonObject();
        gatewayUpdatedSharedAttributeValue.addProperty("device", createdDevice.getName());
        gatewayUpdatedSharedAttributeValue.add("data", updatedSharedAttributes);

        ResponseEntity updatedSharedAttributesResponse = restClient.getRestTemplate()
                .postForEntity(HTTPS_URL + "/api/plugins/telemetry/DEVICE/{deviceId}/SHARED_SCOPE",
                        mapper.readTree(updatedSharedAttributes.toString()), ResponseEntity.class,
                        createdDevice.getId());
        Assert.assertTrue(updatedSharedAttributesResponse.getStatusCode().is2xxSuccessful());

        event = listener.getEvents().poll(10, TimeUnit.SECONDS);
        Assert.assertEquals(updatedSharedAttributeValue,
                mapper.readValue(Objects.requireNonNull(event).getMessage(), JsonNode.class).get("data").get(sharedAttributeName).asText());
    }

    @Test
    public void serverSideRpc() throws Exception {
        String gatewayRpcTopic = "v1/gateway/rpc";
        mqttClient.on(gatewayRpcTopic, listener, MqttQoS.AT_LEAST_ONCE).get();

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
                                createdDevice.getId());
            } catch (IOException e) {
                return ResponseEntity.badRequest().build();
            }
        });

        // Wait for RPC call from the server and send the response
        MqttEvent requestFromServer = listener.getEvents().poll(10, TimeUnit.SECONDS);

        Assert.assertNotNull(requestFromServer);
        Assert.assertNotNull(requestFromServer.getMessage());

        JsonObject requestFromServerJson = new JsonParser().parse(requestFromServer.getMessage()).getAsJsonObject();

        Assert.assertEquals(createdDevice.getName(), requestFromServerJson.get("device").getAsString());

        JsonObject requestFromServerData = requestFromServerJson.get("data").getAsJsonObject();

        Assert.assertEquals("getValue", requestFromServerData.get("method").getAsString());
        Assert.assertTrue(requestFromServerData.get("params").getAsBoolean());

        int requestId = requestFromServerData.get("id").getAsInt();

        JsonObject clientResponse = new JsonObject();
        clientResponse.addProperty("response", "someResponse");
        JsonObject gatewayResponse = new JsonObject();
        gatewayResponse.addProperty("device", createdDevice.getName());
        gatewayResponse.addProperty("id", requestId);
        gatewayResponse.add("data", clientResponse);
        // Send a response to the server's RPC request

        mqttClient.publish(gatewayRpcTopic, Unpooled.wrappedBuffer(gatewayResponse.toString().getBytes())).get();
        ResponseEntity serverResponse = future.get(5, TimeUnit.SECONDS);
        Assert.assertTrue(serverResponse.getStatusCode().is2xxSuccessful());
        Assert.assertEquals(clientResponse.toString(), serverResponse.getBody());
    }

        private void checkAttribute(boolean client, String expectedValue) throws Exception{
        JsonObject gatewayAttributesRequest = new JsonObject();
        int messageId = new Random().nextInt(100);
        gatewayAttributesRequest.addProperty("id", messageId);
        gatewayAttributesRequest.addProperty("device", createdDevice.getName());
        gatewayAttributesRequest.addProperty("client", client);
        String attributeName;
        if (client)
            attributeName = "clientAttr";
        else
            attributeName = "sharedAttr";
        gatewayAttributesRequest.addProperty("key", attributeName);
        log.info(gatewayAttributesRequest.toString());
        mqttClient.publish("v1/gateway/attributes/request", Unpooled.wrappedBuffer(gatewayAttributesRequest.toString().getBytes())).get();
        MqttEvent clientAttributeEvent = listener.getEvents().poll(10, TimeUnit.SECONDS);
        Assert.assertNotNull(clientAttributeEvent);
        JsonObject responseMessage = new JsonParser().parse(Objects.requireNonNull(clientAttributeEvent).getMessage()).getAsJsonObject();

        Assert.assertEquals(messageId, responseMessage.get("id").getAsInt());
        Assert.assertEquals(createdDevice.getName(), responseMessage.get("device").getAsString());
        Assert.assertEquals(3, responseMessage.entrySet().size());
        Assert.assertEquals(expectedValue, responseMessage.get("value").getAsString());
    }

    private Device createDeviceThroughGateway(MqttClient mqttClient, Device gatewayDevice) throws Exception {
        String deviceName = "mqtt_device";
        mqttClient.publish("v1/gateway/connect", Unpooled.wrappedBuffer(createGatewayConnectPayload(deviceName).toString().getBytes())).get();

        TimeUnit.SECONDS.sleep(3);
        List<EntityRelation> relations = restClient.findByFrom(gatewayDevice.getId(), RelationTypeGroup.COMMON);

        Assert.assertEquals(1, relations.size());

        EntityId createdEntityId = relations.get(0).getTo();
        DeviceId createdDeviceId = new DeviceId(createdEntityId.getId());
        Optional<Device> createdDevice = restClient.getDeviceById(createdDeviceId);

        Assert.assertTrue(createdDevice.isPresent());

        return createdDevice.get();
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
