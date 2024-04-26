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
package org.thingsboard.server.msa.connectivity;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.AbstractListeningExecutor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.mqtt.MqttClient;
import org.thingsboard.mqtt.MqttClientConfig;
import org.thingsboard.mqtt.MqttHandler;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.info.RateLimitsNotificationInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.DisableUIListeners;
import org.thingsboard.server.msa.mapper.WsTelemetryResponse;
import org.thingsboard.server.service.ws.notification.cmd.UnreadNotificationsUpdate;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityDataUpdate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.DataConstants.DEVICE;
import static org.thingsboard.server.common.data.DataConstants.SHARED_SCOPE;
import static org.thingsboard.server.msa.prototypes.DevicePrototypes.defaultGatewayPrototype;

@DisableUIListeners
@Slf4j
public class MqttGatewayClientTest extends AbstractContainerTest {
    private Device gatewayDevice;
    private MqttClient mqttClient;
    private Device createdDevice;
    private MqttMessageListener listener;
    private JsonParser jsonParser = new JsonParser();

    AbstractListeningExecutor handlerExecutor;

    @BeforeMethod
    public void beforeTest() throws Exception {
        this.handlerExecutor = new AbstractListeningExecutor() {
            @Override
            protected int getThreadPollSize() {
                return 4;
            }
        };
        handlerExecutor.init();

        testRestClient.login("tenant@thingsboard.org", "tenant");
        gatewayDevice = testRestClient.postDevice("", defaultGatewayPrototype());
        DeviceCredentials gatewayDeviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(gatewayDevice.getId());

        this.listener = new MqttMessageListener();
        this.mqttClient = getMqttClient(gatewayDeviceCredentials, listener);
        this.createdDevice = createDeviceThroughGateway(mqttClient, gatewayDevice);
        wsClient = getWsClient();
    }

    @AfterMethod
    public void afterTest() throws Exception {
        updateTenantProfileWithGatewayTransportLimits("", "", "");
        testRestClient.deleteDeviceIfExists(this.gatewayDevice.getId());
        testRestClient.deleteDeviceIfExists(this.createdDevice.getId());
        this.listener = null;
        this.mqttClient = null;
        this.createdDevice = null;
        if (handlerExecutor != null) {
            handlerExecutor.destroy();
        }
        getWsClient().markAllNotificationsAsRead();
        getWsClient().closeBlocking();
    }

    @Test
    public void telemetryUpload() throws Exception {

        List<String> expectedKeys = Arrays.asList("booleanKey", "stringKey", "doubleKey", "longKey");

        SingleEntityFilter filter = new SingleEntityFilter();
        filter.setSingleEntity(createdDevice.getId());

        long now = System.currentTimeMillis();

        EntityDataUpdate entityDataUpdate = getWsClient().subscribeTsUpdate(expectedKeys, now, TimeUnit.SECONDS.toMillis(1), filter);
        assertThat(entityDataUpdate.getData().getData().size()).isEqualTo(1);
        Map<String, TsValue[]> timeseries = entityDataUpdate.getData().getData().get(0).getTimeseries();
        assertThat(timeseries.keySet()).containsOnlyOnceElementsOf(expectedKeys);

        getWsClient().registerWaitForUpdate();

        mqttClient.publish("v1/gateway/telemetry", Unpooled.wrappedBuffer(createGatewayPayload(createdDevice.getName(), -1).toString().getBytes())).get();

        String updateString = getWsClient().waitForUpdate(3000, true);
        EntityDataUpdate update = JacksonUtil.fromString(updateString, EntityDataUpdate.class);
        assertThat(update).isNotNull();
        assertThat(update.getUpdate()).isNotNull();
        assertThat(update.getUpdate().size()).isEqualTo(1);
        Map<String, TsValue[]> actualLatestTelemetry = update.getUpdate().get(0).getTimeseries();

        assertThat(actualLatestTelemetry.keySet()).containsOnlyOnceElementsOf(expectedKeys);
        assertThat(actualLatestTelemetry.get("booleanKey")[0].getValue()).isEqualTo(Boolean.TRUE.toString());
        assertThat(actualLatestTelemetry.get("stringKey")[0].getValue()).isEqualTo("value1");
        assertThat(actualLatestTelemetry.get("doubleKey")[0].getValue()).isEqualTo(Double.toString(42.0));
        assertThat(actualLatestTelemetry.get("longKey")[0].getValue()).isEqualTo(Long.toString(73));
    }

