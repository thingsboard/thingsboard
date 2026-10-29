// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.trace;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.trace.SpanEntity;
import org.thingsboard.server.dao.model.sql.trace.SpanEntityId;

import java.util.List;
import java.util.UUID;

// Read-only: spans are written by TraceInsertRepository; no save/findById/deleteById.
public interface SpanRepository extends Repository<SpanEntity, SpanEntityId> {
    @Query("SELECT s FROM SpanEntity s WHERE s.tenantId = :tenantId AND s.traceId = :traceId ORDER BY s.startTime ASC")
    List<SpanEntity> findByTenantIdAndTraceId(@Param("tenantId") UUID tenantId, @Param("traceId") String traceId);

    @Query("SELECT s FROM SpanEntity s WHERE s.tenantId = :tenantId AND s.traceId = :traceId AND s.spanId = :spanId")
    List<SpanEntity> findByTenantIdAndTraceIdAndSpanId(@Param("tenantId") UUID tenantId,
                                                        @Param("traceId") String traceId,
                                                        @Param("spanId") String spanId);
}
