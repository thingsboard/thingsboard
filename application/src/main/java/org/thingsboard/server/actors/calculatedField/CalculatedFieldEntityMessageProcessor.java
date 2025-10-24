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
import org.thingsboard.server.actors.shared.AbstractContextAwareMsgProcessor;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
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

    TbActorCtx ctx;
    Map<CalculatedFieldId, CalculatedFieldState> states = new HashMap<>();

    CalculatedFieldEntityMessageProcessor(ActorSystemContext systemContext, TenantId tenantId, EntityId entityId) {
        super(systemContext);
        this.tenantId = tenantId;
        this.entityId = entityId;
        this.cfService = systemContext.getCalculatedFieldProcessingService();
        this.cfStateService = systemContext.getCalculatedFieldStateService();
    }

    void init(TbActorCtx ctx) {
        this.ctx = ctx;
    }

    public void stop(boolean partitionChanged) {
        log.info(partitionChanged ?
                        "[{}][{}] Stopping entity actor due to change partition event." :
                        "[{}][{}] Stopping entity actor.",
                tenantId, entityId);
        states.clear();
        ctx.stop(ctx.getSelf());
    }

    public void process(CalculatedFieldPartitionChangeMsg msg) {
        if (!systemContext.getPartitionService().resolve(ServiceType.TB_RULE_ENGINE, DataConstants.CF_QUEUE_NAME, tenantId, entityId).isMyPartition()) {
            stop(true);
        }
    }

    public void process(CalculatedFieldStateRestoreMsg msg) {
        CalculatedFieldId cfId = msg.getId().cfId();
        log.debug("[{}] [{}] Processing CF state restore msg.", msg.getId().entityId(), cfId);
        if (msg.getState() != null) {
            states.put(cfId, msg.getState());
        } else {
            states.remove(cfId);
        }
    }

    public void process(EntityInitCalculatedFieldMsg msg) throws CalculatedFieldException {
        log.debug("[{}] Processing entity init CF msg.", msg.getCtx().getCfId());
        var ctx = msg.getCtx();
        if (msg.isForceReinit()) {
            log.debug("Force reinitialization of CF: [{}].", ctx.getCfId());
            states.remove(ctx.getCfId());
        }
        try {
            var state = getOrInitState(ctx);
            if (state.isSizeOk()) {
                processStateIfReady(ctx, Collections.singletonList(ctx.getCfId()), state, null, null, msg.getCallback());
            } else {
                throw new RuntimeException(ctx.getSizeExceedsLimitMessage());
            }
        } catch (Exception e) {
            if (e instanceof CalculatedFieldException cfe) {
                throw cfe;
            }
            throw CalculatedFieldException.builder().ctx(ctx).eventEntity(entityId).cause(e).build();
        }
    }

    public void process(CalculatedFieldEntityDeleteMsg msg) {
        log.debug("[{}] Processing CF entity delete msg.", msg.getEntityId());
        if (this.entityId.equals(msg.getEntityId())) {
            if (states.isEmpty()) {
                msg.getCallback().onSuccess();
            } else {
                MultipleTbCallback multipleTbCallback = new MultipleTbCallback(states.size(), msg.getCallback());
                states.forEach((cfId, state) -> cfStateService.removeState(new CalculatedFieldEntityCtxId(tenantId, cfId, entityId), multipleTbCallback));
                ctx.stop(ctx.getSelf());
            }
        } else {
            var cfId = new CalculatedFieldId(msg.getEntityId().getId());
            var state = states.remove(cfId);
            if (state != null) {
                cfStateService.removeState(new CalculatedFieldEntityCtxId(tenantId, cfId, entityId), msg.getCallback());
            } else {
                msg.getCallback().onSuccess();
            }
        }
    }

    public void process(EntityCalculatedFieldTelemetryMsg msg) throws CalculatedFieldException {
        log.debug("[{}] Processing CF telemetry msg.", msg.getEntityId());
        var proto = msg.getProto();
        var numberOfCallbacks = CALLBACKS_PER_CF * (msg.getEntityIdFields().size() + msg.getProfileIdFields().size());
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
        log.debug("[{}] Processing CF link telemetry msg.", msg.getEntityId());
        var proto = msg.getProto();
        var ctx = msg.getCtx();
        var callback = new MultipleTbCallback(CALLBACKS_PER_CF, msg.getCallback());
        try {
            List<CalculatedFieldId> cfIds = getCalculatedFieldIds(proto);
            if (cfIds.contains(ctx.getCfId())) {
                callback.onSuccess(CALLBACKS_PER_CF);
            } else {
                if (proto.getTsDataCount() > 0) {
                    processArgumentValuesUpdate(ctx, cfIds, callback, mapToArguments(ctx, msg.getEntityId(), proto.getTsDataList()), toTbMsgId(proto), toTbMsgType(proto));
                } else if (proto.getAttrDataCount() > 0) {
                    processArgumentValuesUpdate(ctx, cfIds, callback, mapToArguments(ctx, msg.getEntityId(), proto.getScope(), proto.getAttrDataList()), toTbMsgId(proto), toTbMsgType(proto));
                } else if (proto.getRemovedTsKeysCount() > 0) {
                    processArgumentValuesUpdate(ctx, cfIds, callback, mapToArgumentsWithFetchedValue(ctx, proto.getRemovedTsKeysList()), toTbMsgId(proto), toTbMsgType(proto));
                } else if (proto.getRemovedAttrKeysCount() > 0) {
                    processArgumentValuesUpdate(ctx, cfIds, callback, mapToArgumentsWithDefaultValue(ctx, msg.getEntityId(), proto.getScope(), proto.getRemovedAttrKeysList()), toTbMsgId(proto), toTbMsgType(proto));
                } else {
                    callback.onSuccess(CALLBACKS_PER_CF);
                }
            }
        } catch (Exception e) {
            throw CalculatedFieldException.builder().ctx(ctx).eventEntity(entityId).cause(e).build();
        }
    }

    private void process(CalculatedFieldCtx ctx, CalculatedFieldTelemetryMsgProto proto, Collection<CalculatedFieldId> cfIds, List<CalculatedFieldId> cfIdList, MultipleTbCallback callback) throws CalculatedFieldException {
        try {
            if (cfIds.contains(ctx.getCfId())) {
                callback.onSuccess(CALLBACKS_PER_CF);
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
                    callback.onSuccess(CALLBACKS_PER_CF);
                }
            }
        } catch (Exception e) {
            if (e instanceof CalculatedFieldException cfe) {
                throw cfe;
            }
            throw CalculatedFieldException.builder().ctx(ctx).eventEntity(entityId).cause(e).build();
        }
    }

    private void processTelemetry(CalculatedFieldCtx ctx, CalculatedFieldTelemetryMsgProto proto, List<CalculatedFieldId> cfIdList, MultipleTbCallback callback) throws CalculatedFieldException {
        processArgumentValuesUpdate(ctx, cfIdList, callback, mapToArguments(ctx, proto.getTsDataList()), toTbMsgId(proto), toTbMsgType(proto));
    }

    private void processAttributes(CalculatedFieldCtx ctx, CalculatedFieldTelemetryMsgProto proto, List<CalculatedFieldId> cfIdList, MultipleTbCallback callback) throws CalculatedFieldException {
        processArgumentValuesUpdate(ctx, cfIdList, callback, mapToArguments(ctx, proto.getScope(), proto.getAttrDataList()), toTbMsgId(proto), toTbMsgType(proto));
    }

    private void processRemovedTelemetry(CalculatedFieldCtx ctx, CalculatedFieldTelemetryMsgProto proto, List<CalculatedFieldId> cfIdList, MultipleTbCallback callback) throws CalculatedFieldException {
        processArgumentValuesUpdate(ctx, cfIdList, callback, mapToArgumentsWithFetchedValue(ctx, proto.getRemovedTsKeysList()), toTbMsgId(proto), toTbMsgType(proto));
    }

    private void processRemovedAttributes(CalculatedFieldCtx ctx, CalculatedFieldTelemetryMsgProto proto, List<CalculatedFieldId> cfIdList, MultipleTbCallback callback) throws CalculatedFieldException {
        processArgumentValuesUpdate(ctx, cfIdList, callback, mapToArgumentsWithDefaultValue(ctx, proto.getScope(), proto.getRemovedAttrKeysList()), toTbMsgId(proto), toTbMsgType(proto));
    }

    private void processArgumentValuesUpdate(CalculatedFieldCtx ctx, List<CalculatedFieldId> cfIdList, MultipleTbCallback callback,
                                             Map<String, ArgumentEntry> newArgValues, UUID tbMsgId, TbMsgType tbMsgType) throws CalculatedFieldException {
        if (newArgValues.isEmpty()) {
            log.debug("[{}] No new argument values to process for CF.", ctx.getCfId());
            callback.onSuccess(CALLBACKS_PER_CF);
        }
        CalculatedFieldState state = states.get(ctx.getCfId());
        boolean justRestored = false;
        if (state == null) {
            state = getOrInitState(ctx);
            justRestored = true;
        } else if (ctx.shouldFetchDynamicArgumentsFromDb(state)) {
            log.debug("[{}][{}] Going to update dynamic arguments for CF.", entityId, ctx.getCfId());
            try {
                Map<String, ArgumentEntry> dynamicArgsFromDb = cfService.fetchDynamicArgsFromDb(ctx, entityId);
                dynamicArgsFromDb.forEach(newArgValues::putIfAbsent);
                var geofencingState = (GeofencingCalculatedFieldState) state;
                geofencingState.setLastDynamicArgumentsRefreshTs(System.currentTimeMillis());
            } catch (Exception e) {
                throw CalculatedFieldException.builder().ctx(ctx).eventEntity(entityId).cause(e).build();
            }
        }
        if (state.isSizeOk()) {
            if (state.updateState(ctx, newArgValues) || justRestored) {
                cfIdList = new ArrayList<>(cfIdList);
                cfIdList.add(ctx.getCfId());
                processStateIfReady(ctx, cfIdList, state, tbMsgId, tbMsgType, callback);
            } else {
                callback.onSuccess(CALLBACKS_PER_CF);
            }
        } else {
            throw CalculatedFieldException.builder().ctx(ctx).eventEntity(entityId).errorMessage(ctx.getSizeExceedsLimitMessage()).build();
        }
    }

    @SneakyThrows
    private CalculatedFieldState getOrInitState(CalculatedFieldCtx ctx) {
        CalculatedFieldState state = states.get(ctx.getCfId());
        if (state != null) {
            return state;
        } else {
            ListenableFuture<CalculatedFieldState> stateFuture = cfService.fetchStateFromDb(ctx, entityId);
            // Ugly but necessary. We do not expect to often fetch data from DB. Only once per <Entity, CalculatedField> pair lifetime.
            // This call happens while processing the CF pack from the queue consumer. So the timeout should be relatively low.
            // Alternatively, we can fetch the state outside the actor system and push separate command to create this actor,
            // but this will significantly complicate the code.
            state = stateFuture.get(1, TimeUnit.MINUTES);
            state.checkStateSize(new CalculatedFieldEntityCtxId(tenantId, ctx.getCfId(), entityId), ctx.getMaxStateSize());
            states.put(ctx.getCfId(), state);
        }
        return state;
    }

    private void processStateIfReady(CalculatedFieldCtx ctx, List<CalculatedFieldId> cfIdList, CalculatedFieldState state, UUID tbMsgId, TbMsgType tbMsgType, TbCallback callback) throws CalculatedFieldException {
        CalculatedFieldEntityCtxId ctxId = new CalculatedFieldEntityCtxId(tenantId, ctx.getCfId(), entityId);
        boolean stateSizeChecked = false;
        try {
            if (ctx.isInitialized() && state.isReady()) {
                CalculatedFieldResult calculationResult = state.performCalculation(entityId, ctx).get(systemContext.getCfCalculationResultTimeout(), TimeUnit.SECONDS);
                state.checkStateSize(ctxId, ctx.getMaxStateSize());
                stateSizeChecked = true;
                if (state.isSizeOk()) {
                    if (!calculationResult.isEmpty()) {
                        cfService.pushMsgToRuleEngine(tenantId, entityId, calculationResult, cfIdList, callback);
                    } else {
                        callback.onSuccess();
                    }
                    if (DebugModeUtil.isDebugAllAvailable(ctx.getCalculatedField())) {
                        systemContext.persistCalculatedFieldDebugEvent(tenantId, ctx.getCfId(), entityId, state.getArguments(), tbMsgId, tbMsgType, calculationResult.toStringOrElseNull(), null);
                    }
                }
            } else {
                callback.onSuccess();
            }
        } catch (Exception e) {
            throw CalculatedFieldException.builder().ctx(ctx).eventEntity(entityId).msgId(tbMsgId).msgType(tbMsgType).arguments(state.getArguments()).cause(e).build();
        } finally {
            if (!stateSizeChecked) {
                state.checkStateSize(ctxId, ctx.getMaxStateSize());
            }
            if (state.isSizeOk()) {
                cfStateService.persistState(ctxId, state, callback);
            } else {
                removeStateAndRaiseSizeException(ctxId, CalculatedFieldException.builder().ctx(ctx).eventEntity(entityId).errorMessage(ctx.getSizeExceedsLimitMessage()).build(), callback);
            }
        }
    }

    private void removeStateAndRaiseSizeException(CalculatedFieldEntityCtxId ctxId, CalculatedFieldException ex, TbCallback callback) throws CalculatedFieldException {
        // We remove the state, but remember that it is over-sized in a local map.
        cfStateService.removeState(ctxId, new TbCallback() {
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
        return mapToArguments(ctx.getMainEntityArguments(), data);
    }

    private Map<String, ArgumentEntry> mapToArguments(CalculatedFieldCtx ctx, EntityId entityId, List<TsKvProto> data) {
        var argNames = ctx.getLinkedEntityArguments().get(entityId);
        if (argNames.isEmpty()) {
            return Collections.emptyMap();
        }
        return mapToArguments(argNames, data);
    }

    private Map<String, ArgumentEntry> mapToArguments(Map<ReferencedEntityKey, Set<String>> args, List<TsKvProto> data) {
        if (args.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, ArgumentEntry> arguments = new HashMap<>();
        for (TsKvProto item : data) {
            ReferencedEntityKey key = new ReferencedEntityKey(item.getKv().getKey(), ArgumentType.TS_LATEST, null);
            Set<String> argNames = args.get(key);
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
        return arguments;
    }

    private Map<String, ArgumentEntry> mapToArguments(CalculatedFieldCtx ctx, AttributeScopeProto scope, List<AttributeValueProto> attrDataList) {
        return mapToArguments(entityId, ctx.getMainEntityArguments(), ctx.getMainEntityGeofencingArgumentNames(), scope, attrDataList);
    }

    private Map<String, ArgumentEntry> mapToArguments(CalculatedFieldCtx ctx, EntityId entityId, AttributeScopeProto scope, List<AttributeValueProto> attrDataList) {
        var argNames = ctx.getLinkedEntityArguments().get(entityId);
        if (argNames.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> geofencingArgumentNames = ctx.getLinkedEntityGeofencingArgumentNames();
        return mapToArguments(entityId, argNames, geofencingArgumentNames, scope, attrDataList);
    }

    private Map<String, ArgumentEntry> mapToArguments(EntityId entityId, Map<ReferencedEntityKey, Set<String>> args, List<String> geofencingArgNames, AttributeScopeProto scope, List<AttributeValueProto> attrDataList) {
        Map<String, ArgumentEntry> arguments = new HashMap<>();
        for (AttributeValueProto item : attrDataList) {
            ReferencedEntityKey key = new ReferencedEntityKey(item.getKey(), ArgumentType.ATTRIBUTE, AttributeScope.valueOf(scope.name()));
            Set<String> argNames = args.get(key);
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
        var argNames = ctx.getLinkedEntityArguments().get(entityId);
        if (argNames.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> geofencingArgumentNames = ctx.getLinkedEntityGeofencingArgumentNames();
        return mapToArgumentsWithDefaultValue(argNames, ctx.getArguments(), geofencingArgumentNames, scope, removedAttrKeys);
    }

    private Map<String, ArgumentEntry> mapToArgumentsWithDefaultValue(CalculatedFieldCtx ctx, AttributeScopeProto scope, List<String> removedAttrKeys) {
        return mapToArgumentsWithDefaultValue(ctx.getMainEntityArguments(), ctx.getArguments(), ctx.getMainEntityGeofencingArgumentNames(), scope, removedAttrKeys);
    }

    private Map<String, ArgumentEntry> mapToArgumentsWithDefaultValue(Map<ReferencedEntityKey, Set<String>> args, Map<String, Argument> configArguments, List<String> geofencingArgNames, AttributeScopeProto scope, List<String> removedAttrKeys) {
        Map<String, ArgumentEntry> arguments = new HashMap<>();
        for (String removedKey : removedAttrKeys) {
            ReferencedEntityKey key = new ReferencedEntityKey(removedKey, ArgumentType.ATTRIBUTE, AttributeScope.valueOf(scope.name()));
            Set<String> argNames = args.get(key);
            if (argNames == null) {
                continue;
            }
            argNames.forEach(argName -> {
                if (geofencingArgNames.contains(argName)) {
                    arguments.put(argName, new GeofencingArgumentEntry());
                } else {
                    Argument argument = configArguments.get(argName);
                    String defaultValue = (argument != null) ? argument.getDefaultValue() : null;
                    arguments.put(argName, StringUtils.isNotEmpty(defaultValue)
                            ? new SingleValueArgumentEntry(System.currentTimeMillis(), new StringDataEntry(removedKey, defaultValue), null)
                            : new SingleValueArgumentEntry());
                }
            });
        }
        return arguments;
    }

    private Map<String, ArgumentEntry> mapToArgumentsWithFetchedValue(CalculatedFieldCtx ctx, List<String> removedTelemetryKeys) {
        Map<String, Argument> deletedArguments = ctx.getArguments().entrySet().stream()
                .filter(entry -> removedTelemetryKeys.contains(entry.getValue().getRefEntityKey().getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<String, ArgumentEntry> fetchedArgs = cfService.fetchArgsFromDb(tenantId, entityId, deletedArguments);

        fetchedArgs.values().forEach(arg -> arg.setForceResetPrevious(true));
        return fetchedArgs;
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
