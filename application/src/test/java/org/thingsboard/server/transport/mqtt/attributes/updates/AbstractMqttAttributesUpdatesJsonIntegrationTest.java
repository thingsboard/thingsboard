/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.transport.mqtt.attributes.updates;

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.transport.mqtt.attributes.AbstractMqttAttributesIntegrationTest;

@Slf4j
public abstract class AbstractMqttAttributesUpdatesJsonIntegrationTest extends AbstractMqttAttributesIntegrationTest {

    @Before
    public void beforeTest() throws Exception {
        processBeforeTest("Test Subscribe to attribute updates", "Gateway Test Subscribe to attribute updates", TransportPayloadType.JSON, null, null);
    }

    @After
    public void afterTest() throws Exception {
        processAfterTest();
    }

    @Test
    public void testJsonSubscribeToAttributesUpdatesFromTheServer() throws Exception {
        processJsonTestSubscribeToAttributesUpdates(MqttTopics.DEVICE_ATTRIBUTES_TOPIC);
    }

    @Test
    public void testJsonSubscribeToAttributesUpdatesFromTheServerOnShortTopic() throws Exception {
        processJsonTestSubscribeToAttributesUpdates(MqttTopics.DEVICE_ATTRIBUTES_SHORT_TOPIC);
    }

    @Test
    public void testJsonSubscribeToAttributesUpdatesFromTheServerOnShortJsonTopic() throws Exception {
        processJsonTestSubscribeToAttributesUpdates(MqttTopics.DEVICE_ATTRIBUTES_SHORT_JSON_TOPIC);
    }

    @Test
    public void testJsonSubscribeToAttributesUpdatesFromTheServerGateway() throws Exception {
        processJsonGatewayTestSubscribeToAttributesUpdates();
    }
}
