// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.context.chart;

import org.thingsboard.server.common.data.report.configuration.DataKey;

@FunctionalInterface
public interface DataPostProcessFunction {

    Object apply(DataKey dataKey, long timestamp, String value);

}
