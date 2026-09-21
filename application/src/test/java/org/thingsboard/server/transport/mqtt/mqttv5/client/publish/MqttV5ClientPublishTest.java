// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.mqtt.mqttv5.client.publish;

import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;

@DaoSqlTest
public class MqttV5ClientPublishTest extends AbstractMqttV5ClientPublishTest {

    @Before
    public void beforeTest() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test MqttV5 client device")
                .build();
        processBeforeTest(configProperties);
    }

    @Test
    public void testClientPublishToCorrectTopic() throws Exception {
        processClientPublishToCorrectTopicTest();
    }

    @Test
    public void testClientPublishToWrongTopic() throws Exception {
        processClientPublishToWrongTopicTest();
    }

    @Test
    public void testClientPublishWithInvalidPayload() throws Exception {
        processClientPublishWithInvalidPayloadTest();
    }

}
