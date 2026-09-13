// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.mqtt.sparkplug.connection;

import org.eclipse.paho.mqttv5.common.MqttException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.dao.service.DaoSqlTest;

/**
 * Created by nickAS21 on 12.01.23
 */
@DaoSqlTest
public class MqttV5ClientSparkplugBConnectionTest extends AbstractMqttV5ClientSparkplugConnectionTest {

    @Before
    public void beforeTest() throws Exception {
        beforeSparkplugTest();
    }

    @After
    public void afterTest() throws MqttException {
        if (client.isConnected()) {
            client.disconnect();
        }
    }

    @Test
    public void testClientWithCorrectAccessTokenWithNDEATH() throws Exception {
        processClientWithCorrectNodeAccessTokenWithNDEATH_Test();
    }

    @Test
    public void testClientWithCorrectNodeAccessTokenWithoutNDEATH() throws Exception {
        processClientWithCorrectNodeAccessTokenWithoutNDEATH_Test();
    }

    @Test
    public void testClientWithCorrectNodeAccessTokenNameSpaceInvalid() throws Exception {
        processClientWithCorrectNodeAccessTokenNameSpaceInvalid_Test();
    }

    @Test
    public void testClientWithCorrectAccessTokenWithNDEATHCreatedOneDevice() throws Exception {
        processClientWithCorrectAccessTokenWithNDEATHCreatedDevices(1);
    }

    @Test
    public void testClientWithCorrectAccessTokenWithNDEATHCreatedTwoDevice() throws Exception {
        processClientWithCorrectAccessTokenWithNDEATHCreatedDevices(2);
    }

    @Test
    public void testClientWithCorrectAccessTokenWithNDEATH_State_ONLINE_ALL() throws Exception {
        processConnectClientWithCorrectAccessTokenWithNDEATH_State_ONLINE_ALL(3);
    }

    @Test
    public void testConnectClientWithCorrectAccessTokenWithNDEATH_State_ONLINE_All_Then_OneDeviceOFFLINE() throws Exception {
        processConnectClientWithCorrectAccessTokenWithNDEATH_State_ONLINE_All_Then_OneDeviceOFFLINE(3, 1);
    }

    @Test
    public void testConnectClientWithCorrectAccessTokenWithNDEATH_State_ONLINE_All_Then_OFFLINE_All() throws Exception {
        processConnectClientWithCorrectAccessTokenWithNDEATH_State_ONLINE_All_Then_OFFLINE_All(3);
    }

}
