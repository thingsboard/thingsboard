/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.JsonObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttReasonCodeAndPropertiesVariableHeader;
import io.netty.handler.codec.mqtt.MqttReasonCodes;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttVersion;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.AbstractListeningExecutor;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.mqtt.MqttClient;
import org.thingsboard.mqtt.MqttClientCallback;
import org.thingsboard.mqtt.MqttClientConfig;
import org.thingsboard.mqtt.MqttHandler;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rule.NodeConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.DisableUIListeners;
import org.thingsboard.server.msa.WsClient;
import org.thingsboard.server.msa.mapper.AttributesResponse;
import org.thingsboard.server.msa.mapper.WsTelemetryResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;
import static org.thingsboard.server.common.data.AttributeScope.SHARED_SCOPE;
import static org.thingsboard.server.msa.prototypes.DevicePrototypes.defaultDevicePrototype;

@DisableUIListeners
@Slf4j
public class MqttClientTest extends AbstractContainerTest {

    private static final String TRANSPORT_HOST = "localhost";
    private static final int TRANSPORT_PORT = 1883;

    private Device device;
    AbstractListeningExecutor handlerExecutor;

    @BeforeMethod
    public void setUp() throws Exception {
        this.handlerExecutor = new AbstractListeningExecutor() {
            @Override
            protected int getThreadPollSize() {
                return 4;
            }
        };
        handlerExecutor.init();

        testRestClient.login("tenant@thingsboard.org", "tenant");
        device = testRestClient.postDevice("", defaultDevicePrototype("http_"));
    }

    @AfterMethod
    public void tearDown() {
        testRestClient.deleteDeviceIfExists(device.getId());
        if (handlerExecutor != null) {
            handlerExecutor.destroy();
        }
    }

    @Test
    public void telemetryUpload() throws Exception {
        DeviceCredentials deviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        WsClient wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        MqttClient mqttClient = getMqttClient(deviceCredentials, null);
        mqttClient.publish("v1/devices/me/telemetry", Unpooled.wrappedBuffer(createPayload().toString().getBytes())).get();
        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        log.info("Received telemetry: {}", actualLatestTelemetry);
        wsClient.closeBlocking();

        assertThat(actualLatestTelemetry.getData()).hasSize(4);
        assertThat(actualLatestTelemetry.getLatestValues().keySet()).containsOnlyOnceElementsOf(Arrays.asList("booleanKey", "stringKey", "doubleKey", "longKey"));

        assertThat(actualLatestTelemetry.getDataValuesByKey("booleanKey").get(1)).isEqualTo(Boolean.TRUE.toString());
        assertThat(actualLatestTelemetry.getDataValuesByKey("stringKey").get(1)).isEqualTo("value1");
        assertThat(actualLatestTelemetry.getDataValuesByKey("doubleKey").get(1)).isEqualTo(Double.toString(42.0));
        assertThat(actualLatestTelemetry.getDataValuesByKey("longKey").get(1)).isEqualTo(Long.toString(73));
    }

    @Test
    public void telemetryUploadWithTs() throws Exception {
        long ts = 1451649600512L;
        DeviceCredentials deviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        WsClient wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        MqttClient mqttClient = getMqttClient(deviceCredentials, null);
        mqttClient.publish("v1/devices/me/telemetry", Unpooled.wrappedBuffer(createPayload(ts).toString().getBytes())).get();
        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        log.info("Received telemetry: {}", actualLatestTelemetry);
        wsClient.closeBlocking();

        assertThat(actualLatestTelemetry.getData()).hasSize(4);
        assertThat(getExpectedLatestValues(ts)).isEqualTo(actualLatestTelemetry.getLatestValues());

        assertThat(actualLatestTelemetry.getDataValuesByKey("booleanKey").get(1)).isEqualTo(Boolean.TRUE.toString());
        assertThat(actualLatestTelemetry.getDataValuesByKey("stringKey").get(1)).isEqualTo("value1");
        assertThat(actualLatestTelemetry.getDataValuesByKey("doubleKey").get(1)).isEqualTo(Double.toString(42.0));
        assertThat(actualLatestTelemetry.getDataValuesByKey("longKey").get(1)).isEqualTo(Long.toString(73));
    }

