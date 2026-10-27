// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.trace;

import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.TraceGroup;
import org.thingsboard.server.dao.model.sql.trace.SpanAttributeEntity;
import org.thingsboard.server.dao.model.sql.trace.SpanEntity;
import org.thingsboard.server.dao.model.sql.trace.SpanEventEntity;
import org.thingsboard.server.dao.model.sql.trace.SpanLinkEntity;
import org.thingsboard.server.dao.model.sql.trace.TraceProjection;
import org.thingsboard.server.common.data.trace.TracePathDTO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface TraceDao {
    void saveSpans(List<SpanEntity> spans);

    int upsertTraceSummaries(Collection<TraceSummaryInfo> summaries);

    Map<String, TraceGroup> findTraceGroups(long bucketTime, Collection<TraceGroup> groups);

    List<TraceGroup> findTraceGroupsByPathId(UUID tenantId, String pathId, long startTime, long endTime);

    long countTraceGroups(long bucketTime, UUID tenantId);

    void saveTraceGroups(Collection<TraceGroup> toInsertGroups, Collection<TraceGroup> toUpdateGroups);

    PageData<TracePathDTO> findPathsPaginated(UUID tenantId, long startTime, long endTime,
                                              String rootQueueId, String rootMessageType, String rootRuleChainId,
                                              String ruleNodeId, boolean errorsOnly,
                                              int page, int pageSize, TracePathSortField sortField, SortOrder sortOrder);

    TraceProjection findTraceById(UUID tenantId, String traceId);

    List<SpanEntity> findSpansByTraceId(UUID tenantId, String traceId);

    List<SpanAttributeEntity> findSpanAttributes(UUID tenantId, String spanId, String traceId);

    List<SpanAttributeEntity> findSpanAttributesByTraceId(UUID tenantId, String traceId);

    List<SpanEventEntity> findSpanEvents(UUID tenantId, String spanId, String traceId);

    List<SpanLinkEntity> findSpanLinks(UUID tenantId, String spanId, String traceId);

    PageData<TraceProjection> findTracesPaginated(UUID tenantId, String serviceName, long startTime, long endTime,
                                                  String spanName, String queueName, String originatorId, String messageType,
                                                  String messageId, String messageData, String messageMetadata, String traceId,
                                                  boolean errorsOnly,
                                                  String ruleChainId, String ruleNodeId, String pathId, boolean hasTimeOutsByQueue,
                                                  int page, int pageSize,
                                                  TraceSortField sortField, SortOrder sortOrder);

    void deleteByTenantId(UUID tenantId);

    void cleanUp(long spansExpTime, long groupsExpTime, int batchSize);

    void cleanUpPartitionsCache(long spansExpTime);
}
