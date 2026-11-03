// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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

