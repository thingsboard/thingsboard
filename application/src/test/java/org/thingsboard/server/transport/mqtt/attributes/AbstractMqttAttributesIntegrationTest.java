/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.transport.mqtt.attributes;

import com.github.os72.protobuf.dynamic.DynamicSchema;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.IMqttDeliveryToken;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.common.data.device.profile.ProtoTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.TransportPayloadTypeConfiguration;
import org.thingsboard.server.gen.transport.TransportApiProtos;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public abstract class AbstractMqttAttributesIntegrationTest extends AbstractMqttIntegrationTest {

    public static final String ATTRIBUTES_SCHEMA_STR = "syntax =\"proto3\";\n" +
            "\n" +
            "package test;\n" +
            "\n" +
            "message PostAttributes {\n" +
            "  string attribute1 = 1;\n" +
            "  bool attribute2 = 2;\n" +
            "  double attribute3 = 3;\n" +
            "  int32 attribute4 = 4;\n" +
            "  JsonObject attribute5 = 5;\n" +
            "\n" +
            "  message JsonObject {\n" +
            "    int32 someNumber = 6;\n" +
            "    repeated int32 someArray = 7;\n" +
            "    NestedJsonObject someNestedObject = 8;\n" +
            "    message NestedJsonObject {\n" +
            "       string key = 9;\n" +
            "    }\n" +
            "  }\n" +
            "}";

    protected static final String POST_ATTRIBUTES_PAYLOAD = "{\"attribute1\":\"value1\",\"attribute2\":true,\"attribute3\":42.0,\"attribute4\":73," +
            "\"attribute5\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}}";

    private static final String RESPONSE_ATTRIBUTES_PAYLOAD_DELETED = "{\"deleted\":[\"attribute5\"]}";

    protected void processBeforeTest(String deviceName, String gatewayName, TransportPayloadType payloadType, String telemetryTopic, String attributesTopic) throws Exception {
        super.processBeforeTest(deviceName, gatewayName, payloadType, telemetryTopic, attributesTopic);
    }

    protected void processAfterTest() throws Exception {
        super.processAfterTest();
    }

    protected List<TransportProtos.TsKvProto> getTsKvProtoList() {
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

    protected TransportProtos.TsKvProto getTsKvProto(String key, String value, TransportProtos.KeyValueType keyValueType) {
        TransportProtos.TsKvProto.Builder tsKvProtoBuilder = TransportProtos.TsKvProto.newBuilder();
        TransportProtos.KeyValueProto keyValueProto = getKeyValueProto(key, value, keyValueType);
        tsKvProtoBuilder.setKv(keyValueProto);
        return tsKvProtoBuilder.build();
    }

    protected TestMqttCallback getTestMqttCallback() {
        CountDownLatch latch = new CountDownLatch(1);
        return new TestMqttCallback(latch);
    }

    protected static class TestMqttCallback implements MqttCallback {

        private final CountDownLatch latch;
        private Integer qoS;
        private byte[] payloadBytes;

        TestMqttCallback(CountDownLatch latch) {
            this.latch = latch;
        }

        public int getQoS() {
            return qoS;
        }

        public byte[] getPayloadBytes() {
            return payloadBytes;
        }

        public CountDownLatch getLatch() {
            return latch;
        }

        @Override
        public void disconnected(MqttDisconnectResponse disconnectResponse) {

        }

        @Override
        public void mqttErrorOccurred(MqttException exception) {

        }

        @Override
        public void messageArrived(String requestTopic, MqttMessage mqttMessage) throws Exception {
            qoS = mqttMessage.getQos();
            payloadBytes = mqttMessage.getPayload();
            latch.countDown();
        }

        @Override
        public void deliveryComplete(IMqttToken token) {

        }

        @Override
        public void connectComplete(boolean reconnect, String serverURI) {

        }

        @Override
        public void authPacketArrived(int reasonCode, MqttProperties properties) {

        }
    }

    // subscribe to attributes updates from server methods

    protected void processJsonTestSubscribeToAttributesUpdates(String attrSubTopic) throws Exception {

        MqttAsyncClient client = getMqttAsyncClient(accessToken);

        TestMqttCallback onUpdateCallback = getTestMqttCallback();
        client.setCallback(onUpdateCallback);

        client.subscribe(attrSubTopic, MqttQoS.AT_MOST_ONCE.value());

        Thread.sleep(1000);

        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", POST_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        onUpdateCallback.getLatch().await(3, TimeUnit.SECONDS);

        validateUpdateAttributesJsonResponse(onUpdateCallback);

        TestMqttCallback onDeleteCallback = getTestMqttCallback();
        client.setCallback(onDeleteCallback);

        doDelete("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/SHARED_SCOPE?keys=attribute5", String.class);
        onDeleteCallback.getLatch().await(3, TimeUnit.SECONDS);

        validateDeleteAttributesJsonResponse(onDeleteCallback);
    }

    protected void processProtoTestSubscribeToAttributesUpdates(String attrSubTopic) throws Exception {

        MqttAsyncClient client = getMqttAsyncClient(accessToken);

        TestMqttCallback onUpdateCallback = getTestMqttCallback();
        client.setCallback(onUpdateCallback);

        client.subscribe(attrSubTopic, MqttQoS.AT_MOST_ONCE.value());

        Thread.sleep(1000);

        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", POST_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        onUpdateCallback.getLatch().await(3, TimeUnit.SECONDS);

        validateUpdateAttributesProtoResponse(onUpdateCallback);

        TestMqttCallback onDeleteCallback = getTestMqttCallback();
        client.setCallback(onDeleteCallback);

        doDelete("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/SHARED_SCOPE?keys=attribute5", String.class);
        onDeleteCallback.getLatch().await(3, TimeUnit.SECONDS);

        validateDeleteAttributesProtoResponse(onDeleteCallback);
    }

    protected void validateUpdateAttributesJsonResponse(TestMqttCallback callback) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        String response = new String(callback.getPayloadBytes(), StandardCharsets.UTF_8);
        assertEquals(JacksonUtil.toJsonNode(POST_ATTRIBUTES_PAYLOAD), JacksonUtil.toJsonNode(response));
    }

    protected void validateDeleteAttributesJsonResponse(TestMqttCallback callback) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        String response = new String(callback.getPayloadBytes(), StandardCharsets.UTF_8);
        assertEquals(JacksonUtil.toJsonNode(RESPONSE_ATTRIBUTES_PAYLOAD_DELETED), JacksonUtil.toJsonNode(response));
    }

    protected void validateUpdateAttributesProtoResponse(TestMqttCallback callback) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        TransportProtos.AttributeUpdateNotificationMsg.Builder attributeUpdateNotificationMsgBuilder = TransportProtos.AttributeUpdateNotificationMsg.newBuilder();
        List<TransportProtos.TsKvProto> tsKvProtoList = getTsKvProtoList();
        attributeUpdateNotificationMsgBuilder.addAllSharedUpdated(tsKvProtoList);

        TransportProtos.AttributeUpdateNotificationMsg expectedAttributeUpdateNotificationMsg = attributeUpdateNotificationMsgBuilder.build();
        TransportProtos.AttributeUpdateNotificationMsg actualAttributeUpdateNotificationMsg = TransportProtos.AttributeUpdateNotificationMsg.parseFrom(callback.getPayloadBytes());

        List<TransportProtos.KeyValueProto> actualSharedUpdatedList = actualAttributeUpdateNotificationMsg.getSharedUpdatedList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
        List<TransportProtos.KeyValueProto> expectedSharedUpdatedList = expectedAttributeUpdateNotificationMsg.getSharedUpdatedList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());

        assertEquals(expectedSharedUpdatedList.size(), actualSharedUpdatedList.size());
        assertTrue(actualSharedUpdatedList.containsAll(expectedSharedUpdatedList));
    }

    protected void validateDeleteAttributesProtoResponse(TestMqttCallback callback) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        TransportProtos.AttributeUpdateNotificationMsg.Builder attributeUpdateNotificationMsgBuilder = TransportProtos.AttributeUpdateNotificationMsg.newBuilder();
        attributeUpdateNotificationMsgBuilder.addSharedDeleted("attribute5");

        TransportProtos.AttributeUpdateNotificationMsg expectedAttributeUpdateNotificationMsg = attributeUpdateNotificationMsgBuilder.build();
        TransportProtos.AttributeUpdateNotificationMsg actualAttributeUpdateNotificationMsg = TransportProtos.AttributeUpdateNotificationMsg.parseFrom(callback.getPayloadBytes());

        assertEquals(expectedAttributeUpdateNotificationMsg.getSharedDeletedList().size(), actualAttributeUpdateNotificationMsg.getSharedDeletedList().size());
        assertEquals("attribute5", actualAttributeUpdateNotificationMsg.getSharedDeletedList().get(0));
    }

    protected void processJsonGatewayTestSubscribeToAttributesUpdates() throws Exception {

        MqttAsyncClient client = getMqttAsyncClient(gatewayAccessToken);

        TestMqttCallback onUpdateCallback = getTestMqttCallback();
        client.setCallback(onUpdateCallback);

        Device device = new Device();
        device.setName("Gateway Device Subscribe to attribute updates");
        device.setType("default");

        byte[] connectPayloadBytes = getJsonConnectPayloadBytes();

        publishMqttMsg(client, connectPayloadBytes, MqttTopics.GATEWAY_CONNECT_TOPIC);

        Device savedDevice = doExecuteWithRetriesAndInterval(() -> doGet("/api/tenant/devices?deviceName=" + "Gateway Device Subscribe to attribute updates", Device.class),
                20,
                100);

        assertNotNull(savedDevice);

        client.subscribe(MqttTopics.GATEWAY_ATTRIBUTES_TOPIC, MqttQoS.AT_MOST_ONCE.value());

        Thread.sleep(1000);

        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", POST_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        onUpdateCallback.getLatch().await(3, TimeUnit.SECONDS);

        validateJsonGatewayUpdateAttributesResponse(onUpdateCallback);

        TestMqttCallback onDeleteCallback = getTestMqttCallback();
        client.setCallback(onDeleteCallback);

        doDelete("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/SHARED_SCOPE?keys=attribute5", String.class);
        onDeleteCallback.getLatch().await(3, TimeUnit.SECONDS);

        validateJsonGatewayDeleteAttributesResponse(onDeleteCallback);

    }

    protected void processProtoGatewayTestSubscribeToAttributesUpdates() throws Exception {

        MqttAsyncClient client = getMqttAsyncClient(gatewayAccessToken);

        TestMqttCallback onUpdateCallback = getTestMqttCallback();
        client.setCallback(onUpdateCallback);

        Device device = new Device();
        device.setName("Gateway Device Subscribe to attribute updates");
        device.setType("default");

        byte[] connectPayloadBytes = getProtoConnectPayloadBytes();

        publishMqttMsg(client, connectPayloadBytes, MqttTopics.GATEWAY_CONNECT_TOPIC);

        Device savedDevice = doExecuteWithRetriesAndInterval(() -> doGet("/api/tenant/devices?deviceName=" + "Gateway Device Subscribe to attribute updates", Device.class),
                20,
                100);

        assertNotNull(savedDevice);

        client.subscribe(MqttTopics.GATEWAY_ATTRIBUTES_TOPIC, MqttQoS.AT_MOST_ONCE.value());

        Thread.sleep(1000);

        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", POST_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        onUpdateCallback.getLatch().await(3, TimeUnit.SECONDS);

        validateProtoGatewayUpdateAttributesResponse(onUpdateCallback);

        TestMqttCallback onDeleteCallback = getTestMqttCallback();
        client.setCallback(onDeleteCallback);

        doDelete("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/SHARED_SCOPE?keys=attribute5", String.class);
        onDeleteCallback.getLatch().await(3, TimeUnit.SECONDS);

        validateProtoGatewayDeleteAttributesResponse(onDeleteCallback);

    }

    protected void validateJsonGatewayUpdateAttributesResponse(TestMqttCallback callback) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        String s = new String(callback.getPayloadBytes(), StandardCharsets.UTF_8);
        assertEquals(getJsonResponseGatewayAttributesUpdatedPayload(), s);
    }

    protected void validateJsonGatewayDeleteAttributesResponse(TestMqttCallback callback) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        String s = new String(callback.getPayloadBytes(), StandardCharsets.UTF_8);
        assertEquals(s, getJsonResponseGatewayAttributesDeletedPayload());
    }

    protected byte[] getJsonConnectPayloadBytes() {
        String connectPayload = "{\"device\": \"Gateway Device Subscribe to attribute updates\", \"type\": \"" + TransportPayloadType.JSON.name() + "\"}";
        return connectPayload.getBytes();
    }

    private static String getJsonResponseGatewayAttributesUpdatedPayload() {
        return "{\"device\":\"" + "Gateway Device Subscribe to attribute updates" + "\"," +
                "\"data\":{\"attribute1\":\"value1\",\"attribute2\":true,\"attribute3\":42.0,\"attribute4\":73,\"attribute5\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}}}";
    }

    private static String getJsonResponseGatewayAttributesDeletedPayload() {
        return "{\"device\":\"" + "Gateway Device Subscribe to attribute updates" + "\",\"data\":{\"deleted\":[\"attribute5\"]}}";
    }

    protected void validateProtoGatewayUpdateAttributesResponse(TestMqttCallback callback) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());

        TransportProtos.AttributeUpdateNotificationMsg.Builder attributeUpdateNotificationMsgBuilder = TransportProtos.AttributeUpdateNotificationMsg.newBuilder();
        List<TransportProtos.TsKvProto> tsKvProtoList = getTsKvProtoList();
        attributeUpdateNotificationMsgBuilder.addAllSharedUpdated(tsKvProtoList);
        TransportProtos.AttributeUpdateNotificationMsg expectedAttributeUpdateNotificationMsg = attributeUpdateNotificationMsgBuilder.build();

        TransportApiProtos.GatewayAttributeUpdateNotificationMsg.Builder gatewayAttributeUpdateNotificationMsgBuilder = TransportApiProtos.GatewayAttributeUpdateNotificationMsg.newBuilder();
        gatewayAttributeUpdateNotificationMsgBuilder.setDeviceName("Gateway Device Subscribe to attribute updates");
        gatewayAttributeUpdateNotificationMsgBuilder.setNotificationMsg(expectedAttributeUpdateNotificationMsg);

        TransportApiProtos.GatewayAttributeUpdateNotificationMsg expectedGatewayAttributeUpdateNotificationMsg = gatewayAttributeUpdateNotificationMsgBuilder.build();
        TransportApiProtos.GatewayAttributeUpdateNotificationMsg actualGatewayAttributeUpdateNotificationMsg = TransportApiProtos.GatewayAttributeUpdateNotificationMsg.parseFrom(callback.getPayloadBytes());

        assertEquals(expectedGatewayAttributeUpdateNotificationMsg.getDeviceName(), actualGatewayAttributeUpdateNotificationMsg.getDeviceName());

        List<TransportProtos.KeyValueProto> actualSharedUpdatedList = actualGatewayAttributeUpdateNotificationMsg.getNotificationMsg().getSharedUpdatedList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
        List<TransportProtos.KeyValueProto> expectedSharedUpdatedList = expectedGatewayAttributeUpdateNotificationMsg.getNotificationMsg().getSharedUpdatedList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());

        assertEquals(expectedSharedUpdatedList.size(), actualSharedUpdatedList.size());
        assertTrue(actualSharedUpdatedList.containsAll(expectedSharedUpdatedList));

    }

    protected void validateProtoGatewayDeleteAttributesResponse(TestMqttCallback callback) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        TransportProtos.AttributeUpdateNotificationMsg.Builder attributeUpdateNotificationMsgBuilder = TransportProtos.AttributeUpdateNotificationMsg.newBuilder();
        attributeUpdateNotificationMsgBuilder.addSharedDeleted("attribute5");
        TransportProtos.AttributeUpdateNotificationMsg attributeUpdateNotificationMsg = attributeUpdateNotificationMsgBuilder.build();

        TransportApiProtos.GatewayAttributeUpdateNotificationMsg.Builder gatewayAttributeUpdateNotificationMsgBuilder = TransportApiProtos.GatewayAttributeUpdateNotificationMsg.newBuilder();
        gatewayAttributeUpdateNotificationMsgBuilder.setDeviceName("Gateway Device Subscribe to attribute updates");
        gatewayAttributeUpdateNotificationMsgBuilder.setNotificationMsg(attributeUpdateNotificationMsg);

        TransportApiProtos.GatewayAttributeUpdateNotificationMsg expectedGatewayAttributeUpdateNotificationMsg = gatewayAttributeUpdateNotificationMsgBuilder.build();
        TransportApiProtos.GatewayAttributeUpdateNotificationMsg actualGatewayAttributeUpdateNotificationMsg = TransportApiProtos.GatewayAttributeUpdateNotificationMsg.parseFrom(callback.getPayloadBytes());

        assertEquals(expectedGatewayAttributeUpdateNotificationMsg.getDeviceName(), actualGatewayAttributeUpdateNotificationMsg.getDeviceName());

        TransportProtos.AttributeUpdateNotificationMsg expectedAttributeUpdateNotificationMsg = expectedGatewayAttributeUpdateNotificationMsg.getNotificationMsg();
        TransportProtos.AttributeUpdateNotificationMsg actualAttributeUpdateNotificationMsg = actualGatewayAttributeUpdateNotificationMsg.getNotificationMsg();

        assertEquals(expectedAttributeUpdateNotificationMsg.getSharedDeletedList().size(), actualAttributeUpdateNotificationMsg.getSharedDeletedList().size());
        assertEquals("attribute5", actualAttributeUpdateNotificationMsg.getSharedDeletedList().get(0));

    }

    protected byte[] getProtoConnectPayloadBytes() {
        TransportApiProtos.ConnectMsg connectProto = getConnectProto();
        return connectProto.toByteArray();
    }

    private TransportApiProtos.ConnectMsg getConnectProto() {
        TransportApiProtos.ConnectMsg.Builder builder = TransportApiProtos.ConnectMsg.newBuilder();
        builder.setDeviceName("Gateway Device Subscribe to attribute updates");
        builder.setDeviceType(TransportPayloadType.PROTOBUF.name());
        return builder.build();
    }

    // request attributes from server methods

    protected void processJsonTestRequestAttributesValuesFromTheServer(String attrPubTopic, String attrSubTopic, String attrReqTopicPrefix) throws Exception {

        MqttAsyncClient client = getMqttAsyncClient(accessToken);

        postJsonAttributesAndSubscribeToTopic(savedDevice, client, attrPubTopic, attrSubTopic);

        Thread.sleep(5000);

        TestMqttCallback callback = getTestMqttCallback();
        client.setCallback(callback);

        validateJsonResponse(client, callback.getLatch(), callback, attrReqTopicPrefix);
    }

    protected void processProtoTestRequestAttributesValuesFromTheServer(String attrPubTopic, String attrSubTopic, String attrReqTopicPrefix) throws Exception {

        MqttAsyncClient client = getMqttAsyncClient(accessToken);

        postProtoAttributesAndSubscribeToTopic(savedDevice, client, attrPubTopic, attrSubTopic);

        Thread.sleep(5000);

        TestMqttCallback callback = getTestMqttCallback();
        client.setCallback(callback);

        validateProtoResponse(client, callback.getLatch(), callback, attrReqTopicPrefix);
    }

    protected void processJsonTestGatewayRequestAttributesValuesFromTheServer() throws Exception {

        MqttAsyncClient client = getMqttAsyncClient(gatewayAccessToken);

        postJsonGatewayDeviceClientAttributes(client);

        Device savedDevice = doExecuteWithRetriesAndInterval(() -> doGet("/api/tenant/devices?deviceName=" + "Gateway Device Request Attributes", Device.class),
                20,
                100);

        assertNotNull(savedDevice);

        Thread.sleep(2000);

        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", POST_ATTRIBUTES_PAYLOAD, String.class, status().isOk());

        Thread.sleep(5000);

        client.subscribe(MqttTopics.GATEWAY_ATTRIBUTES_RESPONSE_TOPIC, MqttQoS.AT_LEAST_ONCE.value()).waitForCompletion(TimeUnit.MINUTES.toMillis(1));

        TestMqttCallback clientAttributesCallback = getTestMqttCallback();
        client.setCallback(clientAttributesCallback);
        validateJsonClientResponseGateway(client, clientAttributesCallback);

        TestMqttCallback sharedAttributesCallback = getTestMqttCallback();
        client.setCallback(sharedAttributesCallback);
        validateJsonSharedResponseGateway(client, sharedAttributesCallback);
    }

    protected void processProtoTestGatewayRequestAttributesValuesFromTheServer() throws Exception {

        MqttAsyncClient client = getMqttAsyncClient(gatewayAccessToken);

        postProtoGatewayDeviceClientAttributes(client);

        Device savedDevice = doExecuteWithRetriesAndInterval(() -> doGet("/api/tenant/devices?deviceName=" + "Gateway Device Request Attributes", Device.class),
                20,
                100);

        assertNotNull(savedDevice);

        Thread.sleep(2000);

        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", POST_ATTRIBUTES_PAYLOAD, String.class, status().isOk());

        Thread.sleep(5000);

        client.subscribe(MqttTopics.GATEWAY_ATTRIBUTES_RESPONSE_TOPIC, MqttQoS.AT_LEAST_ONCE.value()).waitForCompletion(TimeUnit.MINUTES.toMillis(1));

        TestMqttCallback clientAttributesCallback = getTestMqttCallback();
        client.setCallback(clientAttributesCallback);
        validateProtoClientResponseGateway(client, clientAttributesCallback);

        TestMqttCallback sharedAttributesCallback = getTestMqttCallback();
        client.setCallback(sharedAttributesCallback);
        validateProtoSharedResponseGateway(client, sharedAttributesCallback);
    }

    protected void postJsonAttributesAndSubscribeToTopic(Device savedDevice, MqttAsyncClient client, String attrPubTopic, String attrSubTopic) throws Exception {
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", POST_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        client.publish(attrPubTopic, new MqttMessage(POST_ATTRIBUTES_PAYLOAD.getBytes())).waitForCompletion(TimeUnit.MINUTES.toMillis(1));
        client.subscribe(attrSubTopic, MqttQoS.AT_MOST_ONCE.value()).waitForCompletion(TimeUnit.MINUTES.toMillis(1));
    }

    protected void postProtoAttributesAndSubscribeToTopic(Device savedDevice, MqttAsyncClient client, String attrPubTopic, String attrSubTopic) throws Exception {
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", AbstractMqttAttributesIntegrationTest.POST_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        DeviceProfileTransportConfiguration transportConfiguration = deviceProfile.getProfileData().getTransportConfiguration();
        assertTrue(transportConfiguration instanceof MqttDeviceProfileTransportConfiguration);
        MqttDeviceProfileTransportConfiguration mqttTransportConfiguration = (MqttDeviceProfileTransportConfiguration) transportConfiguration;
        TransportPayloadTypeConfiguration transportPayloadTypeConfiguration = mqttTransportConfiguration.getTransportPayloadTypeConfiguration();
        assertTrue(transportPayloadTypeConfiguration instanceof ProtoTransportPayloadConfiguration);
        ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration = (ProtoTransportPayloadConfiguration) transportPayloadTypeConfiguration;
        ProtoFileElement transportProtoSchema = protoTransportPayloadConfiguration.getTransportProtoSchema(ATTRIBUTES_SCHEMA_STR);
        DynamicSchema attributesSchema = protoTransportPayloadConfiguration.getDynamicSchema(transportProtoSchema, ProtoTransportPayloadConfiguration.ATTRIBUTES_PROTO_SCHEMA);

        DynamicMessage.Builder nestedJsonObjectBuilder = attributesSchema.newMessageBuilder("PostAttributes.JsonObject.NestedJsonObject");
        Descriptors.Descriptor nestedJsonObjectBuilderDescriptor = nestedJsonObjectBuilder.getDescriptorForType();
        assertNotNull(nestedJsonObjectBuilderDescriptor);
        DynamicMessage nestedJsonObject = nestedJsonObjectBuilder.setField(nestedJsonObjectBuilderDescriptor.findFieldByName("key"), "value").build();

        DynamicMessage.Builder jsonObjectBuilder = attributesSchema.newMessageBuilder("PostAttributes.JsonObject");
        Descriptors.Descriptor jsonObjectBuilderDescriptor = jsonObjectBuilder.getDescriptorForType();
        assertNotNull(jsonObjectBuilderDescriptor);
        DynamicMessage jsonObject = jsonObjectBuilder
                .setField(jsonObjectBuilderDescriptor.findFieldByName("someNumber"), 42)
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 1)
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 2)
                .addRepeatedField(jsonObjectBuilderDescriptor.findFieldByName("someArray"), 3)
                .setField(jsonObjectBuilderDescriptor.findFieldByName("someNestedObject"), nestedJsonObject)
                .build();

        DynamicMessage.Builder postAttributesBuilder = attributesSchema.newMessageBuilder("PostAttributes");
        Descriptors.Descriptor postAttributesMsgDescriptor = postAttributesBuilder.getDescriptorForType();
        assertNotNull(postAttributesMsgDescriptor);
        DynamicMessage postAttributesMsg = postAttributesBuilder
                .setField(postAttributesMsgDescriptor.findFieldByName("attribute1"), "value1")
                .setField(postAttributesMsgDescriptor.findFieldByName("attribute2"), true)
                .setField(postAttributesMsgDescriptor.findFieldByName("attribute3"), 42.0)
                .setField(postAttributesMsgDescriptor.findFieldByName("attribute4"), 73)
                .setField(postAttributesMsgDescriptor.findFieldByName("attribute5"), jsonObject)
                .build();
        byte[] payload = postAttributesMsg.toByteArray();
        client.publish(attrPubTopic, new MqttMessage(payload));
        client.subscribe(attrSubTopic, MqttQoS.AT_MOST_ONCE.value());
    }

    protected void postJsonGatewayDeviceClientAttributes(MqttAsyncClient client) throws Exception {
        String postClientAttributes = "{\"" + "Gateway Device Request Attributes" + "\":{\"attribute1\":\"value1\",\"attribute2\":true,\"attribute3\":42.0,\"attribute4\":73,\"attribute5\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}}}";
        client.publish(MqttTopics.GATEWAY_ATTRIBUTES_TOPIC, new MqttMessage(postClientAttributes.getBytes())).waitForCompletion(TimeUnit.MINUTES.toMillis(1));
    }

    protected void postProtoGatewayDeviceClientAttributes(MqttAsyncClient client) throws Exception {
        String keys = "attribute1,attribute2,attribute3,attribute4,attribute5";
        List<String> expectedKeys = Arrays.asList(keys.split(","));
        TransportProtos.PostAttributeMsg postAttributeMsg = getPostAttributeMsg(expectedKeys);
        TransportApiProtos.AttributesMsg.Builder attributesMsgBuilder = TransportApiProtos.AttributesMsg.newBuilder();
        attributesMsgBuilder.setDeviceName("Gateway Device Request Attributes");
        attributesMsgBuilder.setMsg(postAttributeMsg);
        TransportApiProtos.AttributesMsg attributesMsg = attributesMsgBuilder.build();
        TransportApiProtos.GatewayAttributesMsg.Builder gatewayAttributeMsgBuilder = TransportApiProtos.GatewayAttributesMsg.newBuilder();
        gatewayAttributeMsgBuilder.addMsg(attributesMsg);
        byte[] bytes = gatewayAttributeMsgBuilder.build().toByteArray();
        client.publish(MqttTopics.GATEWAY_ATTRIBUTES_TOPIC, new MqttMessage(bytes));
    }

    protected void validateJsonResponse(MqttAsyncClient client, CountDownLatch latch, TestMqttCallback callback, String attrReqTopicPrefix) throws MqttException, InterruptedException, InvalidProtocolBufferException {
        String keys = "attribute1,attribute2,attribute3,attribute4,attribute5";
        String payloadStr = "{\"clientKeys\":\"" + keys + "\", \"sharedKeys\":\"" + keys + "\"}";
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setPayload(payloadStr.getBytes());
        client.publish(attrReqTopicPrefix + "1", mqttMessage).waitForCompletion(TimeUnit.MINUTES.toMillis(1));
        latch.await(1, TimeUnit.MINUTES);
        assertEquals(MqttQoS.AT_MOST_ONCE.value(), callback.getQoS());
        String expectedRequestPayload = "{\"client\":{\"attribute1\":\"value1\",\"attribute2\":true,\"attribute3\":42.0,\"attribute4\":73,\"attribute5\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}},\"shared\":{\"attribute1\":\"value1\",\"attribute2\":true,\"attribute3\":42.0,\"attribute4\":73,\"attribute5\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}}}";
        assertEquals(JacksonUtil.toJsonNode(expectedRequestPayload), JacksonUtil.toJsonNode(new String(callback.getPayloadBytes(), StandardCharsets.UTF_8)));
    }

    protected void validateProtoResponse(MqttAsyncClient client, CountDownLatch latch, TestMqttCallback callback, String attrReqTopic) throws MqttException, InterruptedException, InvalidProtocolBufferException {
        String keys = "attribute1,attribute2,attribute3,attribute4,attribute5";
        TransportApiProtos.AttributesRequest.Builder attributesRequestBuilder = TransportApiProtos.AttributesRequest.newBuilder();
        attributesRequestBuilder.setClientKeys(keys);
        attributesRequestBuilder.setSharedKeys(keys);
        TransportApiProtos.AttributesRequest attributesRequest = attributesRequestBuilder.build();
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setPayload(attributesRequest.toByteArray());
        client.publish(attrReqTopic + "1", mqttMessage);
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

    private TransportProtos.GetAttributeResponseMsg getExpectedAttributeResponseMsg() {
        TransportProtos.GetAttributeResponseMsg.Builder result = TransportProtos.GetAttributeResponseMsg.newBuilder();
        List<TransportProtos.TsKvProto> tsKvProtoList = getTsKvProtoList();
        result.addAllClientAttributeList(tsKvProtoList);
        result.addAllSharedAttributeList(tsKvProtoList);
        result.setRequestId(1);
        return result.build();
    }

    protected void validateJsonClientResponseGateway(MqttAsyncClient client, TestMqttCallback callback) throws MqttException, InterruptedException, InvalidProtocolBufferException {
        String payloadStr = "{\"id\": 1, \"device\": \"" + "Gateway Device Request Attributes" + "\", \"client\": true, \"keys\": [\"attribute1\", \"attribute2\", \"attribute3\", \"attribute4\", \"attribute5\"]}";
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setPayload(payloadStr.getBytes());
        client.publish(MqttTopics.GATEWAY_ATTRIBUTES_REQUEST_TOPIC, mqttMessage).waitForCompletion(TimeUnit.MINUTES.toMillis(1));
        callback.getLatch().await(1, TimeUnit.MINUTES);
        assertEquals(MqttQoS.AT_LEAST_ONCE.value(), callback.getQoS());
        String expectedRequestPayload = "{\"id\":1,\"device\":\"" + "Gateway Device Request Attributes" + "\",\"values\":{\"attribute1\":\"value1\",\"attribute2\":true,\"attribute3\":42.0,\"attribute4\":73,\"attribute5\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}}}";
        assertEquals(JacksonUtil.toJsonNode(expectedRequestPayload), JacksonUtil.toJsonNode(new String(callback.getPayloadBytes(), StandardCharsets.UTF_8)));
    }

    protected void validateJsonSharedResponseGateway(MqttAsyncClient client, TestMqttCallback callback) throws MqttException, InterruptedException, InvalidProtocolBufferException {
        String payloadStr = "{\"id\": 1, \"device\": \"" + "Gateway Device Request Attributes" + "\", \"client\": false, \"keys\": [\"attribute1\", \"attribute2\", \"attribute3\", \"attribute4\", \"attribute5\"]}";
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setPayload(payloadStr.getBytes());
        client.publish(MqttTopics.GATEWAY_ATTRIBUTES_REQUEST_TOPIC, mqttMessage).waitForCompletion(TimeUnit.MINUTES.toMillis(1));
        callback.getLatch().await(1, TimeUnit.MINUTES);
        assertEquals(MqttQoS.AT_LEAST_ONCE.value(), callback.getQoS());
        String expectedRequestPayload = "{\"id\":1,\"device\":\"" + "Gateway Device Request Attributes" + "\",\"values\":{\"attribute1\":\"value1\",\"attribute2\":true,\"attribute3\":42.0,\"attribute4\":73,\"attribute5\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}}}";
        assertEquals(JacksonUtil.toJsonNode(expectedRequestPayload), JacksonUtil.toJsonNode(new String(callback.getPayloadBytes(), StandardCharsets.UTF_8)));
    }

    protected void validateProtoClientResponseGateway(MqttAsyncClient client, AbstractMqttAttributesIntegrationTest.TestMqttCallback callback) throws MqttException, InterruptedException, InvalidProtocolBufferException {
        String keys = "attribute1,attribute2,attribute3,attribute4,attribute5";
        TransportApiProtos.GatewayAttributesRequestMsg gatewayAttributesRequestMsg = getGatewayAttributesRequestMsg(keys, true);
        client.publish(MqttTopics.GATEWAY_ATTRIBUTES_REQUEST_TOPIC, new MqttMessage(gatewayAttributesRequestMsg.toByteArray()));
        callback.getLatch().await(3, TimeUnit.SECONDS);
        assertEquals(MqttQoS.AT_LEAST_ONCE.value(), callback.getQoS());
        TransportApiProtos.GatewayAttributeResponseMsg expectedGatewayAttributeResponseMsg = getExpectedGatewayAttributeResponseMsg(true);
        TransportApiProtos.GatewayAttributeResponseMsg actualGatewayAttributeResponseMsg = TransportApiProtos.GatewayAttributeResponseMsg.parseFrom(callback.getPayloadBytes());
        assertEquals(expectedGatewayAttributeResponseMsg.getDeviceName(), actualGatewayAttributeResponseMsg.getDeviceName());

        TransportProtos.GetAttributeResponseMsg expectedResponseMsg = expectedGatewayAttributeResponseMsg.getResponseMsg();
        TransportProtos.GetAttributeResponseMsg actualResponseMsg = actualGatewayAttributeResponseMsg.getResponseMsg();
        assertEquals(expectedResponseMsg.getRequestId(), actualResponseMsg.getRequestId());

        List<TransportProtos.KeyValueProto> expectedClientKeyValueProtos = expectedResponseMsg.getClientAttributeListList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
        List<TransportProtos.KeyValueProto> actualClientKeyValueProtos = actualResponseMsg.getClientAttributeListList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
        assertTrue(actualClientKeyValueProtos.containsAll(expectedClientKeyValueProtos));
    }

    protected void validateProtoSharedResponseGateway(MqttAsyncClient client, AbstractMqttAttributesIntegrationTest.TestMqttCallback callback) throws MqttException, InterruptedException, InvalidProtocolBufferException {
        String keys = "attribute1,attribute2,attribute3,attribute4,attribute5";
        TransportApiProtos.GatewayAttributesRequestMsg gatewayAttributesRequestMsg = getGatewayAttributesRequestMsg(keys, false);
        client.publish(MqttTopics.GATEWAY_ATTRIBUTES_REQUEST_TOPIC, new MqttMessage(gatewayAttributesRequestMsg.toByteArray()));
        callback.getLatch().await(3, TimeUnit.SECONDS);
        assertEquals(MqttQoS.AT_LEAST_ONCE.value(), callback.getQoS());
        TransportApiProtos.GatewayAttributeResponseMsg expectedGatewayAttributeResponseMsg = getExpectedGatewayAttributeResponseMsg(false);
        TransportApiProtos.GatewayAttributeResponseMsg actualGatewayAttributeResponseMsg = TransportApiProtos.GatewayAttributeResponseMsg.parseFrom(callback.getPayloadBytes());
        assertEquals(expectedGatewayAttributeResponseMsg.getDeviceName(), actualGatewayAttributeResponseMsg.getDeviceName());

        TransportProtos.GetAttributeResponseMsg expectedResponseMsg = expectedGatewayAttributeResponseMsg.getResponseMsg();
        TransportProtos.GetAttributeResponseMsg actualResponseMsg = actualGatewayAttributeResponseMsg.getResponseMsg();
        assertEquals(expectedResponseMsg.getRequestId(), actualResponseMsg.getRequestId());

        List<TransportProtos.KeyValueProto> expectedSharedKeyValueProtos = expectedResponseMsg.getSharedAttributeListList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
        List<TransportProtos.KeyValueProto> actualSharedKeyValueProtos = actualResponseMsg.getSharedAttributeListList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());

        assertTrue(actualSharedKeyValueProtos.containsAll(expectedSharedKeyValueProtos));
    }

    private TransportApiProtos.GatewayAttributeResponseMsg getExpectedGatewayAttributeResponseMsg(boolean client) {
        TransportApiProtos.GatewayAttributeResponseMsg.Builder gatewayAttributeResponseMsg = TransportApiProtos.GatewayAttributeResponseMsg.newBuilder();
        TransportProtos.GetAttributeResponseMsg.Builder getAttributeResponseMsgBuilder = TransportProtos.GetAttributeResponseMsg.newBuilder();
        List<TransportProtos.TsKvProto> tsKvProtoList = getTsKvProtoList();
        if (client) {
            getAttributeResponseMsgBuilder.addAllClientAttributeList(tsKvProtoList);
        } else {
            getAttributeResponseMsgBuilder.addAllSharedAttributeList(tsKvProtoList);
        }
        getAttributeResponseMsgBuilder.setRequestId(1);
        TransportProtos.GetAttributeResponseMsg getAttributeResponseMsg = getAttributeResponseMsgBuilder.build();
        gatewayAttributeResponseMsg.setDeviceName("Gateway Device Request Attributes");
        gatewayAttributeResponseMsg.setResponseMsg(getAttributeResponseMsg);
        return gatewayAttributeResponseMsg.build();
    }

    private TransportApiProtos.GatewayAttributesRequestMsg getGatewayAttributesRequestMsg(String keys, boolean client) {
        return TransportApiProtos.GatewayAttributesRequestMsg.newBuilder()
                .setClient(client)
                .addAllKeys(Arrays.asList(keys.split(",")))
                .setDeviceName("Gateway Device Request Attributes")
                .setId(1).build();
    }
}
