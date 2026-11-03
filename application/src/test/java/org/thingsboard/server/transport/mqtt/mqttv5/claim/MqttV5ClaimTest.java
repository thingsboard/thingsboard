// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.mqtt.mqttv5.claim;

import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;

@DaoSqlTest
public class MqttV5ClaimTest extends AbstractMqttV5ClaimTest {

    @Before
    public void beforeTest() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Claim device")
                .build();
        processBeforeTest(configProperties);
    }

    @Test
    public void testClaimingDevice() throws Exception {
        processTestClaimingDevice();
    }
}
