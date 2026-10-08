package org.thingsboard.server.service.trace;

import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.BufferedSpan;

import java.util.Collection;
import java.util.List;

public interface TraceBuffer {
    void append(Collection<BufferedSpan> spans);

    List<LoadedTrace> drainExpired(long now, int maxSpans);

    List<String> findExpiredTraceKeys(long now);

    List<LoadedTrace> load(Collection<String> traceKeys);

    void remove(Collection<String> traceKeys);

    boolean isShared();
}
