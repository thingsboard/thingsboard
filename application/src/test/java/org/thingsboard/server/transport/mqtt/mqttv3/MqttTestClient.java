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
package org.thingsboard.server.transport.mqtt.mqttv3;

import io.netty.handler.codec.mqtt.MqttQoS;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.thingsboard.server.common.data.StringUtils;

import java.util.concurrent.TimeUnit;

public class MqttTestClient {

    private static final String MQTT_URL = "tcp://localhost:1883";
    private static final int TIMEOUT = 30; // seconds
    public static final long TIMEOUT_MS = TimeUnit.SECONDS.toMillis(TIMEOUT);

    private final MqttAsyncClient client;

    public void setCallback(MqttTestCallback callback) {
        client.setCallback(callback);
    }

    public MqttTestClient() throws MqttException {
        this.client = createClient();
    }

    public MqttTestClient(String clientId) throws MqttException {
        this.client = createClient(clientId);
    }

    public void connectAndWait(String userName, String password) throws MqttException {
        IMqttToken connect = connect(userName, password);
        connect.waitForCompletion(TIMEOUT_MS);
    }

    public void connectAndWait(String userName) throws MqttException {
        connectAndWait(userName, null);
    }

    public void connectAndWait() throws MqttException {
        connectAndWait(null, null);
    }

    private IMqttToken connect(String userName, String password) throws MqttException {
        if (client == null) {
            throw new RuntimeException("Failed to connect! MqttAsyncClient is not initialized!");
        }
        MqttConnectOptions options = new MqttConnectOptions();
        if (StringUtils.isNotEmpty(userName)) {
            options.setUserName(userName);
        }
        if (StringUtils.isNotEmpty(password)) {
            options.setPassword(password.toCharArray());
        }
        return client.connect(options);
    }

    public void disconnectAndWait() throws MqttException {
        disconnect().waitForCompletion(TIMEOUT_MS);
    }

    public IMqttToken disconnect() throws MqttException {
        return client.disconnect();
    }

    public void disconnectForcibly() throws MqttException {
        client.disconnectForcibly(TIMEOUT_MS);
    }

    public void publishAndWait(String topic, byte[] payload) throws MqttException {
        publish(topic, payload).waitForCompletion(TIMEOUT_MS);
    }

    public IMqttDeliveryToken publish(String topic, byte[] payload) throws MqttException {
        MqttMessage message = new MqttMessage();
        message.setPayload(payload);
        return client.publish(topic, message);
    }

    public void subscribeAndWait(String topic, MqttQoS qoS) throws MqttException {
        subscribe(topic, qoS).waitForCompletion(TIMEOUT_MS);
    }

    public IMqttToken subscribe(String topic, MqttQoS qoS) throws MqttException {
        return client.subscribe(topic, qoS.value());
    }

    public boolean isConnected() {
        return client.isConnected();
    }

    public void enableManualAcks() {
        client.setManualAcks(true);
    }

    public void messageArrivedComplete(MqttMessage mqttMessage) throws MqttException {
        client.messageArrivedComplete(mqttMessage.getId(), mqttMessage.getQos());
    }

    private MqttAsyncClient createClient() throws MqttException {
        return createClient(null);
    }

    private MqttAsyncClient createClient(String clientId) throws MqttException {
        if (StringUtils.isEmpty(clientId)) {
            clientId = MqttAsyncClient.generateClientId();
        }
        return new MqttAsyncClient(MQTT_URL, clientId, new MemoryPersistence());
    }

}
