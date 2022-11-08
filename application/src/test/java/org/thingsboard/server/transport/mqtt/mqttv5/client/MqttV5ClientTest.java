package org.thingsboard.server.transport.mqtt.mqttv5.client;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;

@DaoSqlTest
public class MqttV5ClientTest extends AbstractMqttV5ClientTest {

    @Before
    public void beforeTest() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test MqttV5 client device")
                .build();
        processBeforeTest(configProperties);
    }

    @Test
    @Ignore("Not implemented on the server.")
    public void testClientWithPacketSizeLimitation() throws Exception {
        processClientWithPacketSizeLimitationTest();
    }
}
