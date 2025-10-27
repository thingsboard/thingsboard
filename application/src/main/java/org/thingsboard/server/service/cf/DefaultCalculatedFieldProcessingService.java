/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.TriConsumer;
import org.springframework.stereotype.Service;
import org.thingsboard.server.actors.calculatedField.CalculatedFieldTelemetryMsg;
import org.thingsboard.server.actors.calculatedField.MultipleTbCallback;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldLinkedTelemetryMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldLinkedTelemetryMsgProto.Builder;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldTelemetryMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToCalculatedFieldMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCalculatedFieldNotificationMsg;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.thingsboard.server.common.data.cf.configuration.PropagationCalculatedFieldConfiguration.PROPAGATION_CONFIG_ARGUMENT;
import static org.thingsboard.server.utils.CalculatedFieldUtils.toProto;

@TbRuleEngineComponent
@Service
@Slf4j
public class DefaultCalculatedFieldProcessingService extends AbstractCalculatedFieldProcessingService implements CalculatedFieldProcessingService {

    private final TbClusterService clusterService;
    private final PartitionService partitionService;

    public DefaultCalculatedFieldProcessingService(AttributesService attributesService,
                                                   TimeseriesService timeseriesService,
                                                   ApiLimitService apiLimitService,
                                                   RelationService relationService,
                                                   OwnerService ownerService,
                                                   TbClusterService clusterService,
                                                   TelemetrySubscriptionService tsSubService,
                                                   PartitionService partitionService) {
        super(attributesService, timeseriesService, tsSubService, apiLimitService, relationService, ownerService);
        this.clusterService = clusterService;
        this.partitionService = partitionService;
    }

    @Override
    protected String getExecutorNamePrefix() {
        return "calculated-field-callback";
    }

    @Override
    public ListenableFuture<Map<String, ArgumentEntry>> fetchArguments(CalculatedFieldCtx ctx, EntityId entityId) {
        return super.fetchArguments(ctx, entityId, System.currentTimeMillis());
    }

    @Override
    public Map<String, ArgumentEntry> fetchDynamicArgsFromDb(CalculatedFieldCtx ctx, EntityId entityId) {
        return switch (ctx.getCfType()) {
            case GEOFENCING -> resolveArgumentFutures(fetchGeofencingCalculatedFieldArguments(ctx, entityId, true, System.currentTimeMillis()));
            case PROPAGATION -> resolveArgumentFutures(Map.of(PROPAGATION_CONFIG_ARGUMENT, fetchPropagationCalculatedFieldArgument(ctx, entityId)));
            default -> Collections.emptyMap();
        };
    }

    @Override
    public Map<String, ArgumentEntry> fetchArgsFromDb(TenantId tenantId, EntityId entityId, Map<String, Argument> arguments) {
        Map<String, ListenableFuture<ArgumentEntry>> argFutures = new HashMap<>();
        for (var entry : arguments.entrySet()) {
            if (entry.getValue().hasRelationQuerySource()) {
                continue;
            }
            var argEntityId = resolveEntityId(tenantId, entityId, entry.getValue());
            var argValueFuture = fetchArgumentValue(tenantId, argEntityId, entry.getValue(), System.currentTimeMillis());
            argFutures.put(entry.getKey(), argValueFuture);
        }
        return resolveArgumentFutures(argFutures);
    }

    @Override
    public void processResult(TenantId tenantId, EntityId entityId, CalculatedFieldResult result, List<CalculatedFieldId> cfIds, TbCallback callback) {
        switch (result.getOutputStrategy().getType()) {
            case SKIP_RULE_ENGINE -> saveToDB(tenantId, entityId, result, cfIds, callback);
            case PUSH_TO_RULE_ENGINE -> pushMsgToRuleEngine(tenantId, entityId, result, cfIds, callback);
        }
    }

    @Override
    public void saveToDB(TenantId tenantId, EntityId entityId, CalculatedFieldResult result, List<CalculatedFieldId> cfIds, TbCallback callback) {
        if (result instanceof TelemetryCalculatedFieldResult telemetryResult) {
            saveTelemetryResult(tenantId, entityId, telemetryResult, cfIds, callback);
            return;
        }
        if (result instanceof PropagationCalculatedFieldResult propagationResult) {
            handlePropagationResults(propagationResult, callback,
                    (entity, res, cb) -> saveTelemetryResult(tenantId, entityId, res, cfIds, cb));
        }
    }

    @Override
    public void pushMsgToRuleEngine(TenantId tenantId, EntityId entityId, CalculatedFieldResult result, List<CalculatedFieldId> cfIds, TbCallback callback) {
        if (result instanceof PropagationCalculatedFieldResult propagationResult) {
            handlePropagationResults(propagationResult, callback,
                    (entity, res, cb) -> sendMsgToRuleEngine(tenantId, entityId, cb, res.toTbMsg(entity, cfIds)));
            return;
        }

        sendMsgToRuleEngine(tenantId, entityId, callback, result.toTbMsg(entityId, cfIds));
    }

