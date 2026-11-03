// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ws.telemetry.cmd.v2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.IntervalType;

import java.util.List;

@Data
public class TimeSeriesCmd implements GetTsCmd {

    private List<String> keys;
    private long startTs;
    private long timeWindow;
    private IntervalType intervalType;
    private long interval;
    private String timeZoneId;
    private int limit;
    private Aggregation agg;
    private boolean fetchLatestPreviousPoint;

    @JsonIgnore
    @Override
    public long getEndTs() {
        return startTs + timeWindow;
    }
}
