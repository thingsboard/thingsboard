/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.JsonObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.restassured.response.ValidatableResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.AbstractListeningExecutor;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.mqtt.MqttClient;
import org.thingsboard.mqtt.MqttClientConfig;
import org.thingsboard.mqtt.MqttHandler;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.DisableUIListeners;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.DataConstants.DEVICE;
import static org.thingsboard.server.common.data.DataConstants.SHARED_SCOPE;
import static org.thingsboard.server.msa.prototypes.DevicePrototypes.defaultGatewayPrototype;

@DisableUIListeners
@Slf4j
public class MqttGatewayIssueTest extends AbstractContainerTest {
    private Device gatewayDevice;
    private MqttClient mqttClient;
    private List<Device> createdDevices = new ArrayList<>();
    private MqttMessageListener listener;
    private ScheduledExecutorService schedulerExecutor;
    ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(5));
    AbstractListeningExecutor handlerExecutor;

    @BeforeMethod
    public void createTestDevices() throws Exception {
        this.handlerExecutor = new AbstractListeningExecutor() {
            @Override
            protected int getThreadPollSize() {
                return 4;
            }
        };
        handlerExecutor.init();

        schedulerExecutor = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("test-scheduler"));

        testRestClient.login("tenant@thingsboard.org", "tenant");
        gatewayDevice = testRestClient.postDevice("", defaultGatewayPrototype());
        DeviceCredentials gatewayDeviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(gatewayDevice.getId());

        this.listener = new MqttMessageListener();
        this.mqttClient = getMqttClient(listener, gatewayDeviceCredentials);
        createDevicesThroughGateway(mqttClient);
    }

    @AfterMethod
    public void removeGatewayAndDevices()  {
        testRestClient.deleteDeviceIfExists(this.gatewayDevice.getId());
        createdDevices.forEach(device -> testRestClient.deleteDeviceIfExists(device.getId()));

        this.listener = null;
        this.mqttClient = null;
        this.createdDevices = null;
        if (schedulerExecutor != null) {
            schedulerExecutor.shutdownNow();
        }
    }

    @Test
    public void testSharedAttributesUpdate() throws Exception {
        mqttClient.on("v1/gateway/attributes", listener, MqttQoS.AT_LEAST_ONCE).get();
        // Wait until subscription is processed
        TimeUnit.SECONDS.sleep(3 * timeoutMultiplier);

        //schedule telemetry load every minute for created devices
        schedulerExecutor.scheduleAtFixedRate(this::publishDeviceTelemetry, 0, 1, TimeUnit.MINUTES);

        // Update device attributes every 45 seconds during 1 hour
        long startTime = System.currentTimeMillis();
        long durationInMillis = TimeUnit.HOURS.toMillis(7);

        do {
            updateDeviceAttributes();
            Thread.sleep(45000);
        } while (System.currentTimeMillis() - startTime < durationInMillis);
    }

    private void updateDeviceAttributes()  {
        List<ListenableFuture<ValidatableResponse>> attributesRequests = new ArrayList<>();
        createdDevices.forEach(device -> {
            JsonObject sharedAttributes = new JsonObject();
            sharedAttributes.addProperty("sharedAttr", StringUtils.randomAlphanumeric(8));

            attributesRequests.add(service.submit(() -> testRestClient.postTelemetryAttribute(DEVICE, device.getId(), SHARED_SCOPE, mapper.readTree(sharedAttributes.toString()))));
        });

        try {
            Futures.allAsList(attributesRequests).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        log.info("Checking attributes: ");

        for (int i = 0; i < 5; i++) {
            Awaitility.await().atMost(10, TimeUnit.SECONDS)
                  .until(() -> listener.getEvents().peek(), Objects::nonNull);
            String message = Objects.requireNonNull(listener.getEvents().poll()).getMessage();
            log.info("Found attribute: " + message);
        }
    }

    private void publishDeviceTelemetry() {
        createdDevices.forEach(deviceCred -> {
                mqttClient.publish("v1/gateway/telemetry", Unpooled.wrappedBuffer(createGatewayPayload(deviceCred.getName(), System.currentTimeMillis()).toString().getBytes()));
        });
    }

    private void createDevicesThroughGateway(MqttClient mqttClient) throws Exception {
        if (timeoutMultiplier > 1) {
            TimeUnit.SECONDS.sleep(30);
        }

        for (int i = 0; i < 5; i++) {
            String deviceName = "mqtt_device_" + i + "_"+ RandomStringUtils.randomAlphabetic(5);
            mqttClient.publish("v1/gateway/connect", Unpooled.wrappedBuffer(createGatewayConnectPayload(deviceName).toString().getBytes()), MqttQoS.AT_LEAST_ONCE).get();

            if (timeoutMultiplier > 1) {
                TimeUnit.SECONDS.sleep(30);
            }
        }
        List<EntityRelation> relations = testRestClient.findRelationByFrom(gatewayDevice.getId(), RelationTypeGroup.COMMON);
        assertThat(relations).hasSize(5);

        relations.forEach(entityRelation -> {
            DeviceId createdDeviceId = new DeviceId(entityRelation.getTo().getId());
            createdDevices.add(testRestClient.getDeviceById(createdDeviceId));
        });
    }

    private String getOwnerId() {
        return "Tenant[" + gatewayDevice.getTenantId().getId() + "]MqttGatewayClientTestDevice[" + gatewayDevice.getId().getId() + "]";
    }

    private MqttClient getMqttClient(MqttMessageListener listener, DeviceCredentials deviceCredentials) throws InterruptedException, ExecutionException {
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

    protected JsonObject createPayload() {
        JsonObject values = new JsonObject();
        values.addProperty("stringKey", StringUtils.randomAlphabetic(10));
        values.addProperty("booleanKey", true);
        values.addProperty("doubleKey", StringUtils.randomNumeric(5));
        return values;
    }


}
