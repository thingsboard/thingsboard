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
package org.thingsboard.server.actors.calculatedField;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.DebugModeUtil;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.calculatedField.EntityInitCalculatedFieldMsg.StateAction;
import org.thingsboard.server.actors.shared.AbstractContextAwareMsgProcessor;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.CalculatedFieldStatePartitionRestoreMsg;
import org.thingsboard.server.common.msg.cf.CalculatedFieldPartitionChangeMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.gen.transport.TransportProtos.AttributeScopeProto;
import org.thingsboard.server.gen.transport.TransportProtos.AttributeValueProto;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldTelemetryMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.TsKvProto;
import org.thingsboard.server.service.cf.CalculatedFieldProcessingService;
import org.thingsboard.server.service.cf.CalculatedFieldResult;
import org.thingsboard.server.service.cf.CalculatedFieldStateService;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.SingleValueArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.aggregation.RelatedEntitiesAggregationCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.alarm.AlarmCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.geofencing.GeofencingArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.geofencing.GeofencingCalculatedFieldState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.thingsboard.server.utils.CalculatedFieldArgumentUtils.createStateByType;

/**
 * @author Andrew Shvayka
 */
@Slf4j
public class CalculatedFieldEntityMessageProcessor extends AbstractContextAwareMsgProcessor {
    // (1 for result persistence + 1 for the state persistence)
    public static final int CALLBACKS_PER_CF = 2;

    final TenantId tenantId;
    final EntityId entityId;
    final CalculatedFieldProcessingService cfService;
    final CalculatedFieldStateService cfStateService;

    TbActorCtx actorCtx;
    Map<CalculatedFieldId, CalculatedFieldState> states = new HashMap<>();

    CalculatedFieldEntityMessageProcessor(ActorSystemContext systemContext, TenantId tenantId, EntityId entityId) {
        super(systemContext);
        this.tenantId = tenantId;
        this.entityId = entityId;
        this.cfService = systemContext.getCalculatedFieldProcessingService();
        this.cfStateService = systemContext.getCalculatedFieldStateService();
    }

    void init(TbActorCtx ctx) {
        this.actorCtx = ctx;
    }

    public void stop(boolean partitionChanged) {
        log.info(partitionChanged ?
                        "[{}][{}] Stopping entity actor due to change partition event." :
                        "[{}][{}] Stopping entity actor.",
                tenantId, entityId);
        states.values().forEach(this::closeState);
        states.clear();
        actorCtx.stop(actorCtx.getSelf());
    }

    public void process(CalculatedFieldPartitionChangeMsg msg) {
        if (!systemContext.getPartitionService().resolve(ServiceType.TB_RULE_ENGINE, DataConstants.CF_QUEUE_NAME, tenantId, entityId).isMyPartition()) {
            stop(true);
        }
    }

    public void process(CalculatedFieldStateRestoreMsg msg) {
        CalculatedFieldId cfId = msg.getId().cfId();
        log.debug("[{}] [{}] Processing CF state restore msg.", msg.getId().entityId(), cfId);
        CalculatedFieldState state = msg.getState();
        if (state != null) {
            state.setCtx(msg.getCtx(), actorCtx);
            state.setPartition(msg.getPartition());
            state.init(true);
            states.put(cfId, state);
        } else {
            removeState(cfId);
        }
    }

    public void process(CalculatedFieldStatePartitionRestoreMsg msg) {
        log.debug("Processing CF state partition restore msg: {}", msg);
        for (CalculatedFieldState state : states.values()) {
            if (msg.getPartition().equals(state.getPartition())) {
                state.init(false);
            }
        }
    }

    public void process(EntityInitCalculatedFieldMsg msg) throws CalculatedFieldException {
        log.debug("[{}] Processing entity init CF msg: {}", msg.getCtx().getCfId(), msg);
        var ctx = msg.getCtx();
        CalculatedFieldState state;
        if (msg.getStateAction() == StateAction.RECREATE) {
            removeState(ctx.getCfId());
            state = null;
        } else {
            state = states.get(ctx.getCfId());
        }
        try {
            if (state == null) {
                state = createState(ctx);
            } else if (msg.getStateAction() == StateAction.REINIT) {
                log.debug("Force reinitialization of CF: [{}].", ctx.getCfId());
                state.reset();
                initState(state, ctx);
            } else {
                state.setCtx(ctx, actorCtx);
            }
            if (state.isSizeOk()) {
                processStateIfReady(state, Collections.emptyMap(), ctx, Collections.singletonList(ctx.getCfId()), null, null, msg.getCallback());
            } else {
                throw new RuntimeException(ctx.getSizeExceedsLimitMessage());
            }
        } catch (Exception e) {
            log.debug("[{}][{}] Failed to initialize CF state", entityId, ctx.getCfId(), e);
            if (e instanceof CalculatedFieldException cfe) {
                throw cfe;
            }
            throw CalculatedFieldException.builder().ctx(ctx).eventEntity(entityId).cause(e).build();
        }
    }

