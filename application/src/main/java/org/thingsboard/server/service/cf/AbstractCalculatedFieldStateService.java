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

import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.calculatedField.CalculatedFieldStateRestoreMsg;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicKvEntry;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.util.KvProtoUtil;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldEntityCtxIdProto;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldStateMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldStateProto;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.ScriptCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.SimpleCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.SingleValueArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.TsRollingArgumentEntry;

import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

public abstract class AbstractCalculatedFieldStateService implements CalculatedFieldStateService {

    @Autowired
    private ActorSystemContext actorSystemContext;

    @Override
    public final void persistState(CalculatedFieldCtx ctx, CalculatedFieldEntityCtxId stateId, CalculatedFieldState state, TbCallback callback) {
        CalculatedFieldStateMsgProto stateMsg = toProto(stateId, state);
        long maxStateSizeInKBytes = ctx.getMaxStateSizeInKBytes();
        if (maxStateSizeInKBytes <= 0 || stateMsg.getSerializedSize() <= maxStateSizeInKBytes) {
            doPersist(stateId, stateMsg, callback);
        }
    }

    protected abstract void doPersist(CalculatedFieldEntityCtxId stateId, CalculatedFieldStateMsgProto stateMsgProto, TbCallback callback);

    @Override
    public final void removeState(CalculatedFieldEntityCtxId stateId, TbCallback callback) {
        doRemove(stateId, callback);
    }

    protected abstract void doRemove(CalculatedFieldEntityCtxId stateId, TbCallback callback);

    protected void processRestoredState(CalculatedFieldStateMsgProto stateMsg) {
        CalculatedFieldEntityCtxId stateId = fromProto(stateMsg.getId());
        CalculatedFieldState state = stateMsg.hasState() ? fromProto(stateMsg.getState()) : null;
        actorSystemContext.tell(new CalculatedFieldStateRestoreMsg(stateId, state));
    }

    protected CalculatedFieldEntityCtxIdProto toProto(CalculatedFieldEntityCtxId ctxId) {
        return CalculatedFieldEntityCtxIdProto.newBuilder()
                .setTenantIdMSB(ctxId.tenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(ctxId.tenantId().getId().getLeastSignificantBits())
                .setCalculatedFieldIdMSB(ctxId.cfId().getId().getMostSignificantBits())
                .setCalculatedFieldIdLSB(ctxId.cfId().getId().getLeastSignificantBits())
                .setEntityType(ctxId.entityId().getEntityType().name())
                .setEntityIdMSB(ctxId.entityId().getId().getMostSignificantBits())
                .setEntityIdLSB(ctxId.entityId().getId().getLeastSignificantBits())
                .build();
    }

    protected CalculatedFieldEntityCtxId fromProto(CalculatedFieldEntityCtxIdProto ctxIdProto) {
        TenantId tenantId = TenantId.fromUUID(new UUID(ctxIdProto.getTenantIdMSB(), ctxIdProto.getTenantIdLSB()));
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(ctxIdProto.getEntityType(), new UUID(ctxIdProto.getEntityIdMSB(), ctxIdProto.getEntityIdLSB()));
        CalculatedFieldId calculatedFieldId = new CalculatedFieldId(new UUID(ctxIdProto.getCalculatedFieldIdMSB(), ctxIdProto.getCalculatedFieldIdLSB()));
        return new CalculatedFieldEntityCtxId(tenantId, calculatedFieldId, entityId);
    }

    protected CalculatedFieldStateMsgProto toProto(CalculatedFieldEntityCtxId stateId, CalculatedFieldState state) {
        var stateProto = CalculatedFieldStateProto.newBuilder()
                .setType(state.getType().name());
        state.getArguments().forEach((argName, argEntry) -> {
            if (argEntry instanceof SingleValueArgumentEntry singleValueArgumentEntry) {
                stateProto.addSingleValueArguments(toSingleValueArgumentProto(argName, singleValueArgumentEntry));
            } else if (argEntry instanceof TsRollingArgumentEntry rollingArgumentEntry) {
                stateProto.addRollingValueArguments(toRollingArgumentProto(argName, rollingArgumentEntry));
            }
        });
        return CalculatedFieldStateMsgProto.newBuilder()
                .setId(toProto(stateId))
                .setState(stateProto)
                .build();
    }

    protected TransportProtos.SingleValueArgumentProto toSingleValueArgumentProto(String argName, SingleValueArgumentEntry entry) {
        TransportProtos.SingleValueArgumentProto.Builder builder = TransportProtos.SingleValueArgumentProto.newBuilder()
                .setArgName(argName);
        if (entry != SingleValueArgumentEntry.EMPTY) {
            builder.setValue(KvProtoUtil.toTsValueProto(entry.getTs(), entry.getKvEntryValue()));
        }
        Optional.ofNullable(entry.getVersion()).ifPresent(builder::setVersion);
        return builder.build();
    }

    protected TransportProtos.TsValueListProto toRollingArgumentProto(String argName, TsRollingArgumentEntry entry) {
        TransportProtos.TsValueListProto.Builder builder = TransportProtos.TsValueListProto.newBuilder().setKey(argName);
        if (entry != TsRollingArgumentEntry.EMPTY) {
            entry.getTsRecords().forEach((ts, value) -> builder.addTsValue(KvProtoUtil.toTsValueProto(ts, value)));
        }
        return builder.build();
    }

    protected CalculatedFieldState fromProto(CalculatedFieldStateProto proto) {
        if (StringUtils.isEmpty(proto.getType())) {
            return null;
        }

        CalculatedFieldType type = CalculatedFieldType.valueOf(proto.getType());

        CalculatedFieldState state = switch (type) {
            case SIMPLE -> new SimpleCalculatedFieldState();
            case SCRIPT -> new ScriptCalculatedFieldState();
        };

        proto.getSingleValueArgumentsList().forEach(argProto ->
                state.getArguments().put(argProto.getArgName(), fromSingleValueArgumentProto(argProto)));

        if (CalculatedFieldType.SCRIPT.equals(type)) {
            proto.getRollingValueArgumentsList().forEach(argProto ->
                    state.getArguments().put(argProto.getKey(), fromRollingArgumentProto(argProto)));
        }

        return state;
    }

    protected SingleValueArgumentEntry fromSingleValueArgumentProto(TransportProtos.SingleValueArgumentProto proto) {
        if (!proto.hasValue()) {
            return (SingleValueArgumentEntry) SingleValueArgumentEntry.EMPTY;
        }
        TransportProtos.TsValueProto tsValueProto = proto.getValue();
        long ts = tsValueProto.getTs();
        BasicKvEntry kvEntry = (BasicKvEntry) KvProtoUtil.fromTsValueProto(proto.getArgName(), tsValueProto);
        return new SingleValueArgumentEntry(ts, kvEntry, proto.getVersion());
    }

    protected TsRollingArgumentEntry fromRollingArgumentProto(TransportProtos.TsValueListProto proto) {
        if (proto.getTsValueCount() <= 0) {
            return (TsRollingArgumentEntry) TsRollingArgumentEntry.EMPTY;
        }
        TreeMap<Long, BasicKvEntry> tsRecords = new TreeMap<>();
        proto.getTsValueList().forEach(tsValueProto -> {
            BasicKvEntry kvEntry = (BasicKvEntry) KvProtoUtil.fromTsValueProto(proto.getKey(), tsValueProto);
            tsRecords.put(tsValueProto.getTs(), kvEntry);
        });
        return new TsRollingArgumentEntry(tsRecords);
    }

}
