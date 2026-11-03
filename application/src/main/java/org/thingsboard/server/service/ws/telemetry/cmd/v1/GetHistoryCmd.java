// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ws.telemetry.cmd.v1;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.service.ws.WsCmdType;

/**
 * @author Andrew Shvayka
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class GetHistoryCmd implements TelemetryPluginCmd {

    private int cmdId;
    private String entityType;
    private String entityId;
    private String keys;
    private long startTs;
    private long endTs;
    private long interval;
    private int limit;
    private String agg;

    @Override
    public WsCmdType getType() {
        return WsCmdType.TIMESERIES_HISTORY;
    }
}
