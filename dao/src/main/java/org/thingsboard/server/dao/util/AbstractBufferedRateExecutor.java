/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.dao.util;

import com.datastax.oss.driver.api.core.ProtocolVersion;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.ColumnDefinition;
import com.datastax.oss.driver.api.core.cql.ColumnDefinitions;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.type.DataType;
import com.datastax.oss.driver.api.core.type.codec.TypeCodec;
import com.datastax.oss.driver.api.core.type.codec.registry.CodecRegistry;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.common.data.exception.RateLimitExceededException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.stats.DefaultCounter;
import org.thingsboard.server.common.stats.StatsCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsType;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.nosql.CassandraStatementTask;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;

/**
 * Created by ashvayka on 24.10.18.
 */
@Slf4j
public abstract class AbstractBufferedRateExecutor<T extends AsyncTask, F extends ListenableFuture<V>, V> implements BufferedRateExecutor<T, F> {

    public static final String CONCURRENCY_LEVEL = "currBuffer";

    private final long maxWaitTime;
    private final long pollMs;
    private final String bufferName;
    private final BlockingQueue<AsyncTaskContext<T, V>> queue;
    private final ExecutorService dispatcherExecutor;
    private final ExecutorService callbackExecutor;
    private final ScheduledExecutorService timeoutExecutor;
    private final int concurrencyLimit;
    private final int printQueriesFreq;

    private final AtomicInteger printQueriesIdx = new AtomicInteger(0);

    protected final AtomicInteger concurrencyLevel;
    protected final BufferedRateExecutorStats stats;

    private final EntityService entityService;
    private final RateLimitService rateLimitService;

    private final boolean printTenantNames;
    private final Map<TenantId, String> tenantNamesCache = new HashMap<>();

    private final LimitedApi myLimitedApi;

    public AbstractBufferedRateExecutor(int queueLimit, int concurrencyLimit, long maxWaitTime, int dispatcherThreads,
                                        int callbackThreads, long pollMs, int printQueriesFreq, BufferedRateExecutorType executorType, TbServiceInfoProvider serviceInfoProvider,
                                        RateLimitService rateLimitService, StatsFactory statsFactory, EntityService entityService, boolean printTenantNames) {
        this.maxWaitTime = maxWaitTime;
        this.pollMs = pollMs;
        this.bufferName = executorType.getDisplayName();
        this.myLimitedApi = resolveLimitedApi(serviceInfoProvider, executorType);
        this.concurrencyLimit = concurrencyLimit;
        this.printQueriesFreq = printQueriesFreq;
        this.queue = new LinkedBlockingDeque<>(queueLimit);
        this.dispatcherExecutor = Executors.newFixedThreadPool(dispatcherThreads, ThingsBoardThreadFactory.forName("nosql-" + bufferName + "-dispatcher"));
        this.callbackExecutor = ThingsBoardExecutors.newWorkStealingPool(callbackThreads, "nosql-" + bufferName + "-callback");
        this.timeoutExecutor = ThingsBoardExecutors.newSingleThreadScheduledExecutor("nosql-" + bufferName + "-timeout");
        this.stats = new BufferedRateExecutorStats(statsFactory);
        String concurrencyLevelKey = StatsType.RATE_EXECUTOR.getName() + "." + CONCURRENCY_LEVEL + bufferName; //metric name may change with buffer name suffix
        this.concurrencyLevel = statsFactory.createGauge(concurrencyLevelKey, new AtomicInteger(0));

        this.entityService = entityService;
        this.rateLimitService = rateLimitService;
        this.printTenantNames = printTenantNames;

        for (int i = 0; i < dispatcherThreads; i++) {
            dispatcherExecutor.submit(this::dispatch);
        }
    }

    @Override
    public F submit(T task) {
        SettableFuture<V> settableFuture = create();
        F result = wrap(task, settableFuture);

        boolean perTenantLimitReached = false;
        TenantId tenantId = task.getTenantId();
        if (tenantId != null && !tenantId.isSysTenantId()) {
            if (!rateLimitService.checkRateLimit(myLimitedApi, tenantId, tenantId, true)) {
                stats.incrementRateLimitedTenant(tenantId);
                stats.getTotalRateLimited().increment();
                settableFuture.setException(new RateLimitExceededException(myLimitedApi));
                perTenantLimitReached = true;
            }
        } else if (tenantId == null) {
            log.info("[{}] Invalid task received: {}", bufferName, task);
        }

        if (!perTenantLimitReached) {
            try {
                stats.getTotalAdded().increment();
                queue.add(new AsyncTaskContext<>(UUID.randomUUID(), task, settableFuture, System.currentTimeMillis()));
            } catch (IllegalStateException e) {
                stats.getTotalRejected().increment();
                settableFuture.setException(e);
            }
        }
        return result;
    }

