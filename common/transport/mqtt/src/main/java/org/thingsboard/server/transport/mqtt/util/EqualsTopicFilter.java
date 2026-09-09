// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.mqtt.util;

import lombok.Data;

@Data
public class EqualsTopicFilter implements MqttTopicFilter {

    private final String filter;

    @Override
    public boolean filter(String topic) {
        return filter.equals(topic);
    }
}
