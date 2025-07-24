package org.thingsboard.server.service.edge;

import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.service.edge.stats.EdgeStatsCounterService;
import org.thingsboard.server.service.edge.stats.EdgeStatsService;
import org.thingsboard.server.service.edge.stats.MsgCounters;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EdgeStatsReporterTest {

    private EdgeStatsCounterService statsCounterService;
    private TimeseriesService tsService;
    private EdgeStatsService edgeStatsService;

    @Before
    public void setUp() throws Exception {
        statsCounterService = mock(EdgeStatsCounterService.class);
        tsService = mock(TimeseriesService.class);
        TopicService topicService = mock(TopicService.class);
        edgeStatsService = new EdgeStatsService(tsService, statsCounterService, topicService, Optional.empty());

        setField(edgeStatsService, "edgesStatsTtlDays", 30);
        setField(edgeStatsService, "reportIntervalMillis", 600_000L);
    }

    public static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    public void testReportStats_withConcurrentUpdates_shouldNotThrowExceptions() throws InterruptedException {
        // given
        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        EdgeId edgeId = new EdgeId(UUID.randomUUID());

        MsgCounters counters = new MsgCounters(tenantId);
        counters.getMsgsAdded().set(5L);

        ConcurrentHashMap<EdgeId, MsgCounters> concurrentMap = new ConcurrentHashMap<>();
        concurrentMap.put(edgeId, counters);

        when(statsCounterService.getCounterByEdge()).thenReturn(concurrentMap);

        // simulate concurrent clearing while iterating
        doAnswer(invocation -> {
            concurrentMap.remove(edgeId);
            return null;
        }).when(statsCounterService).clear(edgeId);

        // when
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<Void> reporterTask = () -> {
            edgeStatsService.reportStats();
            return null;
        };

        Callable<Void> updaterTask = () -> {
            for (int i = 0; i < 10; i++) {
                concurrentMap.put(edgeId, counters);
                Thread.sleep(10);
            }
            return null;
        };

        List<Callable<Void>> tasks = Arrays.asList(reporterTask, updaterTask);

        // then
        executor.invokeAll(tasks);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // No exception should be thrown
        verify(tsService, atLeastOnce()).save(eq(tenantId), eq(edgeId), anyList(), anyLong());
        verify(statsCounterService, atLeastOnce()).clear(edgeId);
    }

}