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
package org.thingsboard.server.transport.mqtt.mqttv3.attributes.request;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;
import org.thingsboard.server.transport.mqtt.mqttv3.attributes.AbstractMqttAttributesIntegrationTest;

@Slf4j
@DaoSqlTest
public class MqttAttributesRequestJsonIntegrationTest extends AbstractMqttAttributesIntegrationTest {

    @Before
    public void beforeTest() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Request attribute values from the server json")
                .gatewayName("Gateway Test Request attribute values from the server json")
                .transportPayloadType(TransportPayloadType.JSON)
                .build();
        processBeforeTest(configProperties);
    }

    @Test
    public void testRequestAttributesValuesFromTheServer() throws Exception {
        processJsonTestRequestAttributesValuesFromTheServer(MqttTopics.DEVICE_ATTRIBUTES_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_REQUEST_TOPIC_PREFIX);
    }

    @Test
    public void testRequestAttributesValuesFromTheServerOnShortTopic() throws Exception {
        processJsonTestRequestAttributesValuesFromTheServer(MqttTopics.DEVICE_ATTRIBUTES_SHORT_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_SHORT_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_REQUEST_SHORT_TOPIC_PREFIX);
    }

    @Test
    public void testRequestAttributesValuesFromTheServerOnShortJsonTopic() throws Exception {
        processJsonTestRequestAttributesValuesFromTheServer(MqttTopics.DEVICE_ATTRIBUTES_SHORT_JSON_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_SHORT_JSON_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_REQUEST_SHORT_JSON_TOPIC_PREFIX);
    }

    @Test
    public void testRequestAttributesValuesFromTheServerGateway() throws Exception {
        processJsonTestGatewayRequestAttributesValuesFromTheServer();
    }

    @Test
    public void testRequestAllSharedAttributesViaEmptyValue() throws Exception {
        // sharedKeys present + empty => all shared; clientKeys absent => no client
        processJsonTestRequestAttributesWithPayload(
                MqttTopics.DEVICE_ATTRIBUTES_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_REQUEST_TOPIC_PREFIX,
                "{\"sharedKeys\":\"\"}",
                "{\"shared\":" + SHARED_ATTRIBUTES_PAYLOAD + "}");
    }

    @Test
    public void testRequestAllClientAttributesViaEmptyValue() throws Exception {
        // clientKeys present + empty => all client; sharedKeys absent => no shared
        processJsonTestRequestAttributesWithPayload(
                MqttTopics.DEVICE_ATTRIBUTES_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_REQUEST_TOPIC_PREFIX,
                "{\"clientKeys\":\"\"}",
                "{\"client\":" + CLIENT_ATTRIBUTES_PAYLOAD + "}");
    }

    @Test
    public void testRequestAllAttributesViaEmptyObject() throws Exception {
        // both fields absent => fetch everything (both scopes)
        processJsonTestRequestAttributesWithPayload(
                MqttTopics.DEVICE_ATTRIBUTES_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_TOPIC, MqttTopics.DEVICE_ATTRIBUTES_REQUEST_TOPIC_PREFIX,
                "{}",
                "{\"client\":" + CLIENT_ATTRIBUTES_PAYLOAD + ",\"shared\":" + SHARED_ATTRIBUTES_PAYLOAD + "}");
    }

    @Test
    public void testGatewayRequestAllAttributesSeparated() throws Exception {
        // new format: empty clientKeys + empty sharedKeys => all both, scope-separated response
        processJsonTestGatewayRequestAttributesSeparated(
                ", \"clientKeys\": \"\", \"sharedKeys\": \"\"",
                ",\"client\":" + CLIENT_ATTRIBUTES_PAYLOAD + ",\"shared\":" + SHARED_ATTRIBUTES_PAYLOAD);
    }

    @Test
    public void testGatewayRequestAllSharedSeparated() throws Exception {
        // new format: empty sharedKeys only => all shared, no client in the separated response
        processJsonTestGatewayRequestAttributesSeparated(
                ", \"sharedKeys\": \"\"",
                ",\"shared\":" + SHARED_ATTRIBUTES_PAYLOAD);
    }
}
