// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.mqtt.session;

import lombok.Getter;

import java.util.regex.Pattern;

public class MqttTopicMatcher {

    @Getter
    private final String topic;
    private final Pattern topicRegex;

    public MqttTopicMatcher(String topic) {
        if (topic == null) {
            throw new NullPointerException("topic");
        }
        this.topic = topic;
        this.topicRegex = Pattern.compile(topic.replace("+", "[^/]+").replace("#", ".+") + "$");
    }

    public boolean matches(String topic) {
        return this.topicRegex.matcher(topic).matches();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MqttTopicMatcher that = (MqttTopicMatcher) o;

        return topic.equals(that.topic);
    }

    @Override
    public int hashCode() {
        return topic.hashCode();
    }

}
