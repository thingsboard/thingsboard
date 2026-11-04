// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api;

import lombok.Getter;

public enum IntegrationStatisticsMetricName {
    START("start"),
    MSGS_UPLINK("msgUplink"),
    MSGS_DOWNLINK("msgDownlink");

    @Getter
    private final String name;

    IntegrationStatisticsMetricName(String name) {
        this.name = name;
    }

}
