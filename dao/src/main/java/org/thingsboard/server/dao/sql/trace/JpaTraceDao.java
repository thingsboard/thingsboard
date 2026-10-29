// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.trace;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.trace.TracePathDTO;
import org.thingsboard.server.dao.config.DefaultDataSource;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.TraceGroup;
import org.thingsboard.server.dao.model.sql.trace.SpanAttributeEntity;
import org.thingsboard.server.dao.model.sql.trace.SpanEntity;
import org.thingsboard.server.dao.model.sql.trace.SpanEventEntity;
import org.thingsboard.server.dao.model.sql.trace.SpanLinkEntity;
import org.thingsboard.server.dao.model.sql.trace.TraceProjection;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.model.sql.ruleenginemonitoring.TraceGroup.DAY_BUCKET_MS;
import static org.thingsboard.server.dao.model.sql.ruleenginemonitoring.TraceGroup.floorToDay;

@DefaultDataSource
@Component
@SqlDao
@RequiredArgsConstructor
public class JpaTraceDao implements TraceDao {

    private final SpanRepository spanRepository;
    private final TraceInsertRepository traceInsertRepository;
    private final TraceQueryRepository traceQueryRepository;

    @Override
    public void saveSpans(List<SpanEntity> spans) {
        traceInsertRepository.saveSpans(spans);
    }

    @Override
    public int upsertTraceSummaries(Collection<TraceSummaryInfo> summaries) {
        return traceInsertRepository.upsertTraceSummaries(summaries);
    }

    @Override
    public Map<String, TraceGroup> findTraceGroups(long bucketTime, Collection<TraceGroup> groups) {
        return traceQueryRepository.findTraceGroups(bucketTime, groups);
    }

    @Override
    public List<TraceGroup> findTraceGroupsByPathId(UUID tenantId, String pathId, long startTime, long endTime) {
        return traceQueryRepository.findTraceGroupsByPathId(tenantId, pathId, floorToDay(startTime), endTime);
    }

    @Override
    public long countTraceGroups(long bucketTime, UUID tenantId) {
        return traceQueryRepository.countTraceGroups(bucketTime, tenantId);
    }

    @Override
    public void saveTraceGroups(Collection<TraceGroup> toInsertGroups, Collection<TraceGroup> toUpdateGroups) {
        traceInsertRepository.saveTraceGroups(toInsertGroups, toUpdateGroups);
    }

    @Override
    public void saveTraceBatch(Collection<TraceGroup> toInsertGroups, Collection<TraceGroup> toUpdateGroups,
                               List<SpanEntity> spans, Collection<TraceSummaryInfo> summaries) {
        traceInsertRepository.saveTraceBatch(toInsertGroups, toUpdateGroups, spans, summaries);
    }

    @Override
    public PageData<TracePathDTO> findPathsPaginated(UUID tenantId, long startTime, long endTime,
                                                     String rootQueueId, String rootMessageType, String rootRuleChainId,
                                                     String ruleNodeId, boolean errorsOnly,
                                                     int page, int pageSize, TracePathSortField sortField, SortOrder sortOrder) {
        // A window starting mid-day must still match that day's bucket.
        return traceQueryRepository.findPathsPaginated(tenantId, floorToDay(startTime), endTime, rootQueueId, rootMessageType,
                rootRuleChainId, ruleNodeId, errorsOnly, page, pageSize, sortField, sortOrder);
    }

    @Override
    public TraceProjection findTraceById(UUID tenantId, String traceId) {
        return traceQueryRepository.findTraceById(tenantId, traceId);
    }

    @Override
    public List<SpanEntity> findSpansByTraceId(UUID tenantId, String traceId) {
        return spanRepository.findByTenantIdAndTraceId(tenantId, traceId);
    }

    @Override
    public List<SpanAttributeEntity> findSpanAttributes(UUID tenantId, String spanId, String traceId) {
        List<SpanEntity> spans = spanRepository.findByTenantIdAndTraceIdAndSpanId(tenantId, traceId, spanId);
        if (spans.isEmpty()) {
            return Collections.emptyList();
        }
        return toSpanAttributes(spans.get(0));
    }

