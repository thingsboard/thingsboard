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