    private LimitedApi resolveLimitedApi(TbServiceInfoProvider serviceInfoProvider, BufferedRateExecutorType executorType) {
        if (serviceInfoProvider == null || serviceInfoProvider.isMonolith()) {
            return executorType.getMonolithLimitedApi();
        }
        if (serviceInfoProvider.isService(ServiceType.TB_RULE_ENGINE)) {
            return executorType.getRuleEngineLimitedApi();
        }
        return executorType.getCoreLimitedApi();
    }

    public void stop() {
        if (dispatcherExecutor != null) {
            dispatcherExecutor.shutdownNow();
        }
        if (callbackExecutor != null) {
            callbackExecutor.shutdownNow();
        }
        if (timeoutExecutor != null) {
            timeoutExecutor.shutdownNow();
        }
    }

    protected abstract SettableFuture<V> create();

    protected abstract F wrap(T task, SettableFuture<V> future);

    protected abstract ListenableFuture<V> execute(AsyncTaskContext<T, V> taskCtx);

    private void dispatch() {
        log.info("[{}] Buffered rate executor thread started", bufferName);
        while (!Thread.interrupted()) {
            int curLvl = concurrencyLevel.get();
            AsyncTaskContext<T, V> taskCtx = null;
            try {
                if (curLvl <= concurrencyLimit) {
                    taskCtx = queue.take();
                    final AsyncTaskContext<T, V> finalTaskCtx = taskCtx;
                    if (printQueriesFreq > 0) {
                        if (printQueriesIdx.incrementAndGet() >= printQueriesFreq) {
                            printQueriesIdx.set(0);
                            String query = queryToString(finalTaskCtx);
                            log.info("[{}][{}] Cassandra query: {}", bufferName, taskCtx.getId(), query);
                        }
                    }
                    logTask("Processing", finalTaskCtx);
                    concurrencyLevel.incrementAndGet();
                    long timeout = finalTaskCtx.getCreateTime() + maxWaitTime - System.currentTimeMillis();
                    if (timeout > 0) {
                        stats.getTotalLaunched().increment();
                        ListenableFuture<V> result = execute(finalTaskCtx);
                        result = Futures.withTimeout(result, timeout, TimeUnit.MILLISECONDS, timeoutExecutor);
                        Futures.addCallback(result, new FutureCallback<V>() {
                            @Override
                            public void onSuccess(@Nullable V result) {
                                logTask("Releasing", finalTaskCtx);
                                stats.getTotalReleased().increment();
                                concurrencyLevel.decrementAndGet();
                                finalTaskCtx.getFuture().set(result);
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                if (t instanceof TimeoutException) {
                                    logTask("Expired During Execution", finalTaskCtx);
                                } else {
                                    logTask("Failed", finalTaskCtx);
                                }
                                stats.getTotalFailed().increment();
                                concurrencyLevel.decrementAndGet();
                                finalTaskCtx.getFuture().setException(t);
                                log.debug("[{}] Failed to execute task: {}", finalTaskCtx.getId(), finalTaskCtx.getTask(), t);
                            }
                        }, callbackExecutor);
                    } else {
                        logTask("Expired Before Execution", finalTaskCtx);
                        stats.getTotalExpired().increment();
                        concurrencyLevel.decrementAndGet();
                        taskCtx.getFuture().setException(new TimeoutException());
                    }
                } else {
                    Thread.sleep(pollMs);
                }
            } catch (InterruptedException e) {
                break;
            } catch (Throwable e) {
                if (taskCtx != null) {
                    log.debug("[{}] Failed to execute task: {}", taskCtx.getId(), taskCtx, e);
                    stats.getTotalFailed().increment();
                    concurrencyLevel.decrementAndGet();
                } else {
                    log.debug("Failed to queue task:", e);
                }
            }
        }
        log.info("[{}] Buffered rate executor thread stopped", bufferName);
    }

