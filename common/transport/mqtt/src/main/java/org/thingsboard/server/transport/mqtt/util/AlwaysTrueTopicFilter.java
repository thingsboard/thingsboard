// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.mqtt.util;

import lombok.Data;

@Data
public class AlwaysTrueTopicFilter implements MqttTopicFilter {

    @Override
    public boolean filter(String topic) {
        return true;
    }
}
