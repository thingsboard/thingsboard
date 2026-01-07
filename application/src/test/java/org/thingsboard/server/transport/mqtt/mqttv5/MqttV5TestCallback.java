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

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;

import java.util.concurrent.CountDownLatch;

@Slf4j
@Data
public class MqttV5TestCallback implements MqttCallback {

    protected CountDownLatch subscribeLatch;
    protected final CountDownLatch deliveryLatch;
    protected int qoS;
    protected byte[] payloadBytes;
    protected String awaitSubTopic;
    protected boolean pubAckReceived;
    protected MqttMessage lastReceivedMessage;

    public MqttV5TestCallback() {
        this.subscribeLatch = new CountDownLatch(1);
        this.deliveryLatch = new CountDownLatch(1);
    }

    public MqttV5TestCallback(int subscribeCount) {
        this.subscribeLatch = new CountDownLatch(subscribeCount);
        this.deliveryLatch = new CountDownLatch(1);
    }

    public MqttV5TestCallback(String awaitSubTopic) {
        this.subscribeLatch = new CountDownLatch(1);
        this.deliveryLatch = new CountDownLatch(1);
        this.awaitSubTopic = awaitSubTopic;
    }

    @Override
    public void disconnected(MqttDisconnectResponse mqttDisconnectResponse) {
        if (mqttDisconnectResponse.getException() != null) {
            log.warn("connectionLost: ", mqttDisconnectResponse.getException());
            deliveryLatch.countDown();
        }
        log.warn("Disconnected with reason: {}", mqttDisconnectResponse.getReasonString());
    }

    @Override
    public void mqttErrorOccurred(MqttException e) {
        log.warn("Error occurred:", e);
    }

    @Override
    public void messageArrived(String requestTopic, MqttMessage mqttMessage) {
        lastReceivedMessage = mqttMessage;
        if (awaitSubTopic == null) {
            log.warn("messageArrived on topic: {}", requestTopic);
            qoS = mqttMessage.getQos();
            payloadBytes = mqttMessage.getPayload();
            subscribeLatch.countDown();
        } else {
            messageArrivedOnAwaitSubTopic(requestTopic, mqttMessage);
        }
    }

    protected void messageArrivedOnAwaitSubTopic(String requestTopic, MqttMessage mqttMessage) {
        log.warn("messageArrived on topic: {}, awaitSubTopic: {}", requestTopic, awaitSubTopic);
        if (awaitSubTopic.equals(requestTopic)) {
            qoS = mqttMessage.getQos();
            payloadBytes = mqttMessage.getPayload();
            subscribeLatch.countDown();
        }
    }

    @Override
    public void deliveryComplete(IMqttToken iMqttToken) {
        log.warn("delivery complete: {}", iMqttToken.getResponse());
        pubAckReceived = iMqttToken.getResponse().getType() == MqttWireMessage.MESSAGE_TYPE_PUBACK;
        deliveryLatch.countDown();
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        log.warn("Connect completed: reconnect - {}, serverURI - {}", reconnect, serverURI);
    }

    @Override
    public void authPacketArrived(int reasonCode, MqttProperties mqttProperties) {
        log.warn("Auth package received: reasonCode - {}, mqtt properties - {}", reasonCode, mqttProperties);
    }
}
