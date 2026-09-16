// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.mqtt.mqttv5.client.unsubscribe;

import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;

@DaoSqlTest
public class MqttV5ClientUnsubscribeTest extends AbstractMqttV5ClientUnsubscribeTest {

    @Before
    public void beforeTest() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test MqttV5 client device")
                .build();
        processBeforeTest(configProperties);
    }

    @Test
    public void testClientUnsubscribeFromCorrectTopic() throws Exception {
        processClientUnsubscribeFromCorrectTopicTest();
    }

    @Test
    public void testClientUnsubscribeWithoutSubscribeTopic() throws Exception {
        processClientUnsubscribeWithoutSubscribeTopicTest();
    }

}
