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
package org.thingsboard.server.transport.mqtt.util.sparkplug;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.thingsboard.server.transport.mqtt.util.sparkplug.message.SparkplugTopic;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides utility methods for handling Sparkplug MQTT message topics.
 */
public class SparkplugTopicUtil {
	
	private static final Map<String, String[]> SPLIT_TOPIC_CACHE = new HashMap<String, String[]>();
	
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
	 * @throws Exception if an error occurs while parsing
	 */
	public static SparkplugTopic parseTopic(String topic) throws Exception {
		topic = topic.indexOf("#") > 0 ? topic.substring(0, topic.indexOf("#")) : topic;
		return parseTopic(SparkplugTopicUtil.getSplitTopic(topic));
	}

	/**
	 * Parses a Sparkplug MQTT message topic string and returns a {@link SparkplugTopic} instance.
	 *
	 * @param splitTopic a topic split into tokens
	 * @return a {@link SparkplugTopic} instance
	 * @throws Exception if an error occurs while parsing
	 */
	@SuppressWarnings("incomplete-switch")
	public static SparkplugTopic parseTopic(String[] splitTopic) throws Exception {
		SparkplugMessageType type;
		String namespace, edgeNodeId, groupId;
		int length = splitTopic.length;

		if (length < 4 || length > 5) {
			throw new Exception("Invalid number of topic elements: " + length);
		}

		namespace = splitTopic[0];
		groupId = splitTopic[1];
		type = SparkplugMessageType.parseMessageType(splitTopic[2]);
		edgeNodeId = splitTopic[3];

		if (length == 4) {
			// A node topic
			switch (type) {
				case STATE:
				case NBIRTH:
				case NCMD:
				case NDATA:
				case NDEATH:
				case NRECORD:
					return new SparkplugTopic(namespace, groupId, edgeNodeId, type);
			}
		} else {
			// A device topic
			switch (type) {
				case STATE:
				case DBIRTH:
				case DCMD:
				case DDATA:
				case DDEATH:
				case DRECORD:
					return new SparkplugTopic(namespace, groupId, edgeNodeId, splitTopic[4], type);
			}
		}
		throw new Exception("Invalid number of topic elements " + length + " for topic type " + type);
	}
}
