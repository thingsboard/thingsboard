// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.mqtt.util;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.device.profile.MqttTopics;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class MqttTopicFilterFactory {

    private static final ConcurrentMap<String, MqttTopicFilter> filters = new ConcurrentHashMap<>();
    private static final MqttTopicFilter DEFAULT_TELEMETRY_TOPIC_FILTER = toFilter(MqttTopics.DEVICE_TELEMETRY_TOPIC);
    private static final MqttTopicFilter DEFAULT_ATTRIBUTES_TOPIC_FILTER = toFilter(MqttTopics.DEVICE_ATTRIBUTES_TOPIC);

    public static MqttTopicFilter toFilter(String topicFilter) {
        if (topicFilter == null || topicFilter.isEmpty()) {
            throw new IllegalArgumentException("Topic filter can't be empty!");
        }
        return filters.computeIfAbsent(topicFilter, filter -> {
            if (filter.equals("#")) {
                return new AlwaysTrueTopicFilter();
            } else if (filter.contains("+") || filter.contains("#")) {
                String regex = filter
                        .replace("\\", "\\\\")
                        .replace("+", "[^/]+")
                        .replace("/#", "($|/.*)");
                log.debug("Converting [{}] to [{}]", filter, regex);
                return new RegexTopicFilter(regex);
            } else {
                return new EqualsTopicFilter(filter);
            }
        });
    }

    public static MqttTopicFilter getDefaultTelemetryFilter() {
        return DEFAULT_TELEMETRY_TOPIC_FILTER;
    }

    public static MqttTopicFilter getDefaultAttributesFilter() {
        return DEFAULT_ATTRIBUTES_TOPIC_FILTER;
    }
}
