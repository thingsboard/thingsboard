package org.thingsboard.server.service.trace;

import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.BufferedSpan;

import java.util.Set;
import java.util.UUID;

public record BufferedTrace(UUID tenantId, String traceId, boolean truncated, Set<BufferedSpan> spans) {
}
