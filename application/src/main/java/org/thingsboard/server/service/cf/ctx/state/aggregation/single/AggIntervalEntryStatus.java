// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cf.ctx.state.aggregation.single;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AggIntervalEntryStatus {

    private long lastArgsRefreshTs = -1;

    private long lastMetricsEvalTs = -1;

    public AggIntervalEntryStatus(long lastArgsRefreshTs) {
        this.lastArgsRefreshTs = lastArgsRefreshTs;
    }

    public boolean intervalPassed(long checkInterval) {
        return lastMetricsEvalTs <= System.currentTimeMillis() - checkInterval;
    }

    @JsonIgnore
    public boolean argsUpdated() {
        return lastArgsRefreshTs > -1;
    }

}
