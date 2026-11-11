// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.timewindow;

import lombok.Data;
import org.thingsboard.server.common.data.kv.Aggregation;

@Data
public class AggregationConfiguration {
    private Aggregation type;
    private int limit;
}
