// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus;

import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.VersionedInsertRepository;
import org.thingsboard.server.dao.sql.ScheduledLogExecutorComponent;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueueParams;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueueWrapper;
import org.thingsboard.server.dao.sql.TbSqlQueueElement;
import org.thingsboard.server.dao.sql.citus.routing.CitusShardRouter;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Shared wiring for the two co-located KV write queues ({@code attribute_kv} in {@code JpaAttributeDao} and
 * {@code ts_kv_latest} in {@code SqlTimeseriesLatestDao}). Both implement the identical Citus routing policy —
 * a bucket-partitioned queue whose batches flush straight to the worker that owns each bucket when smart
 * routing is on, and a plain N-thread coordinator queue otherwise. Hoisting the build + init forks here keeps
 * that policy in one place so a future change to the routing contract touches a single site instead of two.
 */
public final class CitusKvWriteQueueSupport {

    private CitusKvWriteQueueSupport() {
    }

    /**
     * Builds the write queue. Each mode's inputs are distinct and only what it needs is supplied:
     * <ul>
     *   <li>Citus (partitioner enabled): the queue is partitioned into {@code partitioner.queueCount()} buckets,
     *       one writer thread per bucket, and the queue index is resolved directly from each entity's id via the
     *       bucket resolver — so no separate hash function is used (the resolver supersedes it).</li>
     *   <li>Plain: a queue with {@code plainBatchThreads} writer threads, where entities are spread across threads
     *       by hashing their entity id (derived from {@code entityIdExtractor}).</li>
     * </ul>
     */
    public static <E, R> TbSqlBlockingQueueWrapper<E, R> buildQueue(TbSqlBlockingQueueParams params,
                                                                    int plainBatchThreads,
                                                                    StatsFactory statsFactory,
                                                                    CitusQueuePartitioner partitioner,
                                                                    Function<E, UUID> entityIdExtractor) {
        if (partitioner != null && partitioner.isEnabled()) {
            Function<E, Integer> bucketResolver = partitioner.bucketResolver(entityIdExtractor);
            return new TbSqlBlockingQueueWrapper<>(params, null, partitioner.queueCount(), statsFactory, bucketResolver);
        }
        Function<E, Integer> hashCodeFunction = entity -> entityIdExtractor.apply(entity).hashCode();
        return new TbSqlBlockingQueueWrapper<>(params, hashCodeFunction, plainBatchThreads, statsFactory);
    }

    /**
     * Initializes the write queue, selecting the versioned upsert overload for the active mode from the single
     * {@code insertRepository}. When the partitioner is enabled AND smart routing is active, each batch is flushed
     * to the worker {@code JdbcTemplate} that owns its bucket through {@code router.routedWrite(bucket, ...)} —
     * the failover-observed write path (a flush failing with a failover signature triggers a debounced routing
     * refresh; the failure itself propagates unchanged, preserving the queue's retry behavior). Otherwise the
     * batch writes through the repository's own coordinator {@code JdbcTemplate}. The two conditions mirror the
     * original DAO forks exactly: a bucket-partitioned queue with no active router still falls back to the
     * coordinator save.
     */
    public static <E> void initQueue(TbSqlBlockingQueueWrapper<E, Long> queue,
                                     ScheduledLogExecutorComponent logExecutor,
                                     CitusQueuePartitioner partitioner,
                                     CitusShardRouter router,
                                     VersionedInsertRepository<E> insertRepository,
                                     Comparator<E> comparator,
                                     Function<List<TbSqlQueueElement<E, Long>>, List<TbSqlQueueElement<E, Long>>> filter) {
        if (partitioner != null && partitioner.isEnabled() && CitusShardRouter.isRouting(router)) {
            queue.init(logExecutor, (bucket, entities) ->
                    router.routedWrite(bucket, template -> insertRepository.saveOrUpdate(template, entities)), comparator, filter);
        } else {
            // Typed local so the overloaded saveOrUpdate resolves to the single-arg (coordinator) form, not the routed one.
            Function<List<E>, List<Long>> plainSave = insertRepository::saveOrUpdate;
            queue.init(logExecutor, plainSave, comparator, filter);
        }
    }

}
