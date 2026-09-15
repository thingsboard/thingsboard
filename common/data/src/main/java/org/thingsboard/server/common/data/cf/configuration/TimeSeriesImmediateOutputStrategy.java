// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf.configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TimeSeriesImmediateOutputStrategy implements TimeSeriesOutputStrategy {

    private long ttl;

    private boolean saveTimeSeries;
    private boolean saveLatest;
    private boolean sendWsUpdate;
    private boolean processCfs;

    @Override
    public OutputStrategyType getType() {
        return OutputStrategyType.IMMEDIATE;
    }

    @Override
    public boolean hasContextOnlyChanges(OutputStrategy other) {
        if (!(other instanceof TimeSeriesImmediateOutputStrategy otherStrategy)) {
            return true;
        }
        boolean saveTimeSeriesUpdated = saveTimeSeries != otherStrategy.isSaveTimeSeries();
        boolean saveLatestUpdated = saveLatest != otherStrategy.isSaveLatest();
        boolean sendWsUpdateUpdated = sendWsUpdate != otherStrategy.isSendWsUpdate();
        boolean processCfsUpdated = processCfs != otherStrategy.isProcessCfs();
        return saveTimeSeriesUpdated || saveLatestUpdated || sendWsUpdateUpdated || processCfsUpdated;
    }

    @Override
    public boolean hasRefreshContextOnlyChanges(OutputStrategy other) {
        if (!(other instanceof TimeSeriesImmediateOutputStrategy otherStrategy)) {
            return true;
        }
        return ttl != otherStrategy.getTtl();
    }

}
