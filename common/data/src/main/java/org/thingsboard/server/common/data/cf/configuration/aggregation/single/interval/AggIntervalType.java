// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CFAggIntervalType")
public enum AggIntervalType {

    HOUR,
    DAY,
    WEEK,
    WEEK_SUN_SAT,
    MONTH,
    QUARTER,
    YEAR,
    CUSTOM

}
