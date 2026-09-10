// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.mqtt.mqttv3.client;

import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;

@DaoSqlTest
public class MqttClientConnectionTest extends AbstractMqttClientConnectionTest {

    @Before
    public void beforeTest() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test MqttV5 client device")
                .build();
        processBeforeTest(configProperties);
    }

    @Test
    public void testClientWithCorrectAccessToken() throws Exception {
        processClientWithCorrectAccessTokenTest();
    }

    @Test
    public void testClientWithWrongAccessToken() throws Exception {
        processClientWithWrongAccessTokenTest();
    }

    @Test
    public void testClientWithWrongClientIdAndEmptyUsernamePassword() throws Exception {
        processClientWithWrongClientIdAndEmptyUsernamePasswordTest();
    }
}
