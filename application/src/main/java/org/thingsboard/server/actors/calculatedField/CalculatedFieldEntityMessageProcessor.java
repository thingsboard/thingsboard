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
package org.thingsboard.server.actors.calculatedField;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.DebugModeUtil;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.shared.AbstractContextAwareMsgProcessor;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.gen.transport.TransportProtos.AttributeScopeProto;
import org.thingsboard.server.gen.transport.TransportProtos.AttributeValueProto;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldTelemetryMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.TsKvProto;
import org.thingsboard.server.service.cf.CalculatedFieldExecutionService;
import org.thingsboard.server.service.cf.CalculatedFieldResult;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.SingleValueArgumentEntry;

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


/**
 * @author Andrew Shvayka
 */
@Slf4j
public class CalculatedFieldEntityMessageProcessor extends AbstractContextAwareMsgProcessor {
    // (1 for result persistence + 1 for the state persistence )
    public static final int CALLBACKS_PER_CF = 2;

    final TenantId tenantId;
    final EntityId entityId;
    final CalculatedFieldExecutionService cfService;

    TbActorCtx ctx;
    Map<CalculatedFieldId, CalculatedFieldState> states = new HashMap<>();

    CalculatedFieldEntityMessageProcessor(ActorSystemContext systemContext, TenantId tenantId, EntityId entityId) {
        super(systemContext);
        this.tenantId = tenantId;
        this.entityId = entityId;
        this.cfService = systemContext.getCalculatedFieldExecutionService();
    }

    void init(TbActorCtx ctx) {
        this.ctx = ctx;
    }

    public void process(CalculatedFieldStateRestoreMsg msg) {
        states.put(msg.getId().cfId(), msg.getState());
    }

    public void process(EntityInitCalculatedFieldMsg msg) {
        var cfCtx = msg.getCtx();
        if (msg.isForceReinit()) {
            states.remove(cfCtx.getCfId());
        }
        var cfState = getOrInitState(cfCtx);
        processStateIfReady(cfCtx, Collections.singletonList(cfCtx.getCfId()), cfState, null, null, msg.getCallback());
    }

    public void process(CalculatedFieldEntityDeleteMsg msg) {
        if (this.entityId.equals(msg.getEntityId())) {
            MultipleTbCallback multipleTbCallback = new MultipleTbCallback(states.size(), msg.getCallback());
            states.forEach((cfId, state) -> cfService.deleteStateFromStorage(new CalculatedFieldEntityCtxId(tenantId, cfId, entityId), multipleTbCallback));
            ctx.stop(ctx.getSelf());
        } else {
            var cfId = new CalculatedFieldId(msg.getEntityId().getId());
            var state = states.remove(cfId);
            if (state != null) {
                cfService.deleteStateFromStorage(new CalculatedFieldEntityCtxId(tenantId, cfId, entityId), msg.getCallback());
            }
        }
    }

