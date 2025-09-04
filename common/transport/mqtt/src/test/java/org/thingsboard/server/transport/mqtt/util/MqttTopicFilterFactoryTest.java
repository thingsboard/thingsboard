/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.transport.mqtt.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.script.ScriptException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class MqttTopicFilterFactoryTest {

    private static String TEST_STR_1 = "Sensor/Temperature/House/48";
    private static String TEST_STR_2 = "Sensor/Temperature";
    private static String TEST_STR_3 = "Sensor/Temperature2/House/48";
    private static String TEST_STR_4 = "/Sensor/Temperature2/House/48";
    private static String TEST_STR_5 = "Sensor/ Temperature";
    private static String TEST_STR_6 = "/";

    @Test
    public void metadataCanBeUpdated() throws ScriptException {
        MqttTopicFilter filter = MqttTopicFilterFactory.toFilter("Sensor/Temperature/House/+");
        assertTrue(filter.filter(TEST_STR_1));
        assertFalse(filter.filter(TEST_STR_2));

        filter = MqttTopicFilterFactory.toFilter("Sensor/+/House/#");
        assertTrue(filter.filter(TEST_STR_1));
        assertFalse(filter.filter(TEST_STR_2));

        filter = MqttTopicFilterFactory.toFilter("Sensor/#");
        assertTrue(filter.filter(TEST_STR_1));
        assertTrue(filter.filter(TEST_STR_2));
        assertTrue(filter.filter(TEST_STR_3));

        filter = MqttTopicFilterFactory.toFilter("Sensor/Temperature/#");
        assertTrue(filter.filter(TEST_STR_1));
        assertTrue(filter.filter(TEST_STR_2));
        assertFalse(filter.filter(TEST_STR_3));

        filter = MqttTopicFilterFactory.toFilter("#");
        assertTrue(filter.filter(TEST_STR_1));
        assertTrue(filter.filter(TEST_STR_2));
        assertTrue(filter.filter(TEST_STR_3));
        assertTrue(filter.filter(TEST_STR_4));
        assertTrue(filter.filter(TEST_STR_5));
        assertTrue(filter.filter(TEST_STR_6));

        filter = MqttTopicFilterFactory.toFilter("Sensor/Temperature#");
        assertFalse(filter.filter(TEST_STR_2));
    }

}
