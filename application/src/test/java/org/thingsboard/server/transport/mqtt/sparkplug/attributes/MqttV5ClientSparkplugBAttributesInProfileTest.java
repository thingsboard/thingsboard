// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.mqtt.sparkplug.attributes;

import org.eclipse.paho.mqttv5.common.MqttException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.HashSet;

/**
 * Created by nickAS21 on 12.01.23
 */
@DaoSqlTest
public class MqttV5ClientSparkplugBAttributesInProfileTest extends AbstractMqttV5ClientSparkplugAttributesTest {

    @Before
    public void beforeTest() throws Exception {
        sparkplugAttributesMetricNames = new HashSet<>();
        sparkplugAttributesMetricNames.add(metricBirthName_Int32);
        beforeSparkplugTest();
    }

    @After
    public void afterTest () throws MqttException {
        if (client.isConnected()) {
            client.disconnect();        }
    }

    @Test
    public void testClientNodeWithCorrectAccessTokenPublish_AttributesInProfileContainsKeyAttributes() throws Exception {
        processClientNodeWithCorrectAccessTokenPublish_AttributesInProfileContainsKeyAttributes();
    }

    @Test
    public void testClientDeviceWithCorrectAccessTokenPublish_AttributesInProfileContainsKeyAttributes() throws Exception {
        processClientDeviceWithCorrectAccessTokenPublish_AttributesInProfileContainsKeyAttributes();
    }

}
