// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.mqtt;

import lombok.Getter;
import org.thingsboard.server.common.data.device.profile.MqttTopics;

public enum TopicType {

    V1(MqttTopics.DEVICE_ATTRIBUTES_RESPONSE_TOPIC_PREFIX, MqttTopics.DEVICE_ATTRIBUTES_TOPIC, MqttTopics.DEVICE_RPC_REQUESTS_TOPIC, MqttTopics.DEVICE_RPC_RESPONSE_TOPIC),
    V2(MqttTopics.DEVICE_ATTRIBUTES_RESPONSE_SHORT_TOPIC_PREFIX, MqttTopics.DEVICE_ATTRIBUTES_SHORT_TOPIC, MqttTopics.DEVICE_RPC_REQUESTS_SHORT_TOPIC, MqttTopics.DEVICE_RPC_RESPONSE_SHORT_TOPIC),
    V2_JSON(MqttTopics.DEVICE_ATTRIBUTES_RESPONSE_SHORT_JSON_TOPIC_PREFIX, MqttTopics.DEVICE_ATTRIBUTES_SHORT_JSON_TOPIC, MqttTopics.DEVICE_RPC_REQUESTS_SHORT_JSON_TOPIC, MqttTopics.DEVICE_RPC_RESPONSE_SHORT_JSON_TOPIC),
    V2_PROTO(MqttTopics.DEVICE_ATTRIBUTES_RESPONSE_SHORT_PROTO_TOPIC_PREFIX, MqttTopics.DEVICE_ATTRIBUTES_SHORT_PROTO_TOPIC, MqttTopics.DEVICE_RPC_REQUESTS_SHORT_PROTO_TOPIC, MqttTopics.DEVICE_RPC_RESPONSE_SHORT_PROTO_TOPIC);

    @Getter
    private final String attributesResponseTopicBase;

    @Getter
    private final String attributesSubTopic;

    @Getter
    private final String rpcRequestTopicBase;

    @Getter
    private final String rpcResponseTopicBase;

    TopicType(String attributesRequestTopicBase, String attributesSubTopic, String rpcRequestTopicBase, String rpcResponseTopicBase) {
        this.attributesResponseTopicBase = attributesRequestTopicBase;
        this.attributesSubTopic = attributesSubTopic;
        this.rpcRequestTopicBase = rpcRequestTopicBase;
        this.rpcResponseTopicBase = rpcResponseTopicBase;
    }
}
