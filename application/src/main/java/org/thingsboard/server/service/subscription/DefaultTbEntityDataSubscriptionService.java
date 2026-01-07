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
package org.thingsboard.server.service.subscription;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQueryResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.ComparisonTsValue;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.ws.WebSocketService;
import org.thingsboard.server.service.ws.WebSocketSessionRef;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AggHistoryCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AggKey;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AggTimeSeriesCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmCountCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmCountUpdate;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmDataCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmDataUpdate;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmStatusCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityCountCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityDataCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityDataUpdate;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityHistoryCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.GetTsCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.LatestValueCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.TimeSeriesCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.UnsubscribeCmd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
@Slf4j
@TbCoreComponent
@Service
public class DefaultTbEntityDataSubscriptionService implements TbEntityDataSubscriptionService {

    private static final int DEFAULT_LIMIT = 100;
    private final ConcurrentMap<String, ConcurrentMap<Integer, TbAbstractSubCtx>> subscriptionsBySessionId = new ConcurrentHashMap<>();

    @Autowired
    @Lazy
    private WebSocketService wsService;

    @Autowired
    private EntityService entityService;

    @Autowired
    private AlarmService alarmService;

    @Autowired
    private AttributesService attributesService;

    @Autowired
    @Lazy
    private TbLocalSubscriptionService localSubscriptionService;

    @Autowired
    private TimeseriesService tsService;

    @Autowired
    private TbServiceInfoProvider serviceInfoProvider;

    @Autowired
    @Getter
    private DbCallbackExecutorService dbCallbackExecutor;

    private ScheduledExecutorService scheduler;

    @Value("${database.ts.type}")
    private String databaseTsType;
    @Value("${server.ws.dynamic_page_link.refresh_interval:6}")
    private long dynamicPageLinkRefreshInterval;
    @Value("${server.ws.dynamic_page_link.refresh_pool_size:1}")
    private int dynamicPageLinkRefreshPoolSize;
    @Value("${server.ws.max_entities_per_data_subscription:1000}")
    private int maxEntitiesPerDataSubscription;
    @Value("${server.ws.max_entities_per_alarm_subscription:1000}")
    private int maxEntitiesPerAlarmSubscription;
    @Value("${server.ws.dynamic_page_link.max_alarm_queries_per_refresh_interval:10}")
    private int maxAlarmQueriesPerRefreshInterval;
    @Value("${ui.dashboard.max_datapoints_limit:50000}")
    private int maxDatapointLimit;
    @Value("${server.ws.alarms_per_alarm_status_subscription_cache_size:10}")
    private int alarmsPerAlarmStatusSubscriptionCacheSize;

    private ExecutorService wsCallBackExecutor;
    private boolean tsInSqlDB;
    private String serviceId;
    private SubscriptionServiceStatistics stats = new SubscriptionServiceStatistics();

