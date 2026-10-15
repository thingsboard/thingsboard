// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
