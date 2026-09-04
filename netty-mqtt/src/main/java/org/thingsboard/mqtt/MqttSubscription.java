// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.mqtt;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.regex.Pattern;

final class MqttSubscription {

    @Getter(AccessLevel.PACKAGE)
    private final String topic;
    private final Pattern topicRegex;
    @Getter
    private final MqttHandler handler;
    @Getter(AccessLevel.PACKAGE)
    private final boolean once;
    @Getter(AccessLevel.PACKAGE)
    @Setter(AccessLevel.PACKAGE)
    private volatile boolean called;

    MqttSubscription(String topic, MqttHandler handler, boolean once) {
        if (topic == null) {
            throw new NullPointerException("topic");
        }
        if (handler == null) {
            throw new NullPointerException("handler");
        }
        this.topic = topic;
        this.handler = handler;
        this.once = once;
        this.topicRegex = Pattern.compile(topic.replace("+", "[^/]+").replace("#", ".+") + "$");
    }

    boolean matches(String topic) {
        return this.topicRegex.matcher(topic).matches();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MqttSubscription that = (MqttSubscription) o;

        return once == that.once && topic.equals(that.topic) && handler.equals(that.handler);
    }

    @Override
    public int hashCode() {
        int result = topic.hashCode();
        result = 31 * result + handler.hashCode();
        result = 31 * result + (once ? 1 : 0);
        return result;
    }

}
