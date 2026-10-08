package org.thingsboard.server.service.trace;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.trace.TraceCoverageSetting;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.BufferedSpan;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.TraceGroup;
import org.thingsboard.server.dao.model.sql.trace.SpanEntity;
import org.thingsboard.server.dao.sql.trace.TraceDao;
import org.thingsboard.server.dao.sql.trace.TraceSummaryInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TraceGroupPersistenceService {
    private final TraceDao traceDao;
    private final TraceCoverageSettingService traceCoverageSettingService;

    public TraceGroupPersistenceService(TraceDao traceDao, TraceCoverageSettingService traceCoverageSettingService) {
        this.traceDao = traceDao;
        this.traceCoverageSettingService = traceCoverageSettingService;
    }

    @Transactional
    public void persist(long bucketTime, long now, Collection<TraceGroupContribution> contributions,
                        Map<String, LoadedTrace> tracesByTraceId,
                        Map<String, TraceSummaryInfo> summariesByTraceId) {
        if (CollectionUtils.isEmpty(contributions)) {
            return;
        }
        List<TraceGroup> metrics = contributions.stream().map(TraceGroupContribution::metrics).toList();
        Map<String, TraceGroup> existingByPathId = traceDao.findTraceGroups(bucketTime, metrics);

        List<TraceGroup> toInsert = new ArrayList<>();
        List<TraceGroup> toUpdate = new ArrayList<>();

        List<String> sampledTraceIds = new ArrayList<>();

        Map<UUID, TraceCoverageSetting> settingByTenant = new HashMap<>();
        Map<UUID, Long> existingCountByTenant = new HashMap<>();
        Map<UUID, Integer> admittedByTenant = new HashMap<>();

        for (TraceGroupContribution contribution : contributions) {
            TraceGroup group = contribution.metrics();
            UUID tenantId = group.getTenantId();
            TraceGroup existing = existingByPathId.get(group.getPathId());

            if (existing == null
                    && isOverTraceGroupLimit(tenantId, bucketTime, settingByTenant, existingCountByTenant, admittedByTenant)) {
                log.debug("[{}] Trace group daily limit reached; dropping new path {}", tenantId, group.getPathId());
                continue;
            }

            long lastAdded = existing != null ? existing.getLastRelatedTraceAddedTime() : 0L;
            long intervalMs = resolveSampleIntervalMs(tenantId, settingByTenant);
            String sampledTraceId = (contribution.sampleCandidateTraceId() != null && now - lastAdded >= intervalMs)
                    ? contribution.sampleCandidateTraceId()
                    : null;
            if (existing == null) {
                admittedByTenant.merge(tenantId, 1, Integer::sum);
                prepareNewGroup(group, sampledTraceId, now, toInsert);
            } else {
                prepareExistingGroup(group, existing, sampledTraceId, now, toUpdate);
            }
            if (sampledTraceId != null) {
                sampledTraceIds.add(sampledTraceId);
            }
        }

        traceDao.saveTraceGroups(toInsert, toUpdate);
        saveSampledSpans(sampledTraceIds, tracesByTraceId);
        traceDao.upsertTraceSummaries(summaryInfoByTraceId(sampledTraceIds, summariesByTraceId));
    }

    private long resolveSampleIntervalMs(UUID tenantId, Map<UUID, TraceCoverageSetting> settingCache) {
        return TimeUnit.SECONDS.toMillis(resolveSetting(tenantId, settingCache).getRelatedTraceSampleIntervalSeconds());
    }

    private TraceCoverageSetting resolveSetting(UUID tenantId, Map<UUID, TraceCoverageSetting> settingCache) {
        return settingCache.computeIfAbsent(tenantId,
                id -> traceCoverageSettingService.findTraceCoverageSettingByTenantId(TenantId.fromUUID(id)));
    }

    private boolean isOverTraceGroupLimit(UUID tenantId, long bucketTime,
                                          Map<UUID, TraceCoverageSetting> settingCache,
                                          Map<UUID, Long> existingCountByTenant,
                                          Map<UUID, Integer> admittedByTenant) {
        int limit = resolveSetting(tenantId, settingCache).getMaxTraceGroupsPerDay();
        if (limit <= 0) {
            return false;
        }
        long existingCount = existingCountByTenant.computeIfAbsent(tenantId,
                id -> traceDao.countTraceGroups(bucketTime, id));
        int admitted = admittedByTenant.getOrDefault(tenantId, 0);
        return existingCount + admitted >= limit;
    }


    private void prepareNewGroup(TraceGroup group, String sampledTraceId, long now, List<TraceGroup> toInsert) {
        if (sampledTraceId != null) {
            group.setRelatedTraceIds(new ArrayList<>(List.of(sampledTraceId)));
            group.setLastRelatedTraceAddedTime(now);
            group.setStoredTraces(1);
        } else {
            group.setRelatedTraceIds(new ArrayList<>());
            group.setLastRelatedTraceAddedTime(0L);
            group.setStoredTraces(0);
        }
        toInsert.add(group);
    }

    private void prepareExistingGroup(TraceGroup group, TraceGroup existing, String sampledTraceId, long now,
                                      List<TraceGroup> toUpdate) {
        TraceGroup merged = mergeWithExisting(existing, group);
        List<String> relatedTraceIds = new ArrayList<>(
                existing.getRelatedTraceIds() != null ? existing.getRelatedTraceIds() : List.of());
        if (sampledTraceId != null) {
            relatedTraceIds.add(sampledTraceId);
            merged.setLastRelatedTraceAddedTime(now);
            merged.setStoredTraces(merged.getStoredTraces() + 1);
        } else {
            merged.setLastRelatedTraceAddedTime(existing.getLastRelatedTraceAddedTime());
        }
        merged.setRelatedTraceIds(relatedTraceIds);
        toUpdate.add(merged);
    }

    private TraceGroup mergeWithExisting(TraceGroup existing, TraceGroup contribution) {
        TraceGroup merged = new TraceGroup();
        merged.setBucketTime(existing.getBucketTime());
        merged.setTenantId(existing.getTenantId());
        merged.setPathId(existing.getPathId());
        merged.setRootQueueId(existing.getRootQueueId());
        merged.setRootMessageType(existing.getRootMessageType());
        merged.setRootRuleChainId(existing.getRootRuleChainId());
        merged.setVisitedRuleNodes(existing.getVisitedRuleNodes());
        merged.setTotalTime(existing.getTotalTime());
        merged.setMaxTime(existing.getMaxTime());
        merged.setTotalInQueueTime(existing.getTotalInQueueTime());
        merged.setTotalRuleNodeProcessingTime(existing.getTotalRuleNodeProcessingTime());
        merged.setProcessedTraces(existing.getProcessedTraces());
        merged.setProcessedTracesWithTimeout(existing.getProcessedTracesWithTimeout());
        merged.setProcessedTracesWithError(existing.getProcessedTracesWithError());

        merged.setStoredTraces(existing.getStoredTraces());
        merged.setLastObserved(existing.getLastObserved());
        merged.setRuleNodePathMetrics(existing.getRuleNodePathMetrics() != null
                ? new HashMap<>(existing.getRuleNodePathMetrics())
                : new HashMap<>());
        merged.setProcessedTraces(merged.getProcessedTraces() + contribution.getProcessedTraces());
        merged.setProcessedTracesWithTimeout(merged.getProcessedTracesWithTimeout() + contribution.getProcessedTracesWithTimeout());
        merged.setProcessedTracesWithError(merged.getProcessedTracesWithError() + contribution.getProcessedTracesWithError());
        merged.setLastObserved(Math.max(merged.getLastObserved(), contribution.getLastObserved()));
        merged.setTotalTime(merged.getTotalTime() + contribution.getTotalTime());
        merged.setMaxTime(Math.max(merged.getMaxTime(), contribution.getMaxTime()));
        merged.setTotalInQueueTime(merged.getTotalInQueueTime() + contribution.getTotalInQueueTime());
        merged.setTotalRuleNodeProcessingTime(merged.getTotalRuleNodeProcessingTime() + contribution.getTotalRuleNodeProcessingTime());
        mergeRuleNodePathMetrics(merged.getRuleNodePathMetrics(), contribution.getRuleNodePathMetrics());
        return merged;
    }

    private void mergeRuleNodePathMetrics(Map<String, TraceGroup.RuleNodePathMetric> target,
                                          Map<String, TraceGroup.RuleNodePathMetric> source) {
        if (source == null) {
            return;
        }
        source.forEach((ruleNodeId, metric) -> {
            TraceGroup.RuleNodePathMetric acc = target.computeIfAbsent(ruleNodeId,
                    k -> new TraceGroup.RuleNodePathMetric(0L, 0L, 0L, metric.getRuleChainId()));
            acc.setTotalTime(acc.getTotalTime() + metric.getTotalTime());
            acc.setTotalCount(acc.getTotalCount() + metric.getTotalCount());
            acc.setErrors(acc.getErrors() + metric.getErrors());
            if (acc.getRuleChainId() == null) {
                acc.setRuleChainId(metric.getRuleChainId());
            }
        });
    }

    private List<TraceSummaryInfo> summaryInfoByTraceId(List<String> traceIds,
                                                        Map<String, TraceSummaryInfo> summariesByTraceId) {
        List<TraceSummaryInfo> result = new ArrayList<>();
        for (String traceId : traceIds) {
            TraceSummaryInfo summaryInfo = summariesByTraceId != null ? summariesByTraceId.get(traceId) : null;
            if (summaryInfo != null) {
                result.add(summaryInfo);
            }
        }
        return result;
    }

    private void saveSampledSpans(List<String> traceIds, Map<String, LoadedTrace> tracesByTraceId) {
        if (CollectionUtils.isEmpty(traceIds)) {
            return;
        }
        List<SpanEntity> spanEntities = new ArrayList<>();
        for (String traceId : traceIds) {
            LoadedTrace loaded = tracesByTraceId.get(traceId);
            if (loaded == null || loaded.trace() == null || CollectionUtils.isEmpty(loaded.trace().spans())) {
                continue;
            }
            for (BufferedSpan span : loaded.trace().spans()) {
                spanEntities.add(toSpanEntity(span));
            }
        }
        traceDao.saveSpans(spanEntities);
    }

    private SpanEntity toSpanEntity(BufferedSpan span) {
        Long endTime = span.getEndTime();
        Long startTime = span.getStartTime();
        Long duration = (startTime != null && endTime != null) ? Math.max(endTime - startTime, 0L) : null;
        return new SpanEntity(
                span.getSpanId(),
                span.getTraceId(),
                span.getTenantId() != null ? UUID.fromString(span.getTenantId()) : null,
                span.getParentSpanId(),
                span.getName(),
                null,
                span.getServiceName(),
                span.getAttributesJsonb() != null ? org.thingsboard.common.util.JacksonUtil.toString(span.getAttributesJsonb()) : null,
                span.getEventsJsonb() != null ? org.thingsboard.common.util.JacksonUtil.toString(span.getEventsJsonb()) : null,
                null,
                startTime != null ? startTime : 0L,
                endTime,
                duration,
                span.getStatusCode(),
                span.getStatusMessage(),
                span.getCreatedTime());
    }
}
