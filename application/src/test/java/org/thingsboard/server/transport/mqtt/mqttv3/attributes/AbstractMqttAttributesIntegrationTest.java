/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.transport.mqtt.mqttv3.attributes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.os72.protobuf.dynamic.DynamicSchema;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DynamicProtoUtils;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.ProtoTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.TransportPayloadTypeConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AliasEntityId;
import org.thingsboard.server.common.data.query.DeviceTypeFilter;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.gen.transport.TransportApiProtos;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityDataUpdate;
import org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestCallback;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestClient;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestSubscribeOnTopicCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.GATEWAY_ATTRIBUTES_REQUEST_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.GATEWAY_ATTRIBUTES_RESPONSE_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.GATEWAY_ATTRIBUTES_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.GATEWAY_CONNECT_TOPIC;
import static org.thingsboard.server.common.data.query.EntityKeyType.CLIENT_ATTRIBUTE;
import static org.thingsboard.server.common.data.query.EntityKeyType.SHARED_ATTRIBUTE;

@Slf4j
public abstract class AbstractMqttAttributesIntegrationTest extends AbstractMqttIntegrationTest {

    public static final String ATTRIBUTES_SCHEMA_STR = "syntax =\"proto3\";\n" +
            "\n" +
            "package test;\n" +
            "\n" +
            "message PostAttributes {\n" +
            "  string clientStr = 1;\n" +
            "  bool clientBool = 2;\n" +
            "  double clientDbl = 3;\n" +
            "  int32 clientLong = 4;\n" +
            "  JsonObject clientJson = 5;\n" +
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

    private static final String CLIENT_ATTRIBUTES_PAYLOAD = "{\"clientStr\":\"value1\",\"clientBool\":true,\"clientDbl\":42.0,\"clientLong\":73," +
            "\"clientJson\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}}";

    private static final String SHARED_ATTRIBUTES_PAYLOAD = "{\"sharedStr\":\"value1\",\"sharedBool\":true,\"sharedDbl\":42.0,\"sharedLong\":73," +
            "\"sharedJson\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}}";

    private static final String SHARED_ATTRIBUTES_DELETED_RESPONSE = "{\"deleted\":[\"sharedJson\"]}";

