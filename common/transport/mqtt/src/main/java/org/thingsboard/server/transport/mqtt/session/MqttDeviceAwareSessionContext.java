// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.mqtt.session;

import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.ToString;
import org.thingsboard.server.common.transport.session.DeviceAwareSessionContext;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@ToString(callSuper = true)
public abstract class MqttDeviceAwareSessionContext extends DeviceAwareSessionContext {

    private final ConcurrentMap<MqttTopicMatcher, Integer> mqttQoSMap;

    public MqttDeviceAwareSessionContext(UUID sessionId, ConcurrentMap<MqttTopicMatcher, Integer> mqttQoSMap) {
        super(sessionId);
        this.mqttQoSMap = mqttQoSMap;
    }

    public ConcurrentMap<MqttTopicMatcher, Integer> getMqttQoSMap() {
        return mqttQoSMap;
    }

    public MqttQoS getQoSForTopic(String topic) {
        List<Integer> qosList = mqttQoSMap.entrySet()
                .stream()
                .filter(entry -> entry.getKey().matches(topic))
                .map(Map.Entry::getValue)
                .toList();
        if (!qosList.isEmpty()) {
            return MqttQoS.valueOf(qosList.get(0));
        } else {
            return MqttQoS.AT_LEAST_ONCE;
        }
    }
}
