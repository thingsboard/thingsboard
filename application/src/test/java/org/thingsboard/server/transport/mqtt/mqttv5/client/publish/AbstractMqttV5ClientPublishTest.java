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
package org.thingsboard.server.transport.mqtt.mqttv5.client.publish;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.common.packet.MqttPubAck;
import org.eclipse.paho.mqttv5.common.packet.MqttReturnCode;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;
import org.junit.Assert;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest;
import org.thingsboard.server.transport.mqtt.mqttv5.MqttV5TestClient;

import static org.eclipse.paho.mqttv5.common.packet.MqttWireMessage.MESSAGE_TYPE_PUBACK;

public abstract class AbstractMqttV5ClientPublishTest extends AbstractMqttIntegrationTest {

    protected static final String PAYLOAD_VALUES_STR = "{\"key1\":\"value1\", \"key2\":true, \"key3\": 3.0, \"key4\": 4," +
            " \"key5\": {\"someNumber\": 42, \"someArray\": [1,2,3], \"someNestedObject\": {\"key\": \"value\"}}}";

    protected static final String INVALID_PAYLOAD_VALUES_STR = "\"key1\":\"value1\", \"key2\":true, \"key3\": 3.0, \"key4\": 4," +
            " \"key5\": {\"someNumber\": 42, \"someArray\": [1,2,3], \"someNestedObject\": {\"key\": \"value\"}}}";

    protected void processClientPublishToCorrectTopicTest() throws Exception {
        MqttV5TestClient client = new MqttV5TestClient();
        client.connectAndWait(accessToken);

        IMqttToken publishResult = client.publishAndWait(MqttTopics.DEVICE_ATTRIBUTES_TOPIC, PAYLOAD_VALUES_STR.getBytes());
        MqttWireMessage response = publishResult.getResponse();

        Assert.assertEquals(MESSAGE_TYPE_PUBACK, response.getType());

        MqttPubAck pubAckMsg = (MqttPubAck) response;

        Assert.assertEquals(MqttReturnCode.RETURN_CODE_SUCCESS, pubAckMsg.getReturnCode());

        client.disconnect();
    }

    protected void processClientPublishToWrongTopicTest() throws Exception {
        MqttV5TestClient client = new MqttV5TestClient();
        client.connectAndWait(accessToken);

        IMqttToken iMqttToken = client.publishAndWait("wrong/topic/", PAYLOAD_VALUES_STR.getBytes());
        Assert.assertEquals(MESSAGE_TYPE_PUBACK,iMqttToken.getResponse().getType());
        MqttPubAck pubAck = (MqttPubAck) iMqttToken.getResponse();
        Assert.assertEquals(MqttReturnCode.RETURN_CODE_TOPIC_NAME_INVALID, pubAck.getReturnCode());

        client.disconnect();
    }

    protected void processClientPublishWithInvalidPayloadTest() throws Exception {
        MqttV5TestClient client = new MqttV5TestClient();
        client.connectAndWait(accessToken);

        IMqttToken iMqttToken = client.publishAndWait(MqttTopics.DEVICE_ATTRIBUTES_TOPIC, INVALID_PAYLOAD_VALUES_STR.getBytes());
        Assert.assertEquals(MESSAGE_TYPE_PUBACK,iMqttToken.getResponse().getType());
        MqttPubAck pubAck = (MqttPubAck) iMqttToken.getResponse();
        Assert.assertEquals(MqttReturnCode.RETURN_CODE_PAYLOAD_FORMAT_INVALID, pubAck.getReturnCode());

        client.disconnect();
    }

}
