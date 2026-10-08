package org.thingsboard.server.service.trace;

import java.util.UUID;

public record TraceGroupKey(UUID tenantId, String pathId) {
}