    public void process(CalculatedFieldArgumentResetMsg msg) throws CalculatedFieldException {
        log.debug("[{}] Processing CF argument reset msg.", entityId);
        var ctx = msg.getCtx();
        try {
            Map<String, Argument> dynamicSourceArgs = ctx.getArguments().entrySet().stream()
                    .filter(entry -> entry.getValue().hasOwnerSource())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            Map<String, ArgumentEntry> fetchedArgs = cfService.fetchArgsFromDb(tenantId, entityId, dynamicSourceArgs);
            fetchedArgs.values().forEach(arg -> arg.setForceResetPrevious(true));

            processArgumentValuesUpdate(ctx, Collections.singletonList(ctx.getCfId()), msg.getCallback(), fetchedArgs, null, null);
        } catch (Exception e) {
            throw CalculatedFieldException.builder().ctx(ctx).eventEntity(entityId).cause(e).build();
        }
    }

    public void process(CalculatedFieldEntityDeleteMsg msg) throws CalculatedFieldException {
        log.debug("[{}] Processing CF entity delete msg.", msg.getEntityId());
        if (this.entityId.equals(msg.getEntityId())) {
            if (states.isEmpty()) {
                msg.getCallback().onSuccess();
            } else {
                MultipleTbCallback multipleTbCallback = new MultipleTbCallback(states.size(), msg.getCallback());
                states.forEach((cfId, state) -> cfStateService.deleteState(new CalculatedFieldEntityCtxId(tenantId, cfId, entityId), multipleTbCallback));
                actorCtx.stop(actorCtx.getSelf());
            }
        } else {
            var cfId = new CalculatedFieldId(msg.getEntityId().getId());
            var state = removeState(cfId);
            if (state != null) {
                cfStateService.deleteState(new CalculatedFieldEntityCtxId(tenantId, cfId, entityId), msg.getCallback());
            } else {
                msg.getCallback().onSuccess();
            }
        }
    }

    public void process(CalculatedFieldRelationActionMsg msg) throws CalculatedFieldException {
        log.debug("[{}] Processing CF {} related entity msg.", msg.getRelatedEntityId(), msg.getAction());
        switch (msg.getAction()) {
            case UPDATED -> handleRelationUpdate(msg);
            case DELETED -> handleRelationDelete(msg);
            default -> msg.getCallback().onSuccess();
        }
    }

    private void handleRelationUpdate(CalculatedFieldRelationActionMsg msg) throws CalculatedFieldException {
        CalculatedFieldCtx ctx = msg.getCalculatedField();
        var callback = new MultipleTbCallback(CALLBACKS_PER_CF, msg.getCallback());
        var state = states.get(ctx.getCfId());
        try {
            Map<String, ArgumentEntry> updatedArgs = new HashMap<>();
            if (state == null) {
                state = createState(ctx);
            } else {
                if (state instanceof RelatedEntitiesAggregationCalculatedFieldState relatedEntitiesAggState) {
                    Map<String, ArgumentEntry> fetchedArgs = cfService.fetchArgsFromDb(tenantId, msg.getRelatedEntityId(), ctx.getArguments());
                    updatedArgs = relatedEntitiesAggState.updateEntityData(setEntityIdToSingleEntityArguments(msg.getRelatedEntityId(), fetchedArgs));
                }

                state.checkStateSize(new CalculatedFieldEntityCtxId(tenantId, ctx.getCfId(), entityId), ctx.getMaxStateSize());
            }
            if (state.isSizeOk()) {
                processStateIfReady(state, updatedArgs, ctx, Collections.singletonList(ctx.getCfId()), null, null, callback);
            } else {
                throw CalculatedFieldException.builder().ctx(ctx).eventEntity(entityId).errorMessage(ctx.getSizeExceedsLimitMessage()).build();
            }
        } catch (Exception e) {
            log.debug("[{}][{}] Failed to initialize CF state", entityId, ctx.getCfId(), e);
            if (e instanceof CalculatedFieldException cfe) {
                throw cfe;
            }
            throw CalculatedFieldException.builder().ctx(ctx).eventEntity(entityId).cause(e).build();
        }
    }

