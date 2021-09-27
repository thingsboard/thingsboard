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
package org.thingsboard.server.mqtt;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.thingsboard.server.transport.AbstractTransportObserver;
import org.thingsboard.server.transport.TransportType;
import org.thingsboard.server.WebSocketClientImpl;

import java.util.UUID;

@Component
@ConditionalOnProperty(
        value="mqtt.enabled",
        havingValue = "true")
@Slf4j
public class MqttObserver extends AbstractTransportObserver {

    private static final String DEVICE_TELEMETRY_TOPIC = "v1/devices/me/telemetry";
    private WebSocketClientImpl webSocketClient;

    @Value("${mqtt.monitoring_rate}")
    private int monitoringRate;

    @Value("${mqtt.host}")
    private String mqttUrl;

    @Value("${mqtt.test_device.access_token}")
    private String testDeviceAccessToken;

    @Value("${mqtt.qos}")
    private int qos;

    @Value("${mqtt.timeout}")
    private long timeout;

    @Value("${mqtt.test_device.id}")
    private UUID testDeviceUuid;

    private MqttAsyncClient mqttAsyncClient;

    public MqttObserver() {
        super();
    }

    @Override
    public String pingTransport(String payload) throws Exception {
        if (mqttAsyncClient == null || !mqttAsyncClient.isConnected()) {
            mqttAsyncClient = getMqttAsyncClient();
        }
        webSocketClient = validateWebsocketClient(webSocketClient, testDeviceUuid);

        webSocketClient.registerWaitForUpdate();
        publishMqttMsg(mqttAsyncClient, payload.getBytes());
        return webSocketClient.waitForUpdate(websocketWaitTime);
    }

    @Override
    public int getMonitoringRate() {
        return monitoringRate;
    }

    @Override
    public TransportType getTransportType() {
        return TransportType.MQTT;
    }

    private MqttAsyncClient getMqttAsyncClient() throws MqttException {
        String clientId = MqttAsyncClient.generateClientId();
        MqttAsyncClient client = new MqttAsyncClient(mqttUrl, clientId, new MemoryPersistence());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(testDeviceAccessToken);
        client.connect(options).waitForCompletion(timeout);
        return client;
    }

    private void publishMqttMsg(MqttAsyncClient client, byte[] payload) throws MqttException {
        MqttMessage message = new MqttMessage();
        message.setPayload(payload);
        message.setQos(qos);
        client.publish(MqttObserver.DEVICE_TELEMETRY_TOPIC, message);
    }

}