    @Test
    public void telemetryUploadWithTs() throws Exception {

        List<String> expectedKeys = Arrays.asList("booleanKey", "stringKey", "doubleKey", "longKey");

        SingleEntityFilter filter = new SingleEntityFilter();
        filter.setSingleEntity(createdDevice.getId());

        long ts = 1451649600512L;

        EntityDataUpdate entityDataUpdate = getWsClient().subscribeTsUpdate(expectedKeys, ts - 1, TimeUnit.SECONDS.toMillis(1), filter);
        assertThat(entityDataUpdate.getData().getData().size()).isEqualTo(1);
        Map<String, TsValue[]> timeseries = entityDataUpdate.getData().getData().get(0).getTimeseries();
        assertThat(timeseries.keySet()).containsOnlyOnceElementsOf(expectedKeys);

        getWsClient().registerWaitForUpdate();

        mqttClient.publish("v1/gateway/telemetry", Unpooled.wrappedBuffer(createGatewayPayload(createdDevice.getName(), ts).toString().getBytes())).get();

        String updateString = getWsClient().waitForUpdate(3000, true);
        EntityDataUpdate update = JacksonUtil.fromString(updateString, EntityDataUpdate.class);
        assertThat(update).isNotNull();
        assertThat(update.getUpdate()).isNotNull();
        assertThat(update.getUpdate().size()).isEqualTo(1);
        Map<String, TsValue[]> actualLatestTelemetry = update.getUpdate().get(0).getTimeseries();
        log.info("Received telemetry: {}", actualLatestTelemetry);

        assertThat(actualLatestTelemetry.keySet()).containsOnlyOnceElementsOf(expectedKeys);
        assertThat(actualLatestTelemetry.get("booleanKey")[0].getTs()).isEqualTo(ts);
        assertThat(actualLatestTelemetry.get("booleanKey")[0].getValue()).isEqualTo(Boolean.TRUE.toString());
        assertThat(actualLatestTelemetry.get("stringKey")[0].getTs()).isEqualTo(ts);
        assertThat(actualLatestTelemetry.get("stringKey")[0].getValue()).isEqualTo("value1");
        assertThat(actualLatestTelemetry.get("doubleKey")[0].getTs()).isEqualTo(ts);
        assertThat(actualLatestTelemetry.get("doubleKey")[0].getValue()).isEqualTo(Double.toString(42.0));
        assertThat(actualLatestTelemetry.get("longKey")[0].getTs()).isEqualTo(ts);
        assertThat(actualLatestTelemetry.get("longKey")[0].getValue()).isEqualTo(Long.toString(73));
    }

