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
package org.thingsboard.server.transport.mqtt.mqttv5;

import io.netty.handler.codec.mqtt.MqttQoS;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.thingsboard.server.common.data.StringUtils;

import java.util.concurrent.TimeUnit;

public class MqttV5TestClient { // We should copy part of MqttV3TestClient, due to different package names in import

    private static final String MQTT_URL = "tcp://localhost:1883";
    private static final int TIMEOUT = 30; // seconds
    private static final long TIMEOUT_MS = TimeUnit.SECONDS.toMillis(TIMEOUT);

    private final MqttAsyncClient client;

    public void setCallback(MqttCallback callback) {
        client.setCallback(callback);
    }

    public MqttV5TestClient() throws MqttException {
        this.client = createClient();
    }

    public MqttV5TestClient(String clientId) throws MqttException {
        this.client = createClient(clientId);
    }

    public MqttV5TestClient(boolean generateClientId) throws MqttException {
        this.client = createClient(generateClientId);
    }

    public IMqttToken connectAndWait(String userName, String password) throws MqttException {
        IMqttToken connect = connect(userName, password);
        connect.waitForCompletion(TIMEOUT_MS);
        return connect;
    }

    public IMqttToken connectAndWait(String userName) throws MqttException {
        return connectAndWait(userName, null);
    }

    public IMqttToken connectAndWait() throws MqttException {
        return connectAndWait(null, null);
    }

    public IMqttToken connectAndWait(MqttConnectionOptions options) throws MqttException {
        IMqttToken iMqttToken = connect(options);
        iMqttToken.waitForCompletion(TIMEOUT_MS);
        return iMqttToken;
    }

    private IMqttToken connect(String userName, String password) throws MqttException {
        if (client == null) {
            throw new RuntimeException("Failed to connect! MqttAsyncClient is not initialized!");
        }
        MqttConnectionOptions options = new MqttConnectionOptions();
        if (StringUtils.isNotEmpty(userName)) {
            options.setUserName(userName);
        }
        if (StringUtils.isNotEmpty(password)) {
            options.setPassword(password.getBytes());
        }
        return client.connect(options);
    }

    public IMqttToken connect(MqttConnectionOptions options) throws MqttException {
        if (client == null) {
            throw new RuntimeException("Failed to connect! MqttAsyncClient is not initialized!");
        }
        IMqttToken connect = client.connect(options);
        connect.waitForCompletion(TIMEOUT_MS);
        return connect;
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

    public IMqttToken publishAndWait(String topic, byte[] payload) throws MqttException {
        IMqttToken iMqttToken = publish(topic, payload);
        iMqttToken.waitForCompletion(TIMEOUT_MS);
        return iMqttToken;
    }

    public IMqttToken publish(String topic, byte[] payload) throws MqttException {
        MqttMessage message = new MqttMessage();
        message.setPayload(payload);
        return publish(topic, message);
    }

    public IMqttToken publish(String topic, MqttMessage message) throws MqttException {
        return publish(topic, message.getPayload(), message.getQos(), message.isRetained());
    }

    public IMqttToken publish(String topic, byte[] payload, int qos, boolean retain) throws MqttException {
        return client.publish(topic, payload, qos, retain);
    }

    public IMqttToken subscribeAndWait(String topic, MqttQoS qoS) throws MqttException {
        IMqttToken iMqttToken = subscribe(topic, qoS);
        iMqttToken.waitForCompletion(TIMEOUT_MS);
        return iMqttToken;
    }

    public IMqttToken subscribe(String topic, MqttQoS qoS) throws MqttException {
        return client.subscribe(topic, qoS.value());
    }

    public IMqttToken unsubscribeAndWait(String topic) throws MqttException {
        IMqttToken iMqttToken = unsubscribe(topic);
        iMqttToken.waitForCompletion(TIMEOUT_MS);
        return iMqttToken;
    }

    public IMqttToken unsubscribe(String topic) throws MqttException {
        return client.unsubscribe(topic);
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
        return createClient(true);
    }

    private MqttAsyncClient createClient(boolean generateClientId) throws MqttException {
        String clientId = null;
        if (generateClientId) {
            clientId = "test" + System.nanoTime();
        }
        return createClient(clientId);
    }

    private MqttAsyncClient createClient(String clientId) throws MqttException {
        return new MqttAsyncClient(MQTT_URL, clientId, new MemoryPersistence());
    }

}
