// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.trace;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.trace.TraceCoverageSetting;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.BufferedSpan;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.TraceGroup;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.TraceGroup.RuleNodePathMetric;
import org.thingsboard.server.dao.model.sql.trace.SpanEntity;
import org.thingsboard.server.dao.sql.trace.TraceDao;
import org.thingsboard.server.dao.sql.trace.TraceSummaryInfo;
import org.thingsboard.server.dao.tenant.TenantService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TraceGroupPersistenceServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final long BUCKET_TIME = 86_400_000L;
    private static final long NOW = BUCKET_TIME + 3_600_000L;
    private static final int SAMPLE_INTERVAL_SECONDS = 30;

    private TraceDao traceDao;
    private TraceCoverageSettingService coverageSettingService;
    private TenantService tenantService;
    private TraceGroupPersistenceService service;
    private TraceCoverageSetting setting;

    @BeforeEach
    void setUp() {
        traceDao = mock(TraceDao.class);
        coverageSettingService = mock(TraceCoverageSettingService.class);
        tenantService = mock(TenantService.class);
        when(tenantService.tenantExists(TenantId.fromUUID(TENANT_ID))).thenReturn(true);
        service = new TraceGroupPersistenceService(traceDao, coverageSettingService, tenantService);
        setting = TraceCoverageSetting.defaultSetting(TENANT_ID);
        setting.setRelatedTraceSampleIntervalSeconds(SAMPLE_INTERVAL_SECONDS);
        setting.setMaxTraceGroupsPerDay(0);
        when(coverageSettingService.findTraceCoverageSettingByTenantId(TenantId.fromUUID(TENANT_ID))).thenReturn(setting);
        givenExistingGroups();
    }

    @Test
    void givenNewPath_whenPersist_thenGroupIsInsertedWithItsSampledTraceAndSpans() {
        TraceSummaryInfo summary = mock(TraceSummaryInfo.class);

        service.persist(BUCKET_TIME, NOW, List.of(contribution("path-1", "trace-1")),
                Map.of("trace-1", trace("trace-1")), Map.of("trace-1", summary));

        TraceGroup inserted = single(insertedGroups());
        assertThat(inserted.getPathId()).isEqualTo("path-1");
        assertThat(inserted.getRelatedTraceIds()).containsExactly("trace-1");
        assertThat(inserted.getLastRelatedTraceAddedTime()).isEqualTo(NOW);
        assertThat(inserted.getStoredTraces()).isEqualTo(1);
        assertThat(updatedGroups()).isEmpty();
        assertThat(savedSpans()).extracting(SpanEntity::getSpanId).containsExactly("span-1");
        assertThat(savedSummaries()).containsExactly(summary);
    }

    @Test
    void givenDeletedTenant_whenPersist_thenNothingIsSaved() {
        when(tenantService.tenantExists(TenantId.fromUUID(TENANT_ID))).thenReturn(false);

        service.persist(BUCKET_TIME, NOW, List.of(contribution("path-1", "trace-1")),
                Map.of("trace-1", trace("trace-1")), Map.of("trace-1", mock(TraceSummaryInfo.class)));

        verify(traceDao, never()).saveTraceBatch(any(), any(), any(), any());
    }

    @Test
    void givenDailyLimit_whenNewPathsExceedIt_thenOnlyTheAllowedNumberIsInsertedAndKnownPathsKeepUpdating() {
        setting.setMaxTraceGroupsPerDay(2);
        when(traceDao.countTraceGroups(BUCKET_TIME, TENANT_ID)).thenReturn(1L);
        givenExistingGroups(group("known", 0L, List.of()));

        service.persist(BUCKET_TIME, NOW, List.of(
                        contribution("new-1", null), contribution("new-2", null), contribution("known", null)),
                Map.of(), Map.of());

        // One group exists today and the limit is two, so only the first new path of the batch is admitted.
        assertThat(insertedGroups()).extracting(TraceGroup::getPathId).containsExactly("new-1");
        assertThat(updatedGroups()).extracting(TraceGroup::getPathId).containsExactly("known");
    }

    @Test
    void givenRecentRelatedTrace_whenPersist_thenNoNewTraceIsSampled() {
        givenExistingGroups(group("path-1", NOW - (SAMPLE_INTERVAL_SECONDS - 1) * 1000L, List.of("old-trace")));

        service.persist(BUCKET_TIME, NOW, List.of(contribution("path-1", "trace-1")),
                Map.of("trace-1", trace("trace-1")), Map.of());

        TraceGroup updated = single(updatedGroups());
        assertThat(updated.getRelatedTraceIds()).containsExactly("old-trace");
        assertThat(updated.getLastRelatedTraceAddedTime()).isEqualTo(NOW - (SAMPLE_INTERVAL_SECONDS - 1) * 1000L);
        assertThat(savedSpans()).isEmpty();
    }

    @Test
    void givenSampleIntervalPassed_whenPersist_thenTheCandidateIsAddedToRelatedTraces() {
        givenExistingGroups(group("path-1", NOW - SAMPLE_INTERVAL_SECONDS * 1000L, List.of("old-trace")));

        service.persist(BUCKET_TIME, NOW, List.of(contribution("path-1", "trace-1")),
                Map.of("trace-1", trace("trace-1")), Map.of());

        TraceGroup updated = single(updatedGroups());
        assertThat(updated.getRelatedTraceIds()).containsExactly("old-trace", "trace-1");
        assertThat(updated.getLastRelatedTraceAddedTime()).isEqualTo(NOW);
        assertThat(updated.getStoredTraces()).isEqualTo(2);
        assertThat(savedSpans()).hasSize(1);
    }

    @Test
    void givenExistingGroup_whenPersist_thenContributionIsMergedIntoIt() {
        TraceGroup existing = group("path-1", NOW, List.of("old-trace"));
        existing.setProcessedTraces(5);
        existing.setProcessedTracesWithError(1);
        existing.setProcessedTracesWithTimeout(2);
        existing.setTotalTime(100);
        existing.setMaxTime(40);
        existing.setTotalInQueueTime(10);
        existing.setTotalRuleNodeProcessingTime(90);
        existing.setLastObserved(NOW - 5_000);
        existing.getRuleNodePathMetrics().put("node-1", new RuleNodePathMetric(100, 5, 1, null));
        givenExistingGroups(existing);

        TraceGroupContribution contribution = contribution("path-1", null);
        TraceGroup metrics = contribution.metrics();
        metrics.setProcessedTraces(2);
        metrics.setProcessedTracesWithError(1);
        metrics.setTotalTime(30);
        metrics.setMaxTime(50);
        metrics.setTotalInQueueTime(4);
        metrics.setTotalRuleNodeProcessingTime(26);
        metrics.setLastObserved(NOW);
        metrics.getRuleNodePathMetrics().put("node-1", new RuleNodePathMetric(30, 2, 1, null));

        service.persist(BUCKET_TIME, NOW, List.of(contribution), Map.of(), Map.of());

        TraceGroup merged = single(updatedGroups());
        assertThat(merged.getProcessedTraces()).isEqualTo(7);
        assertThat(merged.getProcessedTracesWithError()).isEqualTo(2);
        assertThat(merged.getProcessedTracesWithTimeout()).isEqualTo(2);
        assertThat(merged.getTotalTime()).isEqualTo(130);
        assertThat(merged.getMaxTime()).isEqualTo(50);
        assertThat(merged.getTotalInQueueTime()).isEqualTo(14);
        assertThat(merged.getTotalRuleNodeProcessingTime()).isEqualTo(116);
        assertThat(merged.getLastObserved()).isEqualTo(NOW);
        assertThat(merged.getStoredTraces()).isEqualTo(1);
        RuleNodePathMetric nodeMetric = merged.getRuleNodePathMetrics().get("node-1");
        assertThat(nodeMetric.getTotalTime()).isEqualTo(130);
        assertThat(nodeMetric.getTotalCount()).isEqualTo(7);
        assertThat(nodeMetric.getErrors()).isEqualTo(2);
    }

    private void givenExistingGroups(TraceGroup... groups) {
        Map<String, TraceGroup> byPathId = new HashMap<>();
        for (TraceGroup group : groups) {
            byPathId.put(group.getPathId(), group);
        }
        when(traceDao.findTraceGroups(anyLong(), any())).thenReturn(byPathId);
    }

    private static TraceGroupContribution contribution(String pathId, String sampleCandidateTraceId) {
        return new TraceGroupContribution(group(pathId, 0L, List.of()), sampleCandidateTraceId);
    }

    private static TraceGroup group(String pathId, long lastRelatedTraceAddedTime, List<String> relatedTraceIds) {
        TraceGroup group = new TraceGroup();
        group.setBucketTime(BUCKET_TIME);
        group.setTenantId(TENANT_ID);
        group.setPathId(pathId);
        group.setRelatedTraceIds(new ArrayList<>(relatedTraceIds));
        group.setLastRelatedTraceAddedTime(lastRelatedTraceAddedTime);
        group.setStoredTraces(relatedTraceIds.size());
        group.setRuleNodePathMetrics(new HashMap<>());
        return group;
    }

    private static BufferedTrace trace(String traceId) {
        BufferedSpan span = new BufferedSpan();
        span.setSpanId("span-1");
        span.setTraceId(traceId);
        span.setTenantId(TENANT_ID.toString());
        span.setName("[Root Rule Chain] [node]");
        span.setStartTime(0L);
        span.setEndTime(10L);
        return new BufferedTrace(TENANT_ID, traceId, false, Set.of(span));
    }

    @SuppressWarnings("unchecked")
    private List<TraceGroup> insertedGroups() {
        ArgumentCaptor<Collection<TraceGroup>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(traceDao).saveTraceBatch(captor.capture(), any(), any(), any());
        return new ArrayList<>(captor.getValue());
    }

    @SuppressWarnings("unchecked")
    private List<TraceGroup> updatedGroups() {
        ArgumentCaptor<Collection<TraceGroup>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(traceDao).saveTraceBatch(any(), captor.capture(), any(), any());
        return new ArrayList<>(captor.getValue());
    }

    @SuppressWarnings("unchecked")
    private List<SpanEntity> savedSpans() {
        ArgumentCaptor<List<SpanEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(traceDao).saveTraceBatch(any(), any(), captor.capture(), any());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private List<TraceSummaryInfo> savedSummaries() {
        ArgumentCaptor<Collection<TraceSummaryInfo>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(traceDao).saveTraceBatch(any(), any(), any(), captor.capture());
        return new ArrayList<>(captor.getValue());
    }

    private static TraceGroup single(List<TraceGroup> groups) {
        assertThat(groups).hasSize(1);
        return groups.get(0);
    }

}