    @Test
    public void publishAttributeUpdateToServer() throws Exception {
        testRestClient.getDeviceCredentialsByDeviceId(createdDevice.getId());

        List<String> expectedKeys = Arrays.asList("attr1", "attr2", "attr3", "attr4");

        SingleEntityFilter filter = new SingleEntityFilter();
        filter.setSingleEntity(createdDevice.getId());

        getWsClient().subscribeForAttributes(createdDevice.getId(), "CLIENT_SCOPE", expectedKeys);
        JsonObject clientAttributes = new JsonObject();
        clientAttributes.addProperty("attr1", "value1");
        clientAttributes.addProperty("attr2", true);
        clientAttributes.addProperty("attr3", 42.0);
        clientAttributes.addProperty("attr4", 73);
        JsonObject gatewayClientAttributes = new JsonObject();
        gatewayClientAttributes.add(createdDevice.getName(), clientAttributes);
        getWsClient().registerWaitForUpdate();
        mqttClient.publish("v1/gateway/attributes", Unpooled.wrappedBuffer(gatewayClientAttributes.toString().getBytes())).get();

        String updateString = getWsClient().waitForUpdate(3000, true);
        WsTelemetryResponse actualLatestTelemetry = JacksonUtil.fromString(updateString, WsTelemetryResponse.class);
        log.info("Received attributes: {}", actualLatestTelemetry);

        assertThat(actualLatestTelemetry.getData()).hasSize(4);
        assertThat(actualLatestTelemetry.getLatestValues().keySet()).containsOnlyOnceElementsOf(Arrays.asList("attr1", "attr2", "attr3", "attr4"));

        assertThat(actualLatestTelemetry.getDataValuesByKey("attr1").get(1)).isEqualTo("value1");
        assertThat(actualLatestTelemetry.getDataValuesByKey("attr2").get(1)).isEqualTo(Boolean.TRUE.toString());
        assertThat(actualLatestTelemetry.getDataValuesByKey("attr3").get(1)).isEqualTo(Double.toString(42.0));
        assertThat(actualLatestTelemetry.getDataValuesByKey("attr4").get(1)).isEqualTo(Long.toString(73));
    }

    @Test
    public void responseDataOnAttributesRequestCheck() throws Exception {
        testRestClient.getDeviceCredentialsByDeviceId(createdDevice.getId());
        JsonObject sharedAttributes = new JsonObject();
        sharedAttributes.addProperty("attr1", "value1");
        sharedAttributes.addProperty("attr2", true);
        sharedAttributes.addProperty("attr3", 42.0);
        sharedAttributes.addProperty("attr4", 73);

        mqttClient.on("v1/gateway/attributes/response", listener, MqttQoS.AT_LEAST_ONCE).get();

        testRestClient.postTelemetryAttribute(DataConstants.DEVICE, createdDevice.getId(), SHARED_SCOPE, mapper.readTree(sharedAttributes.toString()));
        var event = listener.getEvents().poll(10 * timeoutMultiplier, TimeUnit.SECONDS);

        JsonObject requestData = new JsonObject();
        requestData.addProperty("id", 1);
        requestData.addProperty("device", createdDevice.getName());
        requestData.addProperty("client", false);
        requestData.addProperty("key", "attr1");

        mqttClient.on("v1/gateway/attributes/response", listener, MqttQoS.AT_LEAST_ONCE).get();
        mqttClient.publish("v1/gateway/attributes/request", Unpooled.wrappedBuffer(requestData.toString().getBytes())).get();
        event = listener.getEvents().poll(10 * timeoutMultiplier, TimeUnit.SECONDS);

        JsonObject responseData = jsonParser.parse(Objects.requireNonNull(event).getMessage()).getAsJsonObject();
        assertThat(responseData.has("value")).isTrue();
        assertThat(responseData.get("value").getAsString()).isEqualTo(sharedAttributes.get("attr1").getAsString());

        requestData = new JsonObject();
        requestData.addProperty("id", 1);
        requestData.addProperty("device", createdDevice.getName());
        requestData.addProperty("client", false);
        JsonArray keys = new JsonArray();
        keys.add("attr1");
        keys.add("attr2");
        requestData.add("keys", keys);

        mqttClient.on("v1/gateway/attributes/response", listener, MqttQoS.AT_LEAST_ONCE).get();
        mqttClient.publish("v1/gateway/attributes/request", Unpooled.wrappedBuffer(requestData.toString().getBytes())).get();
        event = listener.getEvents().poll(10 * timeoutMultiplier, TimeUnit.SECONDS);
        responseData = jsonParser.parse(Objects.requireNonNull(event).getMessage()).getAsJsonObject();

        assertThat(responseData.has("values")).isTrue();
        assertThat(responseData.get("values").getAsJsonObject().get("attr1").getAsString()).isEqualTo(sharedAttributes.get("attr1").getAsString());
        assertThat(responseData.get("values").getAsJsonObject().get("attr2").getAsString()).isEqualTo(sharedAttributes.get("attr2").getAsString());

        requestData = new JsonObject();
        requestData.addProperty("id", 1);
        requestData.addProperty("device", createdDevice.getName());
        requestData.addProperty("client", false);
        keys = new JsonArray();
        keys.add("attr1");
        keys.add("undefined");
        requestData.add("keys", keys);

        mqttClient.on("v1/gateway/attributes/response", listener, MqttQoS.AT_LEAST_ONCE).get();
        mqttClient.publish("v1/gateway/attributes/request", Unpooled.wrappedBuffer(requestData.toString().getBytes())).get();
        event = listener.getEvents().poll(10 * timeoutMultiplier, TimeUnit.SECONDS);
        responseData = jsonParser.parse(Objects.requireNonNull(event).getMessage()).getAsJsonObject();

        assertThat(responseData.has("values")).isTrue();
        assertThat(responseData.get("values").getAsJsonObject().get("attr1").getAsString()).isEqualTo(sharedAttributes.get("attr1").getAsString());
        assertThat(responseData.get("values").getAsJsonObject().entrySet()).hasSize(1);
    }