    @Test
    public void publishAttributeUpdateToServer() throws Exception {
        DeviceCredentials deviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

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

        assertThat(actualLatestTelemetry.getData()).hasSize(4);
        assertThat(actualLatestTelemetry.getLatestValues().keySet()).containsOnlyOnceElementsOf(Arrays.asList("attr1", "attr2", "attr3", "attr4"));

        assertThat(actualLatestTelemetry.getDataValuesByKey("attr1").get(1)).isEqualTo("value1");
        assertThat(actualLatestTelemetry.getDataValuesByKey("attr2").get(1)).isEqualTo(Boolean.TRUE.toString());
        assertThat(actualLatestTelemetry.getDataValuesByKey("attr3").get(1)).isEqualTo(Double.toString(42.0));
        assertThat(actualLatestTelemetry.getDataValuesByKey("attr4").get(1)).isEqualTo(Long.toString(73));
    }

    @Test
    public void requestAttributeValuesFromServer() throws Exception {
        DeviceCredentials deviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        WsClient wsClient = subscribeToWebSocket(device.getId(), "CLIENT_SCOPE", CmdsType.ATTR_SUB_CMDS);
        MqttMessageListener listener = new MqttMessageListener();
        MqttClient mqttClient = getMqttClient(deviceCredentials, listener);

        // Add a new client attribute
        JsonObject clientAttributes = new JsonObject();
        String clientAttributeValue = StringUtils.randomAlphanumeric(8);
        clientAttributes.addProperty("clientAttr", clientAttributeValue);
        mqttClient.publish("v1/devices/me/attributes", Unpooled.wrappedBuffer(clientAttributes.toString().getBytes())).get();

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        log.info("Received ws telemetry: {}", actualLatestTelemetry);
        wsClient.closeBlocking();

        assertThat(actualLatestTelemetry.getData()).hasSize(1);
        assertThat(actualLatestTelemetry.getLatestValues().keySet()).containsOnly("clientAttr");
        assertThat(actualLatestTelemetry.getDataValuesByKey("clientAttr").get(1)).isEqualTo(clientAttributeValue);

        // Add a new shared attribute
        JsonObject sharedAttributes = new JsonObject();
        String sharedAttributeValue = StringUtils.randomAlphanumeric(8);
        sharedAttributes.addProperty("sharedAttr", sharedAttributeValue);
        JsonNode sharedAttribute = mapper.readTree(sharedAttributes.toString());
        testRestClient.postTelemetryAttribute(device.getId(), SHARED_SCOPE, sharedAttribute);

        // Subscribe to attributes response
        mqttClient.on("v1/devices/me/attributes/response/+", listener, MqttQoS.AT_LEAST_ONCE).get();

        // Wait until subscription is processed
        TimeUnit.SECONDS.sleep(3 * timeoutMultiplier);

        // Request attributes
        JsonObject request = new JsonObject();
        request.addProperty("clientKeys", "clientAttr");
        request.addProperty("sharedKeys", "sharedAttr");
        mqttClient.publish("v1/devices/me/attributes/request/" + new Random().nextInt(100), Unpooled.wrappedBuffer(request.toString().getBytes())).get();
        MqttEvent event = listener.getEvents().poll(10 * timeoutMultiplier, TimeUnit.SECONDS);
        AttributesResponse attributes = mapper.readValue(Objects.requireNonNull(event).getMessage(), AttributesResponse.class);
        log.info("Received telemetry: {}", attributes);

        assertThat(attributes.getClient()).hasSize(1);
        assertThat(attributes.getClient().get("clientAttr")).isEqualTo(clientAttributeValue);

        assertThat(attributes.getShared()).hasSize(1);
        assertThat(attributes.getShared().get("sharedAttr")).isEqualTo(sharedAttributeValue);
    }