    @Override
    public List<SpanAttributeEntity> findSpanAttributesByTraceId(UUID tenantId, String traceId) {
        return spanRepository.findByTenantIdAndTraceId(tenantId, traceId).stream()
                .map(this::toSpanAttributes)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    @Override
    public List<SpanEventEntity> findSpanEvents(UUID tenantId, String spanId, String traceId) {
        List<SpanEntity> spans = spanRepository.findByTenantIdAndTraceIdAndSpanId(tenantId, traceId, spanId);
        if (spans.isEmpty()) {
            return Collections.emptyList();
        }
        return toSpanEvents(spans.get(0));
    }

    @Override
    public List<SpanLinkEntity> findSpanLinks(UUID tenantId, String spanId, String traceId) {
        List<SpanEntity> spans = spanRepository.findByTenantIdAndTraceIdAndSpanId(tenantId, traceId, spanId);
        if (spans.isEmpty()) {
            return Collections.emptyList();
        }
        return toSpanLinks(spans.get(0));
    }

    @Override
    public PageData<TraceProjection> findTracesPaginated(UUID tenantId, String serviceName, long startTime, long endTime, String spanName, String queueName, String originatorId, String messageType, String messageId, String messageData, String messageMetadata, String traceId, boolean errorsOnly, String ruleChainId, String ruleNodeId, String pathId, boolean hasTimeOutsByQueue, int page, int pageSize, TraceSortField sortField, SortOrder sortOrder) {
        if (traceId != null && !traceId.isEmpty()) {
            TraceProjection trace = traceQueryRepository.findTraceById(tenantId, traceId);
            if (trace != null) {
                return new PageData<>(Collections.singletonList(trace), 1, 1, false);
            } else {
                return new PageData<>(Collections.emptyList(), 0, 0, false);
            }
        }

        Collection<String> pathTraceIds = null;
        if (pathId != null && !pathId.isEmpty()) {
            long bucketStart = floorToDay(startTime);
            long bucketEnd = floorToDay(endTime) + DAY_BUCKET_MS;
            List<TraceGroup> groups = traceQueryRepository.findTraceGroupsByPathId(tenantId, pathId, bucketStart, bucketEnd);
            Set<String> ids = new HashSet<>();
            for (TraceGroup group : groups) {
                if (group.getRelatedTraceIds() != null) {
                    for (String id : group.getRelatedTraceIds()) {
                        if (id != null && !id.isEmpty()) {
                            ids.add(id);
                        }
                    }
                }
            }
            if (ids.isEmpty()) {
                return new PageData<>(Collections.emptyList(), 0, 0, false);
            }
            pathTraceIds = ids;
        }

        Page<TraceProjection> resultPage = traceQueryRepository.findTraces(tenantId, serviceName, startTime, endTime, spanName,
                queueName, originatorId, messageType, messageId, messageData, messageMetadata, errorsOnly, ruleChainId,
                ruleNodeId, pathTraceIds, hasTimeOutsByQueue, sortField, sortOrder, PageRequest.of(page, pageSize));
        return new PageData<>(resultPage.getContent(), resultPage.getTotalPages(), resultPage.getTotalElements(), resultPage.hasNext());
    }

    @Override
    public void deleteByTenantId(UUID tenantId) {
        traceInsertRepository.deleteByTenantId(tenantId);
    }

    @Override
    public void cleanUp(long spansExpTime, long groupsExpTime, int batchSize) {
        traceInsertRepository.cleanUp(spansExpTime, groupsExpTime, batchSize);
    }

    @Override
    public void cleanUpPartitionsCache(long spansExpTime) {
        traceInsertRepository.cleanUpPartitionsCache(spansExpTime);
    }

    private List<SpanAttributeEntity> toSpanAttributes(SpanEntity span) {
        Map<String, Object> attributes = parseJsonObject(span.getAttributesJsonb());
        if (attributes.isEmpty()) {
            return Collections.emptyList();
        }

        List<SpanAttributeEntity> result = new ArrayList<>(attributes.size());
        attributes.forEach((key, value) -> {
            SpanAttributeEntity attr = new SpanAttributeEntity();
            attr.setSpanId(span.getSpanId());
            attr.setTraceId(span.getTraceId());
            attr.setAttributeKey(key);
            attr.setCreatedTime(span.getCreatedTime());
            if (value instanceof Boolean boolValue) {
                attr.setAttributeValueType("BOOLEAN");
                attr.setAttributeValueBoolean(boolValue);
            } else if (value instanceof Number numberValue) {
                attr.setAttributeValueType("DOUBLE");
                attr.setAttributeValueNumber(numberValue.doubleValue());
            } else if (value != null) {
                attr.setAttributeValueType("STRING");
                if (value instanceof Map<?, ?> || value instanceof List<?>) {
                    attr.setAttributeValueString(JacksonUtil.toString(value));
                } else {
                    attr.setAttributeValueString(String.valueOf(value));
                }
            } else {
                attr.setAttributeValueType("STRING");
                attr.setAttributeValueString(null);
            }
            result.add(attr);
        });
        return result;
    }

    private List<SpanEventEntity> toSpanEvents(SpanEntity span) {
        List<Map<String, Object>> events = parseJsonArray(span.getEventsJsonb());
        if (events.isEmpty()) {
            return Collections.emptyList();
        }
        List<SpanEventEntity> result = new ArrayList<>(events.size());
        for (Map<String, Object> event : events) {
            SpanEventEntity entity = new SpanEventEntity();
            entity.setId(parseUuid(event.get("id")));
            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID());
            }
            entity.setSpanId(span.getSpanId());
            entity.setTraceId(span.getTraceId());
            entity.setEventName(asString(firstNonNull(event.get("eventName"), event.get("event_name"))));
            entity.setEventTime(asLong(firstNonNull(event.get("eventTime"), event.get("event_time")), 0L));
            Object attributes = firstNonNull(event.get("attributes"), event.get("attributesJson"), event.get("attributes_json"));
            if (attributes != null) {
                entity.setAttributesJson(attributes instanceof String ? (String) attributes : JacksonUtil.toString(attributes));
            }
            entity.setCreatedTime(asLong(firstNonNull(event.get("createdTime"), event.get("created_time")), span.getCreatedTime()));
            result.add(entity);
        }
        return result;
    }

    private List<SpanLinkEntity> toSpanLinks(SpanEntity span) {
        List<Map<String, Object>> links = parseJsonArray(span.getLinksJsonb());
        if (links.isEmpty()) {
            return Collections.emptyList();
        }
        List<SpanLinkEntity> result = new ArrayList<>(links.size());
        for (Map<String, Object> link : links) {
            SpanLinkEntity entity = new SpanLinkEntity();
            entity.setId(parseUuid(link.get("id")));
            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID());
            }
            entity.setSpanId(span.getSpanId());
            entity.setTraceId(span.getTraceId());
            entity.setLinkedTraceId(asString(firstNonNull(link.get("linkedTraceId"), link.get("linked_trace_id"))));
            entity.setLinkedSpanId(asString(firstNonNull(link.get("linkedSpanId"), link.get("linked_span_id"))));
            Object attributes = firstNonNull(link.get("attributes"), link.get("attributesJson"), link.get("attributes_json"));
            if (attributes != null) {
                entity.setAttributesJson(attributes instanceof String ? (String) attributes : JacksonUtil.toString(attributes));
            }
            entity.setCreatedTime(asLong(firstNonNull(link.get("createdTime"), link.get("created_time")), span.getCreatedTime()));
            result.add(entity);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonObject(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            Object value = JacksonUtil.OBJECT_MAPPER.readValue(json, Map.class);
            return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Collections.emptyMap();
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseJsonArray(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            Object value = JacksonUtil.OBJECT_MAPPER.readValue(json, List.class);
            if (!(value instanceof List<?> list)) {
                return Collections.emptyList();
            }
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object entry : list) {
                if (entry instanceof Map<?, ?> map) {
                    result.add((Map<String, Object>) map);
                }
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private UUID parseUuid(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static long asLong(Object value, long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null && !(value instanceof String str && str.isBlank())) {
                return value;
            }
        }
        return null;
    }
}
