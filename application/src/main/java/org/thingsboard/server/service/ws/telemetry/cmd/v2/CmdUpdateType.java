// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.ws.telemetry.cmd.v2;

public enum CmdUpdateType {
    ENTITY_DATA,
    ALARM_DATA,
    ALARM_COUNT_DATA,
    ALARM_STATUS,
    COUNT_DATA,
    NOTIFICATIONS,
    NOTIFICATIONS_COUNT,
    LOGS
}
