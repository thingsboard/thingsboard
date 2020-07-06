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
package org.thingsboard.server.service.subscription;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.telemetry.TelemetryWebSocketService;
import org.thingsboard.server.service.telemetry.TelemetryWebSocketSessionRef;
import org.thingsboard.server.service.telemetry.cmd.v2.AlarmDataCmd;
import org.thingsboard.server.service.telemetry.cmd.v2.AlarmDataUpdate;
import org.thingsboard.server.service.telemetry.cmd.v2.EntityDataCmd;
import org.thingsboard.server.service.telemetry.cmd.v2.EntityDataUnsubscribeCmd;
import org.thingsboard.server.service.telemetry.cmd.v2.EntityDataUpdate;
import org.thingsboard.server.service.telemetry.cmd.v2.EntityHistoryCmd;
import org.thingsboard.server.service.telemetry.cmd.v2.GetTsCmd;
import org.thingsboard.server.service.telemetry.cmd.v2.LatestValueCmd;
import org.thingsboard.server.service.telemetry.cmd.v2.TimeSeriesCmd;
import org.thingsboard.server.service.telemetry.sub.SubscriptionErrorCode;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@TbCoreComponent
@Service
public class DefaultTbEntityDataSubscriptionService implements TbEntityDataSubscriptionService {

    private static final int DEFAULT_LIMIT = 100;
    private final Map<String, Map<Integer, TbAbstractDataSubCtx>> subscriptionsBySessionId = new ConcurrentHashMap<>();

    @Autowired
    private TelemetryWebSocketService wsService;

    @Autowired
    private EntityService entityService;

    @Autowired
    private AlarmService alarmService;

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
    @Value("${server.ws.max_entities_per_alarm_subscription:1000}")
    private int maxEntitiesPerAlarmSubscription;

    private ExecutorService wsCallBackExecutor;
    private boolean tsInSqlDB;
    private String serviceId;
    private AtomicInteger regularQueryInvocationCnt = new AtomicInteger();
    private AtomicInteger dynamicQueryInvocationCnt = new AtomicInteger();
    private AtomicLong regularQueryTimeSpent = new AtomicLong();
    private AtomicLong dynamicQueryTimeSpent = new AtomicLong();