    @PostConstruct
    public void initExecutor() {
        serviceId = serviceInfoProvider.getServiceId();
        wsCallBackExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("ws-entity-sub-callback"));
        tsInSqlDB = databaseTsType.equalsIgnoreCase("sql") || databaseTsType.equalsIgnoreCase("timescale");
        if (dynamicPageLinkRefreshPoolSize == 1) {
            scheduler = ThingsBoardExecutors.newSingleThreadScheduledExecutor("ws-entity-sub-scheduler");
        } else {
            scheduler = ThingsBoardExecutors.newScheduledThreadPool(dynamicPageLinkRefreshPoolSize, "ws-entity-sub-scheduler");
        }
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (wsCallBackExecutor != null) {
            wsCallBackExecutor.shutdownNow();
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    @Override
    public void handleCmd(WebSocketSessionRef session, EntityDataCmd cmd) {
        TbEntityDataSubCtx ctx = getSubCtx(session.getSessionId(), cmd.getCmdId());
        if (ctx != null) {
            log.debug("[{}][{}] Updating existing subscriptions using: {}", session.getSessionId(), cmd.getCmdId(), cmd);
            if (cmd.hasAnyCmd()) {
                ctx.clearEntitySubscriptions();
            }
        } else {
            log.debug("[{}][{}] Creating new subscription using: {}", session.getSessionId(), cmd.getCmdId(), cmd);
            ctx = createSubCtx(session, cmd);
        }
        ctx.setCurrentCmd(cmd);

        // Fetch entity list using entity data query
        if (cmd.getQuery() != null) {
            if (ctx.getQuery() == null) {
                log.debug("[{}][{}] Initializing data using query: {}", session.getSessionId(), cmd.getCmdId(), cmd.getQuery());
            } else {
                log.debug("[{}][{}] Updating data using query: {}", session.getSessionId(), cmd.getCmdId(), cmd.getQuery());
            }
            ctx.setAndResolveQuery(cmd.getQuery());
            EntityDataQuery query = ctx.getQuery();
            //Step 1. Update existing query with the contents of LatestValueCmd
            if (cmd.getLatestCmd() != null) {
                cmd.getLatestCmd().getKeys().forEach(key -> {
                    if (!query.getLatestValues().contains(key)) {
                        query.getLatestValues().add(key);
                    }
                });
            }
            long start = System.currentTimeMillis();
            ctx.fetchData();
            long end = System.currentTimeMillis();
            stats.getRegularQueryInvocationCnt().incrementAndGet();
            stats.getRegularQueryTimeSpent().addAndGet(end - start);
            ctx.cancelTasks();
            if (ctx.getQuery().getPageLink().isDynamic()) {
                //TODO: validate number of dynamic page links against rate limits. Ignore dynamic flag if limit is reached.
                TbEntityDataSubCtx finalCtx = ctx;
                ScheduledFuture<?> task = scheduler.scheduleWithFixedDelay(
                        () -> refreshDynamicQuery(finalCtx),
                        dynamicPageLinkRefreshInterval, dynamicPageLinkRefreshInterval, TimeUnit.SECONDS);
                finalCtx.setRefreshTask(task);
            }
        }

        try {
            List<ListenableFuture<?>> cmdFutures = new ArrayList<>();
            if (cmd.getAggHistoryCmd() != null) {
                cmdFutures.add(handleAggHistoryCmd(ctx, cmd.getAggHistoryCmd()));
            }
            if (cmd.getAggTsCmd() != null) {
                cmdFutures.add(handleAggTsCmd(ctx, cmd.getAggTsCmd()));
            }
            if (cmd.getHistoryCmd() != null) {
                cmdFutures.add(handleHistoryCmd(ctx, cmd.getHistoryCmd()));
            }
            if (cmdFutures.isEmpty()) {
                handleRegularCommands(ctx, cmd);
            } else {
                TbEntityDataSubCtx finalCtx = ctx;
                Futures.addCallback(Futures.allAsList(cmdFutures), new FutureCallback<>() {
                    @Override
                    public void onSuccess(@Nullable List<Object> result) {
                        handleRegularCommands(finalCtx, cmd);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.warn("[{}][{}] Failed to process command", finalCtx.getSessionId(), finalCtx.getCmdId());
                    }
                }, wsCallBackExecutor);
            }
        } catch (RuntimeException e) {
            handleWsCmdRuntimeException(ctx.getSessionId(), e, cmd);
        }
    }

    private void handleRegularCommands(TbEntityDataSubCtx ctx, EntityDataCmd cmd) {
        try {
            if (cmd.getLatestCmd() != null || cmd.getTsCmd() != null) {
                if (cmd.getLatestCmd() != null) {
                    handleLatestCmd(ctx, cmd.getLatestCmd());
                }
                if (cmd.getTsCmd() != null) {
                    handleTimeSeriesCmd(ctx, cmd.getTsCmd());
                }
            } else {
                checkAndSendInitialData(ctx);
            }
        } catch (RuntimeException e) {
            handleWsCmdRuntimeException(ctx.getSessionId(), e, cmd);
        }
    }

    private void checkAndSendInitialData(@Nullable TbEntityDataSubCtx theCtx) {
        if (!theCtx.isInitialDataSent()) {
            EntityDataUpdate update = new EntityDataUpdate(theCtx.getCmdId(), theCtx.getData(), null, theCtx.getMaxEntitiesPerDataSubscription());
            theCtx.sendWsMsg(update);
            theCtx.setInitialDataSent(true);
        }
    }

    private ListenableFuture<TbEntityDataSubCtx> handleAggHistoryCmd(TbEntityDataSubCtx ctx, AggHistoryCmd cmd) {
        ConcurrentMap<Integer, ReadTsKvQueryInfo> queries = new ConcurrentHashMap<>();
        for (AggKey key : cmd.getKeys()) {
            if (key.getPreviousValueOnly() == null || !key.getPreviousValueOnly()) {
                var query = new BaseReadTsKvQuery(key.getKey(), cmd.getStartTs(), cmd.getEndTs(), cmd.getEndTs() - cmd.getStartTs(), 1, key.getAgg());
                queries.put(query.getId(), new ReadTsKvQueryInfo(key, query, false));
            }
            if (key.getPreviousStartTs() != null && key.getPreviousEndTs() != null && key.getPreviousEndTs() >= key.getPreviousStartTs()) {
                var query = new BaseReadTsKvQuery(key.getKey(), key.getPreviousStartTs(), key.getPreviousEndTs(), key.getPreviousEndTs() - key.getPreviousStartTs(), 1, key.getAgg());
                queries.put(query.getId(), new ReadTsKvQueryInfo(key, query, true));
            }
        }
        return handleAggCmd(ctx, cmd.getKeys(), queries, cmd.getStartTs(), cmd.getEndTs(), false);
    }

    private ListenableFuture<TbEntityDataSubCtx> handleAggTsCmd(TbEntityDataSubCtx ctx, AggTimeSeriesCmd cmd) {
        ConcurrentMap<Integer, ReadTsKvQueryInfo> queries = new ConcurrentHashMap<>();
        for (AggKey key : cmd.getKeys()) {
            var query = new BaseReadTsKvQuery(key.getKey(), cmd.getStartTs(), cmd.getStartTs() + cmd.getTimeWindow(), cmd.getTimeWindow(), 1, key.getAgg());
            queries.put(query.getId(), new ReadTsKvQueryInfo(key, query, false));
        }
        return handleAggCmd(ctx, cmd.getKeys(), queries, cmd.getStartTs(), cmd.getStartTs() + cmd.getTimeWindow(), true);
    }

    private ListenableFuture<TbEntityDataSubCtx> handleAggCmd(TbEntityDataSubCtx ctx, List<AggKey> keys, ConcurrentMap<Integer, ReadTsKvQueryInfo> queries,
                                                              long startTs, long endTs, boolean subscribe) {
        Map<EntityData, ListenableFuture<List<ReadTsKvQueryResult>>> fetchResultMap = new HashMap<>();
        List<EntityData> entityDataList = ctx.getData().getData();
        List<ReadTsKvQuery> queryList = queries.values().stream().map(ReadTsKvQueryInfo::getQuery).collect(Collectors.toList());
        entityDataList.forEach(entityData -> fetchResultMap.put(entityData,
                tsService.findAllByQueries(ctx.getTenantId(), entityData.getEntityId(), queryList)));
        return Futures.transform(Futures.allAsList(fetchResultMap.values()), f -> {
            // Map that holds last ts for each key for each entity.
            Map<EntityData, Map<String, Long>> lastTsEntityMap = new HashMap<>();
            fetchResultMap.forEach((entityData, future) -> {
                try {
                    Map<String, Long> lastTsMap = new HashMap<>();
                    lastTsEntityMap.put(entityData, lastTsMap);

                    List<ReadTsKvQueryResult> queryResults = future.get();
                    if (queryResults != null) {
                        for (ReadTsKvQueryResult queryResult : queryResults) {
                            ReadTsKvQueryInfo queryInfo = queries.get(queryResult.getQueryId());
                            ComparisonTsValue comparisonTsValue = entityData.getAggLatest().computeIfAbsent(queryInfo.getKey().getId(), agg -> new ComparisonTsValue());
                            if (queryInfo.isPrevious()) {
                                comparisonTsValue.setPrevious(queryResult.toTsValue(queryInfo.getQuery()));
                            } else {
                                comparisonTsValue.setCurrent(queryResult.toTsValue(queryInfo.getQuery()));
                                lastTsMap.put(queryInfo.getQuery().getKey(), queryResult.getLastEntryTs());
                            }
                        }
                    }
                    // Populate with empty values if no data found.
                    keys.forEach(key -> {
                        entityData.getAggLatest().putIfAbsent(key.getId(), new ComparisonTsValue(TsValue.EMPTY, TsValue.EMPTY));
                    });
                } catch (InterruptedException | ExecutionException e) {
                    log.warn("[{}][{}][{}] Failed to fetch historical data", ctx.getSessionId(), ctx.getCmdId(), entityData.getEntityId(), e);
                    ctx.sendWsMsg(new EntityDataUpdate(ctx.getCmdId(), SubscriptionErrorCode.INTERNAL_ERROR.getCode(), "Failed to fetch historical data!"));
                }
            });
            ctx.getWsLock().lock();
            try {
                EntityDataUpdate update;
                if (!ctx.isInitialDataSent()) {
                    update = new EntityDataUpdate(ctx.getCmdId(), ctx.getData(), null, ctx.getMaxEntitiesPerDataSubscription());
                    ctx.setInitialDataSent(true);
                } else {
                    update = new EntityDataUpdate(ctx.getCmdId(), null, entityDataList, ctx.getMaxEntitiesPerDataSubscription());
                }
                if (subscribe) {
                    ctx.createTimeSeriesSubscriptions(lastTsEntityMap, startTs, endTs, true);
                }
                ctx.sendWsMsg(update);
                entityDataList.forEach(EntityData::clearTsAndAggData);
            } finally {
                ctx.getWsLock().unlock();
            }
            return ctx;
        }, wsCallBackExecutor);
    }

    private void handleWsCmdRuntimeException(String sessionId, RuntimeException e, EntityDataCmd cmd) {
        log.debug("[{}] Failed to process ws cmd: {}", sessionId, cmd, e);
        if (e instanceof TbRateLimitsException) {
            return;
        }
        wsService.close(sessionId, CloseStatus.SERVICE_RESTARTED);
    }

    @Override
    public void handleCmd(WebSocketSessionRef session, EntityCountCmd cmd) {
        TbEntityCountSubCtx ctx = getSubCtx(session.getSessionId(), cmd.getCmdId());
        if (ctx == null) {
            ctx = createSubCtx(session, cmd);
            long start = System.currentTimeMillis();
            ctx.fetchData();
            long end = System.currentTimeMillis();
            stats.getRegularQueryInvocationCnt().incrementAndGet();
            stats.getRegularQueryTimeSpent().addAndGet(end - start);
            TbEntityCountSubCtx finalCtx = ctx;
            ScheduledFuture<?> task = scheduler.scheduleWithFixedDelay(
                    () -> refreshDynamicQuery(finalCtx),
                    dynamicPageLinkRefreshInterval, dynamicPageLinkRefreshInterval, TimeUnit.SECONDS);
            finalCtx.setRefreshTask(task);
        } else {
            log.debug("[{}][{}] Received duplicate command: {}", session.getSessionId(), cmd.getCmdId(), cmd);
        }
    }

    @Override
    public void handleCmd(WebSocketSessionRef session, AlarmDataCmd cmd) {
        TbAlarmDataSubCtx ctx = getSubCtx(session.getSessionId(), cmd.getCmdId());
        if (ctx == null) {
            log.debug("[{}][{}] Creating new alarm subscription using: {}", session.getSessionId(), cmd.getCmdId(), cmd);
            ctx = createSubCtx(session, cmd);
        }
        ctx.setAndResolveQuery(cmd.getQuery());
        AlarmDataQuery adq = ctx.getQuery();
        long start = System.currentTimeMillis();
        ctx.fetchData();
        long end = System.currentTimeMillis();
        stats.getRegularQueryInvocationCnt().incrementAndGet();
        stats.getRegularQueryTimeSpent().addAndGet(end - start);
        List<EntityData> entities = ctx.getEntitiesData();
        ctx.cancelTasks();
        ctx.clearEntitySubscriptions();
        if (entities.isEmpty()) {
            AlarmDataUpdate update = new AlarmDataUpdate(cmd.getCmdId(), new PageData<>(), null, 0, 0);
            ctx.sendWsMsg(update);
        } else {
            ctx.fetchAlarms();
            ctx.createLatestValuesSubscriptions(cmd.getQuery().getLatestValues());
            if (adq.getPageLink().getTimeWindow() > 0) {
                TbAlarmDataSubCtx finalCtx = ctx;
                ScheduledFuture<?> task = scheduler.scheduleWithFixedDelay(
                        () -> refreshAlarmQuery(finalCtx), dynamicPageLinkRefreshInterval, dynamicPageLinkRefreshInterval, TimeUnit.SECONDS);
                finalCtx.setRefreshTask(task);
            }
        }
    }

    @Override
    public void handleCmd(WebSocketSessionRef session, AlarmCountCmd cmd) {
        TbAlarmCountSubCtx ctx = getSubCtx(session.getSessionId(), cmd.getCmdId());
        if (ctx == null) {
            ctx = createSubCtx(session, cmd);
            long start = System.currentTimeMillis();
            ctx.fetchData();
            long end = System.currentTimeMillis();
            stats.getRegularQueryInvocationCnt().incrementAndGet();
            stats.getRegularQueryTimeSpent().addAndGet(end - start);
            Set<EntityId> entitiesIds = ctx.getEntitiesIds();
            ctx.cancelTasks();
            ctx.clearAlarmSubscriptions();
            if (entitiesIds != null && entitiesIds.isEmpty()) {
                AlarmCountUpdate update = new AlarmCountUpdate(cmd.getCmdId(), 0);
                ctx.sendWsMsg(update);
            } else {
                ctx.doFetchAlarmCount();
                if (entitiesIds != null) {
                    ctx.createAlarmSubscriptions();
                }
                TbAlarmCountSubCtx finalCtx = ctx;
                ScheduledFuture<?> task = scheduler.scheduleWithFixedDelay(
                        () -> refreshDynamicQuery(finalCtx),
                        dynamicPageLinkRefreshInterval, dynamicPageLinkRefreshInterval, TimeUnit.SECONDS);
                finalCtx.setRefreshTask(task);
            }
        } else {
            log.debug("[{}][{}] Received duplicate command: {}", session.getSessionId(), cmd.getCmdId(), cmd);
        }
    }

    @Override
    public void handleCmd(WebSocketSessionRef session, AlarmStatusCmd cmd) {
        log.debug("[{}] Handling alarm status subscription cmd (cmdId: {})", session.getSessionId(), cmd.getCmdId());
        TbAlarmStatusSubCtx ctx = getSubCtx(session.getSessionId(), cmd.getCmdId());
        if (ctx == null) {
            ctx = createSubCtx(session, cmd);
            long start = System.currentTimeMillis();
            ctx.fetchActiveAlarms();
            long end = System.currentTimeMillis();
            stats.getAlarmQueryInvocationCnt().incrementAndGet();
            stats.getAlarmQueryTimeSpent().addAndGet(end - start);
            ctx.sendUpdate();
        } else {
            log.debug("[{}][{}] Received duplicate command: {}", session.getSessionId(), cmd.getCmdId(), cmd);
        }
    }

    private boolean validate(TbAbstractSubCtx finalCtx) {
        if (finalCtx.isStopped()) {
            log.warn("[{}][{}][{}] Received validation task for already stopped context.", finalCtx.getTenantId(), finalCtx.getSessionId(), finalCtx.getCmdId());
            return false;
        }
        var cmdMap = subscriptionsBySessionId.get(finalCtx.getSessionId());
        if (cmdMap == null) {
            log.warn("[{}][{}][{}] Received validation task for already removed session.", finalCtx.getTenantId(), finalCtx.getSessionId(), finalCtx.getCmdId());
            return false;
        } else if (!cmdMap.containsKey(finalCtx.getCmdId())) {
            log.warn("[{}][{}][{}] Received validation task for unregistered cmdId.", finalCtx.getTenantId(), finalCtx.getSessionId(), finalCtx.getCmdId());
            return false;
        }
        return true;
    }

    private void refreshDynamicQuery(TbAbstractEntityQuerySubCtx<?> finalCtx) {
        try {
            if (validate(finalCtx)) {
                long start = System.currentTimeMillis();
                finalCtx.update();
                long end = System.currentTimeMillis();
                log.trace("[{}][{}] Executing query: {}", finalCtx.getSessionId(), finalCtx.getCmdId(), finalCtx.getQuery());
                stats.getDynamicQueryInvocationCnt().incrementAndGet();
                stats.getDynamicQueryTimeSpent().addAndGet(end - start);
            } else {
                finalCtx.stop();
            }
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to refresh query", finalCtx.getSessionId(), finalCtx.getCmdId(), e);
        }
    }

    private void refreshAlarmQuery(TbAlarmDataSubCtx finalCtx) {
        if (validate(finalCtx)) {
            finalCtx.checkAndResetInvocationCounter();
        } else {
            finalCtx.stop();
        }
    }

    @Scheduled(fixedDelayString = "${server.ws.dynamic_page_link.stats:10000}")
    public void printStats() {
        int alarmQueryInvocationCntValue = stats.getAlarmQueryInvocationCnt().getAndSet(0);
        long alarmQueryInvocationTimeValue = stats.getAlarmQueryTimeSpent().getAndSet(0);
        int regularQueryInvocationCntValue = stats.getRegularQueryInvocationCnt().getAndSet(0);
        long regularQueryInvocationTimeValue = stats.getRegularQueryTimeSpent().getAndSet(0);
        int dynamicQueryInvocationCntValue = stats.getDynamicQueryInvocationCnt().getAndSet(0);
        long dynamicQueryInvocationTimeValue = stats.getDynamicQueryTimeSpent().getAndSet(0);
        long dynamicQueryCnt = subscriptionsBySessionId.values().stream().mapToLong(m -> m.values().stream().filter(TbAbstractSubCtx::isDynamic).count()).sum();
        if (regularQueryInvocationCntValue > 0 || dynamicQueryInvocationCntValue > 0 || dynamicQueryCnt > 0 || alarmQueryInvocationCntValue > 0) {
            log.info("Stats: regularQueryInvocationCnt = [{}], regularQueryInvocationTime = [{}], " +
                            "dynamicQueryCnt = [{}] dynamicQueryInvocationCnt = [{}], dynamicQueryInvocationTime = [{}], " +
                            "alarmQueryInvocationCnt = [{}], alarmQueryInvocationTime = [{}]",
                    regularQueryInvocationCntValue, regularQueryInvocationTimeValue,
                    dynamicQueryCnt, dynamicQueryInvocationCntValue, dynamicQueryInvocationTimeValue,
                    alarmQueryInvocationCntValue, alarmQueryInvocationTimeValue);
        }
    }

    private TbEntityDataSubCtx createSubCtx(WebSocketSessionRef sessionRef, EntityDataCmd cmd) {
        Map<Integer, TbAbstractSubCtx> sessionSubs = subscriptionsBySessionId.computeIfAbsent(sessionRef.getSessionId(), k -> new ConcurrentHashMap<>());
        TbEntityDataSubCtx ctx = new TbEntityDataSubCtx(serviceId, wsService, entityService, localSubscriptionService,
                attributesService, stats, sessionRef, cmd.getCmdId(), maxEntitiesPerDataSubscription);
        if (cmd.getQuery() != null) {
            ctx.setAndResolveQuery(cmd.getQuery());
        }
        sessionSubs.put(cmd.getCmdId(), ctx);
        return ctx;
    }

    private TbEntityCountSubCtx createSubCtx(WebSocketSessionRef sessionRef, EntityCountCmd cmd) {
        Map<Integer, TbAbstractSubCtx> sessionSubs = subscriptionsBySessionId.computeIfAbsent(sessionRef.getSessionId(), k -> new ConcurrentHashMap<>());
        TbEntityCountSubCtx ctx = new TbEntityCountSubCtx(serviceId, wsService, entityService, localSubscriptionService,
                attributesService, stats, sessionRef, cmd.getCmdId());
        if (cmd.getQuery() != null) {
            ctx.setAndResolveQuery(cmd.getQuery());
        }
        sessionSubs.put(cmd.getCmdId(), ctx);
        return ctx;
    }


    private TbAlarmDataSubCtx createSubCtx(WebSocketSessionRef sessionRef, AlarmDataCmd cmd) {
        Map<Integer, TbAbstractSubCtx> sessionSubs = subscriptionsBySessionId.computeIfAbsent(sessionRef.getSessionId(), k -> new ConcurrentHashMap<>());
        TbAlarmDataSubCtx ctx = new TbAlarmDataSubCtx(serviceId, wsService, entityService, localSubscriptionService,
                attributesService, stats, alarmService, sessionRef, cmd.getCmdId(), maxEntitiesPerAlarmSubscription,
                maxAlarmQueriesPerRefreshInterval);
        ctx.setAndResolveQuery(cmd.getQuery());
        sessionSubs.put(cmd.getCmdId(), ctx);
        return ctx;
    }

    private TbAlarmCountSubCtx createSubCtx(WebSocketSessionRef sessionRef, AlarmCountCmd cmd) {
        Map<Integer, TbAbstractSubCtx> sessionSubs = subscriptionsBySessionId.computeIfAbsent(sessionRef.getSessionId(), k -> new ConcurrentHashMap<>());
        TbAlarmCountSubCtx ctx = new TbAlarmCountSubCtx(serviceId, wsService, entityService, localSubscriptionService,
                attributesService, stats, alarmService, sessionRef, cmd.getCmdId(), maxEntitiesPerAlarmSubscription, maxAlarmQueriesPerRefreshInterval);
        if (cmd.getQuery() != null) {
            ctx.setAndResolveQuery(cmd.getQuery());
        }
        sessionSubs.put(cmd.getCmdId(), ctx);
        return ctx;
    }

    private TbAlarmStatusSubCtx createSubCtx(WebSocketSessionRef sessionRef, AlarmStatusCmd cmd) {
        Map<Integer, TbAbstractSubCtx> sessionSubs = subscriptionsBySessionId.computeIfAbsent(sessionRef.getSessionId(), k -> new ConcurrentHashMap<>());
        TbAlarmStatusSubCtx ctx = new TbAlarmStatusSubCtx(serviceId, wsService, localSubscriptionService,
                stats, alarmService, alarmsPerAlarmStatusSubscriptionCacheSize, sessionRef, cmd.getCmdId());
        ctx.createSubscription(cmd);
        sessionSubs.put(cmd.getCmdId(), ctx);
        return ctx;
    }

    @SuppressWarnings("unchecked")
    private <T extends TbAbstractSubCtx> T getSubCtx(String sessionId, int cmdId) {
        Map<Integer, TbAbstractSubCtx> sessionSubs = subscriptionsBySessionId.get(sessionId);
        if (sessionSubs != null) {
            return (T) sessionSubs.get(cmdId);
        } else {
            return null;
        }
    }

    private ListenableFuture<TbEntityDataSubCtx> handleTimeSeriesCmd(TbEntityDataSubCtx ctx, TimeSeriesCmd cmd) {
        log.debug("[{}][{}] Fetching time-series data for last {} ms for keys: ({})", ctx.getSessionId(), ctx.getCmdId(), cmd.getTimeWindow(), cmd.getKeys());
        return handleGetTsCmd(ctx, cmd, true);
    }


    private ListenableFuture<TbEntityDataSubCtx> handleHistoryCmd(TbEntityDataSubCtx ctx, EntityHistoryCmd cmd) {
        log.debug("[{}][{}] Fetching history data for start {} and end {} ms for keys: ({})", ctx.getSessionId(), ctx.getCmdId(), cmd.getStartTs(), cmd.getEndTs(), cmd.getKeys());
        return handleGetTsCmd(ctx, cmd, false);
    }

    private ListenableFuture<TbEntityDataSubCtx> handleGetTsCmd(TbEntityDataSubCtx ctx, GetTsCmd cmd, boolean subscribe) {
        Map<Integer, String> queriesKeys = new ConcurrentHashMap<>();

        List<String> keys = cmd.getKeys();
        List<ReadTsKvQuery> finalTsKvQueryList;
        List<ReadTsKvQuery> tsKvQueryList = keys.stream().map(key -> {
            var query = new BaseReadTsKvQuery(key, cmd.getStartTs(), cmd.getEndTs(), cmd.toAggregationParams(), getLimit(cmd.getLimit()));
            queriesKeys.put(query.getId(), query.getKey());
            return query;
        }).collect(Collectors.toList());
        if (cmd.isFetchLatestPreviousPoint()) {
            finalTsKvQueryList = new ArrayList<>(tsKvQueryList);
            finalTsKvQueryList.addAll(keys.stream().map(key -> {
                        var query = new BaseReadTsKvQuery(key, cmd.getStartTs() - TimeUnit.DAYS.toMillis(365), cmd.getStartTs(), cmd.toAggregationParams(), 1);
                        queriesKeys.put(query.getId(), query.getKey());
                        return query;
                    }
            ).collect(Collectors.toList()));
        } else {
            finalTsKvQueryList = tsKvQueryList;
        }
        Map<EntityData, ListenableFuture<List<ReadTsKvQueryResult>>> fetchResultMap = new HashMap<>();
        List<EntityData> entityDataList = ctx.getData().getData();
        entityDataList.forEach(entityData -> fetchResultMap.put(entityData,
                tsService.findAllByQueries(ctx.getTenantId(), entityData.getEntityId(), finalTsKvQueryList)));
        return Futures.transform(Futures.allAsList(fetchResultMap.values()), f -> {
            // Map that holds last ts for each key for each entity.
            Map<EntityData, Map<String, Long>> lastTsEntityMap = new HashMap<>();
            fetchResultMap.forEach((entityData, future) -> {
                try {
                    Map<String, Long> lastTsMap = new HashMap<>();
                    lastTsEntityMap.put(entityData, lastTsMap);

                    List<ReadTsKvQueryResult> queryResults = future.get();
                    if (queryResults != null) {
                        for (ReadTsKvQueryResult queryResult : queryResults) {
                            String queryKey = queriesKeys.get(queryResult.getQueryId());
                            if (queryKey != null) {
                                entityData.getTimeseries().merge(queryKey, queryResult.toTsValues(), ArrayUtils::addAll);
                                lastTsMap.merge(queryKey, queryResult.getLastEntryTs(), Math::max);
                            } else {
                                log.warn("ReadTsKvQueryResult for {} {} has queryId not matching the initial query",
                                        entityData.getEntityId().getEntityType(), entityData.getEntityId());
                            }
                        }
                    }
                    // Populate with empty values if no data found.
                    keys.forEach(key -> {
                        if (!entityData.getTimeseries().containsKey(key)) {
                            entityData.getTimeseries().put(key, new TsValue[0]);
                        }
                    });

                    if (cmd.isFetchLatestPreviousPoint()) {
                        entityData.getTimeseries().values().forEach(dataArray -> Arrays.sort(dataArray, (o1, o2) -> Long.compare(o2.getTs(), o1.getTs())));
                    }
                } catch (InterruptedException | ExecutionException e) {
                    log.warn("[{}][{}][{}] Failed to fetch historical data", ctx.getSessionId(), ctx.getCmdId(), entityData.getEntityId(), e);
                    ctx.sendWsMsg(new EntityDataUpdate(ctx.getCmdId(), SubscriptionErrorCode.INTERNAL_ERROR.getCode(), "Failed to fetch historical data!"));
                }
            });
            ctx.getWsLock().lock();
            try {
                EntityDataUpdate update;
                if (!ctx.isInitialDataSent()) {
                    update = new EntityDataUpdate(ctx.getCmdId(), ctx.getData(), null, ctx.getMaxEntitiesPerDataSubscription());
                    ctx.setInitialDataSent(true);
                } else {
                    update = new EntityDataUpdate(ctx.getCmdId(), null, entityDataList, ctx.getMaxEntitiesPerDataSubscription());
                }
                if (subscribe) {
                    ctx.createTimeSeriesSubscriptions(lastTsEntityMap, cmd.getStartTs(), cmd.getEndTs());
                }
                ctx.sendWsMsg(update);
                entityDataList.forEach(EntityData::clearTsAndAggData);
            } finally {
                ctx.getWsLock().unlock();
            }
            return ctx;
        }, wsCallBackExecutor);
    }

    private void handleLatestCmd(TbEntityDataSubCtx ctx, LatestValueCmd latestCmd) {
        log.trace("[{}][{}] Going to process latest command: {}", ctx.getSessionId(), ctx.getCmdId(), latestCmd);
        //Fetch the latest values for telemetry keys in case they are not copied from NoSQL to SQL DB in hybrid mode.
        if (!tsInSqlDB) {
            log.trace("[{}][{}] Going to fetch missing latest values: {}", ctx.getSessionId(), ctx.getCmdId(), latestCmd);
            List<String> allTsKeys = latestCmd.getKeys().stream()
                    .filter(key -> key.getType().equals(EntityKeyType.TIME_SERIES))
                    .map(EntityKey::getKey).collect(Collectors.toList());

            Map<EntityData, ListenableFuture<Map<String, TsValue>>> missingTelemetryFutures = new HashMap<>();
            for (EntityData entityData : ctx.getData().getData()) {
                Map<EntityKeyType, Map<String, TsValue>> latestEntityData = entityData.getLatest();
                Map<String, TsValue> tsEntityData = latestEntityData.get(EntityKeyType.TIME_SERIES);
                Set<String> missingTsKeys = new LinkedHashSet<>(allTsKeys);
                if (tsEntityData != null) {
                    missingTsKeys.removeAll(tsEntityData.keySet());
                } else {
                    tsEntityData = new HashMap<>();
                    latestEntityData.put(EntityKeyType.TIME_SERIES, tsEntityData);
                }

                ListenableFuture<List<TsKvEntry>> missingTsData = tsService.findLatest(ctx.getTenantId(), entityData.getEntityId(), missingTsKeys);
                missingTelemetryFutures.put(entityData, Futures.transform(missingTsData, this::toTsValue, MoreExecutors.directExecutor()));
            }
            Futures.addCallback(Futures.allAsList(missingTelemetryFutures.values()), new FutureCallback<>() {
                @Override
                public void onSuccess(@Nullable List<Map<String, TsValue>> result) {
                    missingTelemetryFutures.forEach((key, value) -> {
                        try {
                            key.getLatest().get(EntityKeyType.TIME_SERIES).putAll(value.get());
                        } catch (InterruptedException | ExecutionException e) {
                            log.warn("[{}][{}] Failed to lookup latest telemetry: {}:{}", ctx.getSessionId(), ctx.getCmdId(), key.getEntityId(), allTsKeys, e);
                        }
                    });
                    EntityDataUpdate update;
                    ctx.getWsLock().lock();
                    try {
                        ctx.createLatestValuesSubscriptions(latestCmd.getKeys());
                        if (!ctx.isInitialDataSent()) {
                            update = new EntityDataUpdate(ctx.getCmdId(), ctx.getData(), null, ctx.getMaxEntitiesPerDataSubscription());
                            ctx.setInitialDataSent(true);
                        } else {
                            // if ctx has timeseries subscription, timeseries values are cleared after each update and is empty in ctx data,
                            // so to avoid sending timeseries update with empty map we set it to null
                            List<EntityData> preparedData = ctx.getData().getData().stream()
                                    .map(entityData -> new EntityData(entityData.getEntityId(), entityData.getLatest(), null))
                                    .toList();
                            update = new EntityDataUpdate(ctx.getCmdId(), null, preparedData, ctx.getMaxEntitiesPerDataSubscription());
                        }
                        ctx.sendWsMsg(update);
                    } finally {
                        ctx.getWsLock().unlock();
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    log.warn("[{}][{}] Failed to process websocket command: {}:{}", ctx.getSessionId(), ctx.getCmdId(), ctx.getQuery(), latestCmd, t);
                    ctx.sendWsMsg(new EntityDataUpdate(ctx.getCmdId(), SubscriptionErrorCode.INTERNAL_ERROR.getCode(), "Failed to process websocket command!"));
                }
            }, wsCallBackExecutor);
        } else {
            ctx.getWsLock().lock();
            try {
                ctx.createLatestValuesSubscriptions(latestCmd.getKeys());
                checkAndSendInitialData(ctx);
            } finally {
                ctx.getWsLock().unlock();
            }
        }
    }

    private Map<String, TsValue> toTsValue(List<TsKvEntry> data) {
        return data.stream().collect(Collectors.toMap(TsKvEntry::getKey, value -> new TsValue(value.getTs(), value.getValueAsString())));
    }

    @Override
    public void cancelSubscription(String sessionId, UnsubscribeCmd cmd) {
        cleanupAndCancel(getSubCtx(sessionId, cmd.getCmdId()));
    }

    private void cleanupAndCancel(TbAbstractSubCtx ctx) {
        if (ctx != null) {
            ctx.stop();
            if (ctx.getSessionId() != null) {
                Map<Integer, TbAbstractSubCtx> sessionSubs = subscriptionsBySessionId.get(ctx.getSessionId());
                if (sessionSubs != null) {
                    sessionSubs.remove(ctx.getCmdId());
                }
            }
        }
    }

    @Override
    public void cancelAllSessionSubscriptions(String sessionId) {
        Map<Integer, TbAbstractSubCtx> sessionSubs = subscriptionsBySessionId.remove(sessionId);
        if (sessionSubs != null) {
            sessionSubs.values().forEach(sub -> {
                        try {
                            cleanupAndCancel(sub);
                        } catch (Exception e) {
                            log.warn("[{}] Failed to remove subscription {} due to ", sub.getTenantId(), sub, e);
                        }
                    }
            );
        }
    }

    private int getLimit(int limit) {
        return limit == 0 ? DEFAULT_LIMIT : limit;
    }

}
