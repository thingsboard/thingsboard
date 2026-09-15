// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
