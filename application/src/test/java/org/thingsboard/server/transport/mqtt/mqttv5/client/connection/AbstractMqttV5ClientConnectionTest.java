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
package org.thingsboard.server.transport.mqtt.mqttv5.client.connection;

import io.netty.handler.codec.mqtt.MqttQoS;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.packet.MqttConnAck;
import org.eclipse.paho.mqttv5.common.packet.MqttReturnCode;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;
import org.junit.Assert;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest;
import org.thingsboard.server.transport.mqtt.mqttv5.MqttV5TestCallback;
import org.thingsboard.server.transport.mqtt.mqttv5.MqttV5TestClient;

import java.util.concurrent.TimeUnit;

import static org.eclipse.paho.mqttv5.common.packet.MqttWireMessage.MESSAGE_TYPE_CONNACK;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class AbstractMqttV5ClientConnectionTest extends AbstractMqttIntegrationTest {

    protected void processClientWithCorrectAccessTokenTest() throws Exception {
        MqttV5TestClient client = new MqttV5TestClient();
        IMqttToken connectionResult = client.connectAndWait(accessToken);
        MqttWireMessage response = connectionResult.getResponse();

        Assert.assertEquals(MESSAGE_TYPE_CONNACK, response.getType());

        MqttConnAck connAckMsg = (MqttConnAck) response;

        Assert.assertEquals(MqttReturnCode.RETURN_CODE_SUCCESS, connAckMsg.getReturnCode());
        client.disconnect();
    }

    protected void processClientWithWrongAccessTokenTest() throws Exception {
        MqttV5TestClient client = new MqttV5TestClient();
        try {
            client.connectAndWait("wrongAccessToken");
        } catch (MqttException e) {
            Assert.assertEquals(MqttReturnCode.RETURN_CODE_BAD_USERNAME_OR_PASSWORD, e.getReasonCode());
        }
    }

    protected void processClientWithWrongClientIdAndEmptyUsernamePasswordTest() throws Exception {
        MqttV5TestClient client = new MqttV5TestClient("unknownClientId");
        try {
            client.connectAndWait();
        } catch (MqttException e) {
            Assert.assertEquals(MqttReturnCode.RETURN_CODE_IDENTIFIER_NOT_VALID, e.getReasonCode());
        }
    }

    protected void processClientWithNoCredentialsTest() throws Exception {
        MqttV5TestClient client = new MqttV5TestClient(false);
        try {
            client.connectAndWait();
        } catch (MqttException e) {
            Assert.assertEquals(MqttReturnCode.RETURN_CODE_NOT_AUTHORIZED, e.getReasonCode());
        }
    }

    protected void processClientWithPacketSizeLimitationTest() throws Exception {
        int packetSizeLimit = 99;
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setMaximumPacketSize((long) packetSizeLimit);
        options.setUserName(accessToken);

        MqttV5TestClient client = new MqttV5TestClient();
        client.connectAndWait(options);

        MqttV5TestCallback possibleSizeCallback = updateAttributeWithStringValue(client, packetSizeLimit / 2);

        Assert.assertTrue("Server should send messages if size less then limitation.", possibleSizeCallback.getPayloadBytes().length < packetSizeLimit);

        MqttV5TestCallback bigMessageCallback = updateAttributeWithStringValue(client, packetSizeLimit * 2);

        Assert.assertNull("Server should not send a message if the message size bigger then set limit.", bigMessageCallback.getLastReceivedMessage());

        client.disconnect();

    }

    private MqttV5TestCallback updateAttributeWithStringValue(MqttV5TestClient client, int valueLen) throws Exception {
        MqttV5TestCallback onUpdateCallback = new MqttV5TestCallback();
        client.setCallback(onUpdateCallback);
        client.subscribeAndWait(MqttTopics.DEVICE_ATTRIBUTES_TOPIC, MqttQoS.AT_MOST_ONCE);
        awaitForDeviceActorToReceiveSubscription(savedDevice.getId(), FeatureType.ATTRIBUTES, 1);

        String payload = "{\"sharedStr\":\"" + StringUtils.repeat("*", valueLen) + "\"}";

        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", payload, String.class, status().isOk());
        onUpdateCallback.getSubscribeLatch().await(3, TimeUnit.SECONDS);
        return onUpdateCallback;
    }
}