    public void process(EntityCalculatedFieldTelemetryMsg msg) {
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

    public void process(EntityCalculatedFieldLinkedTelemetryMsg msg) {
        var proto = msg.getProto();
        var ctx = msg.getCtx();
        var callback = new MultipleTbCallback(CALLBACKS_PER_CF, msg.getCallback());
        List<CalculatedFieldId> cfIds = getCalculatedFieldIds(proto);
        if (cfIds.contains(ctx.getCfId())) {
            callback.onSuccess(CALLBACKS_PER_CF);
        } else {
            if (proto.getTsDataCount() > 0) {
                processArgumentValuesUpdate(ctx, cfIds, callback, mapToArguments(ctx, msg.getEntityId(), proto.getTsDataList()), toTbMsgId(proto), toTbMsgType(proto));
            } else if (proto.getAttrDataCount() > 0) {
                processArgumentValuesUpdate(ctx, cfIds, callback, mapToArguments(ctx, msg.getEntityId(), proto.getScope(), proto.getAttrDataList()), toTbMsgId(proto), toTbMsgType(proto));
            } else {
                callback.onSuccess(CALLBACKS_PER_CF);
            }
        }
    }

    private void process(CalculatedFieldCtx ctx, CalculatedFieldTelemetryMsgProto proto, Collection<CalculatedFieldId> cfIds, List<CalculatedFieldId> cfIdList, MultipleTbCallback callback) {
        if (cfIds.contains(ctx.getCfId())) {
            callback.onSuccess(CALLBACKS_PER_CF);
        } else {
            if (proto.getTsDataCount() > 0) {
                processTelemetry(ctx, proto, cfIdList, callback);
            } else if (proto.getAttrDataCount() > 0) {
                processAttributes(ctx, proto, cfIdList, callback);
            } else {
                callback.onSuccess(CALLBACKS_PER_CF);
            }
        }
    }

    @SneakyThrows
    private void processTelemetry(CalculatedFieldCtx ctx, CalculatedFieldTelemetryMsgProto proto, List<CalculatedFieldId> cfIdList, MultipleTbCallback callback) {
        processArgumentValuesUpdate(ctx, cfIdList, callback, mapToArguments(ctx, proto.getTsDataList()), toTbMsgId(proto), toTbMsgType(proto));
    }

    @SneakyThrows
    private void processAttributes(CalculatedFieldCtx ctx, CalculatedFieldTelemetryMsgProto proto, List<CalculatedFieldId> cfIdList, MultipleTbCallback callback) {
        processArgumentValuesUpdate(ctx, cfIdList, callback, mapToArguments(ctx, proto.getScope(), proto.getAttrDataList()), toTbMsgId(proto), toTbMsgType(proto));
    }

    @SneakyThrows
    private void processArgumentValuesUpdate(CalculatedFieldCtx ctx, List<CalculatedFieldId> cfIdList, MultipleTbCallback callback,
                                             Map<String, ArgumentEntry> newArgValues, UUID tbMsgId, TbMsgType tbMsgType) {
        if (newArgValues.isEmpty()) {
            callback.onSuccess(CALLBACKS_PER_CF);
        }
        CalculatedFieldState state = getOrInitState(ctx);
        if (state.updateState(newArgValues)) {
            cfIdList = new ArrayList<>(cfIdList);
            cfIdList.add(ctx.getCfId());
            processStateIfReady(ctx, cfIdList, state, tbMsgId, tbMsgType, callback);
        } else {
            callback.onSuccess(CALLBACKS_PER_CF);
        }
    }

    @SneakyThrows
    private CalculatedFieldState getOrInitState(CalculatedFieldCtx ctx) {
        CalculatedFieldState state = states.get(ctx.getCfId());
        if (state != null) {
            return state;
        } else {
            ListenableFuture<CalculatedFieldState> stateFuture = systemContext.getCalculatedFieldExecutionService().fetchStateFromDb(ctx, entityId);
            // Ugly but necessary. We do not expect to often fetch data from DB. Only once per <Entity, CalculatedField> pair lifetime.
            // This call happens while processing the CF pack from the queue consumer. So the timeout should be relatively low.
            // Alternatively, we can fetch the state outside the actor system and push separate command to create this actor,
            // but this will significantly complicate the code.
            state = stateFuture.get(1, TimeUnit.MINUTES);
            states.put(ctx.getCfId(), state);
        }
        return state;
    }

    @SneakyThrows
    private void processStateIfReady(CalculatedFieldCtx ctx, List<CalculatedFieldId> cfIdList, CalculatedFieldState state, UUID tbMsgId, TbMsgType tbMsgType, TbCallback callback) {
        if (state.isReady() && ctx.isInitialized()) {
            CalculatedFieldResult calculationResult = state.performCalculation(ctx).get(5, TimeUnit.SECONDS);
            cfService.pushMsgToRuleEngine(tenantId, entityId, calculationResult, cfIdList, callback);
            if (DebugModeUtil.isDebugAllAvailable(ctx.getCalculatedField())) {
                systemContext.persistCalculatedFieldDebugEvent(tenantId, ctx.getCfId(), entityId, state.getArguments(), tbMsgId, tbMsgType, JacksonUtil.writeValueAsString(calculationResult.getResultMap()), null);
            }
        } else {
            callback.onSuccess(); // State was updated but no calculation performed;
        }
        cfService.pushStateToStorage(ctx, new CalculatedFieldEntityCtxId(tenantId, ctx.getCfId(), entityId), state, callback);
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

    private static Map<String, ArgumentEntry> mapToArguments(Map<ReferencedEntityKey, String> argNames, List<TsKvProto> data) {
        if (argNames.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, ArgumentEntry> arguments = new HashMap<>();
        for (TsKvProto item : data) {
            ReferencedEntityKey key = new ReferencedEntityKey(item.getKv().getKey(), ArgumentType.TS_LATEST, null);
            String argName = argNames.get(key);
            if (argName != null) {
                arguments.put(argName, new SingleValueArgumentEntry(item));
            }
            key = new ReferencedEntityKey(item.getKv().getKey(), ArgumentType.TS_ROLLING, null);
            argName = argNames.get(key);
            if (argName != null) {
                arguments.put(argName, new SingleValueArgumentEntry(item));
            }
        }
        return arguments;
    }

    private Map<String, ArgumentEntry> mapToArguments(CalculatedFieldCtx ctx, AttributeScopeProto scope, List<AttributeValueProto> attrDataList) {
        return mapToArguments(ctx.getMainEntityArguments(), scope, attrDataList);
    }

    private Map<String, ArgumentEntry> mapToArguments(CalculatedFieldCtx ctx, EntityId entityId, AttributeScopeProto scope, List<AttributeValueProto> attrDataList) {
        var argNames = ctx.getLinkedEntityArguments().get(entityId);
        if (argNames.isEmpty()) {
            return Collections.emptyMap();
        }
        return mapToArguments(argNames, scope, attrDataList);
    }

    private static Map<String, ArgumentEntry> mapToArguments(Map<ReferencedEntityKey, String> argNames, AttributeScopeProto scope, List<AttributeValueProto> attrDataList) {
        Map<String, ArgumentEntry> arguments = new HashMap<>();
        for (AttributeValueProto item : attrDataList) {
            ReferencedEntityKey key = new ReferencedEntityKey(item.getKey(), ArgumentType.ATTRIBUTE, AttributeScope.valueOf(scope.name()));
            String argName = argNames.get(key);
            if (argName != null) {
                arguments.put(argName, new SingleValueArgumentEntry(item));
            }
        }
        return arguments;
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
