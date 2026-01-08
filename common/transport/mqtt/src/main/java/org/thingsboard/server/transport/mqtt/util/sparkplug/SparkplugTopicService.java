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
package org.thingsboard.server.transport.mqtt.util.sparkplug;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.transport.mqtt.TbMqttTransportComponent;

import java.util.HashMap;
import java.util.Map;

import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType.STATE;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugTopic.parseTopic;

@Slf4j
@Service
@TbMqttTransportComponent
public class SparkplugTopicService {

    private static final Map<String, SparkplugTopic> SPLIT_TOPIC_CACHE = new HashMap<>();
    public static final String TOPIC_ROOT_SPB_V_1_0 = "spBv1.0";
    public static final String TOPIC_ROOT_CERT_SP = "$sparkplug/certificates/";
    public static final String TOPIC_SPLIT_REGEXP = "/";
    public static final String TOPIC_STATE_REGEXP = TOPIC_ROOT_SPB_V_1_0 + TOPIC_SPLIT_REGEXP + STATE.name() + TOPIC_SPLIT_REGEXP;

    public static SparkplugTopic getSplitTopic(String topic) throws ThingsboardException {
        SparkplugTopic sparkplugTopic = SPLIT_TOPIC_CACHE.get(topic);
        if (sparkplugTopic == null) {
            // validation topic
            sparkplugTopic = parseTopic(topic);
            SPLIT_TOPIC_CACHE.put(topic, sparkplugTopic);
        }
        return sparkplugTopic;
    }

    /**
     * all ID Element MUST be a UTF-8 string
     * and with the exception of the reserved characters of + (plus), / (forward slash).
     * Publish: $sparkplug/certificates/spBv1.0/G1/NBIRTH/E1
     * Publish: spBv1.0/G1/NBIRTH/E1
     * Publish: $sparkplug/certificates/spBv1.0/G1/DBIRTH/E1/D1
     * Publish: spBv1.0/G1/DBIRTH/E1/D1
     * @param topic
     * @return
     * @throws ThingsboardException
     */
    public static SparkplugTopic parseTopicPublish(String topic) throws ThingsboardException {
        topic = topic.startsWith(TOPIC_ROOT_CERT_SP) ? topic.substring(TOPIC_ROOT_CERT_SP.length()) : topic;
        topic = topic.indexOf("+") > 0 ? topic.substring(0, topic.indexOf("+")): topic;
        return getSplitTopic(topic);
    }
}

