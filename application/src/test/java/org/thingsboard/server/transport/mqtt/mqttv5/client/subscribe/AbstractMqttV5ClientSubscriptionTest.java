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
package org.thingsboard.server.transport.mqtt.mqttv5.client.subscribe;

import io.netty.handler.codec.mqtt.MqttQoS;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.common.packet.MqttReturnCode;
import org.eclipse.paho.mqttv5.common.packet.MqttSubAck;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;
import org.junit.Assert;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest;
import org.thingsboard.server.transport.mqtt.mqttv5.MqttV5TestClient;

import static org.eclipse.paho.mqttv5.common.packet.MqttWireMessage.MESSAGE_TYPE_SUBACK;

public abstract class AbstractMqttV5ClientSubscriptionTest extends AbstractMqttIntegrationTest {

    protected void processClientSubscriptionToCorrectTopicTest() throws Exception {
        MqttV5TestClient client = new MqttV5TestClient();
        client.connectAndWait(accessToken);

        IMqttToken subscriptionResult = client.subscribeAndWait(MqttTopics.DEVICE_ATTRIBUTES_TOPIC, MqttQoS.AT_MOST_ONCE);
        awaitForDeviceActorToReceiveSubscription(savedDevice.getId(), FeatureType.ATTRIBUTES, 1);

        MqttWireMessage response = subscriptionResult.getResponse();

        Assert.assertEquals(MESSAGE_TYPE_SUBACK, response.getType());

        MqttSubAck subAckMsg = (MqttSubAck) response;

        Assert.assertEquals(1, subAckMsg.getReturnCodes().length);
        Assert.assertEquals(MqttReturnCode.RETURN_CODE_SUCCESS, subAckMsg.getReturnCodes()[0]);

        client.disconnect();
    }

    protected void processClientSubscriptionToWrongTopicTest() throws Exception {
        MqttV5TestClient client = new MqttV5TestClient();
        client.connectAndWait(accessToken);

        IMqttToken iMqttToken = client.subscribeAndWait("wrong/topic/+", MqttQoS.AT_MOST_ONCE);
        awaitForDeviceActorToReceiveSubscription(savedDevice.getId(), FeatureType.ATTRIBUTES, 0);
        Assert.assertEquals(MESSAGE_TYPE_SUBACK,iMqttToken.getResponse().getType());
        MqttSubAck subAck = (MqttSubAck) iMqttToken.getResponse();
        Assert.assertEquals(1, subAck.getReturnCodes().length);
        Assert.assertEquals(MqttReturnCode.RETURN_CODE_TOPIC_FILTER_NOT_VALID, subAck.getReturnCodes()[0]);

        client.disconnect();
    }

}