    @PostConstruct
    public void initExecutor() {
        serviceId = serviceInfoProvider.getServiceId();
        wsCallBackExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("ws-entity-sub-callback"));
        tsInSqlDB = databaseTsType.equalsIgnoreCase("sql") || databaseTsType.equalsIgnoreCase("timescale");
        ThreadFactory tbThreadFactory = ThingsBoardThreadFactory.forName("ws-entity-sub-scheduler");
        if (dynamicPageLinkRefreshPoolSize == 1) {
            scheduler = Executors.newSingleThreadScheduledExecutor(tbThreadFactory);
        } else {
            scheduler = Executors.newScheduledThreadPool(dynamicPageLinkRefreshPoolSize, tbThreadFactory);
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
    public void handleCmd(TelemetryWebSocketSessionRef session, EntityDataCmd cmd) {
        TbEntityDataSubCtx ctx = getSubCtx(session.getSessionId(), cmd.getCmdId());
        if (ctx != null) {
            log.debug("[{}][{}] Updating existing subscriptions using: {}", session.getSessionId(), cmd.getCmdId(), cmd);
            if (cmd.getLatestCmd() != null || cmd.getTsCmd() != null || cmd.getHistoryCmd() != null) {
                clearSubs(ctx);
            }
        } else {
            log.debug("[{}][{}] Creating new subscription using: {}", session.getSessionId(), cmd.getCmdId(), cmd);
            ctx = createSubCtx(session, cmd);
        }
        ctx.setCurrentCmd(cmd);
        if (cmd.getQuery() != null) {
            if (ctx.getQuery() == null) {
                log.debug("[{}][{}] Initializing data using query: {}", session.getSessionId(), cmd.getCmdId(), cmd.getQuery());
            } else {
                log.debug("[{}][{}] Updating data using query: {}", session.getSessionId(), cmd.getCmdId(), cmd.getQuery());
            }
            ctx.setQuery(cmd.getQuery());
            TenantId tenantId = ctx.getTenantId();
            CustomerId customerId = ctx.getCustomerId();
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
            PageData<EntityData> data = entityService.findEntityDataByQuery(tenantId, customerId, ctx.getQuery());
            long end = System.currentTimeMillis();
            regularQueryInvocationCnt.incrementAndGet();
            regularQueryTimeSpent.addAndGet(end - start);

            if (log.isTraceEnabled()) {
                data.getData().forEach(ed -> {
                    log.trace("[{}][{}] EntityData: {}", session.getSessionId(), cmd.getCmdId(), ed);
                });
            }
            ctx.setData(data);
            ctx.cancelRefreshTask();
            if (ctx.getQuery().getPageLink().isDynamic()) {
                //TODO: validate number of dynamic page links against rate limits. Ignore dynamic flag if limit is reached.
                TbEntityDataSubCtx finalCtx = ctx;
                ScheduledFuture<?> task = scheduler.scheduleWithFixedDelay(
                        () -> refreshDynamicQuery(tenantId, customerId, finalCtx),
                        dynamicPageLinkRefreshInterval, dynamicPageLinkRefreshInterval, TimeUnit.SECONDS);
                finalCtx.setRefreshTask(task);
            }
        }
        ListenableFuture<TbEntityDataSubCtx> historyFuture;
        if (cmd.getHistoryCmd() != null) {
            log.trace("[{}][{}] Going to process history command: {}", session.getSessionId(), cmd.getCmdId(), cmd.getHistoryCmd());
            historyFuture = handleHistoryCmd(ctx, cmd.getHistoryCmd());
        } else {
            historyFuture = Futures.immediateFuture(ctx);
        }
        Futures.addCallback(historyFuture, new FutureCallback<TbEntityDataSubCtx>() {
            @Override
            public void onSuccess(@Nullable TbEntityDataSubCtx theCtx) {
                if (cmd.getLatestCmd() != null) {
                    handleLatestCmd(theCtx, cmd.getLatestCmd());
                } else if (cmd.getTsCmd() != null) {
                    handleTimeSeriesCmd(theCtx, cmd.getTsCmd());
                } else if (!theCtx.isInitialDataSent()) {
                    EntityDataUpdate update = new EntityDataUpdate(theCtx.getCmdId(), theCtx.getData(), null);
                    wsService.sendWsMsg(theCtx.getSessionId(), update);
                    theCtx.setInitialDataSent(true);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("[{}][{}] Failed to process command", session.getSessionId(), cmd.getCmdId());
            }
        }, wsCallBackExecutor);
    }

    @Override
    public void handleCmd(TelemetryWebSocketSessionRef session, AlarmDataCmd cmd) {
        TbAlarmDataSubCtx ctx = getSubCtx(session.getSessionId(), cmd.getCmdId());
        if (ctx == null) {
            log.debug("[{}][{}] Creating new alarm subscription using: {}", session.getSessionId(), cmd.getCmdId(), cmd);
            ctx = createSubCtx(session, cmd);
        }
        ctx.setQuery(cmd.getQuery());
        AlarmDataQuery adq = ctx.getQuery();
        EntityDataSortOrder sortOrder = adq.getPageLink().getSortOrder();
        EntityDataSortOrder entitiesSortOrder;
        if (sortOrder == null || sortOrder.getKey().getType().equals(EntityKeyType.ALARM_FIELD)) {
            entitiesSortOrder = new EntityDataSortOrder(new EntityKey(EntityKeyType.ENTITY_FIELD, ModelConstants.CREATED_TIME_PROPERTY));
        } else {
            entitiesSortOrder = sortOrder;
        }
        EntityDataPageLink edpl = new EntityDataPageLink(0, maxEntitiesPerAlarmSubscription, null, entitiesSortOrder);
        EntityDataQuery edq = new EntityDataQuery(adq.getEntityFilter(), edpl, adq.getEntityFields(), adq.getLatestValues(), adq.getKeyFilters());
        PageData<EntityData> entitiesData = entityService.findEntityDataByQuery(ctx.getTenantId(), ctx.getCustomerId(), edq);
        List<EntityData> entities = entitiesData.getData();
        ctx.setEntitiesData(entitiesData);
        if (entities.isEmpty()) {
            AlarmDataUpdate update = new AlarmDataUpdate(cmd.getCmdId(), new PageData<>(Collections.emptyList(), 1, 0, false), null);
            wsService.sendWsMsg(ctx.getSessionId(), update);
        } else {
            ctx.setLastFetchTs(System.currentTimeMillis());
            PageData<AlarmData> alarms = alarmService.findAlarmDataByQueryForEntities(ctx.getTenantId(), ctx.getCustomerId(),
                    ctx.getQuery().getPageLink(), ctx.getOrderedEntityIds());
            alarms = ctx.setAndMergeAlarmsData(alarms);
            AlarmDataUpdate update = new AlarmDataUpdate(cmd.getCmdId(), alarms, null);
            wsService.sendWsMsg(ctx.getSessionId(), update);
            if (adq.getPageLink().getTimeWindow() > 0) {
                //TODO: refresh list of entities periodically (similar to time-series subscription).
                createAlarmSubscriptions(ctx);
            }
        }
    }

    private void refreshDynamicQuery(TenantId tenantId, CustomerId customerId, TbEntityDataSubCtx finalCtx) {
        try {
            long start = System.currentTimeMillis();
            TbEntityDataSubCtx.TbEntityDataSubCtxUpdateResult result = finalCtx.update(entityService.findEntityDataByQuery(tenantId, customerId, finalCtx.getQuery()));
            long end = System.currentTimeMillis();
            dynamicQueryInvocationCnt.incrementAndGet();
            dynamicQueryTimeSpent.addAndGet(end - start);
            result.getSubsToCancel().forEach(subId -> localSubscriptionService.cancelSubscription(finalCtx.getSessionId(), subId));
            result.getSubsToAdd().forEach(localSubscriptionService::addSubscription);
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to refresh query", finalCtx.getSessionId(), finalCtx.getCmdId(), e);
        }
    }

    @Scheduled(fixedDelayString = "${server.ws.dynamic_page_link.stats:10000}")
    public void printStats() {
        int regularQueryInvocationCntValue = regularQueryInvocationCnt.getAndSet(0);
        long regularQueryInvocationTimeValue = regularQueryTimeSpent.getAndSet(0);
        int dynamicQueryInvocationCntValue = dynamicQueryInvocationCnt.getAndSet(0);
        long dynamicQueryInvocationTimeValue = dynamicQueryTimeSpent.getAndSet(0);
        long dynamicQueryCnt = subscriptionsBySessionId.values().stream().map(Map::values).count();
        if (regularQueryInvocationCntValue > 0 || dynamicQueryInvocationCntValue > 0 || dynamicQueryCnt > 0) {
            log.info("Stats: regularQueryInvocationCnt = [{}], regularQueryInvocationTime = [{}], dynamicQueryCnt = [{}] dynamicQueryInvocationCnt = [{}], dynamicQueryInvocationTime = [{}]",
                    regularQueryInvocationCntValue, regularQueryInvocationTimeValue, dynamicQueryCnt, dynamicQueryInvocationCntValue, dynamicQueryInvocationTimeValue);
        }
    }

    private void clearSubs(TbEntityDataSubCtx ctx) {
        Collection<Integer> oldSubIds = ctx.clearSubscriptions();
        oldSubIds.forEach(subId -> localSubscriptionService.cancelSubscription(serviceId, subId));
    }

    private TbEntityDataSubCtx createSubCtx(TelemetryWebSocketSessionRef sessionRef, EntityDataCmd cmd) {
        Map<Integer, TbAbstractDataSubCtx> sessionSubs = subscriptionsBySessionId.computeIfAbsent(sessionRef.getSessionId(), k -> new HashMap<>());
        TbEntityDataSubCtx ctx = new TbEntityDataSubCtx(serviceId, wsService, sessionRef, cmd.getCmdId());
        ctx.setQuery(cmd.getQuery());
        sessionSubs.put(cmd.getCmdId(), ctx);
        return ctx;
    }

    private TbAlarmDataSubCtx createSubCtx(TelemetryWebSocketSessionRef sessionRef, AlarmDataCmd cmd) {
        Map<Integer, TbAbstractDataSubCtx> sessionSubs = subscriptionsBySessionId.computeIfAbsent(sessionRef.getSessionId(), k -> new HashMap<>());
        TbAlarmDataSubCtx ctx = new TbAlarmDataSubCtx(serviceId, wsService, sessionRef, cmd.getCmdId());
        ctx.setQuery(cmd.getQuery());
        sessionSubs.put(cmd.getCmdId(), ctx);
        return ctx;
    }

    private <T extends TbAbstractDataSubCtx> T getSubCtx(String sessionId, int cmdId) {
        Map<Integer, TbAbstractDataSubCtx> sessionSubs = subscriptionsBySessionId.get(sessionId);
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
        List<String> keys = cmd.getKeys();
        List<ReadTsKvQuery> finalTsKvQueryList;
        List<ReadTsKvQuery> tsKvQueryList = cmd.getKeys().stream().map(key -> new BaseReadTsKvQuery(
                key, cmd.getStartTs(), cmd.getEndTs(), cmd.getInterval(), getLimit(cmd.getLimit()), cmd.getAgg()
        )).collect(Collectors.toList());
        if (cmd.isFetchLatestPreviousPoint()) {
            finalTsKvQueryList = new ArrayList<>(tsKvQueryList);
            finalTsKvQueryList.addAll(cmd.getKeys().stream().map(key -> new BaseReadTsKvQuery(
                    key, cmd.getStartTs() - TimeUnit.DAYS.toMillis(365), cmd.getStartTs(), cmd.getInterval(), 1, cmd.getAgg()
            )).collect(Collectors.toList()));
        } else {
            finalTsKvQueryList = tsKvQueryList;
        }
        Map<EntityData, ListenableFuture<List<TsKvEntry>>> fetchResultMap = new HashMap<>();
        ctx.getData().getData().forEach(entityData -> fetchResultMap.put(entityData,
                tsService.findAll(ctx.getTenantId(), entityData.getEntityId(), finalTsKvQueryList)));
        return Futures.transform(Futures.allAsList(fetchResultMap.values()), f -> {
            fetchResultMap.forEach((entityData, future) -> {
                Map<String, List<TsValue>> keyData = new LinkedHashMap<>();
                cmd.getKeys().forEach(key -> keyData.put(key, new ArrayList<>()));
                try {
                    List<TsKvEntry> entityTsData = future.get();
                    if (entityTsData != null) {
                        entityTsData.forEach(entry -> keyData.get(entry.getKey()).add(new TsValue(entry.getTs(), entry.getValueAsString())));
                    }
                    keyData.forEach((k, v) -> entityData.getTimeseries().put(k, v.toArray(new TsValue[v.size()])));
                    if (cmd.isFetchLatestPreviousPoint()) {
                        entityData.getTimeseries().values().forEach(dataArray -> {
                            Arrays.sort(dataArray, (o1, o2) -> Long.compare(o2.getTs(), o1.getTs()));
                        });
                    }
                } catch (InterruptedException | ExecutionException e) {
                    log.warn("[{}][{}][{}] Failed to fetch historical data", ctx.getSessionId(), ctx.getCmdId(), entityData.getEntityId(), e);
                    wsService.sendWsMsg(ctx.getSessionId(),
                            new EntityDataUpdate(ctx.getCmdId(), SubscriptionErrorCode.INTERNAL_ERROR.getCode(), "Failed to fetch historical data!"));
                }
            });
            EntityDataUpdate update;
            if (!ctx.isInitialDataSent()) {
                update = new EntityDataUpdate(ctx.getCmdId(), ctx.getData(), null);
                ctx.setInitialDataSent(true);
            } else {
                update = new EntityDataUpdate(ctx.getCmdId(), null, ctx.getData().getData());
            }
            wsService.sendWsMsg(ctx.getSessionId(), update);
            if (subscribe) {
                createTelemetrySubscriptions(ctx, keys.stream().map(key -> new EntityKey(EntityKeyType.TIME_SERIES, key)).collect(Collectors.toList()), false);
            }
            ctx.getData().getData().forEach(ed -> ed.getTimeseries().clear());
            return ctx;
        }, wsCallBackExecutor);
    }

    private void handleLatestCmd(TbEntityDataSubCtx ctx, LatestValueCmd latestCmd) {
        log.trace("[{}][{}] Going to process latest command: {}", ctx.getSessionId(), ctx.getCmdId(), latestCmd);
        //Fetch the latest values for telemetry keys (in case they are not copied from NoSQL to SQL DB in hybrid mode.
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
            Futures.addCallback(Futures.allAsList(missingTelemetryFutures.values()), new FutureCallback<List<Map<String, TsValue>>>() {
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
                    if (!ctx.isInitialDataSent()) {
                        update = new EntityDataUpdate(ctx.getCmdId(), ctx.getData(), null);
                        ctx.setInitialDataSent(true);
                    } else {
                        update = new EntityDataUpdate(ctx.getCmdId(), null, ctx.getData().getData());
                    }
                    wsService.sendWsMsg(ctx.getSessionId(), update);
                    createTelemetrySubscriptions(ctx, latestCmd.getKeys());
                }

                @Override
                public void onFailure(Throwable t) {
                    log.warn("[{}][{}] Failed to process websocket command: {}:{}", ctx.getSessionId(), ctx.getCmdId(), ctx.getQuery(), latestCmd, t);
                    wsService.sendWsMsg(ctx.getSessionId(),
                            new EntityDataUpdate(ctx.getCmdId(), SubscriptionErrorCode.INTERNAL_ERROR.getCode(), "Failed to process websocket command!"));
                }
            }, wsCallBackExecutor);
        } else {
            if (!ctx.isInitialDataSent()) {
                EntityDataUpdate update = new EntityDataUpdate(ctx.getCmdId(), ctx.getData(), null);
                wsService.sendWsMsg(ctx.getSessionId(), update);
                ctx.setInitialDataSent(true);
            }
            createTelemetrySubscriptions(ctx, latestCmd.getKeys());
        }
    }