    @Test
    public void requestAttributeValuesFromServer() throws Exception {

        SingleEntityFilter filter = new SingleEntityFilter();
        filter.setSingleEntity(createdDevice.getId());

        getWsClient().subscribeForAttributes(createdDevice.getId(), "CLIENT_SCOPE", Collections.singletonList("clientAttr"));
        // Add a new client attribute
        JsonObject clientAttributes = new JsonObject();
        String clientAttributeValue = StringUtils.randomAlphanumeric(8);
        clientAttributes.addProperty("clientAttr", clientAttributeValue);

        JsonObject gatewayClientAttributes = new JsonObject();
        gatewayClientAttributes.add(createdDevice.getName(), clientAttributes);

        getWsClient().registerWaitForUpdate();
        mqttClient.publish("v1/gateway/attributes", Unpooled.wrappedBuffer(gatewayClientAttributes.toString().getBytes())).get();

        String update = getWsClient().waitForUpdate(3000, true);
        WsTelemetryResponse actualLatestTelemetry = JacksonUtil.fromString(update, WsTelemetryResponse.class);
        log.info("Received ws telemetry: {}", actualLatestTelemetry);

        assertThat(actualLatestTelemetry.getData()).hasSize(1);
        assertThat(actualLatestTelemetry.getLatestValues().keySet()).containsOnly("clientAttr");
        assertThat(actualLatestTelemetry.getDataValuesByKey("clientAttr").get(1)).isEqualTo(clientAttributeValue);

        // Add a new shared attribute
        JsonObject sharedAttributes = new JsonObject();
        String sharedAttributeValue = StringUtils.randomAlphanumeric(8);
        sharedAttributes.addProperty("sharedAttr", sharedAttributeValue);

        // Subscribe for attribute update event
        mqttClient.on("v1/gateway/attributes", listener, MqttQoS.AT_LEAST_ONCE).get();

        Thread.sleep(500);
        testRestClient.postTelemetryAttribute(DEVICE, createdDevice.getId(), SHARED_SCOPE, mapper.readTree(sharedAttributes.toString()));
        MqttEvent sharedAttributeEvent = listener.getEvents().poll(10 * timeoutMultiplier, TimeUnit.SECONDS);

        // Catch attribute update event
        assertThat(sharedAttributeEvent).isNotNull();
        assertThat(sharedAttributeEvent.getTopic()).isEqualTo("v1/gateway/attributes");

        // Subscribe to attributes response
        mqttClient.on("v1/gateway/attributes/response", listener, MqttQoS.AT_LEAST_ONCE).get();

        // Wait until subscription is processed
        TimeUnit.SECONDS.sleep(3 * timeoutMultiplier);

        checkAttribute(true, clientAttributeValue);
        checkAttribute(false, sharedAttributeValue);
    }

