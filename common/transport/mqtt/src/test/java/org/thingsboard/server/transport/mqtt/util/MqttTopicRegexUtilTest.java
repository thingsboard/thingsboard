/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import javax.script.ScriptException;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class MqttTopicRegexUtilTest {

    private static String TEST_STR_1 = "Sensor/Temperature/House/48";
    private static String TEST_STR_2 = "Sensor/Temperature";
    private static String TEST_STR_3 = "Sensor/Temperature2/House/48";

    @Test
    public void metadataCanBeUpdated() throws ScriptException {
        Pattern filter = MqttTopicRegexUtil.toRegex("Sensor/Temperature/House/+");
        assertTrue(filter.matcher(TEST_STR_1).matches());
        assertFalse(filter.matcher(TEST_STR_2).matches());

        filter = MqttTopicRegexUtil.toRegex("Sensor/+/House/#");
        assertTrue(filter.matcher(TEST_STR_1).matches());
        assertFalse(filter.matcher(TEST_STR_2).matches());

        filter = MqttTopicRegexUtil.toRegex("Sensor/#");
        assertTrue(filter.matcher(TEST_STR_1).matches());
        assertTrue(filter.matcher(TEST_STR_2).matches());
        assertTrue(filter.matcher(TEST_STR_3).matches());

        filter = MqttTopicRegexUtil.toRegex("Sensor/Temperature/#");
        assertTrue(filter.matcher(TEST_STR_1).matches());
        assertTrue(filter.matcher(TEST_STR_2).matches());
        assertFalse(filter.matcher(TEST_STR_3).matches());
    }

}