    @Test
    public void subscribeToAttributeUpdatesFromServer() throws Exception {
        DeviceCredentials deviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        MqttMessageListener listener = new MqttMessageListener();
        MqttClient mqttClient = getMqttClient(deviceCredentials, listener);
        mqttClient.on("v1/devices/me/attributes", listener, MqttQoS.AT_LEAST_ONCE).get();

        // Wait until subscription is processed
        TimeUnit.SECONDS.sleep(3 * timeoutMultiplier);

        String sharedAttributeName = "sharedAttr";

        // Add a new shared attribute
        JsonObject sharedAttributes = new JsonObject();
        String sharedAttributeValue = StringUtils.randomAlphanumeric(8);
        sharedAttributes.addProperty(sharedAttributeName, sharedAttributeValue);
        JsonNode sharedAttribute = mapper.readTree(sharedAttributes.toString());

        testRestClient.postTelemetryAttribute(device.getId(), SHARED_SCOPE, sharedAttribute);

        MqttEvent event = listener.getEvents().poll(10 * timeoutMultiplier, TimeUnit.SECONDS);
        assertThat(mapper.readValue(Objects.requireNonNull(event).getMessage(), JsonNode.class).get(sharedAttributeName).asText())
                .isEqualTo(sharedAttributeValue);

        // Update the shared attribute value
        JsonObject updatedSharedAttributes = new JsonObject();
        String updatedSharedAttributeValue = StringUtils.randomAlphanumeric(8);
        updatedSharedAttributes.addProperty(sharedAttributeName, updatedSharedAttributeValue);
        testRestClient.postTelemetryAttribute(device.getId(), SHARED_SCOPE, mapper.readTree(updatedSharedAttributes.toString()));

        event = listener.getEvents().poll(10 * timeoutMultiplier, TimeUnit.SECONDS);
        assertThat(mapper.readValue(Objects.requireNonNull(event).getMessage(), JsonNode.class).get(sharedAttributeName).asText())
                .isEqualTo(updatedSharedAttributeValue);
    }

