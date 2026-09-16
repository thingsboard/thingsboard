// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.mqtt.mqttv3.telemetry.attributes;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;

import java.util.Arrays;
import java.util.List;

@Slf4j
@DaoSqlTest
public class MqttAttributesJsonIntegrationTest extends MqttAttributesIntegrationTest {

    private static final String POST_DATA_ATTRIBUTES_TOPIC = "data/attributes";

    @Before
    public void beforeTest() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Post Attributes device")
                .gatewayName("Test Post Attributes gateway")
                .transportPayloadType(TransportPayloadType.JSON)
                .attributesTopicFilter(POST_DATA_ATTRIBUTES_TOPIC)
                .build();
        processBeforeTest(configProperties);
    }

    @Test
    public void testPushAttributes() throws Exception {
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        processJsonPayloadAttributesTest(POST_DATA_ATTRIBUTES_TOPIC, expectedKeys, PAYLOAD_VALUES_STR.getBytes());
    }

    @Test
    public void testPushAttributesOnShortTopic() throws Exception {
        super.testPushAttributesOnShortTopic();
    }

    @Test
    public void testPushAttributesOnShortJsonTopic() throws Exception {
        super.testPushAttributesOnShortJsonTopic();
    }

    @Test
    public void testPushAttributesGateway() throws Exception {
        super.testPushAttributesGateway();
    }
}
