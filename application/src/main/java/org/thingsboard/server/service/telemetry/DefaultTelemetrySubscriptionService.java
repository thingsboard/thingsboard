/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.telemetry;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.rule.engine.api.AttributesDeleteRequest;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.rule.engine.api.RuleEngineTelemetryService;
import org.thingsboard.rule.engine.api.TimeseriesDeleteRequest;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.kv.TsKvLatestRemovingResult;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.util.KvUtils;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.entitiy.entityview.TbEntityViewService;
import org.thingsboard.server.service.subscription.TbSubscriptionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Created by ashvayka on 27.03.18.
 */
@Service
@Slf4j
public class DefaultTelemetrySubscriptionService extends AbstractSubscriptionService implements TelemetrySubscriptionService, RuleEngineTelemetryService {

    private final AttributesService attrService;
    private final TimeseriesService tsService;
    private final TbEntityViewService tbEntityViewService;
    private final TbApiUsageReportClient apiUsageClient;
    private final TbApiUsageStateService apiUsageStateService;

    private ExecutorService tsCallBackExecutor;

    @Value("${sql.ts.value_no_xss_validation:false}")
    private boolean valueNoXssValidation;

    public DefaultTelemetrySubscriptionService(AttributesService attrService,
                                               TimeseriesService tsService,
                                               @Lazy TbEntityViewService tbEntityViewService,
                                               TbApiUsageReportClient apiUsageClient,
                                               TbApiUsageStateService apiUsageStateService) {
        this.attrService = attrService;
        this.tsService = tsService;
        this.tbEntityViewService = tbEntityViewService;
        this.apiUsageClient = apiUsageClient;
        this.apiUsageStateService = apiUsageStateService;
    }

