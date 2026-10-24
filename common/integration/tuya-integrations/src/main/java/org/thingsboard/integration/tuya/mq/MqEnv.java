// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.tuya.mq;

import lombok.Getter;

public enum MqEnv {
    PROD("prod", "event", "online environment"),
    TEST("test", "event-test", "test environment");

    @Getter
    private final String key;
    @Getter
    private final String value;
    @Getter
    private final String description;

    MqEnv(String key, String value, String description) {
        this.key = key;
        this.value = value;
        this.description = description;
    }
}
