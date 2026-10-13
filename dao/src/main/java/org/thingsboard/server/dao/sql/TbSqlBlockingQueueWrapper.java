// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.sql;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.stats.MessagesStats;
import org.thingsboard.server.common.stats.StatsFactory;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
@Getter
public class TbSqlBlockingQueueWrapper<E, R> {
    private final CopyOnWriteArrayList<TbSqlBlockingQueue<E, R>> queues = new CopyOnWriteArrayList<>();
    private final TbSqlBlockingQueueParams params;
    private final Function<E, Integer> hashCodeFunction;
    private final int maxThreads;
    private final StatsFactory statsFactory;
    private final Function<E, Integer> bucketResolver; // nullable; when set, returns the final queue index directly

    public TbSqlBlockingQueueWrapper(TbSqlBlockingQueueParams params, Function<E, Integer> hashCodeFunction,
                                     int maxThreads, StatsFactory statsFactory) {
        this(params, hashCodeFunction, maxThreads, statsFactory, null);
    }

    public TbSqlBlockingQueueWrapper(TbSqlBlockingQueueParams params, Function<E, Integer> hashCodeFunction,
                                     int maxThreads, StatsFactory statsFactory, Function<E, Integer> bucketResolver) {
        this.params = params;
        this.hashCodeFunction = hashCodeFunction;
        this.maxThreads = maxThreads;
        this.statsFactory = statsFactory;
        this.bucketResolver = bucketResolver;
    }

    /**
     * Starts TbSqlBlockingQueues.
     *
     * @param  logExecutor  executor that will be printing logs and statistics
     * @param  saveFunction function to save entities in database
     * @param  batchUpdateComparator comparator to sort entities by primary key to avoid deadlocks in cluster mode
     *                               NOTE: you must use all of primary key parts in your comparator
     */
    public void init(ScheduledLogExecutorComponent logExecutor, Consumer<List<E>> saveFunction, Comparator<E> batchUpdateComparator) {
        init(logExecutor, l -> { saveFunction.accept(l); return null; }, batchUpdateComparator, l -> l);
    }

    public void init(ScheduledLogExecutorComponent logExecutor, Function<List<E>, List<R>> saveFunction, Comparator<E> batchUpdateComparator, Function<List<TbSqlQueueElement<E, R>>, List<TbSqlQueueElement<E, R>>> filter) {
        init(logExecutor, (bucket, batch) -> saveFunction.apply(batch), batchUpdateComparator, filter);
    }

    /**
     * Same as {@link #init(ScheduledLogExecutorComponent, Function, Comparator, Function)}, but the save callback
     * additionally receives the per-queue bucket index. Queue {@code i} holds exactly the elements whose
     * {@link #bucketResolver} returned {@code i}, so consumers (e.g. Citus smart routing) can flush each queue's
     * batch to the worker that owns that shard via the bucket index.
     */
    public void init(ScheduledLogExecutorComponent logExecutor, BiFunction<Integer, List<E>, List<R>> saveFunction, Comparator<E> batchUpdateComparator, Function<List<TbSqlQueueElement<E, R>>, List<TbSqlQueueElement<E, R>>> filter) {
        for (int i = 0; i < maxThreads; i++) {
            MessagesStats stats = statsFactory.createMessagesStats(params.getStatsNamePrefix() + ".queue." + i);
            TbSqlBlockingQueue<E, R> queue = new TbSqlBlockingQueue<>(params, stats);
            queues.add(queue);
            final int bucket = i;
            Function<List<E>, List<R>> perQueueSave = batch -> saveFunction.apply(bucket, batch);
            queue.init(logExecutor, perQueueSave, batchUpdateComparator, filter, i);
        }
    }

    int resolveBucket(E element) {
        if (element == null) {
            return 0;
        }
        if (bucketResolver != null) {
            int bucket = bucketResolver.apply(element);
            if (bucket >= 0 && bucket < maxThreads) {
                return bucket;
            }
            // On the routed path (e.g. Citus smart routing) the bucket is the index of the queue whose owning
            // shard the batch is flushed to. Coercing an out-of-range bucket into range would silently route the
            // element to the wrong shard, so fail fast instead (shard count != queue count misconfiguration).
            throw new IllegalStateException("Bucket resolver returned out-of-range index " + bucket
                    + " for maxThreads " + maxThreads + " (shard count != queue count misconfiguration?)");
        }
        return (hashCodeFunction.apply(element) & 0x7FFFFFFF) % maxThreads;
    }

    public ListenableFuture<R> add(E element) {
        return queues.get(resolveBucket(element)).add(element);
    }

    public void destroy() {
        queues.forEach(TbSqlBlockingQueue::destroy);
    }
}
