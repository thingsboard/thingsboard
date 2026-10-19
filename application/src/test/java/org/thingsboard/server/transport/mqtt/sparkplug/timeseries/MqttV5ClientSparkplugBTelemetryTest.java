// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.mqtt.sparkplug.timeseries;

import org.eclipse.paho.mqttv5.common.MqttException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.dao.service.DaoSqlTest;

/**
 * Created by nickAS21 on 12.01.23
 */
@DaoSqlTest
public class MqttV5ClientSparkplugBTelemetryTest extends AbstractMqttV5ClientSparkplugTelemetryTest {

    @Before
    public void beforeTest() throws Exception {
        beforeSparkplugTest();
    }

    @After
    public void afterTest () throws MqttException {
        if (client.isConnected()) {
            client.disconnect();        }
    }

    @Test
    public void testClientWithCorrectAccessTokenPublishNBIRTH() throws Exception {
        processClientWithCorrectAccessTokenPublishNBIRTH();
    }

    @Test
    public void testClientWithCorrectAccessTokenPushNodeMetricBuildPrimitiveSimple() throws Exception {
        processClientWithCorrectAccessTokenPushNodeMetricBuildPrimitiveSimple();
    }

    @Test
    public void testClientWithCorrectAccessTokenPushNodeMetricBuildPArraysPrimitiveSimple() throws Exception {
        processClientWithCorrectAccessTokenPushNodeMetricBuildArraysPrimitiveSimple();
    }

}