    @PostConstruct
    public void initExecutor() {
        super.initExecutor();
        tsCallBackExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("ts-service-ts-callback"));
    }

    @Override
    protected String getExecutorPrefix() {
        return "ts";
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (tsCallBackExecutor != null) {
            tsCallBackExecutor.shutdownNow();
        }
        super.shutdownExecutor();
    }

    @Override
    public void saveTimeseries(TimeseriesSaveRequest request) {
        TenantId tenantId = request.getTenantId();
        EntityId entityId = request.getEntityId();
        checkInternalEntity(entityId);
        boolean sysTenant = TenantId.SYS_TENANT_ID.equals(tenantId) || tenantId == null;
        if (sysTenant || !request.getStrategy().saveTimeseries() || apiUsageStateService.getApiUsageState(tenantId).isDbStorageEnabled()) {
            KvUtils.validate(request.getEntries(), valueNoXssValidation);
            ListenableFuture<Integer> future = saveTimeseriesInternal(request);
            if (request.getStrategy().saveTimeseries()) {
                FutureCallback<Integer> callback = getApiUsageCallback(tenantId, request.getCustomerId(), sysTenant, request.getCallback());
                Futures.addCallback(future, callback, tsCallBackExecutor);
            }
        } else {
            request.getCallback().onFailure(new RuntimeException("DB storage writes are disabled due to API limits!"));
        }
    }

    @Override
    public ListenableFuture<Integer> saveTimeseriesInternal(TimeseriesSaveRequest request) {
        TenantId tenantId = request.getTenantId();
        EntityId entityId = request.getEntityId();
        TimeseriesSaveRequest.Strategy strategy = request.getStrategy();
        ListenableFuture<Integer> saveFuture;
        if (strategy.saveTimeseries() && strategy.saveLatest()) {
            saveFuture = tsService.save(tenantId, entityId, request.getEntries(), request.getTtl());
        } else if (strategy.saveLatest()) {
            saveFuture = Futures.transform(tsService.saveLatest(tenantId, entityId, request.getEntries()), result -> 0, MoreExecutors.directExecutor());
        } else if (strategy.saveTimeseries()) {
            saveFuture = tsService.saveWithoutLatest(tenantId, entityId, request.getEntries(), request.getTtl());
        } else {
            saveFuture = Futures.immediateFuture(0);
        }

        addMainCallback(saveFuture, request.getCallback());
        if (strategy.sendWsUpdate()) {
            addWsCallback(saveFuture, success -> onTimeSeriesUpdate(tenantId, entityId, request.getEntries()));
        }
        if (strategy.saveLatest()) {
            copyLatestToEntityViews(tenantId, entityId, request.getEntries());
        }
        return saveFuture;
    }

    @Override
    public void saveAttributes(AttributesSaveRequest request) {
        checkInternalEntity(request.getEntityId());
        saveAttributesInternal(request);
    }

    @Override
    public void saveAttributesInternal(AttributesSaveRequest request) {
        log.trace("Executing saveInternal [{}]", request);
        ListenableFuture<List<Long>> saveFuture = attrService.save(request.getTenantId(), request.getEntityId(), request.getScope(), request.getEntries());
        addMainCallback(saveFuture, request.getCallback());
        addWsCallback(saveFuture, success -> onAttributesUpdate(request.getTenantId(), request.getEntityId(), request.getScope().name(), request.getEntries(), request.isNotifyDevice()));
    }

    @Override
    public void deleteAttributes(AttributesDeleteRequest request) {
        checkInternalEntity(request.getEntityId());
        deleteAttributesInternal(request);
    }

    @Override
    public void deleteAttributesInternal(AttributesDeleteRequest request) {
        ListenableFuture<List<String>> deleteFuture = attrService.removeAll(request.getTenantId(), request.getEntityId(), request.getScope(), request.getKeys());
        addMainCallback(deleteFuture, request.getCallback());
        addWsCallback(deleteFuture, success -> onAttributesDelete(request.getTenantId(), request.getEntityId(), request.getScope().name(), request.getKeys(), request.isNotifyDevice()));
    }

    @Override
    public void deleteTimeseries(TimeseriesDeleteRequest request) {
        checkInternalEntity(request.getEntityId());
        deleteTimeseriesInternal(request);
    }

    @Override
    public void deleteTimeseriesInternal(TimeseriesDeleteRequest request) {
        if (CollectionUtils.isNotEmpty(request.getKeys())) {
            ListenableFuture<List<TsKvLatestRemovingResult>> deleteFuture;
            if (request.getDeleteHistoryQueries() == null) {
                deleteFuture = tsService.removeLatest(request.getTenantId(), request.getEntityId(), request.getKeys());
            } else {
                deleteFuture = tsService.remove(request.getTenantId(), request.getEntityId(), request.getDeleteHistoryQueries());
                addWsCallback(deleteFuture, result -> onTimeSeriesDelete(request.getTenantId(), request.getEntityId(), request.getKeys(), result));
            }
            addMainCallback(deleteFuture, __ -> request.getCallback().onSuccess(request.getKeys()), request.getCallback()::onFailure);
        } else {
            ListenableFuture<List<String>> deleteFuture = tsService.removeAllLatest(request.getTenantId(), request.getEntityId());
            addMainCallback(deleteFuture, request.getCallback()::onSuccess, request.getCallback()::onFailure);
        }
    }

    private void copyLatestToEntityViews(TenantId tenantId, EntityId entityId, List<TsKvEntry> ts) {
        if (EntityType.DEVICE.equals(entityId.getEntityType()) || EntityType.ASSET.equals(entityId.getEntityType())) {
            Futures.addCallback(this.tbEntityViewService.findEntityViewsByTenantIdAndEntityIdAsync(tenantId, entityId),
                    new FutureCallback<>() {
                        @Override
                        public void onSuccess(@Nullable List<EntityView> result) {
                            if (result != null && !result.isEmpty()) {
                                Map<String, List<TsKvEntry>> tsMap = new HashMap<>();
                                for (TsKvEntry entry : ts) {
                                    tsMap.computeIfAbsent(entry.getKey(), s -> new ArrayList<>()).add(entry);
                                }
                                for (EntityView entityView : result) {
                                    List<String> keys = entityView.getKeys() != null && entityView.getKeys().getTimeseries() != null ?
                                            entityView.getKeys().getTimeseries() : new ArrayList<>(tsMap.keySet());
                                    List<TsKvEntry> entityViewLatest = new ArrayList<>();
                                    long startTs = entityView.getStartTimeMs();
                                    long endTs = entityView.getEndTimeMs() == 0 ? Long.MAX_VALUE : entityView.getEndTimeMs();
                                    for (String key : keys) {
                                        List<TsKvEntry> entries = tsMap.get(key);
                                        if (entries != null) {
                                            Optional<TsKvEntry> tsKvEntry = entries.stream()
                                                    .filter(entry -> entry.getTs() > startTs && entry.getTs() <= endTs)
                                                    .max(Comparator.comparingLong(TsKvEntry::getTs));
                                            tsKvEntry.ifPresent(entityViewLatest::add);
                                        }
                                    }
                                    if (!entityViewLatest.isEmpty()) {
                                        saveTimeseries(TimeseriesSaveRequest.builder()
                                                .tenantId(tenantId)
                                                .entityId(entityView.getId())
                                                .entries(entityViewLatest)
                                                .strategy(TimeseriesSaveRequest.Strategy.LATEST_AND_WS)
                                                .callback(new FutureCallback<>() {
                                                    @Override
                                                    public void onSuccess(@Nullable Void tmp) {}

                                                    @Override
                                                    public void onFailure(Throwable t) {
                                                        log.error("[{}][{}] Failed to save entity view latest timeseries: {}", tenantId, entityView.getId(), entityViewLatest, t);
                                                    }
                                                })
                                                .build());
                                    }
                                }
                            }
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            log.error("Error while finding entity views by tenantId and entityId", t);
                        }
                    }, MoreExecutors.directExecutor());
        }
    }

    private void onAttributesUpdate(TenantId tenantId, EntityId entityId, String scope, List<AttributeKvEntry> attributes, boolean notifyDevice) {
        forwardToSubscriptionManagerService(tenantId, entityId, subscriptionManagerService -> {
            subscriptionManagerService.onAttributesUpdate(tenantId, entityId, scope, attributes, notifyDevice, TbCallback.EMPTY);
        }, () -> {
            return TbSubscriptionUtils.toAttributesUpdateProto(tenantId, entityId, scope, attributes);
        });
    }

    private void onAttributesDelete(TenantId tenantId, EntityId entityId, String scope, List<String> keys, boolean notifyDevice) {
        forwardToSubscriptionManagerService(tenantId, entityId, subscriptionManagerService -> {
            subscriptionManagerService.onAttributesDelete(tenantId, entityId, scope, keys, notifyDevice, TbCallback.EMPTY);
        }, () -> {
            return TbSubscriptionUtils.toAttributesDeleteProto(tenantId, entityId, scope, keys, notifyDevice);
        });
    }

    private void onTimeSeriesUpdate(TenantId tenantId, EntityId entityId, List<TsKvEntry> ts) {
        forwardToSubscriptionManagerService(tenantId, entityId, subscriptionManagerService -> {
            subscriptionManagerService.onTimeSeriesUpdate(tenantId, entityId, ts, TbCallback.EMPTY);
        }, () -> {
            return TbSubscriptionUtils.toTimeseriesUpdateProto(tenantId, entityId, ts);
        });
    }

    private void onTimeSeriesDelete(TenantId tenantId, EntityId entityId, List<String> keys, List<TsKvLatestRemovingResult> ts) {
        forwardToSubscriptionManagerService(tenantId, entityId, subscriptionManagerService -> {
            List<TsKvEntry> updated = new ArrayList<>();
            List<String> deleted = new ArrayList<>();

            ts.stream().filter(Objects::nonNull).forEach(res -> {
                if (res.isRemoved()) {
                    if (res.getData() != null) {
                        updated.add(res.getData());
                    } else {
                        deleted.add(res.getKey());
                    }
                }
            });

            subscriptionManagerService.onTimeSeriesUpdate(tenantId, entityId, updated, TbCallback.EMPTY);
            subscriptionManagerService.onTimeSeriesDelete(tenantId, entityId, deleted, TbCallback.EMPTY);
        }, () -> {
            return TbSubscriptionUtils.toTimeseriesDeleteProto(tenantId, entityId, keys);
        });
    }

    private <S> void addMainCallback(ListenableFuture<S> saveFuture, final FutureCallback<Void> callback) {
        if (callback == null) return;
        addMainCallback(saveFuture, result -> callback.onSuccess(null), callback::onFailure);
    }

    private <S> void addMainCallback(ListenableFuture<S> saveFuture, Consumer<S> onSuccess, Consumer<Throwable> onFailure) {
        DonAsynchron.withCallback(saveFuture, onSuccess, onFailure, tsCallBackExecutor);
    }

    private void checkInternalEntity(EntityId entityId) {
        if (EntityType.API_USAGE_STATE.equals(entityId.getEntityType())) {
            throw new RuntimeException("Can't update API Usage State!");
        }
    }

    private FutureCallback<Integer> getApiUsageCallback(TenantId tenantId, CustomerId customerId, boolean sysTenant, FutureCallback<Void> callback) {
        return new FutureCallback<>() {
            @Override
            public void onSuccess(Integer result) {
                if (!sysTenant && result != null && result > 0) {
                    apiUsageClient.report(tenantId, customerId, ApiUsageRecordKey.STORAGE_DP_COUNT, result);
                }
                callback.onSuccess(null);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
        };
    }

}
