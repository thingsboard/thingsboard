// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.mqtt.mqttv3;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttMessage;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class MqttTestSubscribeOnTopicCallback extends MqttTestCallback {

    protected final String awaitSubTopic;

    public MqttTestSubscribeOnTopicCallback(String awaitSubTopic) {
        super();
        this.awaitSubTopic = awaitSubTopic;
    }

    @Override
    public void messageArrived(String requestTopic, MqttMessage mqttMessage) {
        log.warn("messageArrived on topic: {}, awaitSubTopic: {}", requestTopic, awaitSubTopic);
        if (awaitSubTopic.equals(requestTopic)) {
            messageArrivedQoS = mqttMessage.getQos();
            payloadBytes = mqttMessage.getPayload();
            subscribeLatch.countDown();
        }
    }

}