    @Test
    public void serverSideRpc() throws Exception {
        DeviceCredentials deviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        MqttMessageListener listener = new MqttMessageListener();
        MqttClient mqttClient = getMqttClient(deviceCredentials, listener);
        mqttClient.on("v1/devices/me/rpc/request/+", listener, MqttQoS.AT_LEAST_ONCE).get();

        // Wait until subscription is processed
        TimeUnit.SECONDS.sleep(3 * timeoutMultiplier);

        // Send an RPC from the server
        JsonObject serverRpcPayload = new JsonObject();
        serverRpcPayload.addProperty("method", "getValue");
        serverRpcPayload.addProperty("params", true);
        ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName(getClass().getSimpleName())));
        ListenableFuture<JsonNode> future = service.submit(() -> {
            try {
                return testRestClient.postServerSideRpc(device.getId(), mapper.readTree(serverRpcPayload.toString()));
            } catch (IOException e) {
                return null;
            }
        });

        // Wait for RPC call from the server and send the response
        MqttEvent requestFromServer = listener.getEvents().poll(10 * timeoutMultiplier, TimeUnit.SECONDS);

        assertThat(Objects.requireNonNull(requestFromServer).getMessage()).isEqualTo("{\"method\":\"getValue\",\"params\":true}");

        int requestId = Integer.parseInt(Objects.requireNonNull(requestFromServer).getTopic().substring("v1/devices/me/rpc/request/".length()));
        JsonObject clientResponse = new JsonObject();
        clientResponse.addProperty("response", "someResponse");
        // Send a response to the server's RPC request
        mqttClient.publish("v1/devices/me/rpc/response/" + requestId, Unpooled.wrappedBuffer(clientResponse.toString().getBytes())).get();

        JsonNode serverResponse = future.get(5 * timeoutMultiplier, TimeUnit.SECONDS);
        service.shutdownNow();
        assertThat(serverResponse).isEqualTo(mapper.readTree(clientResponse.toString()));
    }

    @Test
    public void serverSidePersistedRpc() throws Exception {
        DeviceCredentials deviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        MqttMessageListener listener = new MqttMessageListener();
        MqttClient mqttClient = getMqttClient(deviceCredentials, listener);
        mqttClient.on("v1/devices/me/rpc/request/+", listener, MqttQoS.AT_LEAST_ONCE).get();

        // Wait until subscription is processed
        TimeUnit.SECONDS.sleep(3 * timeoutMultiplier);

        // Send an RPC from the server
        JsonObject serverRpcPayload = new JsonObject();
        serverRpcPayload.addProperty("method", "getValue");
        serverRpcPayload.addProperty("params", true);
        serverRpcPayload.addProperty("persistent", true);

        JsonNode persistentRpcId = testRestClient.postServerSideRpc(device.getId(), mapper.readTree(serverRpcPayload.toString()));

        assertNotNull(persistentRpcId);

        RpcId rpcId = new RpcId(UUID.fromString(persistentRpcId.get("rpcId").asText()));

        // Wait for RPC call from the server and send the response
        MqttEvent requestFromServer = listener.getEvents().poll(10 * timeoutMultiplier, TimeUnit.SECONDS);

        assertThat(Objects.requireNonNull(requestFromServer).getMessage()).isEqualTo("{\"method\":\"getValue\",\"params\":true}");

        int requestId = Integer.parseInt(Objects.requireNonNull(requestFromServer).getTopic().substring("v1/devices/me/rpc/request/".length()));
        JsonObject clientResponse = new JsonObject();
        clientResponse.addProperty("response", "someResponse");
        // Send a response to the server's RPC request
        mqttClient.publish("v1/devices/me/rpc/response/" + requestId, Unpooled.wrappedBuffer(clientResponse.toString().getBytes())).get();

        PageLink pageLink = new PageLink(10);

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(5 * timeoutMultiplier, TimeUnit.SECONDS)
                .until(() -> {
                    PageData<Rpc> rpcByDevice = testRestClient.getPersistedRpcByDevice(device.getId(), pageLink);
                    for (Rpc rpc : rpcByDevice.getData()) {
                        if (rpc.getId().equals(rpcId)) {
                            return true;
                        }
                    }
                    return false;
                });

        Rpc persistentRpc = testRestClient.getPersistedRpc(rpcId);

        assertThat(persistentRpc.getResponse()).isEqualTo(mapper.readTree(clientResponse.toString()));
    }

    @Test
    public void clientSideRpc() throws Exception {
        DeviceCredentials deviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        MqttMessageListener listener = new MqttMessageListener();
        MqttClient mqttClient = getMqttClient(deviceCredentials, listener);
        mqttClient.on("v1/devices/me/rpc/request/+", listener, MqttQoS.AT_LEAST_ONCE).get();

        // Get the default rule chain id to make it root again after test finished
        RuleChainId defaultRuleChainId = getDefaultRuleChainId();

        // Create a new root rule chain
        RuleChainId ruleChainId = createRootRuleChainForRpcResponse();

        TimeUnit.SECONDS.sleep(3 * timeoutMultiplier);
        // Send the request to the server
        JsonObject clientRequest = new JsonObject();
        clientRequest.addProperty("method", "getResponse");
        clientRequest.addProperty("params", true);
        Integer requestId = 42;
        mqttClient.publish("v1/devices/me/rpc/request/" + requestId, Unpooled.wrappedBuffer(clientRequest.toString().getBytes())).get();

        // Check the response from the server
        TimeUnit.SECONDS.sleep(3 * timeoutMultiplier);
        MqttEvent responseFromServer = listener.getEvents().poll(3 * timeoutMultiplier, TimeUnit.SECONDS);
        Integer responseId = Integer.valueOf(Objects.requireNonNull(responseFromServer).getTopic().substring("v1/devices/me/rpc/response/".length()));
        assertThat(responseId).isEqualTo(requestId);
        assertThat(mapper.readTree(responseFromServer.getMessage()).get("response").asText()).isEqualTo("requestReceived");

        // Make the default rule chain a root again
        testRestClient.setRootRuleChain(defaultRuleChainId);

        // Delete the created rule chain
        testRestClient.deleteRuleChain(ruleChainId);
    }

    @Test
    public void deviceDeletedClosingSession() throws Exception {
        DeviceCredentials deviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        MqttMessageListener listener = new MqttMessageListener();
        MqttClient mqttClient = getMqttClient(deviceCredentials, listener);

        testRestClient.deleteDeviceIfExists(device.getId());

        Awaitility
                .await()
                .alias("Check device connection.")
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> !mqttClient.isConnected());
    }

    @Test
    public void provisionRequestForDeviceWithPreProvisionedStrategy() throws Exception {

        DeviceProfile deviceProfile = testRestClient.getDeviceProfileById(device.getDeviceProfileId());
        deviceProfile = updateDeviceProfileWithProvisioningStrategy(deviceProfile, DeviceProfileProvisionType.CHECK_PRE_PROVISIONED_DEVICES);

        DeviceCredentials expectedDeviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        MqttMessageListener listener = new MqttMessageListener();
        MqttClient mqttClient = getMqttClient("provision", listener);

        JsonObject provisionRequest = new JsonObject();
        provisionRequest.addProperty("provisionDeviceKey", TEST_PROVISION_DEVICE_KEY);
        provisionRequest.addProperty("provisionDeviceSecret", TEST_PROVISION_DEVICE_SECRET);
        provisionRequest.addProperty("deviceName", device.getName());

        mqttClient.publish("/provision/request", Unpooled.wrappedBuffer(provisionRequest.toString().getBytes())).get();

        //Wait for response
        TimeUnit.SECONDS.sleep(3 * timeoutMultiplier);

        MqttEvent provisionResponseMsg = listener.getEvents().poll(timeoutMultiplier, TimeUnit.SECONDS);

        assertThat(provisionResponseMsg).isNotNull();

        JsonNode provisionResponse = mapper.readTree(provisionResponseMsg.getMessage());

        assertThat(provisionResponse.get("credentialsType").asText()).isEqualTo(expectedDeviceCredentials.getCredentialsType().name());
        assertThat(provisionResponse.get("credentialsValue").asText()).isEqualTo(expectedDeviceCredentials.getCredentialsId());
        assertThat(provisionResponse.get("status").asText()).isEqualTo("SUCCESS");

        updateDeviceProfileWithProvisioningStrategy(deviceProfile, DeviceProfileProvisionType.DISABLED);
    }

    @Test
    public void provisionRequestForDeviceWithAllowToCreateNewDevicesStrategy() throws Exception {

        String testDeviceName = "test_provision_device";

        DeviceProfile deviceProfile = testRestClient.getDeviceProfileById(device.getDeviceProfileId());

        deviceProfile = updateDeviceProfileWithProvisioningStrategy(deviceProfile, DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES);

        MqttMessageListener listener = new MqttMessageListener();
        MqttClient mqttClient = getMqttClient("provision", listener);

        JsonObject provisionRequest = new JsonObject();
        provisionRequest.addProperty("provisionDeviceKey", TEST_PROVISION_DEVICE_KEY);
        provisionRequest.addProperty("provisionDeviceSecret", TEST_PROVISION_DEVICE_SECRET);
        provisionRequest.addProperty("deviceName", testDeviceName);

        mqttClient.publish("/provision/request", Unpooled.wrappedBuffer(provisionRequest.toString().getBytes())).get();

        //Wait for response
        TimeUnit.SECONDS.sleep(3 * timeoutMultiplier);

        MqttEvent provisionResponseMsg = listener.getEvents().poll(timeoutMultiplier, TimeUnit.SECONDS);

        assertThat(provisionResponseMsg).isNotNull();

        JsonNode provisionResponse = mapper.readTree(provisionResponseMsg.getMessage());

        testRestClient.deleteDeviceIfExists(device.getId());
        device = testRestClient.getDeviceByName(testDeviceName);

        DeviceCredentials expectedDeviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        assertThat(provisionResponse.get("credentialsType").asText()).isEqualTo(expectedDeviceCredentials.getCredentialsType().name());
        assertThat(provisionResponse.get("credentialsValue").asText()).isEqualTo(expectedDeviceCredentials.getCredentialsId());
        assertThat(provisionResponse.get("status").asText()).isEqualTo("SUCCESS");

        updateDeviceProfileWithProvisioningStrategy(deviceProfile, DeviceProfileProvisionType.DISABLED);
    }

    @Test
    public void provisionRequestForCheckSubAckReceived() throws Exception {

        DeviceProfile deviceProfile = testRestClient.getDeviceProfileById(device.getDeviceProfileId());
        deviceProfile = updateDeviceProfileWithProvisioningStrategy(deviceProfile, DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES);

        MqttMessageListener listener = new MqttMessageListener();
        MqttClient mqttClient = getMqttClient("provision", listener, MqttVersion.MQTT_5);
        final MqttReasonCodes.SubAck[] subAckResult = new MqttReasonCodes.SubAck[1];
        mqttClient.setCallback(new MqttClientCallback() {
                                   @Override
                                   public void connectionLost(Throwable cause) {
                                   }

                                   @Override
                                   public void onSuccessfulReconnect() {
                                   }

                                   @Override
                                   public void onSubAck(MqttSubAckMessage subAckMessage) {
                                       subAckResult[0] = subAckMessage.payload().typedReasonCodes().get(0);
                                   }
                               }
        );

        mqttClient.on("/provision/response", listener, MqttQoS.AT_LEAST_ONCE).get(3 * timeoutMultiplier, TimeUnit.SECONDS);
        TimeUnit.SECONDS.sleep(2 * timeoutMultiplier);
        assertThat(subAckResult[0]).isNotNull();
        assertThat(MqttReasonCodes.SubAck.GRANTED_QOS_1).isEqualTo(subAckResult[0]);

        subAckResult[0] = null;
        mqttClient.on("v1/devices/me/attributes", listener, MqttQoS.AT_LEAST_ONCE).get(3 * timeoutMultiplier, TimeUnit.SECONDS);
        TimeUnit.SECONDS.sleep(2 * timeoutMultiplier);
        assertThat(subAckResult[0]).isNotNull();
        assertThat(MqttReasonCodes.SubAck.TOPIC_FILTER_INVALID).isEqualTo(subAckResult[0]);

        testRestClient.deleteDeviceIfExists(device.getId());
        updateDeviceProfileWithProvisioningStrategy(deviceProfile, DeviceProfileProvisionType.DISABLED);
    }



    @Test
    public void provisionRequestForDeviceWithDisabledProvisioningStrategy() throws Exception {

        MqttMessageListener listener = new MqttMessageListener();
        MqttClient mqttClient = getMqttClient("provision", listener);

        JsonObject provisionRequest = new JsonObject();
        provisionRequest.addProperty("provisionDeviceKey", TEST_PROVISION_DEVICE_KEY);
        provisionRequest.addProperty("provisionDeviceSecret", TEST_PROVISION_DEVICE_SECRET);

        mqttClient.publish("/provision/request", Unpooled.wrappedBuffer(provisionRequest.toString().getBytes())).get();

        //Wait for response
        TimeUnit.SECONDS.sleep(3 * timeoutMultiplier);

        MqttEvent provisionResponseMsg = listener.getEvents().poll(timeoutMultiplier, TimeUnit.SECONDS);

        assertThat(provisionResponseMsg).isNotNull();

        JsonNode provisionResponse = mapper.readTree(provisionResponseMsg.getMessage());

        assertThat(provisionResponse.get("status").asText()).isEqualTo("NOT_FOUND");
    }

    @Test
    public void clientSessionTakenOverDisconnect() throws Exception {
        DeviceCredentials deviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        MqttMessageListener listener = new MqttMessageListener();
        MqttClient mqttClient = getMqttClient(deviceCredentials, listener, MqttVersion.MQTT_5);
        final List<Byte> returnCodeByteValue = new ArrayList<>();
        MqttClientCallback callbackForDisconnectWithReturnCode = getCallbackWrapperForDisconnectWithReturnCode(returnCodeByteValue);
        mqttClient.setCallback(callbackForDisconnectWithReturnCode);

        Thread.sleep(1000);

        MqttMessageListener dummyListener = new MqttMessageListener();
        MqttClient dummyMqttClient = getMqttClient(deviceCredentials, dummyListener, MqttVersion.MQTT_5);
        final List<Byte> returnCodeByteValueSecondClient = new ArrayList<>();
        MqttClientCallback callbackForDisconnectWithReturnCodeDummy = getCallbackWrapperForDisconnectWithReturnCode(returnCodeByteValueSecondClient);
        dummyMqttClient.setCallback(callbackForDisconnectWithReturnCodeDummy);

        Awaitility
                .await()
                .alias("Check device disconnect.")
                .atMost(TIMEOUT * timeoutMultiplier, TimeUnit.SECONDS)
                .until(() -> !returnCodeByteValue.isEmpty());

        assertThat(returnCodeByteValueSecondClient).isEmpty();
        assertThat(returnCodeByteValue).isNotEmpty();

        MqttReasonCodes.Disconnect returnCode = MqttReasonCodes.Disconnect.valueOf(returnCodeByteValue.get(0));

        dummyMqttClient.disconnect();

        assertThat(returnCode).isEqualTo(MqttReasonCodes.Disconnect.SESSION_TAKEN_OVER);
    }

    @Test
    public void clientPublishForRegularTopicByProvisionClient() throws Exception {
        MqttClient mqttClient = getMqttClient("provision", new MqttMessageListener(), MqttVersion.MQTT_5);
        final List<Byte> returnCodeByteValue = new ArrayList<>();
        MqttClientCallback callbackForDisconnectWithReturnCode = getCallbackWrapperForDisconnectWithReturnCode(returnCodeByteValue);
        mqttClient.setCallback(callbackForDisconnectWithReturnCode);
        mqttClient.publish("v1/devices/me/telemetry", Unpooled.wrappedBuffer("test".getBytes()), MqttQoS.AT_LEAST_ONCE).get();
        Thread.sleep(1000);
        assertThat(returnCodeByteValue).isNotEmpty();
        MqttReasonCodes.Disconnect returnCode = MqttReasonCodes.Disconnect.valueOf(returnCodeByteValue.get(0));
        assertThat(returnCode).isEqualTo(MqttReasonCodes.Disconnect.TOPIC_NAME_INVALID);
    }

    @Test
    public void clientConnectWithBadCredentials() throws Exception {
        MqttClient mqttClient = getMqttClient("unknownAccessToken", new MqttMessageListener(), MqttVersion.MQTT_5, false);
        final List<Byte> returnCodeByteValue = new ArrayList<>();
        MqttClientCallback callbackForDisconnectWithReturnCode = getCallbackWrapperForDisconnectWithReturnCode(returnCodeByteValue);
        mqttClient.setCallback(callbackForDisconnectWithReturnCode);
        try {
            mqttClient.connect(TRANSPORT_HOST, TRANSPORT_PORT).get(1, TimeUnit.SECONDS);
        } catch (TimeoutException ignored) {
        }
        assertThat(returnCodeByteValue).isNotEmpty();
        MqttConnectReturnCode returnCode = MqttConnectReturnCode.valueOf(returnCodeByteValue.get(0));
        assertThat(returnCode).isIn(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD, MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USERNAME_OR_PASSWORD);
    }

    private RuleChainId createRootRuleChainForRpcResponse() throws Exception {
        RuleChain newRuleChain = new RuleChain();
        newRuleChain.setName("testRuleChain");

        RuleChain ruleChain = testRestClient.postRuleChain(newRuleChain);

        JsonNode configuration = mapper.readTree(this.getClass().getClassLoader().getResourceAsStream("RpcResponseRuleChainMetadata.json"));
        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        ruleChainMetaData.setRuleChainId(ruleChain.getId());
        ruleChainMetaData.setFirstNodeIndex(configuration.get("firstNodeIndex").asInt());
        ruleChainMetaData.setNodes(Arrays.asList(mapper.treeToValue(configuration.get("nodes"), RuleNode[].class)));
        ruleChainMetaData.setConnections(Arrays.asList(mapper.treeToValue(configuration.get("connections"), NodeConnectionInfo[].class)));

        testRestClient.postRuleChainMetadata(ruleChainMetaData);

        // Set a new rule chain as root
        testRestClient.setRootRuleChain(ruleChain.getId());
        return ruleChain.getId();
    }

    private RuleChainId getDefaultRuleChainId() {
        PageData<RuleChain> ruleChains = testRestClient.getRuleChains(new PageLink(40, 0));

        Optional<RuleChain> defaultRuleChain = ruleChains.getData()
                .stream()
                .filter(RuleChain::isRoot)
                .findFirst();
        if (defaultRuleChain.isEmpty()) {
            fail("Root rule chain wasn't found");
        }
        return defaultRuleChain.get().getId();
    }

    private MqttClientCallback getCallbackWrapperForDisconnectWithReturnCode(List<Byte> returnCodeByteValueWrapper) {
        return new MqttClientCallback() {
            @Override
            public void connectionLost(Throwable cause) {
            }

            @Override
            public void onSuccessfulReconnect() {
            }

            @Override
            public void onDisconnect(MqttMessage mqttDisconnectMessage) {
                log.info("Disconnected with reason: {}", mqttDisconnectMessage);
                returnCodeByteValueWrapper.add(((MqttReasonCodeAndPropertiesVariableHeader) mqttDisconnectMessage.variableHeader()).reasonCode());
            }
        };
    }

    private MqttClient getMqttClient(DeviceCredentials deviceCredentials, MqttMessageListener listener) throws InterruptedException, ExecutionException {
        return getMqttClient(deviceCredentials.getCredentialsId(), listener, MqttVersion.MQTT_3_1_1, true);
    }

    private MqttClient getMqttClient(DeviceCredentials deviceCredentials, MqttMessageListener listener, MqttVersion mqttVersion) throws InterruptedException, ExecutionException {
        return getMqttClient(deviceCredentials.getCredentialsId(), listener, mqttVersion, true);
    }

    private MqttClient getMqttClient(DeviceCredentials deviceCredentials, MqttMessageListener listener, MqttVersion mqttVersion, boolean connect) throws InterruptedException, ExecutionException {
        return getMqttClient(deviceCredentials.getCredentialsId(), listener, mqttVersion, connect);
    }

    private String getOwnerId() {
        return "Tenant[" + device.getTenantId().getId() + "]MqttClientTestDevice[" + device.getId().getId() + "]";
    }

    private MqttClient getMqttClient(String username, MqttMessageListener listener) throws InterruptedException, ExecutionException {
        return getMqttClient(username, listener, MqttVersion.MQTT_3_1_1, true);
    }

    private MqttClient getMqttClient(String username, MqttMessageListener listener, MqttVersion mqttVersion) throws InterruptedException, ExecutionException {
        return getMqttClient(username, listener, mqttVersion, true);
    }

    private MqttClient getMqttClient(String username, MqttMessageListener listener, MqttVersion mqttVersion, boolean connect) throws InterruptedException, ExecutionException {
        MqttClientConfig clientConfig = new MqttClientConfig();
        clientConfig.setOwnerId(getOwnerId());
        clientConfig.setClientId("MQTT client from test");
        clientConfig.setUsername(username);
        clientConfig.setProtocolVersion(mqttVersion);
        clientConfig.setRetransmissionConfig(new MqttClientConfig.RetransmissionConfig(3, 5000L, 0.15d)); // same as defaults in thingsboard.yml as of time of this writing
        MqttClient mqttClient = MqttClient.create(clientConfig, listener, handlerExecutor);
        if (connect) {
            mqttClient.connect(TRANSPORT_HOST, TRANSPORT_PORT).get();
        }
        return mqttClient;
    }

    @Data
    private class MqttMessageListener implements MqttHandler {
        private final BlockingQueue<MqttEvent> events;

        private MqttMessageListener() {
            events = new ArrayBlockingQueue<>(100);
        }

        @Override
        public ListenableFuture<Void> onMessage(String topic, ByteBuf message) {
            log.info("MQTT message [{}], topic [{}]", message.toString(StandardCharsets.UTF_8), topic);
            events.add(new MqttEvent(topic, message.toString(StandardCharsets.UTF_8)));
            return Futures.immediateVoidFuture();
        }
    }

    @Data
    private class MqttEvent {
        private final String topic;
        private final String message;
    }
}
