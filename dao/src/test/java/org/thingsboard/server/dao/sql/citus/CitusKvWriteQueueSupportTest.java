// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.thingsboard.server.dao.VersionedInsertRepository;
import org.thingsboard.server.dao.sql.ScheduledLogExecutorComponent;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueueWrapper;
import org.thingsboard.server.dao.sql.TbSqlQueueElement;
import org.thingsboard.server.dao.sql.citus.routing.CitusShardRouter;

import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins {@link CitusKvWriteQueueSupport#initQueue}'s mode fork. The "Citus enabled but smart routing
 * inactive" combination (partitioner enabled, router null or not routing) is a supported operator
 * configuration ({@code DATABASE_CITUS_SMART_ROUTING_ENABLED=false} with Citus on) that no
 * container-backed test exercises — the Citus test harness hardcodes smart routing on — so the
 * coordinator fallback to the single-arg {@code saveOrUpdate} overload is asserted here, with the
 * routed branch for contrast.
 */
class CitusKvWriteQueueSupportTest {

    @SuppressWarnings("unchecked")
    private final TbSqlBlockingQueueWrapper<String, Long> queue = mock(TbSqlBlockingQueueWrapper.class);
    private final ScheduledLogExecutorComponent logExecutor = mock(ScheduledLogExecutorComponent.class);
    private final CitusQueuePartitioner partitioner = mock(CitusQueuePartitioner.class);
    @SuppressWarnings("unchecked")
    private final VersionedInsertRepository<String> insertRepository = mock(VersionedInsertRepository.class);
    private final Comparator<String> comparator = Comparator.naturalOrder();
    private final Function<List<TbSqlQueueElement<String, Long>>, List<TbSqlQueueElement<String, Long>>> filter =
            Function.identity();

    /**
     * Asserts initQueue fell back to the plain (non-partitioned-flush) init: the queue's save function is
     * the single-arg coordinator {@code insertRepository.saveOrUpdate(List)} overload, never the routed
     * two-arg one.
     */
    @SuppressWarnings("unchecked")
    private void assertCoordinatorFallback(CitusShardRouter router) {
        when(partitioner.isEnabled()).thenReturn(true);

        CitusKvWriteQueueSupport.initQueue(queue, logExecutor, partitioner, router, insertRepository, comparator, filter);

        ArgumentCaptor<Function<List<String>, List<Long>>> saveFunction =
                (ArgumentCaptor<Function<List<String>, List<Long>>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(Function.class);
        verify(queue).init(eq(logExecutor), saveFunction.capture(), eq(comparator), eq(filter));

        List<String> entities = List.of("a", "b");
        when(insertRepository.saveOrUpdate(entities)).thenReturn(List.of(1L, 2L));

        assertThat(saveFunction.getValue().apply(entities)).containsExactly(1L, 2L);
        verify(insertRepository).saveOrUpdate(entities);
        verify(insertRepository, never()).saveOrUpdate(any(JdbcTemplate.class), anyList());
    }

    @Test
    void enabledPartitionerWithNullRouterFallsBackToCoordinatorSave() {
        assertCoordinatorFallback(null);
    }

    @Test
    void enabledPartitionerWithNotRoutingRouterFallsBackToCoordinatorSave() {
        CitusShardRouter router = mock(CitusShardRouter.class);
        when(router.isEnabled()).thenReturn(false);

        assertCoordinatorFallback(router);
    }

    @Test
    @SuppressWarnings("unchecked")
    void enabledPartitionerWithRoutingRouterFlushesBucketsThroughRoutedWrite() {
        when(partitioner.isEnabled()).thenReturn(true);
        CitusShardRouter router = mock(CitusShardRouter.class);
        when(router.isEnabled()).thenReturn(true);
        JdbcTemplate workerTemplate = mock(JdbcTemplate.class);
        // routedWrite hands the bucket owner's template to the flush function; emulate that.
        when(router.routedWrite(eq(5), any())).thenAnswer(invocation ->
                invocation.getArgument(1, Function.class).apply(workerTemplate));

        CitusKvWriteQueueSupport.initQueue(queue, logExecutor, partitioner, router, insertRepository, comparator, filter);

        ArgumentCaptor<BiFunction<Integer, List<String>, List<Long>>> saveFunction =
                (ArgumentCaptor<BiFunction<Integer, List<String>, List<Long>>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(BiFunction.class);
        verify(queue).init(eq(logExecutor), saveFunction.capture(), eq(comparator), eq(filter));

        List<String> entities = List.of("a", "b");
        when(insertRepository.saveOrUpdate(workerTemplate, entities)).thenReturn(List.of(1L, 2L));

        assertThat(saveFunction.getValue().apply(5, entities)).containsExactly(1L, 2L);
        verify(insertRepository).saveOrUpdate(workerTemplate, entities);
        verify(insertRepository, never()).saveOrUpdate(anyList());
    }
}
