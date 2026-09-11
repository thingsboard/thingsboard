// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.mqtt.mqttv5.attributes.updates;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;
import org.thingsboard.server.transport.mqtt.mqttv5.attributes.AbstractAttributesMqttV5Test;

@Slf4j
@DaoSqlTest
public class AttributesUpdatesTest extends AbstractAttributesMqttV5Test {

    @Before
    public void beforeTest() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Subscribe to attribute updates")
                .transportPayloadType(TransportPayloadType.JSON)
                .build();
        processBeforeTest(configProperties);
    }

    @Test
    public void testAttributeMqttV5SimpleClientUpdates() throws Exception {
        processAttributesUpdatesTest();
    }
}
