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
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldEntityCtxIdProto;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldStateProto;
import org.thingsboard.server.gen.transport.TransportProtos.RollingArgumentProto;
import org.thingsboard.server.gen.transport.TransportProtos.SingleValueArgumentProto;
import org.thingsboard.server.gen.transport.TransportProtos.SingleValueProto;
import org.thingsboard.server.service.cf.RocksDBService;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldStateService;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@ConditionalOnExpression("'${service.type:null}'=='monolith'")
public class RocksDBStateService implements CalculatedFieldStateService {

    private final RocksDBService rocksDBService;

    @Override
    public Map<CalculatedFieldEntityCtxId, CalculatedFieldState> restoreStates() {
        return rocksDBService.getAll().entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> fromProto(entry.getKey()),
                        entry -> fromProto(entry.getValue())
                ));
    }

    @Override
    public void persistState(CalculatedFieldEntityCtxId stateId, CalculatedFieldState state, TbCallback callback) {
        rocksDBService.put(toProto(stateId), toProto(stateId, state));
        callback.onSuccess();
    }

    @Override
    public void removeState(CalculatedFieldEntityCtxId ctxId, TbCallback callback) {
        rocksDBService.delete(JacksonUtil.writeValueAsString(ctxId));
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
                .setType(state.getType().name())
                .addAllRequiredArguments(state.getRequiredArguments());

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
        SingleValueProto.Builder singleValueProtoBuilder = SingleValueProto.newBuilder()
                .setTs(entry.getTs());

        if (entry.getVersion() != null) {
            singleValueProtoBuilder.setVersion(entry.getVersion());
        }

        KvEntry value = entry.getKvEntryValue();
        if (value != null) {
            singleValueProtoBuilder.setHasV(true)
                    .setValue(ProtoUtils.toKeyValueProto(value));
        }

        return SingleValueArgumentProto.newBuilder()
                .setArgName(argName)
                .setValue(singleValueProtoBuilder.build())
                .build();
    }

    private RollingArgumentProto toRollingArgumentProto(String argName, TsRollingArgumentEntry entry) {
        RollingArgumentProto.Builder rollingArgumentProtoBuilder = RollingArgumentProto.newBuilder()
                .setArgName(argName);

        entry.getTsRecords().forEach((ts, value) -> {
            SingleValueProto.Builder singleValueProtoBuilder = SingleValueProto.newBuilder()
                    .setTs(ts);

            if (value != null) {
                singleValueProtoBuilder.setHasV(true)
                        .setValue(ProtoUtils.toKeyValueProto(value));
            }

            rollingArgumentProtoBuilder.addValues(singleValueProtoBuilder.build());
        });

        return rollingArgumentProtoBuilder.build();
    }

    private CalculatedFieldState fromProto(CalculatedFieldStateProto proto) {
        if (StringUtils.isEmpty(proto.getType())) {
            return null;
        }

        CalculatedFieldType type = CalculatedFieldType.valueOf(proto.getType());

        CalculatedFieldState state = switch (type) {
            case SIMPLE -> new SimpleCalculatedFieldState(proto.getRequiredArgumentsList());
            case SCRIPT -> new ScriptCalculatedFieldState(proto.getRequiredArgumentsList());
        };

        proto.getSingleValueArgumentsList().forEach(argProto ->
                state.getArguments().put(argProto.getArgName(), fromSingleValueArgumentProto(argProto)));

        if (CalculatedFieldType.SCRIPT.equals(type)) {
            proto.getRollingValueArgumentsList().forEach(argProto ->
                    state.getArguments().put(argProto.getArgName(), fromRollingArgumentProto(argProto)));
        }

        return state;
    }

    private SingleValueArgumentEntry fromSingleValueArgumentProto(SingleValueArgumentProto proto) {
        SingleValueProto valueProto = proto.getValue();
        BasicKvEntry value = valueProto.getHasV() ? ProtoUtils.fromProto(valueProto.getValue()) : null;

        return new SingleValueArgumentEntry(valueProto.getTs(), value, valueProto.getVersion());
    }

    private TsRollingArgumentEntry fromRollingArgumentProto(RollingArgumentProto proto) {
        TreeMap<Long, BasicKvEntry> tsRecords = new TreeMap<>();

        proto.getValuesList().forEach(singleValueProto -> {
            BasicKvEntry value = singleValueProto.getHasV() ? ProtoUtils.fromProto(singleValueProto.getValue()) : null;
            tsRecords.put(singleValueProto.getTs(), value);
        });

        return new TsRollingArgumentEntry(tsRecords);
    }

}