    private void handleRelationDelete(CalculatedFieldRelationActionMsg msg) throws CalculatedFieldException {
        CalculatedFieldCtx ctx = msg.getCalculatedField();
        CalculatedFieldId cfId = ctx.getCfId();
        CalculatedFieldState state = states.get(cfId);
        if (state == null) {
            msg.getCallback().onSuccess();
            return;
        }
        if (state instanceof RelatedEntitiesAggregationCalculatedFieldState aggState) {
            aggState.cleanupEntityData(msg.getRelatedEntityId());

            state.checkStateSize(new CalculatedFieldEntityCtxId(tenantId, ctx.getCfId(), entityId), ctx.getMaxStateSize());

            if (state.isSizeOk()) {
                processStateIfReady(state, Collections.emptyMap(), ctx, Collections.singletonList(ctx.getCfId()), null, null, msg.getCallback());
            } else {
                throw new RuntimeException(ctx.getSizeExceedsLimitMessage());
            }
        } else {
            msg.getCallback().onSuccess();
        }
    }

    public void process(EntityCalculatedFieldTelemetryMsg msg) throws CalculatedFieldException {
        log.trace("[{}] Processing CF telemetry msg: {}", msg.getEntityId(), msg);
        var proto = msg.getProto();
        var numberOfCallbacks = msg.getEntityIdFields().size() + msg.getProfileIdFields().size();
        MultipleTbCallback callback = new MultipleTbCallback(numberOfCallbacks, msg.getCallback());
        List<CalculatedFieldId> cfIdList = getCalculatedFieldIds(proto);
        Set<CalculatedFieldId> cfIdSet = new HashSet<>(cfIdList);
        for (var ctx : msg.getEntityIdFields()) {
            process(ctx, proto, cfIdSet, cfIdList, callback);
        }
        for (var ctx : msg.getProfileIdFields()) {
            process(ctx, proto, cfIdSet, cfIdList, callback);
        }
    }

    public void process(EntityCalculatedFieldLinkedTelemetryMsg msg) throws CalculatedFieldException {
        log.trace("[{}] Processing CF link telemetry msg: {}", msg.getEntityId(), msg);
        var proto = msg.getProto();
        var ctx = msg.getCtx();
        var callback = msg.getCallback();
        try {
            List<CalculatedFieldId> cfIds = getCalculatedFieldIds(proto);
            if (cfIds.contains(ctx.getCfId())) {
                callback.onSuccess();
            } else {
                if (proto.getTsDataCount() > 0) {
                    processArgumentValuesUpdate(ctx, cfIds, callback, mapToArguments(ctx, msg.getEntityId(), proto.getTsDataList()), toTbMsgId(proto), toTbMsgType(proto));
                } else if (proto.getAttrDataCount() > 0) {
                    processArgumentValuesUpdate(ctx, cfIds, callback, mapToArguments(ctx, msg.getEntityId(), proto.getScope(), proto.getAttrDataList()), toTbMsgId(proto), toTbMsgType(proto));
                } else if (proto.getRemovedTsKeysCount() > 0) {
                    processArgumentValuesUpdate(ctx, cfIds, callback, mapToArgumentsWithFetchedValue(ctx, msg.getEntityId(), proto.getRemovedTsKeysList()), toTbMsgId(proto), toTbMsgType(proto));
                } else if (proto.getRemovedAttrKeysCount() > 0) {
                    processArgumentValuesUpdate(ctx, cfIds, callback, mapToArgumentsWithDefaultValue(ctx, msg.getEntityId(), proto.getScope(), proto.getRemovedAttrKeysList()), toTbMsgId(proto), toTbMsgType(proto));
                } else {
                    callback.onSuccess();
                }
            }
        } catch (Exception e) {
            log.debug("[{}][{}] Failed to process linked CF telemetry msg: {}", entityId, ctx.getCfId(), msg, e);
            throw CalculatedFieldException.builder().ctx(ctx).eventEntity(entityId).cause(e).build();
        }
    }

