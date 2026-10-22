// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.edge.stats;

import lombok.Data;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Data
public class MsgCounters {

    private final TenantId tenantId;
    private final AtomicLong msgsAdded = new AtomicLong();
    private final AtomicLong msgsPushed = new AtomicLong();
    private final AtomicLong msgsPermanentlyFailed = new AtomicLong();
    private final AtomicLong msgsTmpFailed = new AtomicLong();
    private final AtomicLong msgsLag = new AtomicLong();
    // true once a live session measured the lag in the current report interval
    private final AtomicBoolean msgsLagMeasured = new AtomicBoolean();

    public void clear() {
        msgsAdded.set(0);
        msgsPushed.set(0);
        msgsPermanentlyFailed.set(0);
        msgsTmpFailed.set(0);
        msgsLag.set(0);
        msgsLagMeasured.set(false);
    }

}
