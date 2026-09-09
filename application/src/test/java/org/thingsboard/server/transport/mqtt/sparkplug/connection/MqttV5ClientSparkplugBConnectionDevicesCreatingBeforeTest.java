// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.mqtt.sparkplug.connection;

import org.eclipse.paho.mqttv5.common.MqttException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.dao.service.DaoSqlTest;

/**
 * Created by nickAS21 on 16.02.26
 */
@DaoSqlTest
public class MqttV5ClientSparkplugBConnectionDevicesCreatingBeforeTest extends AbstractMqttV5ClientSparkplugConnectionTest {

    /**
     * String deviceName_1 = deviceId + "_1"; Only name device. Without a complete topic: how it was in the old version.
     * String deviceName_2 = groupId + DEVICE_NAME_SPLIT_REGEXP + edgeNode + DEVICE_NAME_SPLIT_REGEXP + deviceId + "_2"; With complete topic: how it was in the new version.
     * @throws Exception
     */
    @Before
    public void beforeTest() throws Exception {
        beforeSparkplugTest();
        seedLegacyAndFullPathDevices();
    }

    @After
    public void afterTest() throws MqttException {
        if (client.isConnected()) {
            client.disconnect();
        }
    }

    @Test
    public void testClientWithCorrectAccessTokenWithNDEATHTwoDevicesCreatingBeforeFirstNameDeviceIdSecondNameFull() throws Exception {
        connectClientWithCorrectAccessTokenWithNDEATHDevicesCreatingBefore_Test(2);
    }

    @Test
    public void testRenameWhenDeviceFullPathAlreadyExists_Collision() throws Exception {
        renameCollisionWhenTargetNameAlreadyExists_Test();
    }

    @Test
    public void testUnauthorizedRenameAttempt() throws Exception {
        unauthorizedRenameAttempt_Test();
    }

    @Test
    public void testConcurrentFirstMessageRegistration() throws Exception {
        concurrentFirstMessageRegistration_Test();
    }

    @Test
    public void testSparkplugSessionStaysAliveWithZeroMsgId() throws Exception {
        sparkplugSessionStaysAliveWithZeroMsgId_Test();
    }
}