    private void process(CalculatedFieldCtx ctx, CalculatedFieldTelemetryMsgProto proto, Collection<CalculatedFieldId> cfIds, List<CalculatedFieldId> cfIdList, TbCallback callback) throws CalculatedFieldException {
        try {
            if (cfIds.contains(ctx.getCfId())) {
                callback.onSuccess();
            } else {
                if (proto.getTsDataCount() > 0) {
                    processTelemetry(ctx, proto, cfIdList, callback);
                } else if (proto.getAttrDataCount() > 0) {
                    processAttributes(ctx, proto, cfIdList, callback);
                } else if (proto.getRemovedTsKeysCount() > 0) {
                    processRemovedTelemetry(ctx, proto, cfIdList, callback);
                } else if (proto.getRemovedAttrKeysCount() > 0) {
                    processRemovedAttributes(ctx, proto, cfIdList, callback);
                } else {
                    callback.onSuccess();
                }
            }
        } catch (Exception e) {
            log.debug("[{}][{}] Failed to process CF telemetry msg: {}", entityId, ctx.getCfId(), proto, e);
            if (e instanceof CalculatedFieldException cfe) {
                throw cfe;
            }
            throw CalculatedFieldException.builder().ctx(ctx).eventEntity(entityId).cause(e).build();
        }
    }

    public void process(CalculatedFieldReevaluateMsg msg) throws CalculatedFieldException {
        CalculatedFieldId cfId = msg.getCtx().getCfId();
        CalculatedFieldState state = states.get(cfId);
        if (state == null) {
            log.debug("[{}][{}] Failed to find CF state for entity to handle {}", entityId, cfId, msg);
        } else {
            if (state.isSizeOk()) {
                log.debug("[{}][{}] Reevaluating CF state", entityId, cfId);
                processStateIfReady(state, null, msg.getCtx(), Collections.singletonList(cfId), null, null, msg.getCallback());
            } else {
                throw new RuntimeException(msg.getCtx().getSizeExceedsLimitMessage());
            }
        }
    }

    public void process(CalculatedFieldAlarmActionMsg msg) {
        log.debug("[{}] Processing alarm action event msg: {}", entityId, msg);
        for (CalculatedFieldState state : states.values()) {
            if (state instanceof AlarmCalculatedFieldState alarmCfState) {
                Alarm stateAlarm = alarmCfState.getCurrentAlarm();
                if (stateAlarm != null && stateAlarm.getId().equals(msg.getAlarm().getId())) {
                    alarmCfState.processAlarmAction(msg.getAlarm(), msg.getAction());
                }
            }
        }
        msg.getCallback().onSuccess();
    }

    private void processTelemetry(CalculatedFieldCtx ctx, CalculatedFieldTelemetryMsgProto proto, List<CalculatedFieldId> cfIdList, TbCallback callback) throws CalculatedFieldException {
        processArgumentValuesUpdate(ctx, cfIdList, callback, mapToArguments(ctx, proto.getTsDataList()), toTbMsgId(proto), toTbMsgType(proto));
    }

    private void processAttributes(CalculatedFieldCtx ctx, CalculatedFieldTelemetryMsgProto proto, List<CalculatedFieldId> cfIdList, TbCallback callback) throws CalculatedFieldException {
        processArgumentValuesUpdate(ctx, cfIdList, callback, mapToArguments(ctx, proto.getScope(), proto.getAttrDataList()), toTbMsgId(proto), toTbMsgType(proto));
    }

    private void processRemovedTelemetry(CalculatedFieldCtx ctx, CalculatedFieldTelemetryMsgProto proto, List<CalculatedFieldId> cfIdList, TbCallback callback) throws CalculatedFieldException {
        processArgumentValuesUpdate(ctx, cfIdList, callback, mapToArgumentsWithFetchedValue(ctx, entityId, proto.getRemovedTsKeysList()), toTbMsgId(proto), toTbMsgType(proto));
    }

    private void processRemovedAttributes(CalculatedFieldCtx ctx, CalculatedFieldTelemetryMsgProto proto, List<CalculatedFieldId> cfIdList, TbCallback callback) throws CalculatedFieldException {
        processArgumentValuesUpdate(ctx, cfIdList, callback, mapToArgumentsWithDefaultValue(ctx, proto.getScope(), proto.getRemovedAttrKeysList()), toTbMsgId(proto), toTbMsgType(proto));
    }