    private List<TransportProtos.TsKvProto> getTsKvProtoList(String attributePrefix) {
        TransportProtos.TsKvProto tsKvProtoAttribute1 = getTsKvProto(attributePrefix + "Str", "value1", TransportProtos.KeyValueType.STRING_V);
        TransportProtos.TsKvProto tsKvProtoAttribute2 = getTsKvProto(attributePrefix + "Bool", "true", TransportProtos.KeyValueType.BOOLEAN_V);
        TransportProtos.TsKvProto tsKvProtoAttribute3 = getTsKvProto(attributePrefix + "Dbl", "42.0", TransportProtos.KeyValueType.DOUBLE_V);
        TransportProtos.TsKvProto tsKvProtoAttribute4 = getTsKvProto(attributePrefix + "Long", "73", TransportProtos.KeyValueType.LONG_V);
        TransportProtos.TsKvProto tsKvProtoAttribute5 = getTsKvProto(attributePrefix + "Json", "{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}", TransportProtos.KeyValueType.JSON_V);
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

    // subscribe to attributes updates from server methods

    protected void processJsonTestSubscribeToAttributesUpdates(String attrSubTopic) throws Exception {
        DeviceId deviceId = savedDevice.getId();

        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(accessToken);
        MqttTestCallback onUpdateCallback = new MqttTestCallback();
        client.setCallback(onUpdateCallback);

        subscribeAndWait(client, attrSubTopic, deviceId, FeatureType.ATTRIBUTES);

        doPostAsync("/api/plugins/telemetry/DEVICE/" + deviceId.getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        assertThat(onUpdateCallback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .as("await onUpdateCallback").isTrue();

        validateUpdateAttributesJsonResponse(onUpdateCallback, SHARED_ATTRIBUTES_PAYLOAD);

        MqttTestCallback onDeleteCallback = new MqttTestCallback();
        client.setCallback(onDeleteCallback);
        doDelete("/api/plugins/telemetry/DEVICE/" + deviceId.getId() + "/SHARED_SCOPE?keys=sharedJson", String.class);
        assertThat(onDeleteCallback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .as("await onDeleteCallback").isTrue();
        validateUpdateAttributesJsonResponse(onDeleteCallback, SHARED_ATTRIBUTES_DELETED_RESPONSE);
        client.disconnect();
    }

    protected void processProtoTestSubscribeToAttributesUpdates(String attrSubTopic) throws Exception {
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(accessToken);
        MqttTestCallback onUpdateCallback = new MqttTestCallback();
        client.setCallback(onUpdateCallback);
        subscribeAndWait(client, attrSubTopic, savedDevice.getId(), FeatureType.ATTRIBUTES);

        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        assertThat(onUpdateCallback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .as("await onUpdateCallback").isTrue();
        validateUpdateAttributesProtoResponse(onUpdateCallback);

        MqttTestCallback onDeleteCallback = new MqttTestCallback();
        client.setCallback(onDeleteCallback);
        doDelete("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/SHARED_SCOPE?keys=sharedJson", String.class);
        assertThat(onDeleteCallback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .as("await onDeleteCallback").isTrue();
        validateDeleteAttributesProtoResponse(onDeleteCallback);
        client.disconnect();
    }

    protected void validateUpdateAttributesJsonResponse(MqttTestCallback callback, String expectedResponse) {
        assertNotNull(callback.getPayloadBytes());
        assertEquals(JacksonUtil.toJsonNode(expectedResponse), JacksonUtil.fromBytes(callback.getPayloadBytes()));
    }

    protected void validateUpdateAttributesProtoResponse(MqttTestCallback callback) throws InvalidProtocolBufferException {
        assertThat(callback.getPayloadBytes()).as("callback payload non-null").isNotNull();
        TransportProtos.AttributeUpdateNotificationMsg.Builder attributeUpdateNotificationMsgBuilder = TransportProtos.AttributeUpdateNotificationMsg.newBuilder();
        List<TransportProtos.TsKvProto> tsKvProtoList = getTsKvProtoList("shared");
        attributeUpdateNotificationMsgBuilder.addAllSharedUpdated(tsKvProtoList);

        TransportProtos.AttributeUpdateNotificationMsg expectedAttributeUpdateNotificationMsg = attributeUpdateNotificationMsgBuilder.build();
        TransportProtos.AttributeUpdateNotificationMsg actualAttributeUpdateNotificationMsg = TransportProtos.AttributeUpdateNotificationMsg.parseFrom(callback.getPayloadBytes());

        List<TransportProtos.KeyValueProto> actualSharedUpdatedList = actualAttributeUpdateNotificationMsg.getSharedUpdatedList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
        List<TransportProtos.KeyValueProto> expectedSharedUpdatedList = expectedAttributeUpdateNotificationMsg.getSharedUpdatedList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());

        assertEquals(expectedSharedUpdatedList.size(), actualSharedUpdatedList.size());
        assertTrue(actualSharedUpdatedList.containsAll(expectedSharedUpdatedList));
    }

    protected void validateDeleteAttributesProtoResponse(MqttTestCallback callback) throws InvalidProtocolBufferException {
        assertThat(callback.getPayloadBytes()).as("callback payload non-null").isNotNull();
        TransportProtos.AttributeUpdateNotificationMsg.Builder attributeUpdateNotificationMsgBuilder = TransportProtos.AttributeUpdateNotificationMsg.newBuilder();
        attributeUpdateNotificationMsgBuilder.addSharedDeleted("sharedJson");

        TransportProtos.AttributeUpdateNotificationMsg expectedAttributeUpdateNotificationMsg = attributeUpdateNotificationMsgBuilder.build();
        TransportProtos.AttributeUpdateNotificationMsg actualAttributeUpdateNotificationMsg = TransportProtos.AttributeUpdateNotificationMsg.parseFrom(callback.getPayloadBytes());

        assertEquals(expectedAttributeUpdateNotificationMsg.getSharedDeletedList().size(), actualAttributeUpdateNotificationMsg.getSharedDeletedList().size());
        assertEquals("sharedJson", actualAttributeUpdateNotificationMsg.getSharedDeletedList().get(0));
    }

    protected void processJsonGatewayTestSubscribeToAttributesUpdates() throws Exception {
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(gatewayAccessToken);
        MqttTestCallback onUpdateCallback = new MqttTestCallback();
        client.setCallback(onUpdateCallback);

        String deviceName = "Gateway Device Subscribe to attribute updates";
        byte[] connectPayloadBytes = getJsonConnectPayloadBytes(deviceName, deviceProfile.getTransportType().name());

        client.publishAndWait(GATEWAY_CONNECT_TOPIC, connectPayloadBytes);

        Device savedDevice = doExecuteWithRetriesAndInterval(() -> doGet("/api/tenant/devices?deviceName=" + deviceName, Device.class),
                20,
                100);

        assertNotNull(savedDevice);

        subscribeAndCheckSubscription(client, GATEWAY_ATTRIBUTES_TOPIC, savedDevice.getId(), FeatureType.ATTRIBUTES);

        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        assertThat(onUpdateCallback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .as("await onUpdateCallback").isTrue();

        validateJsonGatewayUpdateAttributesResponse(onUpdateCallback, deviceName, SHARED_ATTRIBUTES_PAYLOAD);

        MqttTestCallback onDeleteCallback = new MqttTestCallback();
        client.setCallback(onDeleteCallback);

        doDelete("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/SHARED_SCOPE?keys=sharedJson", String.class);
        assertThat(onDeleteCallback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .as("await onDeleteCallback").isTrue();

        validateJsonGatewayUpdateAttributesResponse(onDeleteCallback, deviceName, SHARED_ATTRIBUTES_DELETED_RESPONSE);
        client.disconnect();
    }

    protected void processProtoGatewayTestSubscribeToAttributesUpdates() throws Exception {
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(gatewayAccessToken);
        MqttTestCallback onUpdateCallback = new MqttTestCallback();
        client.setCallback(onUpdateCallback);
        String deviceName = "Gateway Device Subscribe to attribute updates";
        byte[] connectPayloadBytes = getProtoConnectPayloadBytes(deviceName, TransportPayloadType.PROTOBUF.name());
        client.publishAndWait(GATEWAY_CONNECT_TOPIC, connectPayloadBytes);
        Device device = doExecuteWithRetriesAndInterval(() -> doGet("/api/tenant/devices?deviceName=" + deviceName, Device.class),
                20,
                100);
        assertNotNull(device);

        subscribeAndCheckSubscription(client, GATEWAY_ATTRIBUTES_TOPIC, device.getId(), FeatureType.ATTRIBUTES);
        doPostAsync("/api/plugins/telemetry/DEVICE/" + device.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        validateProtoGatewayUpdateAttributesResponse(onUpdateCallback, deviceName);
        MqttTestCallback onDeleteCallback = new MqttTestCallback();
        client.setCallback(onDeleteCallback);
        doDelete("/api/plugins/telemetry/DEVICE/" + device.getId().getId() + "/SHARED_SCOPE?keys=sharedJson", String.class);
        validateProtoGatewayDeleteAttributesResponse(onDeleteCallback, deviceName);
        client.disconnect();
    }

    protected void validateJsonGatewayUpdateAttributesResponse(MqttTestCallback callback, String deviceName, String expectResultData) {
        assertThat(callback.getPayloadBytes()).as("callback payload non-null").isNotNull();
        assertEquals(JacksonUtil.toJsonNode(getGatewayAttributesResponseJson(deviceName, expectResultData)), JacksonUtil.fromBytes(callback.getPayloadBytes()));
    }

    protected byte[] getJsonConnectPayloadBytes(String deviceName, String deviceType) {
        String connectPayload = "{\"device\":\"" + deviceName + "\", \"type\": \"" + deviceType + "\"}";
        return connectPayload.getBytes();
    }

    private static String getGatewayAttributesResponseJson(String deviceName, String expectResultData) {
        return "{\"device\":\"" + deviceName + "\"," + "\"data\":" + expectResultData + "}";
    }

    protected void validateProtoGatewayUpdateAttributesResponse(MqttTestCallback callback, String deviceName) throws InvalidProtocolBufferException, InterruptedException {
        assertThat(callback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .as("await callback").isTrue();
        assertThat(callback.getPayloadBytes()).as("callback payload non-null").isNotNull();

        TransportProtos.AttributeUpdateNotificationMsg.Builder attributeUpdateNotificationMsgBuilder = TransportProtos.AttributeUpdateNotificationMsg.newBuilder();
        List<TransportProtos.TsKvProto> tsKvProtoList = getTsKvProtoList("shared");
        attributeUpdateNotificationMsgBuilder.addAllSharedUpdated(tsKvProtoList);
        TransportProtos.AttributeUpdateNotificationMsg expectedAttributeUpdateNotificationMsg = attributeUpdateNotificationMsgBuilder.build();

        TransportApiProtos.GatewayAttributeUpdateNotificationMsg.Builder gatewayAttributeUpdateNotificationMsgBuilder = TransportApiProtos.GatewayAttributeUpdateNotificationMsg.newBuilder();
        gatewayAttributeUpdateNotificationMsgBuilder.setDeviceName(deviceName);
        gatewayAttributeUpdateNotificationMsgBuilder.setNotificationMsg(expectedAttributeUpdateNotificationMsg);

        TransportApiProtos.GatewayAttributeUpdateNotificationMsg expectedGatewayAttributeUpdateNotificationMsg = gatewayAttributeUpdateNotificationMsgBuilder.build();
        TransportApiProtos.GatewayAttributeUpdateNotificationMsg actualGatewayAttributeUpdateNotificationMsg = TransportApiProtos.GatewayAttributeUpdateNotificationMsg.parseFrom(callback.getPayloadBytes());

        assertEquals(expectedGatewayAttributeUpdateNotificationMsg.getDeviceName(), actualGatewayAttributeUpdateNotificationMsg.getDeviceName());

        List<TransportProtos.KeyValueProto> actualSharedUpdatedList = actualGatewayAttributeUpdateNotificationMsg.getNotificationMsg().getSharedUpdatedList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
        List<TransportProtos.KeyValueProto> expectedSharedUpdatedList = expectedGatewayAttributeUpdateNotificationMsg.getNotificationMsg().getSharedUpdatedList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());

        assertEquals(expectedSharedUpdatedList.size(), actualSharedUpdatedList.size());
        assertTrue(actualSharedUpdatedList.containsAll(expectedSharedUpdatedList));
    }

    protected void validateProtoGatewayDeleteAttributesResponse(MqttTestCallback callback, String deviceName) throws InvalidProtocolBufferException, InterruptedException {
        assertThat(callback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .as("await callback").isTrue();
        assertThat(callback.getPayloadBytes()).as("callback payload non-null").isNotNull();
        TransportProtos.AttributeUpdateNotificationMsg.Builder attributeUpdateNotificationMsgBuilder = TransportProtos.AttributeUpdateNotificationMsg.newBuilder();
        attributeUpdateNotificationMsgBuilder.addSharedDeleted("sharedJson");
        TransportProtos.AttributeUpdateNotificationMsg attributeUpdateNotificationMsg = attributeUpdateNotificationMsgBuilder.build();

        TransportApiProtos.GatewayAttributeUpdateNotificationMsg.Builder gatewayAttributeUpdateNotificationMsgBuilder = TransportApiProtos.GatewayAttributeUpdateNotificationMsg.newBuilder();
        gatewayAttributeUpdateNotificationMsgBuilder.setDeviceName(deviceName);
        gatewayAttributeUpdateNotificationMsgBuilder.setNotificationMsg(attributeUpdateNotificationMsg);

        TransportApiProtos.GatewayAttributeUpdateNotificationMsg expectedGatewayAttributeUpdateNotificationMsg = gatewayAttributeUpdateNotificationMsgBuilder.build();
        TransportApiProtos.GatewayAttributeUpdateNotificationMsg actualGatewayAttributeUpdateNotificationMsg = TransportApiProtos.GatewayAttributeUpdateNotificationMsg.parseFrom(callback.getPayloadBytes());

        assertEquals(expectedGatewayAttributeUpdateNotificationMsg.getDeviceName(), actualGatewayAttributeUpdateNotificationMsg.getDeviceName());

        TransportProtos.AttributeUpdateNotificationMsg expectedAttributeUpdateNotificationMsg = expectedGatewayAttributeUpdateNotificationMsg.getNotificationMsg();
        TransportProtos.AttributeUpdateNotificationMsg actualAttributeUpdateNotificationMsg = actualGatewayAttributeUpdateNotificationMsg.getNotificationMsg();

        assertEquals(expectedAttributeUpdateNotificationMsg.getSharedDeletedList().size(), actualAttributeUpdateNotificationMsg.getSharedDeletedList().size());
        assertEquals("sharedJson", actualAttributeUpdateNotificationMsg.getSharedDeletedList().get(0));
    }

    private byte[] getProtoConnectPayloadBytes(String deviceName, String deviceType) {
        TransportApiProtos.ConnectMsg connectMsg = TransportApiProtos.ConnectMsg.newBuilder()
                .setDeviceName(deviceName)
                .setDeviceType(deviceType)
                .build();
        return connectMsg.toByteArray();
    }

    // request attributes from server methods

    protected void processJsonTestRequestAttributesValuesFromTheServer(String attrPubTopic, String attrSubTopic, String attrReqTopicPrefix) throws Exception {
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(accessToken);
        SingleEntityFilter dtf = new SingleEntityFilter();
        dtf.setSingleEntity(AliasEntityId.fromEntityId(savedDevice.getId()));
        String clientKeysStr = "clientStr,clientBool,clientDbl,clientLong,clientJson";
        String sharedKeysStr = "sharedStr,sharedBool,sharedDbl,sharedLong,sharedJson";
        List<String> clientKeysList = List.of(clientKeysStr.split(","));
        List<String> sharedKeysList = List.of(sharedKeysStr.split(","));
        List<EntityKey> csKeys = getEntityKeys(clientKeysList, CLIENT_ATTRIBUTE);
        List<EntityKey> shKeys = getEntityKeys(sharedKeysList, SHARED_ATTRIBUTE);
        List<EntityKey> keys = new ArrayList<>();
        keys.addAll(csKeys);
        keys.addAll(shKeys);
        getWsClient().subscribeLatestUpdate(keys, dtf);
        getWsClient().registerWaitForUpdate(2);
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE",
                SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        client.publishAndWait(attrPubTopic, CLIENT_ATTRIBUTES_PAYLOAD.getBytes());
        client.subscribeAndWait(attrSubTopic, MqttQoS.AT_MOST_ONCE);
        //RequestAttributes does not make any subscriptions in device actor

        String update = getWsClient().waitForUpdate();
        assertThat(update).as("ws update received").isNotBlank();
        MqttTestCallback callback = new MqttTestSubscribeOnTopicCallback(attrSubTopic.replace("+", "1"));
        client.setCallback(callback);
        String payloadStr = "{\"clientKeys\":\"" + clientKeysStr + "\", \"sharedKeys\":\"" + sharedKeysStr + "\"}";
        client.publishAndWait(attrReqTopicPrefix + "1", payloadStr.getBytes());
        String expectedResponse = "{\"client\":" + CLIENT_ATTRIBUTES_PAYLOAD + ",\"shared\":" + SHARED_ATTRIBUTES_PAYLOAD + "}";
        validateJsonResponse(callback, expectedResponse);
        client.disconnect();
    }

    protected void processProtoTestRequestAttributesValuesFromTheServer(String attrPubTopic, String attrSubTopic, String attrReqTopicPrefix) throws Exception {
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(accessToken);
        DeviceTypeFilter dtf = new DeviceTypeFilter(List.of(savedDevice.getType()), savedDevice.getName());
        String clientKeysStr = "clientStr,clientBool,clientDbl,clientLong,clientJson";
        String sharedKeysStr = "sharedStr,sharedBool,sharedDbl,sharedLong,sharedJson";
        List<String> clientKeysList = List.of(clientKeysStr.split(","));
        List<String> sharedKeysList = List.of(sharedKeysStr.split(","));
        List<EntityKey> csKeys = getEntityKeys(clientKeysList, CLIENT_ATTRIBUTE);
        List<EntityKey> shKeys = getEntityKeys(sharedKeysList, SHARED_ATTRIBUTE);
        List<EntityKey> keys = new ArrayList<>();
        keys.addAll(csKeys);
        keys.addAll(shKeys);
        getWsClient().subscribeLatestUpdate(keys, dtf);
        getWsClient().registerWaitForUpdate(2);
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        client.publishAndWait(attrPubTopic, getAttributesProtoPayloadBytes());
        client.subscribeAndWait(attrSubTopic, MqttQoS.AT_MOST_ONCE);
        //RequestAttributes does not make any subscriptions in device actor

        String update = getWsClient().waitForUpdate();
        assertThat(update).as("ws update received").isNotBlank();
        MqttTestCallback callback = new MqttTestSubscribeOnTopicCallback(attrSubTopic.replace("+", "1"));
        client.setCallback(callback);
        TransportApiProtos.AttributesRequest.Builder attributesRequestBuilder = TransportApiProtos.AttributesRequest.newBuilder();
        attributesRequestBuilder.setClientKeys(clientKeysStr);
        attributesRequestBuilder.setSharedKeys(sharedKeysStr);
        TransportApiProtos.AttributesRequest attributesRequest = attributesRequestBuilder.build();
        client.publishAndWait(attrReqTopicPrefix + "1", attributesRequest.toByteArray());
        validateProtoResponse(callback, getExpectedAttributeResponseMsg());
        client.disconnect();
    }

    protected void processJsonTestGatewayRequestAttributesValuesFromTheServer() throws Exception {
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(gatewayAccessToken);
        String deviceName = "Gateway Device Request Attributes";
        String postClientAttributes = "{\"" + deviceName + "\":" + CLIENT_ATTRIBUTES_PAYLOAD + "}";
        client.publishAndWait(GATEWAY_ATTRIBUTES_TOPIC, postClientAttributes.getBytes());

        Device device = doExecuteWithRetriesAndInterval(() -> doGet("/api/tenant/devices?deviceName=" + deviceName, Device.class),
                20,
                100);
        assertNotNull(device);

        String clientKeysStr = "clientStr,clientBool,clientDbl,clientLong,clientJson";

        String attributeValuesUrl = "/api/plugins/telemetry/DEVICE/" + device.getId() + "/values/attributes/CLIENT_SCOPE?keys=" + clientKeysStr;

        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    List<Map<String, Object>> attributes = doGetAsyncTyped(attributeValuesUrl, new TypeReference<>() {
                    });
                    return attributes.size() == 5;
                });

        SingleEntityFilter dtf = new SingleEntityFilter();
        dtf.setSingleEntity(AliasEntityId.fromEntityId(device.getId()));
        String sharedKeysStr = "sharedStr,sharedBool,sharedDbl,sharedLong,sharedJson";
        List<String> clientKeysList = List.of(clientKeysStr.split(","));
        List<String> sharedKeysList = List.of(sharedKeysStr.split(","));
        List<EntityKey> csKeys = getEntityKeys(clientKeysList, CLIENT_ATTRIBUTE);
        List<EntityKey> shKeys = getEntityKeys(sharedKeysList, SHARED_ATTRIBUTE);
        List<EntityKey> keys = new ArrayList<>();
        keys.addAll(csKeys);
        keys.addAll(shKeys);
        EntityDataUpdate initUpdate = getWsClient().subscribeLatestUpdate(keys, dtf);
        assertNotNull(initUpdate);
        PageData<EntityData> data = initUpdate.getData();
        assertNotNull(data);
        assertFalse(data.getData().isEmpty());
        getWsClient().registerWaitForUpdate();

        doPostAsync("/api/plugins/telemetry/DEVICE/" + device.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        String update = getWsClient().waitForUpdate();
        assertThat(update).as("ws update received").isNotBlank();

        client.subscribeAndWait(GATEWAY_ATTRIBUTES_RESPONSE_TOPIC, MqttQoS.AT_LEAST_ONCE);
        //RequestAttributes does not make any subscriptions in device actor

        MqttTestCallback clientAttributesCallback = new MqttTestSubscribeOnTopicCallback(GATEWAY_ATTRIBUTES_RESPONSE_TOPIC);
        client.setCallback(clientAttributesCallback);
        String csKeysStr = "[\"clientStr\", \"clientBool\", \"clientDbl\", \"clientLong\", \"clientJson\"]";
        String csRequestPayloadStr = "{\"id\": 1, \"device\": \"" + deviceName + "\", \"client\": true, \"keys\": " + csKeysStr + "}";
        client.publishAndWait(GATEWAY_ATTRIBUTES_REQUEST_TOPIC, csRequestPayloadStr.getBytes());
        validateJsonResponseGateway(clientAttributesCallback, deviceName, CLIENT_ATTRIBUTES_PAYLOAD);

        MqttTestCallback sharedAttributesCallback = new MqttTestSubscribeOnTopicCallback(GATEWAY_ATTRIBUTES_RESPONSE_TOPIC);
        client.setCallback(sharedAttributesCallback);
        String shKeysStr = "[\"sharedStr\", \"sharedBool\", \"sharedDbl\", \"sharedLong\", \"sharedJson\"]";
        String shRequestPayloadStr = "{\"id\": 1, \"device\": \"" + deviceName + "\", \"client\": false, \"keys\": " + shKeysStr + "}";
        client.publishAndWait(GATEWAY_ATTRIBUTES_REQUEST_TOPIC, shRequestPayloadStr.getBytes());
        validateJsonResponseGateway(sharedAttributesCallback, deviceName, SHARED_ATTRIBUTES_PAYLOAD);

        client.disconnect();
    }

    protected void processProtoTestGatewayRequestAttributesValuesFromTheServer() throws Exception {
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(gatewayAccessToken);

        String deviceName = "Gateway Device Request Attributes";
        String clientKeysStr = "clientStr,clientBool,clientDbl,clientLong,clientJson";
        List<String> clientKeysList = List.of(clientKeysStr.split(","));
        client.publishAndWait(GATEWAY_ATTRIBUTES_TOPIC, getProtoGatewayDeviceClientAttributesPayload(deviceName, clientKeysList));

        Device device = doExecuteWithRetriesAndInterval(() -> doGet("/api/tenant/devices?deviceName=" + deviceName, Device.class),
                20,
                100);
        assertNotNull(device);

        SingleEntityFilter dtf = new SingleEntityFilter();
        dtf.setSingleEntity(AliasEntityId.fromEntityId(device.getId()));
        String sharedKeysStr = "sharedStr,sharedBool,sharedDbl,sharedLong,sharedJson";
        List<String> sharedKeysList = List.of(sharedKeysStr.split(","));
        List<EntityKey> csKeys = getEntityKeys(clientKeysList, CLIENT_ATTRIBUTE);
        List<EntityKey> shKeys = getEntityKeys(sharedKeysList, SHARED_ATTRIBUTE);
        List<EntityKey> keys = new ArrayList<>();
        keys.addAll(csKeys);
        keys.addAll(shKeys);
        EntityDataUpdate initUpdate = getWsClient().subscribeLatestUpdate(keys, dtf);
        assertNotNull(initUpdate);
        PageData<EntityData> data = initUpdate.getData();
        assertNotNull(data);
        assertFalse(data.getData().isEmpty());
        getWsClient().registerWaitForUpdate();

        doPostAsync("/api/plugins/telemetry/DEVICE/" + device.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        String update = getWsClient().waitForUpdate();
        assertThat(update).as("ws update received").isNotBlank();

        client.subscribeAndWait(GATEWAY_ATTRIBUTES_RESPONSE_TOPIC, MqttQoS.AT_LEAST_ONCE);
        awaitForDeviceActorToReceiveSubscription(device.getId(), FeatureType.ATTRIBUTES, 1);

        MqttTestCallback clientAttributesCallback = new MqttTestSubscribeOnTopicCallback(GATEWAY_ATTRIBUTES_RESPONSE_TOPIC);
        client.setCallback(clientAttributesCallback);
        TransportApiProtos.GatewayAttributesRequestMsg gatewayAttributesRequestMsg = getGatewayAttributesRequestMsg(deviceName, clientKeysList, true);
        client.publishAndWait(GATEWAY_ATTRIBUTES_REQUEST_TOPIC, gatewayAttributesRequestMsg.toByteArray());
        validateProtoClientResponseGateway(clientAttributesCallback, deviceName);

        MqttTestCallback sharedAttributesCallback = new MqttTestSubscribeOnTopicCallback(GATEWAY_ATTRIBUTES_RESPONSE_TOPIC);
        client.setCallback(sharedAttributesCallback);
        gatewayAttributesRequestMsg = getGatewayAttributesRequestMsg(deviceName, sharedKeysList, false);
        client.publishAndWait(GATEWAY_ATTRIBUTES_REQUEST_TOPIC, gatewayAttributesRequestMsg.toByteArray());
        validateProtoSharedResponseGateway(sharedAttributesCallback, deviceName);

        client.disconnect();
    }

    private List<EntityKey> getEntityKeys(List<String> keys, EntityKeyType scope) {
        return keys.stream().map(key -> new EntityKey(scope, key)).collect(Collectors.toList());
    }

    private byte[] getAttributesProtoPayloadBytes() {
        DeviceProfileTransportConfiguration transportConfiguration = deviceProfile.getProfileData().getTransportConfiguration();
        assertTrue(transportConfiguration instanceof MqttDeviceProfileTransportConfiguration);
        MqttDeviceProfileTransportConfiguration mqttTransportConfiguration = (MqttDeviceProfileTransportConfiguration) transportConfiguration;
        TransportPayloadTypeConfiguration transportPayloadTypeConfiguration = mqttTransportConfiguration.getTransportPayloadTypeConfiguration();
        assertTrue(transportPayloadTypeConfiguration instanceof ProtoTransportPayloadConfiguration);
        ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration = (ProtoTransportPayloadConfiguration) transportPayloadTypeConfiguration;
        ProtoFileElement protoFileElement = DynamicProtoUtils.getProtoFileElement(protoTransportPayloadConfiguration.getDeviceAttributesProtoSchema());
        DynamicSchema attributesSchema = DynamicProtoUtils.getDynamicSchema(protoFileElement, ProtoTransportPayloadConfiguration.ATTRIBUTES_PROTO_SCHEMA);

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
                .setField(postAttributesMsgDescriptor.findFieldByName("clientStr"), "value1")
                .setField(postAttributesMsgDescriptor.findFieldByName("clientBool"), true)
                .setField(postAttributesMsgDescriptor.findFieldByName("clientDbl"), 42.0)
                .setField(postAttributesMsgDescriptor.findFieldByName("clientLong"), 73)
                .setField(postAttributesMsgDescriptor.findFieldByName("clientJson"), jsonObject)
                .build();
        return postAttributesMsg.toByteArray();
    }

    protected byte[] getProtoGatewayDeviceClientAttributesPayload(String deviceName, List<String> clientKeysList) {
        TransportProtos.PostAttributeMsg postAttributeMsg = getPostAttributeMsg(clientKeysList);
        TransportApiProtos.AttributesMsg.Builder attributesMsgBuilder = TransportApiProtos.AttributesMsg.newBuilder();
        attributesMsgBuilder.setDeviceName(deviceName);
        attributesMsgBuilder.setMsg(postAttributeMsg);
        TransportApiProtos.AttributesMsg attributesMsg = attributesMsgBuilder.build();
        TransportApiProtos.GatewayAttributesMsg.Builder gatewayAttributeMsgBuilder = TransportApiProtos.GatewayAttributesMsg.newBuilder();
        gatewayAttributeMsgBuilder.addMsg(attributesMsg);
        return gatewayAttributeMsgBuilder.build().toByteArray();
    }

    protected void validateJsonResponse(MqttTestCallback callback, String expectedResponse) throws InterruptedException {
        assertThat(callback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .as("await callback").isTrue();
        assertEquals(MqttQoS.AT_MOST_ONCE.value(), callback.getMessageArrivedQoS());
        assertEquals(JacksonUtil.toJsonNode(expectedResponse), JacksonUtil.fromBytes(callback.getPayloadBytes()));
    }

    protected void validateProtoResponse(MqttTestCallback callback, TransportProtos.GetAttributeResponseMsg expectedResponse) throws InterruptedException, InvalidProtocolBufferException {
        assertThat(callback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .as("await callback").isTrue();
        assertEquals(MqttQoS.AT_MOST_ONCE.value(), callback.getMessageArrivedQoS());
        TransportProtos.GetAttributeResponseMsg actualAttributesResponse = TransportProtos.GetAttributeResponseMsg.parseFrom(callback.getPayloadBytes());
        assertEquals(expectedResponse.getRequestId(), actualAttributesResponse.getRequestId());
        List<TransportProtos.KeyValueProto> expectedClientKeyValueProtos = expectedResponse.getClientAttributeListList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
        List<TransportProtos.KeyValueProto> expectedSharedKeyValueProtos = expectedResponse.getSharedAttributeListList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
        List<TransportProtos.KeyValueProto> actualClientKeyValueProtos = actualAttributesResponse.getClientAttributeListList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
        List<TransportProtos.KeyValueProto> actualSharedKeyValueProtos = actualAttributesResponse.getSharedAttributeListList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
        assertTrue(actualClientKeyValueProtos.containsAll(expectedClientKeyValueProtos));
        assertTrue(actualSharedKeyValueProtos.containsAll(expectedSharedKeyValueProtos));
    }

    private TransportProtos.GetAttributeResponseMsg getExpectedAttributeResponseMsg() {
        TransportProtos.GetAttributeResponseMsg.Builder result = TransportProtos.GetAttributeResponseMsg.newBuilder();
        List<TransportProtos.TsKvProto> csTsKvProtoList = getTsKvProtoList("client");
        List<TransportProtos.TsKvProto> shTsKvProtoList = getTsKvProtoList("shared");
        result.addAllClientAttributeList(csTsKvProtoList);
        result.addAllSharedAttributeList(shTsKvProtoList);
        result.setRequestId(1);
        return result.build();
    }

    protected void validateJsonResponseGateway(MqttTestCallback callback, String deviceName, String expectedValues) throws InterruptedException {
        assertThat(callback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .as("await callback").isTrue();
        assertEquals(MqttQoS.AT_LEAST_ONCE.value(), callback.getMessageArrivedQoS());
        String expectedRequestPayload = "{\"id\":1,\"device\":\"" + deviceName + "\",\"values\":" + expectedValues + "}";
        assertEquals(JacksonUtil.toJsonNode(expectedRequestPayload), JacksonUtil.fromBytes(callback.getPayloadBytes()));
    }

    protected void validateProtoClientResponseGateway(MqttTestCallback callback, String deviceName) throws InterruptedException, InvalidProtocolBufferException {
        assertThat(callback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .as("await callback").isTrue();
        assertEquals(MqttQoS.AT_LEAST_ONCE.value(), callback.getMessageArrivedQoS());
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

    protected void validateProtoSharedResponseGateway(MqttTestCallback callback, String deviceName) throws InterruptedException, InvalidProtocolBufferException {
        assertThat(callback.getSubscribeLatch().await(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .as("await callback").isTrue();
        assertEquals(MqttQoS.AT_LEAST_ONCE.value(), callback.getMessageArrivedQoS());
        TransportApiProtos.GatewayAttributeResponseMsg expectedGatewayAttributeResponseMsg = getExpectedGatewayAttributeResponseMsg(deviceName, false);
        TransportApiProtos.GatewayAttributeResponseMsg actualGatewayAttributeResponseMsg = TransportApiProtos.GatewayAttributeResponseMsg.parseFrom(callback.getPayloadBytes());
        assertEquals(expectedGatewayAttributeResponseMsg.getDeviceName(), actualGatewayAttributeResponseMsg.getDeviceName());

        TransportProtos.GetAttributeResponseMsg expectedResponseMsg = expectedGatewayAttributeResponseMsg.getResponseMsg();
        TransportProtos.GetAttributeResponseMsg actualResponseMsg = actualGatewayAttributeResponseMsg.getResponseMsg();
        assertEquals(expectedResponseMsg.getRequestId(), actualResponseMsg.getRequestId());

        List<TransportProtos.KeyValueProto> expectedSharedKeyValueProtos = expectedResponseMsg.getSharedAttributeListList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());
        List<TransportProtos.KeyValueProto> actualSharedKeyValueProtos = actualResponseMsg.getSharedAttributeListList().stream().map(TransportProtos.TsKvProto::getKv).collect(Collectors.toList());

        assertTrue(actualSharedKeyValueProtos.containsAll(expectedSharedKeyValueProtos));
    }

    private TransportApiProtos.GatewayAttributeResponseMsg getExpectedGatewayAttributeResponseMsg(String deviceName, boolean client) {
        TransportApiProtos.GatewayAttributeResponseMsg.Builder gatewayAttributeResponseMsg = TransportApiProtos.GatewayAttributeResponseMsg.newBuilder();
        TransportProtos.GetAttributeResponseMsg.Builder getAttributeResponseMsgBuilder = TransportProtos.GetAttributeResponseMsg.newBuilder();
        if (client) {
            getAttributeResponseMsgBuilder.addAllClientAttributeList(getTsKvProtoList("client"));
        } else {
            getAttributeResponseMsgBuilder.addAllSharedAttributeList(getTsKvProtoList("shared"));
        }
        getAttributeResponseMsgBuilder.setRequestId(1);
        TransportProtos.GetAttributeResponseMsg getAttributeResponseMsg = getAttributeResponseMsgBuilder.build();
        gatewayAttributeResponseMsg.setDeviceName(deviceName);
        gatewayAttributeResponseMsg.setResponseMsg(getAttributeResponseMsg);
        return gatewayAttributeResponseMsg.build();
    }

    private TransportApiProtos.GatewayAttributesRequestMsg getGatewayAttributesRequestMsg(String deviceName, List<String> keysList, boolean client) {
        return TransportApiProtos.GatewayAttributesRequestMsg.newBuilder()
                .setDeviceName(deviceName)
                .addAllKeys(keysList)
                .setClient(client)
                .setId(1).build();
    }
}
