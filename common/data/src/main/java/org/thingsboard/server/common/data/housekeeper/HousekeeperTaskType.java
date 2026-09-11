// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.housekeeper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum HousekeeperTaskType {

    DELETE_ATTRIBUTES("attributes deletion"),
    DELETE_TELEMETRY("telemetry deletion"),
    DELETE_LATEST_TS("latest telemetry deletion"),
    DELETE_TS_HISTORY("timeseries history deletion"),
    DELETE_EVENTS("events deletion"),
    DELETE_ALARMS("alarms deletion"),
    UNASSIGN_ALARMS("alarms unassigning"),
    DELETE_TENANT_ENTITIES("tenant entities deletion"),
    DELETE_ENTITIES("entities deletion"),
    DELETE_CALCULATED_FIELDS("calculated fields deletion"),
    DELETE_JOBS("jobs deletion");

    private final String description;

}