    private void processArgumentValuesUpdate(CalculatedFieldCtx ctx, List<CalculatedFieldId> cfIdList, TbCallback callback,
                                             Map<String, ArgumentEntry> newArgValues, UUID tbMsgId, TbMsgType tbMsgType) throws CalculatedFieldException {
        if (newArgValues.isEmpty()) {
            log.debug("[{}] No new argument values to process for CF.", ctx.getCfId());
            callback.onSuccess();
        }
        CalculatedFieldState state = states.get(ctx.getCfId());
        boolean justRestored = false;
        if (state == null) {
            state = createState(ctx);
            justRestored = true;
        } else if (ctx.shouldFetchRelationQueryDynamicArgumentsFromDb(state)) {
            log.debug("[{}][{}] Going to update dynamic arguments for CF.", entityId, ctx.getCfId());
            try {
                Map<String, ArgumentEntry> dynamicArgsFromDb = cfService.fetchDynamicArgsFromDb(ctx, entityId);
                dynamicArgsFromDb.forEach(newArgValues::putIfAbsent);
                if (ctx.getCfType() == CalculatedFieldType.GEOFENCING) {
                    var geofencingState = (GeofencingCalculatedFieldState) state;
                    geofencingState.updateLastDynamicArgumentsRefreshTs();
                }
            } catch (Exception e) {
                throw CalculatedFieldException.builder().ctx(ctx).eventEntity(entityId).cause(e).build();
            }
        } else if (ctx.shouldFetchEntityRelations(state)) {
            log.debug("[{}][{}] Going to update related entities for CF.", entityId, ctx.getCfId());
            try {
                if (state instanceof RelatedEntitiesAggregationCalculatedFieldState relatedEntitiesState) {
                    List<EntityId> relatedEntities = cfService.fetchRelatedEntities(ctx, entityId);
                    List<EntityId> missingEntities = relatedEntitiesState.checkRelatedEntities(relatedEntities);
                    if (!missingEntities.isEmpty()) {
                        missingEntities.forEach(missingEntityId -> {
                            Map<String, ArgumentEntry> fetchedArgs = cfService.fetchArgsFromDb(tenantId, missingEntityId, ctx.getArguments());
                            relatedEntitiesState.updateEntityData(setEntityIdToSingleEntityArguments(missingEntityId, fetchedArgs));
                        });
                        justRestored = true;
                    }
                }
            } catch (Exception e) {
                throw CalculatedFieldException.builder().ctx(ctx).eventEntity(entityId).cause(e).build();
            }
        }
        if (state.isSizeOk()) {
            Map<String, ArgumentEntry> updatedArgs = state.update(newArgValues, ctx);
            if (!updatedArgs.isEmpty() || justRestored) {
                cfIdList = new ArrayList<>(cfIdList);
                cfIdList.add(ctx.getCfId());
                processStateIfReady(state, updatedArgs, ctx, cfIdList, tbMsgId, tbMsgType, callback);
            } else {
                callback.onSuccess();
            }
        } else {
            throw CalculatedFieldException.builder().ctx(ctx).eventEntity(entityId).errorMessage(ctx.getSizeExceedsLimitMessage()).build();
        }
    }

    private CalculatedFieldState createState(CalculatedFieldCtx ctx) {
        CalculatedFieldState state = createStateByType(ctx, entityId);
        initState(state, ctx);
        return state;
    }

    private void initState(CalculatedFieldState state, CalculatedFieldCtx ctx) {
        state.setCtx(ctx, actorCtx);
        state.init(false);

        if (ctx.getCfType() == CalculatedFieldType.GEOFENCING && ctx.isRelationQueryDynamicArguments()) {
            GeofencingCalculatedFieldState geofencingState = (GeofencingCalculatedFieldState) state;
            geofencingState.updateLastDynamicArgumentsRefreshTs();
        }

        Map<String, ArgumentEntry> arguments = fetchArguments(ctx);
        state.update(arguments, ctx);

        state.checkStateSize(new CalculatedFieldEntityCtxId(tenantId, ctx.getCfId(), entityId), ctx.getMaxStateSize());
        states.put(ctx.getCfId(), state);
    }

    @SneakyThrows
    private Map<String, ArgumentEntry> fetchArguments(CalculatedFieldCtx ctx) {
        ListenableFuture<Map<String, ArgumentEntry>> argumentsFuture = cfService.fetchArguments(ctx, entityId);
        // Ugly but necessary. We do not expect to often fetch data from DB. Only once per <Entity, CalculatedField> pair lifetime.
        // This call happens while processing the CF pack from the queue consumer. So the timeout should be relatively low.
        // Alternatively, we can fetch the state outside the actor system and push separate command to create this actor,
        // but this will significantly complicate the code.
        return argumentsFuture.get(1, TimeUnit.MINUTES);
    }

