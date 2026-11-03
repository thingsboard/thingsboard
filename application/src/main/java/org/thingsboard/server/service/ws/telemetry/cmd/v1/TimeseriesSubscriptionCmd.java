// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ws.telemetry.cmd.v1;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.service.ws.WsCmdType;

/**
 * @author Andrew Shvayka
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
public class TimeseriesSubscriptionCmd extends SubscriptionCmd {

    private long startTs;
    private long timeWindow;
    private long interval;
    private int limit;
    private String agg;

    @Override
    public WsCmdType getType() {
        return WsCmdType.TIMESERIES;
    }
}
