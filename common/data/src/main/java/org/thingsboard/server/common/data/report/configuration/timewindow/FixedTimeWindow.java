// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.timewindow;

import lombok.Data;

@Data
public class FixedTimeWindow {
    private long startTimeMs;
    private long endTimeMs;
}
