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
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.rule.engine.api.AttributesDeleteRequest;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.rule.engine.api.DeviceStateManager;
import org.thingsboard.rule.engine.api.RuleEngineTelemetryService;
import org.thingsboard.rule.engine.api.TimeseriesDeleteRequest;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.AttributesSaveResult;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TimeseriesSaveResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.kv.TsKvLatestRemovingResult;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.rule.engine.DeviceAttributesEventNotificationMsg;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.util.KvUtils;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.cf.CalculatedFieldQueueService;
import org.thingsboard.server.service.entitiy.entityview.TbEntityViewService;
import org.thingsboard.server.service.state.DefaultDeviceStateService;
import org.thingsboard.server.service.subscription.TbSubscriptionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingLong;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;

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
    private final CalculatedFieldQueueService calculatedFieldQueueService;
    private final DeviceStateManager deviceStateManager;

    private ExecutorService tsCallBackExecutor;

    @Value("${sql.ts.value_no_xss_validation:false}")
    private boolean valueNoXssValidation;
    @Value("${sql.ts.callback_thread_pool_size:12}")
    private int callbackThreadPoolSize;

    public DefaultTelemetrySubscriptionService(AttributesService attrService,
                                               TimeseriesService tsService,
                                               @Lazy TbEntityViewService tbEntityViewService,
                                               TbApiUsageReportClient apiUsageClient,
                                               TbApiUsageStateService apiUsageStateService,
                                               CalculatedFieldQueueService calculatedFieldQueueService,
                                               DeviceStateManager deviceStateManager) {
        this.attrService = attrService;
        this.tsService = tsService;
        this.tbEntityViewService = tbEntityViewService;
        this.apiUsageClient = apiUsageClient;
        this.apiUsageStateService = apiUsageStateService;
        this.calculatedFieldQueueService = calculatedFieldQueueService;
        this.deviceStateManager = deviceStateManager;
    }

    @PostConstruct
    public void initExecutor() {
        super.initExecutor();
        tsCallBackExecutor = ThingsBoardExecutors.newWorkStealingPool(callbackThreadPoolSize, "ts-service-ts-callback");
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
            ListenableFuture<TimeseriesSaveResult> future = saveTimeseriesInternal(request);
            if (request.getStrategy().saveTimeseries()) {
                Futures.addCallback(future, getApiUsageCallback(tenantId, request.getCustomerId(), sysTenant), tsCallBackExecutor);
            }
        } else {
            request.getCallback().onFailure(new RuntimeException("DB storage writes are disabled due to API limits!"));
        }
    }

    @Override
    public ListenableFuture<TimeseriesSaveResult> saveTimeseriesInternal(TimeseriesSaveRequest request) {
        TenantId tenantId = request.getTenantId();
        EntityId entityId = request.getEntityId();
        TimeseriesSaveRequest.Strategy strategy = request.getStrategy();
        ListenableFuture<TimeseriesSaveResult> resultFuture;

        if (strategy.saveTimeseries() && strategy.saveLatest()) {
            resultFuture = tsService.save(tenantId, entityId, request.getEntries(), request.getTtl());
        } else if (strategy.saveLatest()) {
            resultFuture = tsService.saveLatest(tenantId, entityId, request.getEntries());
        } else if (strategy.saveTimeseries()) {
            resultFuture = tsService.saveWithoutLatest(tenantId, entityId, request.getEntries(), request.getTtl());
        } else {
            resultFuture = Futures.immediateFuture(TimeseriesSaveResult.EMPTY);
        }

        addMainCallback(resultFuture, result -> {
            if (strategy.processCalculatedFields()) {
                calculatedFieldQueueService.pushRequestToQueue(request, result, request.getCallback());
            } else {
                request.getCallback().onSuccess(null);
            }
        }, t -> request.getCallback().onFailure(t));

        if (strategy.sendWsUpdate()) {
            addWsCallback(resultFuture, success -> onTimeSeriesUpdate(tenantId, entityId, request.getEntries()));
        }
        if (strategy.saveLatest() && entityId.getEntityType().isOneOf(EntityType.DEVICE, EntityType.ASSET)) {
            addMainCallback(resultFuture, __ -> copyLatestToEntityViews(tenantId, entityId, request.getEntries()));
        }
        return resultFuture;
    }

    @Override
    public void saveAttributes(AttributesSaveRequest request) {
        checkInternalEntity(request.getEntityId());
        saveAttributesInternal(request);
    }

    @Override
    public ListenableFuture<AttributesSaveResult> saveAttributesInternal(AttributesSaveRequest request) {
        TenantId tenantId = request.getTenantId();
        EntityId entityId = request.getEntityId();
        AttributesSaveRequest.Strategy strategy = request.getStrategy();
        ListenableFuture<AttributesSaveResult> resultFuture;

        if (strategy.saveAttributes()) {
            resultFuture = attrService.save(tenantId, entityId, request.getScope(), request.getEntries());
        } else {
            resultFuture = Futures.immediateFuture(AttributesSaveResult.EMPTY);
        }

        addMainCallback(resultFuture, result -> {
            if (strategy.processCalculatedFields()) {
                calculatedFieldQueueService.pushRequestToQueue(request, result, request.getCallback());
            } else {
                request.getCallback().onSuccess(null);
            }
        }, t -> request.getCallback().onFailure(t));

        if (shouldSendSharedAttributesUpdatedNotification(request)) {
            addMainCallback(resultFuture, success -> clusterService.pushMsgToCore(
                    DeviceAttributesEventNotificationMsg.onUpdate(tenantId, new DeviceId(entityId.getId()), DataConstants.SHARED_SCOPE, request.getEntries()), null
            ));
        }

        if (shouldCheckForInactivityTimeoutUpdates(request)) {
            findNewInactivityTimeout(request.getEntries()).ifPresent(newInactivityTimeout ->
                    addMainCallback(resultFuture, success -> deviceStateManager.onDeviceInactivityTimeoutUpdate(
                            tenantId, new DeviceId(entityId.getId()), newInactivityTimeout, TbCallback.EMPTY)
                    )
            );
        }

        if (strategy.sendWsUpdate()) {
            addWsCallback(resultFuture, success -> onAttributesUpdate(tenantId, entityId, request.getScope().name(), request.getEntries()));
        }
        return resultFuture;
    }

    private static boolean shouldSendSharedAttributesUpdatedNotification(AttributesSaveRequest request) {
        return request.getStrategy().saveAttributes() && shouldSendSharedAttributesNotification(request.getEntityId(), request.getScope(), request.isNotifyDevice());
    }

    private static boolean shouldCheckForInactivityTimeoutUpdates(AttributesSaveRequest request) {
        return request.getStrategy().saveAttributes()
                && request.getEntityId().getEntityType() == EntityType.DEVICE
                && request.getScope() == AttributeScope.SERVER_SCOPE;
    }

    private static Optional<Long> findNewInactivityTimeout(List<AttributeKvEntry> entries) {
        return entries.stream()
                .filter(entry -> Objects.equals(DefaultDeviceStateService.INACTIVITY_TIMEOUT, entry.getKey()))
                // Select the entry with the highest version, or if the versions are equal, the one with the most recent update timestamp
                .max(comparing(AttributeKvEntry::getVersion, nullsFirst(naturalOrder())).thenComparingLong(AttributeKvEntry::getLastUpdateTs))
                .map(DefaultTelemetrySubscriptionService::parseAsLong);
    }

    private static long parseAsLong(KvEntry kve) {
        try {
            return Long.parseLong(kve.getValueAsString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    @Override
    public void deleteAttributes(AttributesDeleteRequest request) {
        checkInternalEntity(request.getEntityId());
        deleteAttributesInternal(request);
    }

    @Override
    public void deleteAttributesInternal(AttributesDeleteRequest request) {
        TenantId tenantId = request.getTenantId();
        EntityId entityId = request.getEntityId();

        ListenableFuture<List<String>> deleteFuture = attrService.removeAll(tenantId, entityId, request.getScope(), request.getKeys());

        addMainCallback(deleteFuture,
                result -> calculatedFieldQueueService.pushRequestToQueue(request, result, request.getCallback()),
                t -> request.getCallback().onFailure(t)
        );

        if (shouldSendSharedAttributesDeletedNotification(request)) {
            addMainCallback(deleteFuture, success -> clusterService.pushMsgToCore(
                    DeviceAttributesEventNotificationMsg.onDelete(tenantId, new DeviceId(entityId.getId()), DataConstants.SHARED_SCOPE, request.getKeys()), null
            ));
        }

        if (inactivityTimeoutDeleted(request)) {
            addMainCallback(deleteFuture, success -> deviceStateManager.onDeviceInactivityTimeoutUpdate(
                    tenantId, new DeviceId(entityId.getId()), 0L, TbCallback.EMPTY)
            );
        }

        addWsCallback(deleteFuture, success -> onAttributesDelete(tenantId, entityId, request.getScope().name(), request.getKeys()));
    }

    private static boolean shouldSendSharedAttributesDeletedNotification(AttributesDeleteRequest request) {
        return shouldSendSharedAttributesNotification(request.getEntityId(), request.getScope(), request.isNotifyDevice());
    }

    private static boolean shouldSendSharedAttributesNotification(EntityId entityId, AttributeScope scope, boolean notifyDevice) {
        return entityId.getEntityType() == EntityType.DEVICE
                && scope == AttributeScope.SHARED_SCOPE
                && notifyDevice;
    }

    private static boolean inactivityTimeoutDeleted(AttributesDeleteRequest request) {
        return request.getEntityId().getEntityType() == EntityType.DEVICE
                && request.getScope() == AttributeScope.SERVER_SCOPE
                && request.getKeys().stream().anyMatch(key -> Objects.equals(DefaultDeviceStateService.INACTIVITY_TIMEOUT, key));
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
            DonAsynchron.withCallback(deleteFuture, result -> {
                calculatedFieldQueueService.pushRequestToQueue(request, request.getKeys(), getCalculatedFieldCallback(request.getCallback(), request.getKeys()));
            }, safeCallback(getCalculatedFieldCallback(request.getCallback(), request.getKeys())), tsCallBackExecutor);
        } else {
            ListenableFuture<List<String>> deleteFuture = tsService.removeAllLatest(request.getTenantId(), request.getEntityId());
            DonAsynchron.withCallback(deleteFuture, result -> {
                calculatedFieldQueueService.pushRequestToQueue(request, request.getKeys(), getCalculatedFieldCallback(request.getCallback(), result));
            }, safeCallback(getCalculatedFieldCallback(request.getCallback(), request.getKeys())), tsCallBackExecutor);
        }
    }

    private void copyLatestToEntityViews(TenantId tenantId, EntityId entityId, List<TsKvEntry> ts) {
        Futures.addCallback(tbEntityViewService.findEntityViewsByTenantIdAndEntityIdAsync(tenantId, entityId),
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
                                                .max(comparingLong(TsKvEntry::getTs));
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

    private void onAttributesUpdate(TenantId tenantId, EntityId entityId, String scope, List<AttributeKvEntry> attributes) {
        forwardToSubscriptionManagerService(tenantId, entityId,
                subscriptionManagerService -> subscriptionManagerService.onAttributesUpdate(tenantId, entityId, scope, attributes, TbCallback.EMPTY),
                () -> TbSubscriptionUtils.toAttributesUpdateProto(tenantId, entityId, scope, attributes));
    }

    private void onAttributesDelete(TenantId tenantId, EntityId entityId, String scope, List<String> keys) {
        forwardToSubscriptionManagerService(tenantId, entityId,
                subscriptionManagerService -> subscriptionManagerService.onAttributesDelete(tenantId, entityId, scope, keys, TbCallback.EMPTY),
                () -> TbSubscriptionUtils.toAttributesDeleteProto(tenantId, entityId, scope, keys));
    }

    private void onTimeSeriesUpdate(TenantId tenantId, EntityId entityId, List<TsKvEntry> ts) {
        forwardToSubscriptionManagerService(tenantId, entityId,
                subscriptionManagerService -> subscriptionManagerService.onTimeSeriesUpdate(tenantId, entityId, ts, TbCallback.EMPTY),
                () -> TbSubscriptionUtils.toTimeseriesUpdateProto(tenantId, entityId, ts));
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
        }, () -> TbSubscriptionUtils.toTimeseriesDeleteProto(tenantId, entityId, keys));
    }

    private <S> void addMainCallback(ListenableFuture<S> saveFuture, final FutureCallback<Void> callback) {
        if (callback == null) return;
        addMainCallback(saveFuture, result -> callback.onSuccess(null), callback::onFailure);
    }

    private <S> void addMainCallback(ListenableFuture<S> saveFuture, Consumer<S> onSuccess) {
        addMainCallback(saveFuture, onSuccess, null);
    }

    private <S> void addMainCallback(ListenableFuture<S> saveFuture, Consumer<S> onSuccess, Consumer<Throwable> onFailure) {
        DonAsynchron.withCallback(saveFuture, onSuccess, onFailure, tsCallBackExecutor);
    }

    private void checkInternalEntity(EntityId entityId) {
        if (EntityType.API_USAGE_STATE.equals(entityId.getEntityType())) {
            throw new RuntimeException("Can't update API Usage State!");
        }
    }

    private FutureCallback<TimeseriesSaveResult> getApiUsageCallback(TenantId tenantId, CustomerId customerId, boolean sysTenant) {
        return new FutureCallback<>() {
            @Override
            public void onSuccess(TimeseriesSaveResult result) {
                Integer dataPoints = result.getDataPoints();
                if (!sysTenant && dataPoints != null && dataPoints > 0) {
                    apiUsageClient.report(tenantId, customerId, ApiUsageRecordKey.STORAGE_DP_COUNT, dataPoints);
                }
            }

            @Override
            public void onFailure(Throwable t) {}
        };
    }

    private FutureCallback<Void> getCalculatedFieldCallback(FutureCallback<List<String>> originalCallback, List<String> keys) {
        return new FutureCallback<>() {
            @Override
            public void onSuccess(Void unused) {
                originalCallback.onSuccess(keys);
            }

            @Override
            public void onFailure(Throwable t) {
                originalCallback.onFailure(t);
            }
        };
    }

}
