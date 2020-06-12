/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.mqtt.attributes.request;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.gen.transport.TransportApiProtos;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.mqtt.MqttTopics;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public abstract class AbstractMqttAttributesRequestIntegrationTest extends AbstractControllerTest {

    private static final String MQTT_URL = "tcp://localhost:1883";
    protected static final String POST_ATTRIBUTES_PAYLOAD = "{\"attribute1\": \"value1\", \"attribute2\": true, \"attribute3\": 42.0, \"attribute4\": 73," +
            " \"attribute5\": {\"someNumber\": 42, \"someArray\": [1,2,3], \"someNestedObject\": {\"key\": \"value\"}}}";

    private Tenant savedTenant;
    private User tenantAdmin;

    private static final AtomicInteger atomicInteger = new AtomicInteger(2);


    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant" + atomicInteger.getAndIncrement() + "@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        createUserAndLogin(tenantAdmin, "testPassword1");
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();
        if (savedTenant != null) {
            doDelete("/api/tenant/" + savedTenant.getId().getId().toString()).andExpect(status().isOk());
        }
    }

    @Test
    public void testRequestAttributesValuesFromTheServerV1Json() throws Exception {
        processTestRequestAttributesValuesFromTheServer(
                "Test Request attribute values from the server V1 Json",
                MqttTopics.DEVICE_ATTRIBUTES_TOPIC_V1_JSON,
                MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_TOPIC_V1_JSON,
                MqttTopics.DEVICE_ATTRIBUTES_REQUEST_TOPIC_PREFIX_V1_JSON);
    }

    @Test
    public void testRequestAttributesValuesFromTheServerV2Json() throws Exception {
        processTestRequestAttributesValuesFromTheServer(
                "Test Request attribute values from the server V2 Json",
                MqttTopics.DEVICE_ATTRIBUTES_TOPIC_V2_JSON,
                MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_TOPIC_V2_JSON,
                MqttTopics.DEVICE_ATTRIBUTES_REQUEST_TOPIC_PREFIX_V2_JSON);
    }

    @Test
    public void testRequestAttributesValuesFromTheServerV2Proto() throws Exception {
        processTestRequestAttributesValuesFromTheServer(
                "Test Request attribute values from the server V2 Proto",
                MqttTopics.DEVICE_ATTRIBUTES_TOPIC_V2_PROTO,
                MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_TOPIC_V2_PROTO,
                MqttTopics.DEVICE_ATTRIBUTES_REQUEST_TOPIC_PREFIX_V2_PROTO);
    }

    @Test
    public void testRequestAttributesValuesFromTheServerV1GatewayJson() throws Exception {
        processTestGatewayRequestAttributesValuesFromTheServer(
                "Gateway Test Request attribute values from the server V1 Json",
                "Gateway Device V1 Json",
                MqttTopics.GATEWAY_ATTRIBUTES_TOPIC_V1_JSON,
                MqttTopics.GATEWAY_ATTRIBUTES_RESPONSE_TOPIC_V1_JSON,
                MqttTopics.GATEWAY_ATTRIBUTES_REQUEST_TOPIC_V1_JSON);
    }

    @Test
    public void testRequestAttributesValuesFromTheServerV2GatewayJson() throws Exception {
        processTestGatewayRequestAttributesValuesFromTheServer(
                "Gateway Test Request attribute values from the server V2 Json",
                "Gateway Device V2 Json",
                MqttTopics.GATEWAY_ATTRIBUTES_TOPIC_V2_JSON,
                MqttTopics.GATEWAY_ATTRIBUTES_RESPONSE_TOPIC_V2_JSON,
                MqttTopics.GATEWAY_ATTRIBUTES_REQUEST_TOPIC_V2_JSON);
    }

    @Test
    public void testRequestAttributesValuesFromTheServerV2GatewayProto() throws Exception {
        processTestGatewayRequestAttributesValuesFromTheServer(
                "Gateway Test Request attribute values from the server V2 Proto",
                "Gateway Device V2 Proto",
                MqttTopics.GATEWAY_ATTRIBUTES_TOPIC_V2_PROTO,
                MqttTopics.GATEWAY_ATTRIBUTES_RESPONSE_TOPIC_V2_PROTO,
                MqttTopics.GATEWAY_ATTRIBUTES_REQUEST_TOPIC_V2_PROTO);
    }

    private void processTestRequestAttributesValuesFromTheServer(String deviceName,
                                                                 String topicToPostAttributes,
                                                                 String topicToSubscribeForAttributesValues,
                                                                 String topicToRequestAttributesValues) throws Exception {
        Device device = new Device();
        device.setName(deviceName);
        device.setType("default");
        Device savedDevice = getSavedDevice(device);
        DeviceCredentials deviceCredentials = getDeviceCredentials(savedDevice);
        assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        String accessToken = deviceCredentials.getCredentialsId();
        assertNotNull(accessToken);

        MqttAsyncClient client = getMqttAsyncClient(accessToken);

        postAttributesAndSubscribeToTopic(savedDevice, client, topicToPostAttributes, topicToSubscribeForAttributesValues);

        Thread.sleep(1000);

        CountDownLatch latch = new CountDownLatch(1);
        TestMqttCallback callback = new TestMqttCallback(latch);
        client.setCallback(callback);

        validateResponse(client, latch, callback, topicToRequestAttributesValues);
    }

    private void processTestGatewayRequestAttributesValuesFromTheServer(String gatewayName, String deviceName,
                                                                        String topicToPostAttributes,
                                                                        String topicToSubscribeForAttributesValues,
                                                                        String topicToRequestAttributesValues) throws Exception {
        Device gateway = new Device();
        gateway.setName(gatewayName);
        gateway.setType("gateway");
        ObjectNode additionalInfo = mapper.createObjectNode();
        additionalInfo.put("gateway", true);
        gateway.setAdditionalInfo(additionalInfo);
        Device savedGateway = getSavedDevice(gateway);
        DeviceCredentials gatewayCredentials = getDeviceCredentials(savedGateway);
        assertEquals(savedGateway.getId(), gatewayCredentials.getDeviceId());
        String gatewayAccessToken = gatewayCredentials.getCredentialsId();
        assertNotNull(gatewayAccessToken);

        MqttAsyncClient client = getMqttAsyncClient(gatewayAccessToken);

        postGatewayDeviceClientAttributes(client, topicToPostAttributes, deviceName);

        Thread.sleep(1000);

        Device savedDevice = doGet("/api/tenant/devices?deviceName=" + deviceName, Device.class);
        assertNotNull(savedDevice);
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", POST_ATTRIBUTES_PAYLOAD, String.class, status().isOk());

        Thread.sleep(1000);

        client.subscribe(topicToSubscribeForAttributesValues, MqttQoS.AT_LEAST_ONCE.value());

        TestMqttCallback clientAttributesCallback = getTestMqttCallback();
        client.setCallback(clientAttributesCallback);
        validateClientResponseGateway(client, clientAttributesCallback, deviceName, topicToRequestAttributesValues);

        TestMqttCallback sharedAttributesCallback = getTestMqttCallback();
        client.setCallback(sharedAttributesCallback);
        validateSharedResponseGateway(client, sharedAttributesCallback, deviceName, topicToRequestAttributesValues, false);

        doDelete("/api/plugins/telemetry/" + savedDevice.getId().getId() + "/SHARED_SCOPE?keys=attribute5");

        Thread.sleep(1000);

        TestMqttCallback sharedDeletedAttributesCallback = getTestMqttCallback();
        client.setCallback(sharedDeletedAttributesCallback);
        validateSharedResponseGateway(client, sharedDeletedAttributesCallback, deviceName, topicToRequestAttributesValues, true);

    }

    private TestMqttCallback getTestMqttCallback() {
        CountDownLatch latch = new CountDownLatch(1);
        return new TestMqttCallback(latch);
    }

    private MqttAsyncClient getMqttAsyncClient(String accessToken) throws MqttException {
        String clientId = MqttAsyncClient.generateClientId();
        MqttAsyncClient client = new MqttAsyncClient(MQTT_URL, clientId);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(accessToken);
        client.connect(options).waitForCompletion();
        return client;
    }

    private void postAttributesAndSubscribeToTopic(Device savedDevice, MqttAsyncClient client, String topicToPost, String topicToSubscribe) throws Exception {
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", POST_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        if (topicToPost.startsWith(MqttTopics.BASE_DEVICE_API_TOPIC_V1_JSON) || topicToPost.startsWith(MqttTopics.BASE_DEVICE_API_TOPIC_V2_JSON)) {
            client.publish(topicToPost, new MqttMessage(POST_ATTRIBUTES_PAYLOAD.getBytes()));
        } else {
            TransportProtos.PostAttributeMsg postAttributeMsg = getPostAttributeMsg();
            byte[] payload = postAttributeMsg.toByteArray();
            client.publish(topicToPost, new MqttMessage(payload));
        }
        client.subscribe(topicToSubscribe, MqttQoS.AT_MOST_ONCE.value());
    }

    private void postGatewayDeviceClientAttributes(MqttAsyncClient client, String topicToPost, String deviceName) throws Exception {
        if (topicToPost.startsWith(MqttTopics.BASE_GATEWAY_API_TOPIC_V1_JSON) || topicToPost.startsWith(MqttTopics.BASE_GATEWAY_API_TOPIC_V2_JSON)) {
            String postClientAttributes = "{\"" + deviceName + "\":{\"attribute1\":\"value1\",\"attribute2\":true,\"attribute3\":42.0,\"attribute4\":73,\"attribute5\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}}}";
            client.publish(topicToPost, new MqttMessage(postClientAttributes.getBytes()));
        } else {
            TransportProtos.PostAttributeMsg postAttributeMsg = getPostAttributeMsg();
            TransportApiProtos.AttributesMsg.Builder attributesMsgBuilder = TransportApiProtos.AttributesMsg.newBuilder();
            attributesMsgBuilder.setDeviceName(deviceName);
            attributesMsgBuilder.setMsg(postAttributeMsg);
            TransportApiProtos.AttributesMsg attributesMsg = attributesMsgBuilder.build();
            TransportApiProtos.GatewayAttributesMsg.Builder gatewayAttributeMsgBuilder = TransportApiProtos.GatewayAttributesMsg.newBuilder();
            gatewayAttributeMsgBuilder.addMsg(attributesMsg);
            byte[] bytes = gatewayAttributeMsgBuilder.build().toByteArray();
            client.publish(topicToPost, new MqttMessage(bytes));
        }
    }

    private void validateResponse(MqttAsyncClient client, CountDownLatch latch, TestMqttCallback callback, String topic) throws MqttException, InterruptedException, InvalidProtocolBufferException {
        String keys = "attribute1,attribute2,attribute3,attribute4,attribute5";
        if (topic.startsWith(MqttTopics.BASE_DEVICE_API_TOPIC_V1_JSON) || topic.startsWith(MqttTopics.BASE_DEVICE_API_TOPIC_V2_JSON)) {
            String payloadStr = "{\"clientKeys\":\"" + keys + "\", \"sharedKeys\":\"" + keys + "\"}";
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setPayload(payloadStr.getBytes());
            client.publish(topic + "1", mqttMessage);
            latch.await(3, TimeUnit.SECONDS);
            assertEquals(MqttQoS.AT_MOST_ONCE.value(), callback.getQoS());
            String expectedRequestPayload = "{\"client\":{\"attribute5\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}},\"attribute4\":73,\"attribute1\":\"value1\",\"attribute3\":42.0,\"attribute2\":true},\"shared\":{\"attribute5\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}},\"attribute4\":73,\"attribute1\":\"value1\",\"attribute3\":42.0,\"attribute2\":true}}";
            assertEquals(expectedRequestPayload, new String(callback.getPayloadBytes(), StandardCharsets.UTF_8));
        } else {
            TransportApiProtos.AttributesRequest.Builder attributesRequestBuilder = TransportApiProtos.AttributesRequest.newBuilder();
            attributesRequestBuilder.setClientKeys(keys);
            attributesRequestBuilder.setSharedKeys(keys);
            TransportApiProtos.AttributesRequest attributesRequest = attributesRequestBuilder.build();
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setPayload(attributesRequest.toByteArray());
            client.publish(topic + "1", mqttMessage);
            latch.await(3, TimeUnit.SECONDS);
            assertEquals(MqttQoS.AT_MOST_ONCE.value(), callback.getQoS());
            TransportProtos.GetAttributeResponseMsg expectedAttributesResponse = getExpectedAttributeResponseMsg();
            TransportProtos.GetAttributeResponseMsg actualAttributesResponse = TransportProtos.GetAttributeResponseMsg.parseFrom(callback.getPayloadBytes());
            assertEquals(expectedAttributesResponse.getRequestId(), actualAttributesResponse.getRequestId());
            List<TransportProtos.KeyValueProto> expectedClientKeyValueProtos = expectedAttributesResponse.getClientAttributeListList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
            List<TransportProtos.KeyValueProto> expectedSharedKeyValueProtos = expectedAttributesResponse.getSharedAttributeListList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
            List<TransportProtos.KeyValueProto> actualClientKeyValueProtos = actualAttributesResponse.getClientAttributeListList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
            List<TransportProtos.KeyValueProto> actualSharedKeyValueProtos = actualAttributesResponse.getSharedAttributeListList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
            assertTrue(actualClientKeyValueProtos.containsAll(expectedClientKeyValueProtos));
            assertTrue(actualSharedKeyValueProtos.containsAll(expectedSharedKeyValueProtos));
        }
    }

    private void validateClientResponseGateway(MqttAsyncClient client, TestMqttCallback callback, String deviceName, String topic) throws MqttException, InterruptedException, InvalidProtocolBufferException {
        if (topic.startsWith(MqttTopics.BASE_GATEWAY_API_TOPIC_V1_JSON) || topic.startsWith(MqttTopics.BASE_GATEWAY_API_TOPIC_V2_JSON)) {
            String payloadStr = "{\"id\": 1, \"device\": \"" + deviceName + "\", \"client\": true, \"keys\": [\"attribute1\", \"attribute2\", \"attribute3\", \"attribute4\", \"attribute5\"]}";
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setPayload(payloadStr.getBytes());
            client.publish(topic, mqttMessage);
            callback.getLatch().await(3, TimeUnit.SECONDS);
            // TODO: 6/4/20 why qos 1 when subscribe with 0?
            assertEquals(MqttQoS.AT_LEAST_ONCE.value(), callback.getQoS());
            String expectedRequestPayload = "{\"id\":1,\"device\":\"" + deviceName + "\",\"values\":{\"attribute5\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}},\"attribute4\":73,\"attribute1\":\"value1\",\"attribute3\":42.0,\"attribute2\":true}}";
            assertEquals(expectedRequestPayload, new String(callback.getPayloadBytes(), StandardCharsets.UTF_8));
        } else {
            String keys = "attribute1,attribute2,attribute3,attribute4,attribute5";
            TransportApiProtos.GatewayAttributesRequestMsg gatewayAttributesRequestMsg = getGatewayAttributesRequestMsg(deviceName, keys, true);
            client.publish(topic, new MqttMessage(gatewayAttributesRequestMsg.toByteArray()));
            callback.getLatch().await(3, TimeUnit.SECONDS);
            // TODO: 6/4/20 why qos 1 when subscribe with 0?
            assertEquals(MqttQoS.AT_LEAST_ONCE.value(), callback.getQoS());
            TransportApiProtos.GatewayAttributeResponseMsg expectedGatewayAttributeResponseMsg = getExpectedGatewayAttributeResponseMsg(deviceName, true);
            TransportApiProtos.GatewayAttributeResponseMsg actualGatewayAttributeResponseMsg = TransportApiProtos.GatewayAttributeResponseMsg.parseFrom(callback.getPayloadBytes());
            assertEquals(expectedGatewayAttributeResponseMsg.getDeviceName(), actualGatewayAttributeResponseMsg.getDeviceName());

            TransportProtos.GetAttributeResponseMsg expectedResponseMsg = expectedGatewayAttributeResponseMsg.getResponseMsg();
            TransportProtos.GetAttributeResponseMsg actualResponseMsg = actualGatewayAttributeResponseMsg.getResponseMsg();
            assertEquals(expectedResponseMsg.getRequestId(), actualResponseMsg.getRequestId());

            List<TransportProtos.KeyValueProto> expectedClientKeyValueProtos = expectedResponseMsg.getClientAttributeListList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
            List<TransportProtos.KeyValueProto> actualClientKeyValueProtos = actualResponseMsg.getClientAttributeListList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
            assertTrue(actualClientKeyValueProtos.containsAll(expectedClientKeyValueProtos));
        }
    }

    private void validateSharedResponseGateway(MqttAsyncClient client, TestMqttCallback callback, String deviceName, String topic, boolean checkForDeleted) throws MqttException, InterruptedException, InvalidProtocolBufferException {
        if (topic.startsWith(MqttTopics.BASE_GATEWAY_API_TOPIC_V1_JSON) || topic.startsWith(MqttTopics.BASE_GATEWAY_API_TOPIC_V2_JSON)) {
            String payloadStr = "{\"id\": 1, \"device\": \"" + deviceName + "\", \"client\": false, \"keys\": [\"attribute1\", \"attribute2\", \"attribute3\", \"attribute4\", \"attribute5\"]}";
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setPayload(payloadStr.getBytes());
            client.publish(topic, mqttMessage);
            callback.getLatch().await(3, TimeUnit.SECONDS);
            // TODO: 6/4/20 why qos 1 when subscribe with 0?
            assertEquals(MqttQoS.AT_LEAST_ONCE.value(), callback.getQoS());
            if (!checkForDeleted) {
                String expectedRequestPayload = "{\"id\":1,\"device\":\"" + deviceName + "\",\"values\":{\"attribute5\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}},\"attribute4\":73,\"attribute1\":\"value1\",\"attribute3\":42.0,\"attribute2\":true}}";
                assertEquals(expectedRequestPayload, new String(callback.getPayloadBytes(), StandardCharsets.UTF_8));
            } else {
                String expectedRequestPayload = "{\"id\":1,\"device\":\"" + deviceName + "\",\"values\":{\"attribute4\":73,\"attribute1\":\"value1\",\"attribute3\":42.0,\"attribute2\":true},\"deleted\":[\"attribute5\"]}";
                assertEquals(expectedRequestPayload, new String(callback.getPayloadBytes(), StandardCharsets.UTF_8));
            }
        } else {
            String keys = "attribute1,attribute2,attribute3,attribute4,attribute5";
            TransportApiProtos.GatewayAttributesRequestMsg gatewayAttributesRequestMsg = getGatewayAttributesRequestMsg(deviceName, keys, false);
            client.publish(topic, new MqttMessage(gatewayAttributesRequestMsg.toByteArray()));
            callback.getLatch().await(3, TimeUnit.SECONDS);
            // TODO: 6/4/20 why qos 1 when subscribe with 0?
            assertEquals(MqttQoS.AT_LEAST_ONCE.value(), callback.getQoS());
            TransportApiProtos.GatewayAttributeResponseMsg expectedGatewayAttributeResponseMsg = getExpectedGatewayAttributeResponseMsg(deviceName, false);
            TransportApiProtos.GatewayAttributeResponseMsg actualGatewayAttributeResponseMsg = TransportApiProtos.GatewayAttributeResponseMsg.parseFrom(callback.getPayloadBytes());
            assertEquals(expectedGatewayAttributeResponseMsg.getDeviceName(), actualGatewayAttributeResponseMsg.getDeviceName());

            TransportProtos.GetAttributeResponseMsg expectedResponseMsg = expectedGatewayAttributeResponseMsg.getResponseMsg();
            TransportProtos.GetAttributeResponseMsg actualResponseMsg = actualGatewayAttributeResponseMsg.getResponseMsg();
            assertEquals(expectedResponseMsg.getRequestId(), actualResponseMsg.getRequestId());

            List<TransportProtos.KeyValueProto> expectedSharedKeyValueProtos = expectedResponseMsg.getSharedAttributeListList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
            List<TransportProtos.KeyValueProto> actualSharedKeyValueProtos = actualResponseMsg.getSharedAttributeListList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
            List<String> actualDeletedKeys = actualResponseMsg.getDeletedAttributeKeysList();

            if (!checkForDeleted) {
                assertTrue(actualSharedKeyValueProtos.containsAll(expectedSharedKeyValueProtos));
            } else {
                assertEquals(4, actualSharedKeyValueProtos.size());
                assertEquals(1, actualDeletedKeys.size());
                assertEquals("attribute5", actualDeletedKeys.get(0));
                assertFalse(actualSharedKeyValueProtos.stream().map(TransportProtos.KeyValueProto::getKey).collect(Collectors.toList()).contains("attribute5"));
                assertTrue(expectedSharedKeyValueProtos.containsAll(actualSharedKeyValueProtos));
            }
        }
    }

    private TransportApiProtos.GatewayAttributesRequestMsg getGatewayAttributesRequestMsg(String deviceName, String keys, boolean client) {
        return TransportApiProtos.GatewayAttributesRequestMsg.newBuilder()
                .setClient(client)
                .addAllKeys(Arrays.asList(keys.split(",")))
                .setDeviceName(deviceName)
                .setId(1).build();
    }

    private TransportProtos.GetAttributeResponseMsg getExpectedAttributeResponseMsg() {
        TransportProtos.GetAttributeResponseMsg.Builder result = TransportProtos.GetAttributeResponseMsg.newBuilder();
        List<TransportProtos.TsKvProto> tsKvProtoList = getTsKvProtoList();
        result.addAllClientAttributeList(tsKvProtoList);
        result.addAllSharedAttributeList(tsKvProtoList);
        result.setRequestId(1);
        return result.build();
    }

    private TransportApiProtos.GatewayAttributeResponseMsg getExpectedGatewayAttributeResponseMsg(String deviceName, boolean client) {
        TransportApiProtos.GatewayAttributeResponseMsg.Builder gatewayAttributeResponseMsg =TransportApiProtos.GatewayAttributeResponseMsg.newBuilder();
        TransportProtos.GetAttributeResponseMsg.Builder getAttributeResponseMsgBuilder = TransportProtos.GetAttributeResponseMsg.newBuilder();
        List<TransportProtos.TsKvProto> tsKvProtoList = getTsKvProtoList();
        if (client) {
            getAttributeResponseMsgBuilder.addAllClientAttributeList(tsKvProtoList);
        } else {
            getAttributeResponseMsgBuilder.addAllSharedAttributeList(tsKvProtoList);
        }
        getAttributeResponseMsgBuilder.setRequestId(1);
        TransportProtos.GetAttributeResponseMsg getAttributeResponseMsg = getAttributeResponseMsgBuilder.build();
        gatewayAttributeResponseMsg.setDeviceName(deviceName);
        gatewayAttributeResponseMsg.setResponseMsg(getAttributeResponseMsg);
        return gatewayAttributeResponseMsg.build();
    }

    private List<TransportProtos.TsKvProto> getTsKvProtoList() {
        TransportProtos.TsKvProto tsKvProtoAttribute1 = getTsKvProto("attribute1", "value1", TransportProtos.KeyValueType.STRING_V);
        TransportProtos.TsKvProto tsKvProtoAttribute2 = getTsKvProto("attribute2", "true", TransportProtos.KeyValueType.BOOLEAN_V);
        TransportProtos.TsKvProto tsKvProtoAttribute3 = getTsKvProto("attribute3", "42.0", TransportProtos.KeyValueType.DOUBLE_V);
        TransportProtos.TsKvProto tsKvProtoAttribute4 = getTsKvProto("attribute4", "73", TransportProtos.KeyValueType.LONG_V);
        TransportProtos.TsKvProto tsKvProtoAttribute5 = getTsKvProto("attribute5", "{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}", TransportProtos.KeyValueType.JSON_V);
        List<TransportProtos.TsKvProto> tsKvProtoList = new ArrayList<>();
        tsKvProtoList.add(tsKvProtoAttribute1);
        tsKvProtoList.add(tsKvProtoAttribute2);
        tsKvProtoList.add(tsKvProtoAttribute3);
        tsKvProtoList.add(tsKvProtoAttribute4);
        tsKvProtoList.add(tsKvProtoAttribute5);
        return tsKvProtoList;
    }

    private TransportProtos.TsKvProto getTsKvProto(String key, String value, TransportProtos.KeyValueType keyValueType) {
        TransportProtos.TsKvProto.Builder tsKvProtoBuilder = TransportProtos.TsKvProto.newBuilder();
        TransportProtos.KeyValueProto keyValueProto = getKeyValueProto(key, value, keyValueType);
        tsKvProtoBuilder.setKv(keyValueProto);
        return tsKvProtoBuilder.build();
    }

    private TransportProtos.PostAttributeMsg getPostAttributeMsg() {
        List<TransportProtos.KeyValueProto> kvProtos = getKvProtos();
        TransportProtos.PostAttributeMsg.Builder builder = TransportProtos.PostAttributeMsg.newBuilder();
        builder.addAllKv(kvProtos);
        return builder.build();
    }

    private List<TransportProtos.KeyValueProto> getKvProtos() {
        List<TransportProtos.KeyValueProto> keyValueProtos = new ArrayList<>();
        TransportProtos.KeyValueProto strKeyValueProto = getKeyValueProto("attribute1", "value1", TransportProtos.KeyValueType.STRING_V);
        TransportProtos.KeyValueProto boolKeyValueProto = getKeyValueProto("attribute2", "true", TransportProtos.KeyValueType.BOOLEAN_V);
        TransportProtos.KeyValueProto dblKeyValueProto = getKeyValueProto("attribute3", "42.0", TransportProtos.KeyValueType.DOUBLE_V);
        TransportProtos.KeyValueProto longKeyValueProto = getKeyValueProto("attribute4", "73", TransportProtos.KeyValueType.LONG_V);
        TransportProtos.KeyValueProto jsonKeyValueProto = getKeyValueProto("attribute5", "{\"someNumber\": 42, \"someArray\": [1,2,3], \"someNestedObject\": {\"key\": \"value\"}}", TransportProtos.KeyValueType.JSON_V
        );
        keyValueProtos.add(strKeyValueProto);
        keyValueProtos.add(boolKeyValueProto);
        keyValueProtos.add(dblKeyValueProto);
        keyValueProtos.add(longKeyValueProto);
        keyValueProtos.add(jsonKeyValueProto);
        return keyValueProtos;
    }

    private TransportProtos.KeyValueProto getKeyValueProto(String key, String strValue, TransportProtos.KeyValueType type) {
        TransportProtos.KeyValueProto.Builder keyValueProtoBuilder = TransportProtos.KeyValueProto.newBuilder();
        keyValueProtoBuilder.setKey(key);
        keyValueProtoBuilder.setType(type);
        switch (type) {
            case BOOLEAN_V:
                keyValueProtoBuilder.setBoolV(Boolean.parseBoolean(strValue));
                break;
            case LONG_V:
                keyValueProtoBuilder.setLongV(Long.parseLong(strValue));
                break;
            case DOUBLE_V:
                keyValueProtoBuilder.setDoubleV(Double.parseDouble(strValue));
                break;
            case STRING_V:
                keyValueProtoBuilder.setStringV(strValue);
                break;
            case JSON_V:
                keyValueProtoBuilder.setJsonV(strValue);
                break;
        }
        return keyValueProtoBuilder.build();
    }


    private Device getSavedDevice(Device device) throws Exception {
        return doPost("/api/device", device, Device.class);
    }

    private DeviceCredentials getDeviceCredentials(Device savedDevice) throws Exception {
        return doGet("/api/device/" + savedDevice.getId().getId().toString() + "/credentials", DeviceCredentials.class);
    }

    private static class TestMqttCallback implements MqttCallback {

        private final CountDownLatch latch;
        private Integer qoS;
        private byte[] payloadBytes;

        TestMqttCallback(CountDownLatch latch) {
            this.latch = latch;
        }

        int getQoS() {
            return qoS;
        }

        byte[] getPayloadBytes() {
            return payloadBytes;
        }

        public CountDownLatch getLatch() { return latch; }

        @Override
        public void connectionLost(Throwable throwable) {
        }

        @Override
        public void messageArrived(String requestTopic, MqttMessage mqttMessage) throws Exception {
            qoS = mqttMessage.getQos();
            payloadBytes = mqttMessage.getPayload();
            latch.countDown();
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

        }
    }
}