    private void processStateIfReady(CalculatedFieldState state, Map<String, ArgumentEntry> updatedArgs, CalculatedFieldCtx ctx,
                                     List<CalculatedFieldId> cfIdList, UUID tbMsgId, TbMsgType tbMsgType, TbCallback callback) throws CalculatedFieldException {
        callback = new MultipleTbCallback(CALLBACKS_PER_CF, callback);
        log.trace("[{}][{}] Processing state if ready. Current args: {}, updated args: {}", entityId, ctx.getCfId(), state.getArguments(), updatedArgs);
        CalculatedFieldEntityCtxId ctxId = new CalculatedFieldEntityCtxId(tenantId, ctx.getCfId(), entityId);
        boolean stateSizeChecked = false;
        try {
            if (ctx.isInitialized() && state.isReady()) {
                log.trace("[{}][{}] Performing calculation. Updated args: {}", entityId, ctx.getCfId(), updatedArgs);
                CalculatedFieldResult calculationResult = state.performCalculation(updatedArgs, ctx).get(systemContext.getCfCalculationResultTimeout(), TimeUnit.SECONDS);
                state.checkStateSize(ctxId, ctx.getMaxStateSize());
                stateSizeChecked = true;
                if (state.isSizeOk()) {
                    if (!calculationResult.isEmpty()) {
                        cfService.pushMsgToRuleEngine(tenantId, entityId, calculationResult, cfIdList, callback);
                    } else {
                        callback.onSuccess();
                    }
                    if (DebugModeUtil.isDebugAllAvailable(ctx.getCalculatedField())) {
                        systemContext.persistCalculatedFieldDebugEvent(tenantId, ctx.getCfId(), entityId, state.getArguments(), tbMsgId, tbMsgType, calculationResult.stringValue(), null);
                    }
                }
            } else {
                if (DebugModeUtil.isDebugFailuresAvailable(ctx.getCalculatedField())) {
                    String errorMsg = ctx.isInitialized() ? state.getReadinessStatus().errorMsg() : "Calculated field state is not initialized!";
                    systemContext.persistCalculatedFieldDebugEvent(tenantId, ctx.getCfId(), entityId, state.getArguments(), tbMsgId, tbMsgType, null, errorMsg);
                }
                callback.onSuccess();
            }
        } catch (Exception e) {
            log.debug("[{}][{}] Failed to process CF state", entityId, ctx.getCfId(), e);
            throw CalculatedFieldException.builder().ctx(ctx).eventEntity(entityId).msgId(tbMsgId).msgType(tbMsgType).arguments(state.getArguments()).cause(e).build();
        } finally {
            if (!stateSizeChecked) {
                state.checkStateSize(ctxId, ctx.getMaxStateSize());
            }
            if (state.isSizeOk()) {
                cfStateService.persistState(ctxId, state, callback);
            } else {
                deleteStateAndRaiseSizeException(ctxId, CalculatedFieldException.builder().ctx(ctx).eventEntity(entityId).errorMessage(ctx.getSizeExceedsLimitMessage()).build(), callback);
            }
        }
    }

    private CalculatedFieldState removeState(CalculatedFieldId cfId) {
        CalculatedFieldState state = states.remove(cfId);
        closeState(state);
        return state;
    }

    private void closeState(CalculatedFieldState state) {
        if (state != null) {
            try {
                state.close();
            } catch (Exception e) {
                log.warn("[{}][{}] Failed to close CF state", tenantId, state.getEntityId(), e);
            }
        }
    }

    private void deleteStateAndRaiseSizeException(CalculatedFieldEntityCtxId ctxId, CalculatedFieldException ex, TbCallback callback) throws CalculatedFieldException {
        // We remove the state, but remember that it is over-sized in a local map.
        cfStateService.deleteState(ctxId, new TbCallback() {
            @Override
            public void onSuccess() {
                callback.onFailure(ex);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(ex);
            }
        });
        throw ex;
    }

    private Map<String, ArgumentEntry> mapToArguments(CalculatedFieldCtx ctx, List<TsKvProto> data) {
        return mapToArguments(entityId, ctx.getMainEntityArguments(), Collections.emptyMap(), data);
    }

    private Map<String, ArgumentEntry> mapToArguments(CalculatedFieldCtx ctx, EntityId entityId, List<TsKvProto> data) {
        return mapToArguments(entityId, ctx.getLinkedAndDynamicArgs(entityId), ctx.getRelatedEntityArguments(), data);
    }

