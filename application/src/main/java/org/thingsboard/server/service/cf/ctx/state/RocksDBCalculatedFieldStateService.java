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
package org.thingsboard.server.service.cf.ctx.state;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
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
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldEntityCtxIdProto;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldStateProto;
import org.thingsboard.server.gen.transport.TransportProtos.SingleValueArgumentProto;
import org.thingsboard.server.gen.transport.TransportProtos.TsValueListProto;
import org.thingsboard.server.gen.transport.TransportProtos.TsValueProto;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.service.cf.RocksDBService;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldStateService;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "zk", value = "enabled", havingValue = "false", matchIfMissing = true)
public class RocksDBCalculatedFieldStateService implements CalculatedFieldStateService {

    private final ActorSystemContext actorSystemContext;
    private final RocksDBService rocksDBService;

    public Map<CalculatedFieldEntityCtxId, CalculatedFieldState> restoreStates() {
        return rocksDBService.getAll().entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> fromProto(entry.getKey()),
                        entry -> fromProto(entry.getValue())
                ));
    }

    @AfterStartUp(order = AfterStartUp.CF_STATE_RESTORE_SERVICE)
    public void initCalculatedFieldStates() {
        restoreStates().forEach((k, v) -> actorSystemContext.tell(new CalculatedFieldStateRestoreMsg(k, v)));
    }


    @Override
    public void persistState(CalculatedFieldCtx ctx, CalculatedFieldEntityCtxId stateId, CalculatedFieldState state, TbCallback callback) {
        CalculatedFieldStateProto stateProto = toProto(stateId, state);
        long maxStateSizeInKBytes = ctx.getMaxStateSizeInKBytes();
        if (maxStateSizeInKBytes <= 0 || stateProto.getSerializedSize() <= ctx.getMaxStateSizeInKBytes()) {
            rocksDBService.put(toProto(stateId), stateProto);
        }
        callback.onSuccess();
    }

    @Override
    public void removeState(CalculatedFieldEntityCtxId ctxId, TbCallback callback) {
        rocksDBService.delete(toProto(ctxId));
        callback.onSuccess();
    }

    private CalculatedFieldEntityCtxIdProto toProto(CalculatedFieldEntityCtxId ctxId) {
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

    private CalculatedFieldEntityCtxId fromProto(CalculatedFieldEntityCtxIdProto ctxIdProto) {
        TenantId tenantId = TenantId.fromUUID(new UUID(ctxIdProto.getTenantIdMSB(), ctxIdProto.getTenantIdLSB()));
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(ctxIdProto.getEntityType(), new UUID(ctxIdProto.getEntityIdMSB(), ctxIdProto.getEntityIdLSB()));
        CalculatedFieldId calculatedFieldId = new CalculatedFieldId(new UUID(ctxIdProto.getCalculatedFieldIdMSB(), ctxIdProto.getCalculatedFieldIdLSB()));
        return new CalculatedFieldEntityCtxId(tenantId, calculatedFieldId, entityId);
    }

    private CalculatedFieldStateProto toProto(CalculatedFieldEntityCtxId stateId, CalculatedFieldState state) {
        CalculatedFieldStateProto.Builder builder = CalculatedFieldStateProto.newBuilder()
                .setId(toProto(stateId))
                .setType(state.getType().name());

        state.getArguments().forEach((argName, argEntry) -> {
            if (argEntry instanceof SingleValueArgumentEntry singleValueArgumentEntry) {
                builder.addSingleValueArguments(toSingleValueArgumentProto(argName, singleValueArgumentEntry));
            } else if (argEntry instanceof TsRollingArgumentEntry rollingArgumentEntry) {
                builder.addRollingValueArguments(toRollingArgumentProto(argName, rollingArgumentEntry));
            }
        });

        return builder.build();
    }

    private SingleValueArgumentProto toSingleValueArgumentProto(String argName, SingleValueArgumentEntry entry) {
        SingleValueArgumentProto.Builder builder = SingleValueArgumentProto.newBuilder()
                .setArgName(argName);

        if (entry != SingleValueArgumentEntry.EMPTY) {
            builder.setValue(KvProtoUtil.toTsValueProto(entry.getTs(), entry.getKvEntryValue()));
        }

        Optional.ofNullable(entry.getVersion()).ifPresent(builder::setVersion);

        return builder.build();
    }

    private TsValueListProto toRollingArgumentProto(String argName, TsRollingArgumentEntry entry) {
        TsValueListProto.Builder builder = TsValueListProto.newBuilder().setKey(argName);

        if (entry != TsRollingArgumentEntry.EMPTY) {
            entry.getTsRecords().forEach((ts, value) -> builder.addTsValue(KvProtoUtil.toTsValueProto(ts, value)));
        }

        return builder.build();
    }

    private CalculatedFieldState fromProto(CalculatedFieldStateProto proto) {
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

    private SingleValueArgumentEntry fromSingleValueArgumentProto(SingleValueArgumentProto proto) {
        if (!proto.hasValue()) {
            return (SingleValueArgumentEntry) SingleValueArgumentEntry.EMPTY;
        }
        TsValueProto tsValueProto = proto.getValue();
        long ts = tsValueProto.getTs();
        BasicKvEntry kvEntry = (BasicKvEntry) KvProtoUtil.fromTsValueProto(proto.getArgName(), tsValueProto);
        return new SingleValueArgumentEntry(ts, kvEntry, proto.getVersion());
    }

    private TsRollingArgumentEntry fromRollingArgumentProto(TsValueListProto proto) {
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
