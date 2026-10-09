// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.mqtt;

import lombok.Data;

@Data
public class MqttTopicFilter {

    private String filter;
    private int qos;

}