    private void handlePropagationResults(PropagationCalculatedFieldResult propagationResult, TbCallback callback,
                                          TriConsumer<EntityId, TelemetryCalculatedFieldResult, TbCallback> telemetryResultHandler) {
        List<EntityId> propagationEntityIds = propagationResult.getPropagationEntityIds();
        if (propagationEntityIds.isEmpty()) {
            callback.onSuccess();
        }
        if (propagationEntityIds.size() == 1) {
            EntityId propagationEntityId = propagationEntityIds.get(0);
            telemetryResultHandler.accept(propagationEntityId, propagationResult.getResult(), callback);
            return;
        }
        MultipleTbCallback multipleTbCallback = new MultipleTbCallback(propagationEntityIds.size(), callback);
        for (var propagationEntityId : propagationEntityIds) {
            telemetryResultHandler.accept(propagationEntityId, propagationResult.getResult(), multipleTbCallback);
        }
    }

    private void sendMsgToRuleEngine(TenantId tenantId, EntityId entityId, TbCallback callback, TbMsg msg) {
        try {
            clusterService.pushMsgToRuleEngine(tenantId, entityId, msg, new TbQueueCallback() {
                @Override
                public void onSuccess(TbQueueMsgMetadata metadata) {
                    log.trace("[{}][{}] Pushed message to rule engine: {} ", tenantId, entityId, msg);
                    callback.onSuccess();
                }

                @Override
                public void onFailure(Throwable t) {
                    callback.onFailure(t);
                }
            });
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push message to rule engine: {}", tenantId, entityId, msg, e);
            callback.onFailure(e);
        }
    }

    @Override
    public void pushMsgToLinks(CalculatedFieldTelemetryMsg msg, List<CalculatedFieldEntityCtxId> linkedCalculatedFields, TbCallback callback) {
        Map<TopicPartitionInfo, List<CalculatedFieldEntityCtxId>> unicasts = new HashMap<>();
        List<CalculatedFieldEntityCtxId> broadcasts = new ArrayList<>();
        for (CalculatedFieldEntityCtxId link : linkedCalculatedFields) {
            var linkEntityId = link.entityId();
            var linkEntityType = linkEntityId.getEntityType();
            // Let's assume number of entities in profile is N, and number of partitions is P. If N > P, we save by broadcasting to all partitions. Usually N >> P.
            boolean broadcast = EntityType.DEVICE_PROFILE.equals(linkEntityType) || EntityType.ASSET_PROFILE.equals(linkEntityType);
            if (broadcast) {
                broadcasts.add(link);
            } else {
                TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, DataConstants.CF_QUEUE_NAME, link.tenantId(), link.entityId());
                unicasts.computeIfAbsent(tpi, k -> new ArrayList<>()).add(link);
            }
        }
        MultipleTbCallback linkCallback = new MultipleTbCallback(2, callback);
        if (!broadcasts.isEmpty()) {
            broadcast(broadcasts, msg, linkCallback);
        } else {
            linkCallback.onSuccess();
        }
        if (!unicasts.isEmpty()) {
            unicast(unicasts, msg, linkCallback);
        } else {
            linkCallback.onSuccess();
        }
    }

    private void unicast(Map<TopicPartitionInfo, List<CalculatedFieldEntityCtxId>> unicasts, CalculatedFieldTelemetryMsg msg, MultipleTbCallback mainCallback) {
        TbQueueCallback callback = new TbCallbackWrapper(new MultipleTbCallback(unicasts.size(), mainCallback));
        unicasts.forEach((topicPartitionInfo, ctxIds) -> {
            CalculatedFieldLinkedTelemetryMsgProto linkedTelemetryMsgProto = buildLinkedTelemetryMsgProto(msg.getProto(), ctxIds);
            clusterService.pushMsgToCalculatedFields(topicPartitionInfo, UUID.randomUUID(),
                    ToCalculatedFieldMsg.newBuilder().setLinkedTelemetryMsg(linkedTelemetryMsgProto).build(), callback);
        });
    }

    private void broadcast(List<CalculatedFieldEntityCtxId> broadcasts, CalculatedFieldTelemetryMsg msg, MultipleTbCallback mainCallback) {
        TbQueueCallback callback = new TbCallbackWrapper(mainCallback);
        CalculatedFieldLinkedTelemetryMsgProto linkedTelemetryMsgProto = buildLinkedTelemetryMsgProto(msg.getProto(), broadcasts);
        clusterService.broadcastToCalculatedFields(ToCalculatedFieldNotificationMsg.newBuilder().setLinkedTelemetryMsg(linkedTelemetryMsgProto).build(), callback);
    }

    private CalculatedFieldLinkedTelemetryMsgProto buildLinkedTelemetryMsgProto(CalculatedFieldTelemetryMsgProto telemetryProto, List<CalculatedFieldEntityCtxId> links) {
        Builder builder = CalculatedFieldLinkedTelemetryMsgProto.newBuilder();
        builder.setMsg(telemetryProto);
        for (CalculatedFieldEntityCtxId link : links) {
            builder.addLinks(toProto(link));
        }
        return builder.build();
    }

    private static class TbCallbackWrapper implements TbQueueCallback {
        private final TbCallback callback;

        public TbCallbackWrapper(TbCallback callback) {
            this.callback = callback;
        }

        @Override
        public void onSuccess(TbQueueMsgMetadata metadata) {
            callback.onSuccess();
        }

        @Override
        public void onFailure(Throwable t) {
            callback.onFailure(t);
        }

    }

}
