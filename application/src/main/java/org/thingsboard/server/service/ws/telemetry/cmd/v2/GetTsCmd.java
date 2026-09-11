// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ws.telemetry.cmd.v2;

import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.AggregationParams;
import org.thingsboard.server.common.data.kv.IntervalType;

import java.util.List;

public interface GetTsCmd {

    long getStartTs();

    long getEndTs();

    List<String> getKeys();

    IntervalType getIntervalType();

    long getInterval();

    String getTimeZoneId();

    int getLimit();

    Aggregation getAgg();

    boolean isFetchLatestPreviousPoint();

    default AggregationParams toAggregationParams() {
        var agg = getAgg();
        var intervalType = getIntervalType();
        if (agg == null || Aggregation.NONE.equals(agg)) {
            return AggregationParams.none();
        } else if (intervalType == null || IntervalType.MILLISECONDS.equals(intervalType)) {
            return AggregationParams.milliseconds(agg, getInterval());
        } else {
            return AggregationParams.calendar(agg, intervalType, getTimeZoneId());
        }
    }

}