    @Test
    public void subscribeToAttributeUpdatesFromServer() throws Exception {
        mqttClient.on("v1/gateway/attributes", listener, MqttQoS.AT_LEAST_ONCE).get();
        // Wait until subscription is processed
        TimeUnit.SECONDS.sleep(3 * timeoutMultiplier);
        String sharedAttributeName = "sharedAttr";
        // Add a new shared attribute

        JsonObject sharedAttributes = new JsonObject();
        String sharedAttributeValue = StringUtils.randomAlphanumeric(8);
        sharedAttributes.addProperty(sharedAttributeName, sharedAttributeValue);

        JsonObject gatewaySharedAttributeValue = new JsonObject();
        gatewaySharedAttributeValue.addProperty("device", createdDevice.getName());
        gatewaySharedAttributeValue.add("data", sharedAttributes);

        testRestClient.postTelemetryAttribute(DEVICE, createdDevice.getId(), SHARED_SCOPE, mapper.readTree(sharedAttributes.toString()));

        MqttEvent event = listener.getEvents().poll(10 * timeoutMultiplier, TimeUnit.SECONDS);
        assertThat(mapper.readValue(Objects.requireNonNull(event).getMessage(), JsonNode.class).get("data").get(sharedAttributeName).asText())
                .isEqualTo(sharedAttributeValue);

        // Update the shared attribute value
        JsonObject updatedSharedAttributes = new JsonObject();
        String updatedSharedAttributeValue = StringUtils.randomAlphanumeric(8);
        updatedSharedAttributes.addProperty(sharedAttributeName, updatedSharedAttributeValue);

        JsonObject gatewayUpdatedSharedAttributeValue = new JsonObject();
        gatewayUpdatedSharedAttributeValue.addProperty("device", createdDevice.getName());
        gatewayUpdatedSharedAttributeValue.add("data", updatedSharedAttributes);

        testRestClient.postTelemetryAttribute(DEVICE, createdDevice.getId(), SHARED_SCOPE, mapper.readTree(updatedSharedAttributes.toString()));
        event = listener.getEvents().poll(10 * timeoutMultiplier, TimeUnit.SECONDS);
        assertThat(mapper.readValue(Objects.requireNonNull(event).getMessage(), JsonNode.class).get("data").get(sharedAttributeName).asText())
                .isEqualTo(updatedSharedAttributeValue);
    }

