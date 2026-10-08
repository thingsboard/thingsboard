package org.thingsboard.server.service.trace;

import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.TraceGroup;

import java.util.Set;

public record TraceGroupContribution(TraceGroup metrics, String sampleCandidateTraceId, Set<String> traceIds,
                                      Set<String> sourceBufferKeys) {
}