    private void logTask(String action, AsyncTaskContext<T, V> taskCtx) {
        if (log.isTraceEnabled()) {
            if (taskCtx.getTask() instanceof CassandraStatementTask) {
                String query = queryToString(taskCtx);
                log.trace("[{}] {} task: {}, BoundStatement query: {}", taskCtx.getId(), action, taskCtx, query);
            } else {
                log.trace("[{}] {} task: {}", taskCtx.getId(), action, taskCtx);
            }
        } else {
            log.debug("[{}] {} task", taskCtx.getId(), action);
        }
    }

    private String queryToString(AsyncTaskContext<T, V> taskCtx) {
        CassandraStatementTask cassStmtTask = (CassandraStatementTask) taskCtx.getTask();
        if (cassStmtTask.getStatement() instanceof BoundStatement) {
            BoundStatement stmt = (BoundStatement) cassStmtTask.getStatement();
            String query = stmt.getPreparedStatement().getQuery();
            try {
                query = toStringWithValues(stmt, ProtocolVersion.V5);
            } catch (Exception e) {
                log.warn("Can't convert to query with values", e);
            }
            return query;
        } else {
            return "Not Cassandra Statement Task";
        }
    }

    private static String toStringWithValues(BoundStatement boundStatement, ProtocolVersion protocolVersion) {
        CodecRegistry codecRegistry = boundStatement.codecRegistry();
        PreparedStatement preparedStatement = boundStatement.getPreparedStatement();
        String query = preparedStatement.getQuery();
        ColumnDefinitions defs = preparedStatement.getVariableDefinitions();
        int index = 0;
        for (ColumnDefinition def : defs) {
            DataType type = def.getType();
            TypeCodec<Object> codec = codecRegistry.codecFor(type);
            if (boundStatement.getBytesUnsafe(index) != null) {
                Object value = codec.decode(boundStatement.getBytesUnsafe(index), protocolVersion);
                String replacement = Matcher.quoteReplacement(codec.format(value));
                query = query.replaceFirst("\\?", replacement);
            }
            index++;
        }
        return query;
    }

    protected int getQueueSize() {
        return queue.size();
    }

    public void printStats() {
        int queueSize = getQueueSize();
        int rateLimitedTenantsCount = (int) stats.getRateLimitedTenants().values().stream()
                .filter(defaultCounter -> defaultCounter.get() > 0)
                .count();

        if (queueSize > 0
            || rateLimitedTenantsCount > 0
            || concurrencyLevel.get() > 0
            || stats.getStatsCounters().stream().anyMatch(counter -> counter.get() > 0)
        ) {
            StringBuilder statsBuilder = new StringBuilder();

            statsBuilder.append("queueSize").append(" = [").append(queueSize).append("] ");
            stats.getStatsCounters().forEach(counter -> {
                statsBuilder.append(counter.getName()).append(" = [").append(counter.get()).append("] ");
            });
            statsBuilder.append("totalRateLimitedTenants").append(" = [").append(rateLimitedTenantsCount).append("] ");
            statsBuilder.append(CONCURRENCY_LEVEL).append(" = [").append(concurrencyLevel.get()).append("] ");

            stats.getStatsCounters().forEach(StatsCounter::clear);
            log.info("[{}] Permits {}", bufferName, statsBuilder);
        }

        stats.getRateLimitedTenants().entrySet().stream()
                .filter(entry -> entry.getValue().get() > 0)
                .forEach(entry -> {
                    TenantId tenantId = entry.getKey();
                    DefaultCounter counter = entry.getValue();
                    int rateLimitedRequests = counter.get();
                    counter.clear();
                    if (printTenantNames) {
                        String name = tenantNamesCache.computeIfAbsent(tenantId, tId -> {
                            String defaultName = "N/A";
                            try {
                                return entityService.fetchEntityName(TenantId.SYS_TENANT_ID, tenantId).orElse(defaultName);
                            } catch (Exception e) {
                                log.error("[{}][{}] Failed to get tenant name", bufferName, tenantId, e);
                                return defaultName;
                            }
                        });
                        log.info("[{}][{}][{}] Rate limited requests: {}", bufferName, tenantId, name, rateLimitedRequests);
                    } else {
                        log.info("[{}][{}] Rate limited requests: {}", bufferName, tenantId, rateLimitedRequests);
                    }
                });
    }
}
