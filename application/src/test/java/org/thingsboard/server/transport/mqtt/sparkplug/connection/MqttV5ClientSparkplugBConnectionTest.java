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
