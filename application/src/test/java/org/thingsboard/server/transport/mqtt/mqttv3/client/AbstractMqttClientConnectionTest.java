// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.mqtt.mqttv3.client;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.Assert;
import org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestClient;

public abstract class AbstractMqttClientConnectionTest extends AbstractMqttIntegrationTest {

    protected void processClientWithCorrectAccessTokenTest() throws Exception {
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(accessToken);
        Assert.assertTrue(client.isConnected());
        client.disconnect();
    }

    protected void processClientWithWrongAccessTokenTest() throws Exception {
        MqttTestClient client = new MqttTestClient();
        try {
            client.connectAndWait("wrongAccessToken");
        } catch (MqttException e) {
            Assert.assertEquals(MqttException.REASON_CODE_NOT_AUTHORIZED, e.getReasonCode());
        }
    }

    protected void processClientWithWrongClientIdAndEmptyUsernamePasswordTest() throws Exception {
        MqttTestClient client = new MqttTestClient("unknownClientId");
        try {
            client.connectAndWait();
        } catch (MqttException e) {
            Assert.assertEquals(MqttException.REASON_CODE_INVALID_CLIENT_ID, e.getReasonCode());
        }
    }

}