    private Map<String, ArgumentEntry> mapToArguments(EntityId originator, Map<ReferencedEntityKey, Set<String>> args, Map<ReferencedEntityKey, Set<String>> relatedEntityArgs, List<TsKvProto> data) {
        Map<String, ArgumentEntry> arguments = new HashMap<>();
        if (!relatedEntityArgs.isEmpty() || !args.isEmpty()) {
            for (TsKvProto item : data) {
                ReferencedEntityKey key = new ReferencedEntityKey(item.getKv().getKey(), ArgumentType.TS_LATEST, null);
                Set<String> argNames = relatedEntityArgs.get(key);
                if (argNames != null) {
                    argNames.forEach(argName -> {
                        arguments.put(argName, new SingleValueArgumentEntry(originator, item));
                    });
                }
                argNames = args.get(key);
                if (argNames != null) {
                    argNames.forEach(argName -> {
                        arguments.put(argName, new SingleValueArgumentEntry(item));
                    });
                }
                key = new ReferencedEntityKey(item.getKv().getKey(), ArgumentType.TS_ROLLING, null);
                argNames = args.get(key);
                if (argNames != null) {
                    argNames.forEach(argName -> {
                        arguments.put(argName, new SingleValueArgumentEntry(item));
                    });
                }
            }
        }
        return arguments;
    }

    private Map<String, ArgumentEntry> mapToArguments(CalculatedFieldCtx ctx, AttributeScopeProto scope, List<AttributeValueProto> attrDataList) {
        return mapToArguments(entityId, ctx.getMainEntityArguments(), ctx.getMainEntityGeofencingArgumentNames(), Collections.emptyMap(), scope, attrDataList);
    }

    private Map<String, ArgumentEntry> mapToArguments(CalculatedFieldCtx ctx, EntityId entityId, AttributeScopeProto scope, List<AttributeValueProto> attrDataList) {
        var args = ctx.getLinkedAndDynamicArgs(entityId);
        var relatedEntityArgs = ctx.getRelatedEntityArguments();
        List<String> geofencingArgumentNames = ctx.getLinkedEntityAndCurrentOwnerGeofencingArgumentNames();
        return mapToArguments(entityId, args, geofencingArgumentNames, relatedEntityArgs, scope, attrDataList);
    }