    private void createTelemetrySubscriptions(TbEntityDataSubCtx ctx, List<EntityKey> keys) {
        createTelemetrySubscriptions(ctx, keys, true);
    }

    private void createAlarmSubscriptions(TbAlarmDataSubCtx ctx) {
        List<TbSubscription> subscriptions = ctx.createSubscriptions();
        subscriptions.forEach(localSubscriptionService::addSubscription);
    }

    private void createTelemetrySubscriptions(TbEntityDataSubCtx ctx, List<EntityKey> keys, boolean latest) {
        List<TbSubscription> tbSubs = ctx.createSubscriptions(keys, latest);
        tbSubs.forEach(sub -> localSubscriptionService.addSubscription(sub));
    }

    private Map<String, TsValue> toTsValue(List<TsKvEntry> data) {
        return data.stream().collect(Collectors.toMap(TsKvEntry::getKey, value -> new TsValue(value.getTs(), value.getValueAsString())));
    }

    @Override
    public void cancelSubscription(String sessionId, EntityDataUnsubscribeCmd cmd) {
        cleanupAndCancel(getSubCtx(sessionId, cmd.getCmdId()));
    }

    private void cleanupAndCancel(TbEntityDataSubCtx ctx) {
        if (ctx != null) {
            ctx.cancelRefreshTask();
            clearSubs(ctx);
        }
    }

    @Override
    public void cancelAllSessionSubscriptions(String sessionId) {
        Map<Integer, TbAbstractDataSubCtx> sessionSubs = subscriptionsBySessionId.remove(sessionId);
        if (sessionSubs != null) {
            sessionSubs.values().stream().filter(sub -> sub instanceof TbEntityDataSubCtx).map(sub -> (TbEntityDataSubCtx) sub).forEach(this::cleanupAndCancel);
        }
    }

    private int getLimit(int limit) {
        return limit == 0 ? DEFAULT_LIMIT : limit;
    }

}
