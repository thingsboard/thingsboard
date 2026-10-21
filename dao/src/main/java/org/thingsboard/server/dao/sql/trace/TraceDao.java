/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
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
                                              int page, int pageSize, String sortField, String sortOrder);

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
                                                  String sortField, String sortOrder);

    boolean tryAcquireAggregationLeader(String serviceId, long nowMs, long leaseMs);

    void deleteByTenantId(UUID tenantId);
}
