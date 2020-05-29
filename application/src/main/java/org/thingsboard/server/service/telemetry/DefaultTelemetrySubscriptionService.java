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
package org.thingsboard.server.service.telemetry;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.PartitionChangeEvent;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.service.queue.TbClusterService;
import org.thingsboard.server.service.subscription.SubscriptionManagerService;
import org.thingsboard.server.service.subscription.TbSubscriptionUtils;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Created by ashvayka on 27.03.18.
 */
@Service
@Slf4j
public class DefaultTelemetrySubscriptionService implements TelemetrySubscriptionService {

    private final Set<TopicPartitionInfo> currentPartitions = ConcurrentHashMap.newKeySet();

    private final AttributesService attrService;
    private final TimeseriesService tsService;
    private final TbClusterService clusterService;
    private final PartitionService partitionService;
    private Optional<SubscriptionManagerService> subscriptionManagerService;

    private ExecutorService tsCallBackExecutor;
    private ExecutorService wsCallBackExecutor;

    public DefaultTelemetrySubscriptionService(AttributesService attrService,
                                               TimeseriesService tsService,
                                               TbClusterService clusterService,
                                               PartitionService partitionService) {
        this.attrService = attrService;
        this.tsService = tsService;
        this.clusterService = clusterService;
        this.partitionService = partitionService;
    }

    @Autowired(required = false)
    public void setSubscriptionManagerService(Optional<SubscriptionManagerService> subscriptionManagerService) {
        this.subscriptionManagerService = subscriptionManagerService;
    }

    @PostConstruct
    public void initExecutor() {
        tsCallBackExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("ts-service-ts-callback"));
        wsCallBackExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("ts-service-ws-callback"));
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (tsCallBackExecutor != null) {
            tsCallBackExecutor.shutdownNow();
        }
        if (wsCallBackExecutor != null) {
            wsCallBackExecutor.shutdownNow();
        }
    }

    @Override
    @EventListener(PartitionChangeEvent.class)
    public void onApplicationEvent(PartitionChangeEvent partitionChangeEvent) {
        if (ServiceType.TB_CORE.equals(partitionChangeEvent.getServiceType())) {
            currentPartitions.clear();
            currentPartitions.addAll(partitionChangeEvent.getPartitions());
        }
    }

    @Override
    public void saveAndNotify(TenantId tenantId, EntityId entityId, List<TsKvEntry> ts, FutureCallback<Void> callback) {
        saveAndNotify(tenantId, entityId, ts, 0L, callback);
    }

    @Override
    public void saveAndNotify(TenantId tenantId, EntityId entityId, List<TsKvEntry> ts, long ttl, FutureCallback<Void> callback) {
        ListenableFuture<List<Void>> saveFuture = tsService.save(tenantId, entityId, ts, ttl);
        addMainCallback(saveFuture, callback);
        addWsCallback(saveFuture, success -> onTimeSeriesUpdate(tenantId, entityId, ts));
    }

    @Override
    public void saveAndNotify(TenantId tenantId, EntityId entityId, String scope, List<AttributeKvEntry> attributes, FutureCallback<Void> callback) {
        ListenableFuture<List<Void>> saveFuture = attrService.save(tenantId, entityId, scope, attributes);
        addMainCallback(saveFuture, callback);
        addWsCallback(saveFuture, success -> onAttributesUpdate(tenantId, entityId, scope, attributes));
    }

    @Override
    public void saveAttrAndNotify(TenantId tenantId, EntityId entityId, String scope, String key, long value, FutureCallback<Void> callback) {
        saveAndNotify(tenantId, entityId, scope, Collections.singletonList(new BaseAttributeKvEntry(new LongDataEntry(key, value)
                , System.currentTimeMillis())), callback);
    }

    @Override
    public void saveAttrAndNotify(TenantId tenantId, EntityId entityId, String scope, String key, String value, FutureCallback<Void> callback) {
        saveAndNotify(tenantId, entityId, scope, Collections.singletonList(new BaseAttributeKvEntry(new StringDataEntry(key, value)
                , System.currentTimeMillis())), callback);
    }

    @Override
    public void saveAttrAndNotify(TenantId tenantId, EntityId entityId, String scope, String key, double value, FutureCallback<Void> callback) {
        saveAndNotify(tenantId, entityId, scope, Collections.singletonList(new BaseAttributeKvEntry(new DoubleDataEntry(key, value)
                , System.currentTimeMillis())), callback);
    }

    @Override
    public void saveAttrAndNotify(TenantId tenantId, EntityId entityId, String scope, String key, boolean value, FutureCallback<Void> callback) {
        saveAndNotify(tenantId, entityId, scope, Collections.singletonList(new BaseAttributeKvEntry(new BooleanDataEntry(key, value)
                , System.currentTimeMillis())), callback);
    }

    private void onAttributesUpdate(TenantId tenantId, EntityId entityId, String scope, List<AttributeKvEntry> attributes) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, entityId);
        if (currentPartitions.contains(tpi)) {
            if (subscriptionManagerService.isPresent()) {
                subscriptionManagerService.get().onAttributesUpdate(tenantId, entityId, scope, attributes, TbCallback.EMPTY);
            } else {
                log.warn("Possible misconfiguration because subscriptionManagerService is null!");
            }
        } else {
            TransportProtos.ToCoreMsg toCoreMsg = TbSubscriptionUtils.toAttributesUpdateProto(tenantId, entityId, scope, attributes);
            clusterService.pushMsgToCore(tpi, entityId.getId(), toCoreMsg, null);
        }
    }

    private void onTimeSeriesUpdate(TenantId tenantId, EntityId entityId, List<TsKvEntry> ts) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, entityId);
        if (currentPartitions.contains(tpi)) {
            if (subscriptionManagerService.isPresent()) {
                subscriptionManagerService.get().onTimeSeriesUpdate(tenantId, entityId, ts, TbCallback.EMPTY);
            } else {
                log.warn("Possible misconfiguration because subscriptionManagerService is null!");
            }
        } else {
            TransportProtos.ToCoreMsg toCoreMsg = TbSubscriptionUtils.toTimeseriesUpdateProto(tenantId, entityId, ts);
            clusterService.pushMsgToCore(tpi, entityId.getId(), toCoreMsg, null);
        }
    }

    private void addMainCallback(ListenableFuture<List<Void>> saveFuture, final FutureCallback<Void> callback) {
        Futures.addCallback(saveFuture, new FutureCallback<List<Void>>() {
            @Override
            public void onSuccess(@Nullable List<Void> result) {
                callback.onSuccess(null);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
        }, tsCallBackExecutor);
    }

    private void addWsCallback(ListenableFuture<List<Void>> saveFuture, Consumer<Void> callback) {
        Futures.addCallback(saveFuture, new FutureCallback<List<Void>>() {
            @Override
            public void onSuccess(@Nullable List<Void> result) {
                callback.accept(null);
            }

            @Override
            public void onFailure(Throwable t) {
            }
        }, wsCallBackExecutor);
    }
}