    private Map<String, ArgumentEntry> mapToArguments(EntityId entityId, Map<ReferencedEntityKey, Set<String>> args, List<String> geofencingArgNames, Map<ReferencedEntityKey, Set<String>> relatedEntityArgs, AttributeScopeProto scope, List<AttributeValueProto> attrDataList) {
        if (args.isEmpty() && relatedEntityArgs.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, ArgumentEntry> arguments = new HashMap<>();
        for (AttributeValueProto item : attrDataList) {
            ReferencedEntityKey key = new ReferencedEntityKey(item.getKey(), ArgumentType.ATTRIBUTE, AttributeScope.valueOf(scope.name()));
            Set<String> argNames = relatedEntityArgs.get(key);
            if (argNames != null) {
                argNames.forEach(argName -> {
                    arguments.put(argName, new SingleValueArgumentEntry(entityId, item));
                });
            }
            argNames = args.get(key);
            if (argNames == null) {
                continue;
            }
            argNames.forEach(argName -> {
                if (geofencingArgNames.contains(argName)) {
                    arguments.put(argName, new GeofencingArgumentEntry(entityId, item));
                } else {
                    arguments.put(argName, new SingleValueArgumentEntry(item));
                }
            });
        }
        return arguments;
    }

    private Map<String, ArgumentEntry> mapToArgumentsWithDefaultValue(CalculatedFieldCtx ctx, EntityId entityId, AttributeScopeProto scope, List<String> removedAttrKeys) {
        var args = ctx.getLinkedAndDynamicArgs(entityId);
        var relatedEntityArgs = ctx.getRelatedEntityArguments();
        List<String> geofencingArgumentNames = ctx.getLinkedEntityAndCurrentOwnerGeofencingArgumentNames();
        return mapToArgumentsWithDefaultValue(entityId, args, ctx.getArguments(), geofencingArgumentNames, relatedEntityArgs, scope, removedAttrKeys);
    }

    private Map<String, ArgumentEntry> mapToArgumentsWithDefaultValue(CalculatedFieldCtx ctx, AttributeScopeProto scope, List<String> removedAttrKeys) {
        return mapToArgumentsWithDefaultValue(null, ctx.getMainEntityArguments(), ctx.getArguments(), ctx.getMainEntityGeofencingArgumentNames(), Collections.emptyMap(), scope, removedAttrKeys);
    }

    private Map<String, ArgumentEntry> mapToArgumentsWithDefaultValue(EntityId msgEntityId,
                                                                      Map<ReferencedEntityKey, Set<String>> args,
                                                                      Map<String, Argument> configArguments,
                                                                      List<String> geofencingArgNames,
                                                                      Map<ReferencedEntityKey, Set<String>> relatedEntityArgs,
                                                                      AttributeScopeProto scope,
                                                                      List<String> removedAttrKeys) {
        if (args.isEmpty() && relatedEntityArgs.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, ArgumentEntry> arguments = new HashMap<>();
        for (String removedKey : removedAttrKeys) {
            ReferencedEntityKey key = new ReferencedEntityKey(removedKey, ArgumentType.ATTRIBUTE, AttributeScope.valueOf(scope.name()));
            Set<String> argNames = relatedEntityArgs.get(key);
            if (argNames != null) {
                argNames.forEach(argName -> {
                    String defaultValue = getDefaultValue(configArguments, argName);
                    SingleValueArgumentEntry argumentEntry = buildSingleValue(removedKey, defaultValue, System.currentTimeMillis());
                    arguments.put(argName, new SingleValueArgumentEntry(msgEntityId, argumentEntry));
                });
            }
            argNames = args.get(key);
            if (argNames == null) {
                continue;
            }
            argNames.forEach(argName -> {
                if (geofencingArgNames.contains(argName)) {
                    arguments.put(argName, new GeofencingArgumentEntry());
                } else {
                    String defaultValue = getDefaultValue(configArguments, argName);
                    SingleValueArgumentEntry argumentEntry = buildSingleValue(removedKey, defaultValue, System.currentTimeMillis());
                    arguments.put(argName, new SingleValueArgumentEntry(argumentEntry));
                }
            });
        }
        return arguments;
    }

    private String getDefaultValue(Map<String, Argument> configArguments, String argNames) {
        Argument argument = configArguments.get(argNames);
        return argument != null ? argument.getDefaultValue() : null;
    }

    private SingleValueArgumentEntry buildSingleValue(String attrKey, String defaultValue, long ts) {
        return StringUtils.isNotEmpty(defaultValue)
                ? new SingleValueArgumentEntry(ts, new StringDataEntry(attrKey, defaultValue), null)
                : new SingleValueArgumentEntry();
    }

    private Map<String, ArgumentEntry> mapToArgumentsWithFetchedValue(CalculatedFieldCtx ctx, EntityId entityId, List<String> removedTelemetryKeys) {
        Map<String, Argument> deletedArguments = ctx.getArguments().entrySet().stream()
                .filter(entry -> removedTelemetryKeys.contains(entry.getValue().getRefEntityKey().getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<String, ArgumentEntry> fetchedArgs = cfService.fetchArgsFromDb(tenantId, entityId, deletedArguments);

        if (CalculatedFieldType.RELATED_ENTITIES_AGGREGATION.equals(ctx.getCfType())) {
            fetchedArgs = setEntityIdToSingleEntityArguments(entityId, fetchedArgs);
        }
        fetchedArgs.values().forEach(arg -> arg.setForceResetPrevious(true));

        return fetchedArgs;
    }

    private Map<String, ArgumentEntry> setEntityIdToSingleEntityArguments(EntityId relatedEntityId, Map<String, ArgumentEntry> fetchedArgs) {
        return fetchedArgs.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        argEntry -> new SingleValueArgumentEntry(relatedEntityId, argEntry.getValue())
                ));
    }

    private static List<CalculatedFieldId> getCalculatedFieldIds(CalculatedFieldTelemetryMsgProto proto) {
        List<CalculatedFieldId> cfIds = new LinkedList<>();
        for (var cfId : proto.getPreviousCalculatedFieldsList()) {
            cfIds.add(new CalculatedFieldId(new UUID(cfId.getCalculatedFieldIdMSB(), cfId.getCalculatedFieldIdLSB())));
        }
        return cfIds;
    }

    private UUID toTbMsgId(CalculatedFieldTelemetryMsgProto proto) {
        if (proto.getTbMsgIdMSB() != 0 && proto.getTbMsgIdLSB() != 0) {
            return new UUID(proto.getTbMsgIdMSB(), proto.getTbMsgIdLSB());
        }
        return null;
    }

    private TbMsgType toTbMsgType(CalculatedFieldTelemetryMsgProto proto) {
        if (!proto.getTbMsgType().isEmpty()) {
            return TbMsgType.valueOf(proto.getTbMsgType());
        }
        return null;
    }

}
