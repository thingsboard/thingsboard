// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.mqtt.mqttv5.client.connection;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;

@DaoSqlTest
public class MqttV5ClientConnectionTest extends AbstractMqttV5ClientConnectionTest {

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

    @Test
    public void testClientWithNoCredentialsTest() throws Exception {
        processClientWithNoCredentialsTest();
    }

    @Test
    @Ignore("Not implemented on the server.")
    public void testClientWithPacketSizeLimitation() throws Exception {
        processClientWithPacketSizeLimitationTest();
    }
}
