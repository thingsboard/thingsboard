// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
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
    DELETE_ALARM_COMMENTS("alarm comments deletion"),
    UNASSIGN_ALARMS("alarms unassigning"),
    DELETE_TENANT_ENTITIES("tenant entities deletion"),
    DELETE_ENTITIES("entities deletion"),
    CLEANUP_ENTITIES("entities cleanup"),
    DELETE_CALCULATED_FIELDS("calculated fields deletion"),
    DELETE_JOBS("jobs deletion"),
    DELETE_AI_USER_DATA("AI user data deletion"),
    DELETE_AI_TENANT_DATA("AI tenant data deletion");

    private final String description;

}
