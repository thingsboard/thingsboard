// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.mqtt.mqttv5.rpc;

import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;

@DaoSqlTest
public class MqttV5RpcTest extends AbstractMqttV5RpcTest {

    @Before
    public void beforeTest() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("RPC test device")
                .build();
        processBeforeTest(configProperties);
    }

    @Test
    public void testServerMqttV5SimpleClientOneWayRpc() throws Exception {
        processOneWayRpcTest();
    }

    @Test
    public void testServerMqttV5SimpleClientTwoWayRpc() throws Exception {
        processJsonTwoWayRpcTest();
    }
}
