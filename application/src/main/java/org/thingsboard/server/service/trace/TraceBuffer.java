// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.trace;

import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.BufferedSpan;

import java.util.Collection;
import java.util.List;

public interface TraceBuffer {
    void append(Collection<BufferedSpan> spans);

    List<BufferedTrace> drainExpired(long now, int maxSpans);

    boolean isShared();
}
