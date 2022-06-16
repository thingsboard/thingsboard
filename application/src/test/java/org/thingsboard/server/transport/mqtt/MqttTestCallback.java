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
package org.thingsboard.server.transport.mqtt;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.IMqttDeliveryToken;
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
public class MqttTestCallback implements MqttCallback {

    protected CountDownLatch subscribeLatch;
    protected final CountDownLatch deliveryLatch;
    protected int qoS;
    protected byte[] payloadBytes;
    protected String awaitSubTopic;
    protected boolean pubAckReceived;

    public MqttTestCallback() {
        this.subscribeLatch = new CountDownLatch(1);
        this.deliveryLatch = new CountDownLatch(1);
    }

    public MqttTestCallback(int subscribeCount) {
        this.subscribeLatch = new CountDownLatch(subscribeCount);
        this.deliveryLatch = new CountDownLatch(1);
    }

    public MqttTestCallback(String awaitSubTopic) {
        this.subscribeLatch = new CountDownLatch(1);
        this.deliveryLatch = new CountDownLatch(1);
        this.awaitSubTopic = awaitSubTopic;
    }

    @Override
    public void disconnected(MqttDisconnectResponse disconnectResponse) {
        log.warn("disconnected: ", disconnectResponse.getException());
        deliveryLatch.countDown();
    }

    @Override
    public void mqttErrorOccurred(MqttException exception) {
        log.warn("mqttErrorOccurred: ", exception);
    }

    @Override
    public void messageArrived(String requestTopic, MqttMessage mqttMessage) {
        if (awaitSubTopic == null) {
            log.warn("messageArrived on topic: {}", requestTopic);
            qoS = mqttMessage.getQos();
            payloadBytes = mqttMessage.getPayload();
            subscribeLatch.countDown();
        } else {
            messageArrivedOnAwaitSubTopic(requestTopic, mqttMessage);
        }
    }

    @Override
    public void deliveryComplete(IMqttToken token) {
        log.warn("deliveryComplete: {}", token.getResponse());
        pubAckReceived = token.getResponse().getType() == MqttWireMessage.MESSAGE_TYPE_PUBACK;
        deliveryLatch.countDown();
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        log.warn("connectComplete: reconnect[{}], server uri[{}]", reconnect, serverURI);
    }

    @Override
    public void authPacketArrived(int reasonCode, MqttProperties properties) {
        log.warn("authPacketArrived: reason code[{}], mqtt properties[{}]", reasonCode, properties);
    }

    protected void messageArrivedOnAwaitSubTopic(String requestTopic, MqttMessage mqttMessage) {
        log.warn("messageArrived on topic: {}, awaitSubTopic: {}", requestTopic, awaitSubTopic);
        if (awaitSubTopic.equals(requestTopic)) {
            qoS = mqttMessage.getQos();
            payloadBytes = mqttMessage.getPayload();
            subscribeLatch.countDown();
        }
    }
}
