/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides utility methods for handling Sparkplug MQTT message topics.
 */
public class SparkplugTopicUtil {

    private static final Map<String, String[]> SPLIT_TOPIC_CACHE = new HashMap<String, String[]>();
    private static final String TOPIC_INVALID_NUMBER = "Invalid number of topic elements: ";

    public static String[] getSplitTopic(String topic) {
        String[] splitTopic = SPLIT_TOPIC_CACHE.get(topic);
        if (splitTopic == null) {
            splitTopic = topic.split("/");
            SPLIT_TOPIC_CACHE.put(topic, splitTopic);
        }

        return splitTopic;
    }

    /**
     * Serializes a {@link SparkplugTopic} instance in to a JSON string.
     *
     * @param topic a {@link SparkplugTopic} instance
     * @return a JSON string
     * @throws JsonProcessingException
     */
    public static String sparkplugTopicToString(SparkplugTopic topic) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(topic);
    }

    /**
     * Parses a Sparkplug MQTT message topic string and returns a {@link SparkplugTopic} instance.
     *
     * @param topic a topic string
     * @return a {@link SparkplugTopic} instance
     * @throws ThingsboardException if an error occurs while parsing
     */
    public static SparkplugTopic parseTopicSubscribe(String topic) throws ThingsboardException {
        // TODO "+", "$"
        topic = topic.indexOf("#") > 0 ? topic.substring(0, topic.indexOf("#")) : topic;
        return parseTopic(SparkplugTopicUtil.getSplitTopic(topic));
    }

    public static SparkplugTopic parseTopicPublish(String topic) throws ThingsboardException {
        if (topic.contains("#") || topic.contains("$") || topic.contains("+")) {
            throw new ThingsboardException("Invalid of topic elements for Publish", ThingsboardErrorCode.INVALID_ARGUMENTS);
        } else {
            String[] splitTopic = SparkplugTopicUtil.getSplitTopic(topic);
            if (splitTopic.length < 4 || splitTopic.length > 5) {
                throw new ThingsboardException(TOPIC_INVALID_NUMBER + splitTopic.length, ThingsboardErrorCode.INVALID_ARGUMENTS);
            }
            return parseTopic(splitTopic);
        }
    }

    /**
     * Parses a Sparkplug MQTT message topic string and returns a {@link SparkplugTopic} instance.
     *
     * @param splitTopic a topic split into tokens
     * @return a {@link SparkplugTopic} instance
     * @throws Exception if an error occurs while parsing
     */
    @SuppressWarnings("incomplete-switch")
    public static SparkplugTopic parseTopic(String[] splitTopic) throws ThingsboardException {
        int length = splitTopic.length;
        if (length == 0) {
			throw new ThingsboardException(TOPIC_INVALID_NUMBER + length, ThingsboardErrorCode.INVALID_ARGUMENTS);
        } else {
            SparkplugMessageType type;
            String namespace, edgeNodeId, groupId, deviceId;
            namespace = splitTopic[0];
            groupId = length > 1 ? splitTopic[1] : null;
            type = length > 2 ? SparkplugMessageType.parseMessageType(splitTopic[2]) : null;
            edgeNodeId = length > 3 ? splitTopic[3] : null;
			deviceId = length > 4 ? splitTopic[4] : null;
			return new SparkplugTopic(namespace, groupId, edgeNodeId, deviceId, type);
        }
    }

}
