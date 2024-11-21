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
package org.thingsboard.server.service.cf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldCtx;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldCtxId;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.ScriptCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.SimpleCalculatedFieldState;
import org.thingsboard.server.service.partition.AbstractPartitionBasedService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.thingsboard.server.common.data.DataConstants.SCOPE;

@TbCoreComponent
@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultCalculatedFieldExecutionService extends AbstractPartitionBasedService<CalculatedFieldId> implements CalculatedFieldExecutionService {

    private final CalculatedFieldService calculatedFieldService;
    private final AssetService assetService;
    private final DeviceService deviceService;
    private final AttributesService attributesService;
    private final TimeseriesService timeseriesService;
    private final RocksDBService rocksDBService;
    private final TbClusterService clusterService;
    private final TbelInvokeService tbelInvokeService;

    private ListeningExecutorService calculatedFieldExecutor;
    private ListeningExecutorService calculatedFieldCallbackExecutor;

    private final ConcurrentMap<CalculatedFieldId, CalculatedField> calculatedFields = new ConcurrentHashMap<>();
    private final ConcurrentMap<CalculatedFieldId, List<CalculatedFieldLink>> calculatedFieldLinks = new ConcurrentHashMap<>();
    private final ConcurrentMap<CalculatedFieldCtxId, CalculatedFieldCtx> states = new ConcurrentHashMap<>();

    @Value("${calculatedField.initFetchPackSize:50000}")
    @Getter
    private int initFetchPackSize;

    @PostConstruct
    public void init() {
        super.init();
        calculatedFieldExecutor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(
                Math.max(4, Runtime.getRuntime().availableProcessors()), "calculated-field"));
        calculatedFieldCallbackExecutor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(
                Math.max(4, Runtime.getRuntime().availableProcessors()), "calculated-field-callback"));
        scheduledExecutor.submit(this::fetchCalculatedFields);
    }

    @PreDestroy
    public void stop() {
        if (calculatedFieldExecutor != null) {
            calculatedFieldExecutor.shutdownNow();
        }
        if (calculatedFieldCallbackExecutor != null) {
            calculatedFieldCallbackExecutor.shutdownNow();
        }
    }

    @Override
    protected String getServiceName() {
        return "Calculated Field Execution";
    }

    @Override
    protected String getSchedulerExecutorName() {
        return "calculated-field-scheduled";
    }

    @Override
    protected Map<TopicPartitionInfo, List<ListenableFuture<?>>> onAddedPartitions(Set<TopicPartitionInfo> addedPartitions) {
        // TODO: implementation for cluster mode
        return Map.of();
    }

    @Override
    protected void cleanupEntityOnPartitionRemoval(CalculatedFieldId entityId) {
        // TODO: implementation for cluster mode
    }

    @Override
    public void onCalculatedFieldMsg(TransportProtos.CalculatedFieldMsgProto proto, TbCallback callback) {
        try {
            TenantId tenantId = TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB()));
            CalculatedFieldId calculatedFieldId = new CalculatedFieldId(new UUID(proto.getCalculatedFieldIdMSB(), proto.getCalculatedFieldIdLSB()));
            log.info("Received CalculatedFieldMsgProto for processing: tenantId=[{}], calculatedFieldId=[{}]", tenantId, calculatedFieldId);
            if (proto.getDeleted()) {
                log.warn("Executing onCalculatedFieldDelete, calculatedFieldId=[{}]", calculatedFieldId);
                onCalculatedFieldDelete(calculatedFieldId, callback);
                callback.onSuccess();
            }
            CalculatedField cf = calculatedFieldService.findById(tenantId, calculatedFieldId);
            if (proto.getUpdated()) {
                log.info("Executing onCalculatedFieldUpdate, calculatedFieldId=[{}]", calculatedFieldId);
                boolean shouldReinit = onCalculatedFieldUpdate(cf, callback);
                if (!shouldReinit) {
                    return;
                }
            }
            List<CalculatedFieldLink> links = calculatedFieldService.findAllCalculatedFieldLinksById(tenantId, calculatedFieldId);
            if (cf != null) {
                EntityId entityId = cf.getEntityId();
                calculatedFields.put(calculatedFieldId, cf);
                calculatedFieldLinks.put(calculatedFieldId, links);
                switch (entityId.getEntityType()) {
                    case ASSET, DEVICE -> {
                        log.info("Initializing state for entity: tenantId=[{}], entityId=[{}]", tenantId, entityId);
                        initializeStateForEntity(tenantId, cf, entityId, callback);
                    }
                    case ASSET_PROFILE -> {
                        log.info("Initializing state for all assets in profile: tenantId=[{}], assetProfileId=[{}]", tenantId, entityId);
                        PageDataIterable<AssetId> assetIds = new PageDataIterable<>(pageLink ->
                                assetService.findAssetIdsByTenantIdAndAssetProfileId(tenantId, (AssetProfileId) entityId, pageLink), initFetchPackSize);
                        assetIds.forEach(assetId -> initializeStateForEntity(tenantId, cf, assetId, callback));
                    }
                    case DEVICE_PROFILE -> {
                        log.info("Initializing state for all devices in profile: tenantId=[{}], deviceProfileId=[{}]", tenantId, entityId);
                        PageDataIterable<DeviceId> deviceIds = new PageDataIterable<>(pageLink ->
                                deviceService.findDeviceIdsByTenantIdAndDeviceProfileId(tenantId, (DeviceProfileId) entityId, pageLink), initFetchPackSize);
                        deviceIds.forEach(deviceId -> initializeStateForEntity(tenantId, cf, deviceId, callback));
                    }
                    default ->
                            throw new IllegalArgumentException("Entity type '" + calculatedFieldId.getEntityType() + "' does not support calculated fields.");
                }
            } else {
                //Calculated field was probably deleted while message was in queue;
                log.warn("Calculated field not found, possibly deleted: {}", calculatedFieldId);
                callback.onSuccess();
            }
            callback.onSuccess();
            log.info("Successfully processed calculated field message for calculatedFieldId: [{}]", calculatedFieldId);
        } catch (Exception e) {
            log.trace("Failed to process calculated field msg: [{}]", proto, e);
            callback.onFailure(e);
        }
    }

    @Override
    public void onTelemetryUpdate(TenantId tenantId, CalculatedFieldId calculatedFieldId, Map<String, KvEntry> updatedTelemetry) {
        try {
            CalculatedField calculatedField = calculatedFields.computeIfAbsent(calculatedFieldId, id -> calculatedFieldService.findById(tenantId, id));
            updateOrInitializeState(calculatedField, calculatedField.getEntityId(), updatedTelemetry);
            log.info("Successfully updated time series for calculatedFieldId: [{}]", calculatedFieldId);
        } catch (Exception e) {
            log.trace("Failed to update time series for calculatedFieldId: [{}]", calculatedFieldId, e);
        }
    }

    private boolean onCalculatedFieldUpdate(CalculatedField newCalculatedField, TbCallback callback) {
        CalculatedField oldCalculatedField = calculatedFields.get(newCalculatedField.getId());
        boolean shouldReinit = true;
        if (hasSignificantChanges(oldCalculatedField, newCalculatedField)) {
            onCalculatedFieldDelete(newCalculatedField.getId(), callback);
        } else {
            calculatedFields.put(newCalculatedField.getId(), newCalculatedField);
            callback.onSuccess();
            shouldReinit = false;
        }
        return shouldReinit;
    }

    private void onCalculatedFieldDelete(CalculatedFieldId calculatedFieldId, TbCallback callback) {
        try {
            calculatedFieldLinks.remove(calculatedFieldId);
            calculatedFields.remove(calculatedFieldId);
            states.keySet().removeIf(ctxId -> calculatedFields.keySet().stream().noneMatch(id -> ctxId.cfId().equals(id.getId())));
            List<String> statesToRemove = states.keySet().stream()
                    .filter(ctxId -> !calculatedFields.containsKey(new CalculatedFieldId(ctxId.cfId())))
                    .map(JacksonUtil::writeValueAsString)
                    .toList();
            rocksDBService.deleteAll(statesToRemove);
        } catch (Exception e) {
            log.trace("Failed to delete calculated field: [{}]", calculatedFieldId, e);
            callback.onFailure(e);
        }
    }

    private boolean hasSignificantChanges(CalculatedField oldCalculatedField, CalculatedField newCalculatedField) {
        if (oldCalculatedField == null) {
            return true;
        }
        boolean entityIdChanged = !oldCalculatedField.getEntityId().equals(newCalculatedField.getEntityId());
        boolean typeChanged = !oldCalculatedField.getType().equals(newCalculatedField.getType());
        CalculatedFieldConfiguration oldConfig = oldCalculatedField.getConfiguration();
        CalculatedFieldConfiguration newConfig = newCalculatedField.getConfiguration();
        boolean argumentsChanged = !oldConfig.getArguments().equals(newConfig.getArguments());
        boolean outputTypeChanged = !oldConfig.getOutput().getType().equals(newConfig.getOutput().getType());
        boolean expressionChanged = !oldConfig.getExpression().equals(newConfig.getExpression());

        return entityIdChanged || typeChanged || argumentsChanged || outputTypeChanged || expressionChanged;
    }

    private void fetchCalculatedFields() {
        PageDataIterable<CalculatedField> cfs = new PageDataIterable<>(calculatedFieldService::findAllCalculatedFields, initFetchPackSize);
        cfs.forEach(cf -> calculatedFields.putIfAbsent(cf.getId(), cf));
        PageDataIterable<CalculatedFieldLink> cfls = new PageDataIterable<>(calculatedFieldService::findAllCalculatedFieldLinks, initFetchPackSize);
        cfls.forEach(link -> calculatedFieldLinks.computeIfAbsent(link.getCalculatedFieldId(), id -> new ArrayList<>()).add(link));
        rocksDBService.getAll().forEach((ctxId, ctx) -> states.put(JacksonUtil.fromString(ctxId, CalculatedFieldCtxId.class), JacksonUtil.fromString(ctx, CalculatedFieldCtx.class)));
        states.keySet().removeIf(ctxId -> calculatedFields.keySet().stream().noneMatch(id -> ctxId.cfId().equals(id.getId())));
    }

    private void initializeStateForEntity(TenantId tenantId, CalculatedField calculatedField, EntityId entityId, TbCallback callback) {
        Map<String, Argument> arguments = calculatedField.getConfiguration().getArguments();
        Map<String, KvEntry> argumentValues = new HashMap<>();
        AtomicInteger remaining = new AtomicInteger(arguments.size());
        arguments.forEach((key, argument) -> Futures.addCallback(fetchArgumentValue(tenantId, argument), new FutureCallback<>() {
            @Override
            public void onSuccess(Optional<? extends KvEntry> result) {
                // todo: should be rewritten implementation for default value
                argumentValues.put(key, result.orElse(null));
                if (remaining.decrementAndGet() == 0) {
                    updateOrInitializeState(calculatedField, entityId, argumentValues);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("Failed to initialize state for entity: [{}]", entityId, t);
                callback.onFailure(t);
            }
        }, calculatedFieldCallbackExecutor));
    }

    private ListenableFuture<Optional<? extends KvEntry>> fetchArgumentValue(TenantId tenantId, Argument argument) {
        return switch (argument.getType()) {
            case "ATTRIBUTES" -> Futures.transform(
                    attributesService.find(tenantId, argument.getEntityId(), argument.getScope(), argument.getKey()),
                    result -> result.map(entry -> (KvEntry) entry),
                    MoreExecutors.directExecutor());
            case "TIME_SERIES" -> Futures.transform(
                    timeseriesService.findLatest(tenantId, argument.getEntityId(), argument.getKey()),
                    result -> result.map(entry -> (KvEntry) entry),
                    MoreExecutors.directExecutor());
            default -> throw new IllegalArgumentException("Invalid argument type '" + argument.getType() + "'.");
        };
    }

    private void updateOrInitializeState(CalculatedField calculatedField, EntityId entityId, Map<String, KvEntry> argumentValues) {
        CalculatedFieldCtxId ctxId = new CalculatedFieldCtxId(calculatedField.getUuidId(), entityId.getId());
        CalculatedFieldCtx calculatedFieldCtx = states.computeIfAbsent(ctxId, ctx -> new CalculatedFieldCtx(ctxId, null));

        CalculatedFieldState state = calculatedFieldCtx.getState();

        if (state == null) {
            state = createStateByType(calculatedField.getType());
        }
        state.initState(argumentValues);
        calculatedFieldCtx.setState(state);
        states.put(ctxId, calculatedFieldCtx);
        rocksDBService.put(JacksonUtil.writeValueAsString(ctxId), JacksonUtil.writeValueAsString(calculatedFieldCtx));

        ListenableFuture<CalculatedFieldResult> resultFuture = state.performCalculation(calculatedField.getTenantId(), calculatedField.getConfiguration(), tbelInvokeService);
        Futures.addCallback(resultFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(CalculatedFieldResult result) {
                if (result != null) {
                    pushMsgToRuleEngine(calculatedField.getTenantId(), calculatedField.getEntityId(), result);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("[{}] Failed to perform calculation. entityId: [{}]", calculatedField.getId(), entityId, t);
            }
        }, MoreExecutors.directExecutor());

    }

    private void pushMsgToRuleEngine(TenantId tenantId, EntityId originatorId, CalculatedFieldResult calculatedFieldResult) {
        try {
            String type = calculatedFieldResult.getType();
            TbMsgType msgType = "ATTRIBUTES".equals(type) ? TbMsgType.POST_ATTRIBUTES_REQUEST : TbMsgType.POST_TELEMETRY_REQUEST;
            TbMsgMetaData md = "ATTRIBUTES".equals(type) ? new TbMsgMetaData(Map.of(SCOPE, calculatedFieldResult.getScope().name())) : TbMsgMetaData.EMPTY;
            ObjectNode payload = createJsonPayload(calculatedFieldResult);
            TbMsg msg = TbMsg.newMsg(msgType, originatorId, md, JacksonUtil.writeValueAsString(payload));
            clusterService.pushMsgToRuleEngine(tenantId, originatorId, msg, null);
        } catch (Exception e) {
            log.warn("[{}] Failed to push message to rule engine. CalculatedFieldResult: {}", originatorId, calculatedFieldResult, e);
        }
    }

    private ObjectNode createJsonPayload(CalculatedFieldResult calculatedFieldResult) {
        ObjectNode payload = JacksonUtil.newObjectNode();
        Map<String, Object> resultMap = calculatedFieldResult.getResultMap();
        resultMap.forEach((k, v) -> payload.set(k, JacksonUtil.convertValue(v, JsonNode.class)));
        return payload;
    }

    private CalculatedFieldState createStateByType(CalculatedFieldType calculatedFieldType) {
        return switch (calculatedFieldType) {
            case SIMPLE -> new SimpleCalculatedFieldState();
            case SCRIPT -> new ScriptCalculatedFieldState();
        };
    }

}
