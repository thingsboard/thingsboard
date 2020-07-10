/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.sql;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.msg.stats.MessagesStats;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class TbSqlBlockingQueue<E> implements TbSqlQueue<E> {

    private final BlockingQueue<TbSqlQueueElement<E>> queue = new LinkedBlockingQueue<>();
    private final TbSqlBlockingQueueParams params;

    private ExecutorService executor;
    private ScheduledLogExecutorComponent logExecutor;
    private MessagesStats stats;

    public TbSqlBlockingQueue(TbSqlBlockingQueueParams params) {
        this.params = params;
        this.stats = params.getStats();
    }

    @Override
    public void init(ScheduledLogExecutorComponent logExecutor, Consumer<List<E>> saveFunction) {
        this.logExecutor = logExecutor;
        executor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("sql-queue-" + params.getLogName().toLowerCase()));
        executor.submit(() -> {
            String logName = params.getLogName();
            int batchSize = params.getBatchSize();
            long maxDelay = params.getMaxDelay();
            List<TbSqlQueueElement<E>> entities = new ArrayList<>(batchSize);
            while (!Thread.interrupted()) {
                try {
                    long currentTs = System.currentTimeMillis();
                    TbSqlQueueElement<E> attr = queue.poll(maxDelay, TimeUnit.MILLISECONDS);
                    if (attr == null) {
                        continue;
                    } else {
                        entities.add(attr);
                    }
                    queue.drainTo(entities, batchSize - 1);
                    boolean fullPack = entities.size() == batchSize;
                    log.debug("[{}] Going to save {} entities", logName, entities.size());
                    saveFunction.accept(entities.stream().map(TbSqlQueueElement::getEntity).collect(Collectors.toList()));
                    entities.forEach(v -> v.getFuture().set(null));
                    stats.incrementSuccessful(entities.size());
                    if (!fullPack) {
                        long remainingDelay = maxDelay - (System.currentTimeMillis() - currentTs);
                        if (remainingDelay > 0) {
                            Thread.sleep(remainingDelay);
                        }
                    }
                } catch (Exception e) {
                    stats.incrementFailed(entities.size());
                    entities.forEach(entityFutureWrapper -> entityFutureWrapper.getFuture().setException(e));
                    if (e instanceof InterruptedException) {
                        log.info("[{}] Queue polling was interrupted", logName);
                        break;
                    } else {
                        log.error("[{}] Failed to save {} entities", logName, entities.size(), e);
                    }
                } finally {
                    entities.clear();
                }
            }
        });

        logExecutor.scheduleAtFixedRate(() -> {
            if (queue.size() > 0 || stats.getTotal() > 0 || stats.getSuccessful() > 0 || stats.getFailed() > 0) {
                log.info("[{}] queueSize [{}] totalAdded [{}] totalSaved [{}] totalFailed [{}]",
                        params.getLogName(), queue.size(), stats.getTotal(), stats.getSuccessful(), stats.getFailed());
                stats.reset();
            }
        }, params.getStatsPrintIntervalMs(), params.getStatsPrintIntervalMs(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void destroy() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Override
    public ListenableFuture<Void> add(E element) {
        SettableFuture<Void> future = SettableFuture.create();
        queue.add(new TbSqlQueueElement<>(future, element));
        params.getStats().incrementTotal();
        return future;
    }
}
