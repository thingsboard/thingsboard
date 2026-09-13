// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cf.ctx.state.aggregation.single;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AggIntervalEntry {

    private Long startTs;
    private Long endTs;

    public boolean belongsToInterval(long ts) {
        return ts >= startTs && ts < endTs;
    }

    public long getIntervalDuration() {
        return endTs - startTs;
    }

}