    @Test
    public void serverSideRpc() throws Exception {
        String gatewayRpcTopic = "v1/gateway/rpc";
        mqttClient.on(gatewayRpcTopic, listener, MqttQoS.AT_LEAST_ONCE).get();

        // Wait until subscription is processed
        TimeUnit.SECONDS.sleep(3 * timeoutMultiplier);

        // Send an RPC from the server
        JsonObject serverRpcPayload = new JsonObject();
        serverRpcPayload.addProperty("method", "getValue");
        serverRpcPayload.addProperty("params", true);
        ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName(getClass().getSimpleName())));
        ListenableFuture<JsonNode> future = service.submit(() -> {
            try {
                return testRestClient.postServerSideRpc(createdDevice.getId(), mapper.readTree(serverRpcPayload.toString()));
            } catch (IOException e) {
                return null;
            }
        });

        // Wait for RPC call from the server and send the response
        MqttEvent requestFromServer = listener.getEvents().poll(10 * timeoutMultiplier, TimeUnit.SECONDS);
        service.shutdownNow();

        assertThat(requestFromServer).isNotNull();
        assertThat(requestFromServer.getMessage()).isNotNull();
        JsonNode requestFromServerJson = JacksonUtil.toJsonNode(requestFromServer.getMessage());
        assertThat(requestFromServerJson.get("device").asText()).isEqualTo(createdDevice.getName());
        assertThat(requestFromServerJson.get("data").get("method").asText()).isEqualTo("getValue");
        assertThat(requestFromServerJson.get("data").get("params").asText()).isEqualTo("true");
        int requestId = requestFromServerJson.get("data").get("id").asInt();

        JsonObject clientResponse = new JsonObject();
        clientResponse.addProperty("response", "someResponse");
        JsonObject gatewayResponse = new JsonObject();
        gatewayResponse.addProperty("device", createdDevice.getName());
        gatewayResponse.addProperty("id", requestId);
        gatewayResponse.add("data", clientResponse);
        // Send a response to the server's RPC request

        mqttClient.publish(gatewayRpcTopic, Unpooled.wrappedBuffer(gatewayResponse.toString().getBytes())).get();
        JsonNode serverResponse = future.get(5 * timeoutMultiplier, TimeUnit.SECONDS);

        assertThat(serverResponse).isEqualTo(mapper.readTree(clientResponse.toString()));
    }

    @Test
    public void deviceCreationAfterDeleted() throws Exception {
        testRestClient.deleteDevice(this.createdDevice.getId());
        testRestClient.getDeviceById(this.createdDevice.getId(), HttpStatus.NOT_FOUND.value());
        this.createdDevice = createDeviceThroughGateway(mqttClient, gatewayDevice);
    }

    @Test
    public void testMsgRateLimitsForGatewayDevice() throws Exception {
        getWsClient().markAllNotificationsAsRead();
        updateTenantProfileWithGatewayTransportLimits("1:30", "", "");
        getWsClient().subscribeForUnreadNotifications(10);
        getWsClient().registerWaitForUpdate();
        Thread.sleep(1000);
        mqttClient = getMqttClient(testRestClient.getDeviceCredentialsByDeviceId(gatewayDevice.getId()), listener);
        Thread.sleep(1000);
        mqttClient.publish("v1/devices/me/telemetry", Unpooled.wrappedBuffer(createPayload().toString().getBytes())).get(3, TimeUnit.SECONDS);
        Thread.sleep(1000);

        String notificationUpdate = getWsClient().waitForUpdate(3000, true);

        UnreadNotificationsUpdate unreadNotificationsUpdate = JacksonUtil.fromString(notificationUpdate, UnreadNotificationsUpdate.class);
        assertThat(unreadNotificationsUpdate).isNotNull();
        Notification update = unreadNotificationsUpdate.getUpdate();
        assertThat(update).isNotNull();
        assertThat(update.getSubject()).isEqualTo("Rate limits exceeded");
        assertThat(update.getInfo()).isInstanceOf(RateLimitsNotificationInfo.class);
        RateLimitsNotificationInfo info = (RateLimitsNotificationInfo) update.getInfo();
        assertThat(info.getApi()).isEqualTo(LimitedApi.TRANSPORT_MESSAGES_PER_GATEWAY);
        assertThat(info.getLimitLevel()).isEqualTo(gatewayDevice.getId());
    }

    @Test
    public void testTelemetryMsgRateLimitsForGatewayDevice() throws Exception {
        getWsClient().markAllNotificationsAsRead();
        updateTenantProfileWithGatewayTransportLimits("", "1:30", "");
        Thread.sleep(500);

        List<String> expectedKeys = Arrays.asList("booleanKey", "stringKey", "doubleKey", "longKey");

        SingleEntityFilter filter = new SingleEntityFilter();
        filter.setSingleEntity(gatewayDevice.getId());

        long now = System.currentTimeMillis();

        EntityDataUpdate entityDataUpdate = getWsClient().subscribeTsUpdate(expectedKeys, now, TimeUnit.SECONDS.toMillis(1), filter);
        assertThat(entityDataUpdate.getData().getData().size()).isEqualTo(1);
        Map<String, TsValue[]> timeseries = entityDataUpdate.getData().getData().get(0).getTimeseries();
        assertThat(timeseries.keySet()).containsOnlyOnceElementsOf(expectedKeys);

        getWsClient().registerWaitForUpdate();

        mqttClient.publish("v1/devices/me/telemetry", Unpooled.wrappedBuffer(createPayload().toString().getBytes())).get(3, TimeUnit.SECONDS);

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

        Thread.sleep(500);

        getWsClient().markAllNotificationsAsRead();
        getWsClient().subscribeForUnreadNotifications(10);
        getWsClient().registerWaitForUpdate();

        mqttClient.publish("v1/devices/me/telemetry", Unpooled.wrappedBuffer(createAltPayload().toString().getBytes())).get(3, TimeUnit.SECONDS);

        Thread.sleep(500);

        String notificaitonsUpdateString = getWsClient().waitForUpdate(3000, true);

        UnreadNotificationsUpdate unreadNotificationsUpdate = JacksonUtil.fromString(notificaitonsUpdateString, UnreadNotificationsUpdate.class);
        assertThat(unreadNotificationsUpdate).isNotNull();
        Notification notificationsUpdate = unreadNotificationsUpdate.getUpdate();
        assertThat(notificationsUpdate).isNotNull();
        assertThat(notificationsUpdate.getSubject()).isEqualTo("Rate limits exceeded");
        assertThat(notificationsUpdate.getInfo()).isInstanceOf(RateLimitsNotificationInfo.class);
        RateLimitsNotificationInfo info = (RateLimitsNotificationInfo) notificationsUpdate.getInfo();
        assertThat(info.getApi()).isEqualTo(LimitedApi.TRANSPORT_MESSAGES_PER_GATEWAY);
        assertThat(info.getLimitLevel()).isEqualTo(gatewayDevice.getId());
    }

    @Test
    public void testTelemetryDataPointsRateLimitsForGatewayDevice() throws Exception {
        getWsClient().markAllNotificationsAsRead();
        updateTenantProfileWithGatewayTransportLimits("", "", "4:30");
        Thread.sleep(500);

        List<String> expectedKeys = Arrays.asList("booleanKey", "stringKey", "doubleKey", "longKey");

        SingleEntityFilter filter = new SingleEntityFilter();
        filter.setSingleEntity(gatewayDevice.getId());

        long now = System.currentTimeMillis();

        EntityDataUpdate entityDataUpdate = getWsClient().subscribeTsUpdate(expectedKeys, now, TimeUnit.SECONDS.toMillis(1), filter);
        assertThat(entityDataUpdate.getData().getData().size()).isEqualTo(1);
        Map<String, TsValue[]> timeseries = entityDataUpdate.getData().getData().get(0).getTimeseries();
        assertThat(timeseries.keySet()).containsOnlyOnceElementsOf(expectedKeys);

        getWsClient().registerWaitForUpdate();

        mqttClient.publish("v1/devices/me/telemetry", Unpooled.wrappedBuffer(createPayload().toString().getBytes())).get(3, TimeUnit.SECONDS);

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

        Thread.sleep(500);

        getWsClient().markAllNotificationsAsRead();
        getWsClient().subscribeForUnreadNotifications(10);
        getWsClient().registerWaitForUpdate();

        mqttClient.publish("v1/devices/me/telemetry", Unpooled.wrappedBuffer(createAltPayload().toString().getBytes())).get(3, TimeUnit.SECONDS);

        Thread.sleep(500);

        String notificaitonsUpdateString = getWsClient().waitForUpdate(3000, true);

        UnreadNotificationsUpdate unreadNotificationsUpdate = JacksonUtil.fromString(notificaitonsUpdateString, UnreadNotificationsUpdate.class);
        assertThat(unreadNotificationsUpdate).isNotNull();
        Notification notificationsUpdate = unreadNotificationsUpdate.getUpdate();
        assertThat(notificationsUpdate).isNotNull();
        assertThat(notificationsUpdate.getSubject()).isEqualTo("Rate limits exceeded");
        assertThat(notificationsUpdate.getInfo()).isInstanceOf(RateLimitsNotificationInfo.class);
        RateLimitsNotificationInfo info = (RateLimitsNotificationInfo) notificationsUpdate.getInfo();
        assertThat(info.getApi()).isEqualTo(LimitedApi.TRANSPORT_MESSAGES_PER_GATEWAY);
        assertThat(info.getLimitLevel()).isEqualTo(gatewayDevice.getId());
    }

    private void checkAttribute(boolean client, String expectedValue) throws Exception {
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
        MqttEvent clientAttributeEvent = listener.getEvents().poll(10 * timeoutMultiplier, TimeUnit.SECONDS);
        assertThat(clientAttributeEvent).isNotNull();
        JsonObject responseMessage = new JsonParser().parse(Objects.requireNonNull(clientAttributeEvent).getMessage()).getAsJsonObject();

        assertThat(responseMessage.get("id").getAsInt()).isEqualTo(messageId);
        assertThat(responseMessage.get("device").getAsString()).isEqualTo(createdDevice.getName());
        assertThat(responseMessage.entrySet()).hasSize(3);
        assertThat(responseMessage.get("value").getAsString()).isEqualTo(expectedValue);
    }

    private Device createDeviceThroughGateway(MqttClient mqttClient, Device gatewayDevice) throws Exception {
        if (timeoutMultiplier > 1) {
            TimeUnit.SECONDS.sleep(30);
        }

        String deviceName = "mqtt_device" + RandomStringUtils.randomAlphabetic(5);
        mqttClient.publish("v1/gateway/connect", Unpooled.wrappedBuffer(createGatewayConnectPayload(deviceName).toString().getBytes()), MqttQoS.AT_LEAST_ONCE).get();

        if (timeoutMultiplier > 1) {
            TimeUnit.SECONDS.sleep(30);
        }

        List<EntityRelation> relations = testRestClient.findRelationByFrom(gatewayDevice.getId(), RelationTypeGroup.COMMON);
        assertThat(relations).hasSize(1);

        EntityId createdEntityId = relations.get(0).getTo();
        DeviceId createdDeviceId = new DeviceId(createdEntityId.getId());
        return testRestClient.getDeviceById(createdDeviceId);
    }

    private String getOwnerId() {
        return "Tenant[" + gatewayDevice.getTenantId().getId() + "]MqttGatewayClientTestDevice[" + gatewayDevice.getId().getId() + "]";
    }

    private MqttClient getMqttClient(DeviceCredentials deviceCredentials, MqttMessageListener listener) throws InterruptedException, ExecutionException {
        MqttClientConfig clientConfig = new MqttClientConfig();
        clientConfig.setOwnerId(getOwnerId());
        clientConfig.setClientId("MQTT client from test");
        clientConfig.setUsername(deviceCredentials.getCredentialsId());
        MqttClient mqttClient = MqttClient.create(clientConfig, listener, handlerExecutor);
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

    protected JsonObject createAltPayload() {
        JsonObject values = new JsonObject();
        values.addProperty("stringAltKey", "value2");
        values.addProperty("booleanAltKey", false);
        values.addProperty("doubleAltKey", 45.0);
        values.addProperty("longAltKey", 78L);

        return values;
    }

    private void updateTenantProfileWithGatewayTransportLimits(String msgRateLimit, String telemetryMsgRateLimit, String telemetryDataPointsRateLimit) {
        testRestClient.login("sysadmin@thingsboard.org", "sysadmin");
        PageLink pageLink = new PageLink(10);
        PageData<TenantProfile> tenantProfiles = testRestClient.getTenantProfiles(pageLink);
        Optional<TenantProfile> defaultTenantProfile = tenantProfiles.getData().stream().filter(tenantProfile -> tenantProfile.getName().equals("Default")).findFirst();

        assertThat(defaultTenantProfile).isPresent();

        TenantProfile tenantProfile = defaultTenantProfile.get();
        TenantProfileData profileData = tenantProfile.getProfileData();
        DefaultTenantProfileConfiguration configuration = (DefaultTenantProfileConfiguration) profileData.getConfiguration();

        configuration.setTransportGatewayMsgRateLimit(msgRateLimit);
        configuration.setTransportGatewayTelemetryMsgRateLimit(telemetryMsgRateLimit);
        configuration.setTransportGatewayTelemetryDataPointsRateLimit(telemetryDataPointsRateLimit);
        profileData.setConfiguration(configuration);
        tenantProfile.setProfileData(profileData);
        testRestClient.postTenantProfile(tenantProfile);
        testRestClient.login("tenant@thingsboard.org", "tenant");
    }

}
