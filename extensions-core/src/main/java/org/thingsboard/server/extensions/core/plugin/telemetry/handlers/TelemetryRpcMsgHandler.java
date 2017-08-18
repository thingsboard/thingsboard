/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.extensions.core.plugin.telemetry.handlers;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.kv.*;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.handlers.RpcMsgHandler;
import org.thingsboard.server.extensions.api.plugins.rpc.RpcMsg;
import org.thingsboard.server.extensions.core.plugin.telemetry.SubscriptionManager;
import org.thingsboard.server.extensions.core.plugin.telemetry.gen.TelemetryPluginProtos.*;
import org.thingsboard.server.extensions.core.plugin.telemetry.sub.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * @author Andrew Shvayka
 */
@Slf4j
@RequiredArgsConstructor
public class TelemetryRpcMsgHandler implements RpcMsgHandler {
    private final SubscriptionManager subscriptionManager;

    private static final int SUBSCRIPTION_CLAZZ = 1;
    private static final int ATTRIBUTES_UPDATE_CLAZZ = 2;
    private static final int TIMESERIES_UPDATE_CLAZZ = 3;
    private static final int SUBSCRIPTION_UPDATE_CLAZZ = 4;
    private static final int SESSION_CLOSE_CLAZZ = 5;
    private static final int SUBSCRIPTION_CLOSE_CLAZZ = 6;

    @Override
    public void process(PluginContext ctx, RpcMsg msg) {
        switch (msg.getMsgClazz()) {
            case SUBSCRIPTION_CLAZZ:
                processSubscriptionCmd(ctx, msg);
                break;
            case SUBSCRIPTION_UPDATE_CLAZZ:
                processRemoteSubscriptionUpdate(ctx, msg);
                break;
            case ATTRIBUTES_UPDATE_CLAZZ:
                processAttributeUpdate(ctx, msg);
                break;
            case TIMESERIES_UPDATE_CLAZZ:
                processTimeseriesUpdate(ctx, msg);
                break;
            case SESSION_CLOSE_CLAZZ:
                processSessionClose(ctx, msg);
                break;
            case SUBSCRIPTION_CLOSE_CLAZZ:
                processSubscriptionClose(ctx, msg);
                break;
            default:
                throw new RuntimeException("Unknown command id: " + msg.getMsgClazz());
        }
    }

    private void processRemoteSubscriptionUpdate(PluginContext ctx, RpcMsg msg) {
        SubscriptionUpdateProto proto;
        try {
            proto = SubscriptionUpdateProto.parseFrom(msg.getMsgData());
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        subscriptionManager.onRemoteSubscriptionUpdate(ctx, proto.getSessionId(), convert(proto));
    }

    private void processAttributeUpdate(PluginContext ctx, RpcMsg msg) {
        AttributeUpdateProto proto;
        try {
            proto = AttributeUpdateProto.parseFrom(msg.getMsgData());
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        subscriptionManager.onAttributesUpdateFromServer(ctx, EntityIdFactory.getByTypeAndId(proto.getEntityType(), proto.getEntityId()), proto.getScope(),
                proto.getDataList().stream().map(this::toAttribute).collect(Collectors.toList()));
    }

    private void processTimeseriesUpdate(PluginContext ctx, RpcMsg msg) {
        TimeseriesUpdateProto proto;
        try {
            proto = TimeseriesUpdateProto.parseFrom(msg.getMsgData());
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        subscriptionManager.onTimeseriesUpdateFromServer(ctx, EntityIdFactory.getByTypeAndId(proto.getEntityType(), proto.getEntityId()),
                proto.getDataList().stream().map(this::toTimeseries).collect(Collectors.toList()));
    }

    private void processSubscriptionCmd(PluginContext ctx, RpcMsg msg) {
        SubscriptionProto proto;
        try {
            proto = SubscriptionProto.parseFrom(msg.getMsgData());
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        Map<String, Long> statesMap = proto.getKeyStatesList().stream().collect(Collectors.toMap(SubscriptionKetStateProto::getKey, SubscriptionKetStateProto::getTs));
        Subscription subscription = new Subscription(
                new SubscriptionState(proto.getSessionId(), proto.getSubscriptionId(), EntityIdFactory.getByTypeAndId(proto.getEntityType(), proto.getEntityId()), SubscriptionType.valueOf(proto.getType()), proto.getAllKeys(), statesMap),
                false, msg.getServerAddress());
        subscriptionManager.addRemoteWsSubscription(ctx, msg.getServerAddress(), proto.getSessionId(), subscription);
    }

    public void onNewSubscription(PluginContext ctx, ServerAddress address, String sessionId, Subscription cmd) {
        SubscriptionProto.Builder builder = SubscriptionProto.newBuilder();
        builder.setSessionId(sessionId);
        builder.setSubscriptionId(cmd.getSubscriptionId());
        builder.setEntityType(cmd.getEntityId().getEntityType().name());
        builder.setEntityId(cmd.getEntityId().getId().toString());
        builder.setType(cmd.getType().name());
        builder.setAllKeys(cmd.isAllKeys());
        cmd.getKeyStates().entrySet().forEach(e -> builder.addKeyStates(SubscriptionKetStateProto.newBuilder().setKey(e.getKey()).setTs(e.getValue()).build()));
        ctx.sendPluginRpcMsg(new RpcMsg(address, SUBSCRIPTION_CLAZZ, builder.build().toByteArray()));
    }

    public void onSubscriptionUpdate(PluginContext ctx, ServerAddress address, String sessionId, SubscriptionUpdate update) {
        SubscriptionUpdateProto proto = getSubscriptionUpdateProto(sessionId, update);
        ctx.sendPluginRpcMsg(new RpcMsg(address, SUBSCRIPTION_UPDATE_CLAZZ, proto.toByteArray()));
    }

    public void onSessionClose(PluginContext ctx, ServerAddress address, String vSessionId) {
        SessionCloseProto proto = SessionCloseProto.newBuilder().setSessionId(vSessionId).build();
        ctx.sendPluginRpcMsg(new RpcMsg(address, SESSION_CLOSE_CLAZZ, proto.toByteArray()));
    }

    public void onSubscriptionClose(PluginContext ctx, ServerAddress address, String vSessionId, int subscriptionId) {
        SubscriptionCloseProto proto = SubscriptionCloseProto.newBuilder().setSessionId(vSessionId).setSubscriptionId(subscriptionId).build();
        ctx.sendPluginRpcMsg(new RpcMsg(address, SUBSCRIPTION_CLOSE_CLAZZ, proto.toByteArray()));
    }

    private void processSessionClose(PluginContext ctx, RpcMsg msg) {
        SessionCloseProto proto;
        try {
            proto = SessionCloseProto.parseFrom(msg.getMsgData());
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        subscriptionManager.cleanupRemoteWsSessionSubscriptions(ctx, proto.getSessionId());
    }

    private void processSubscriptionClose(PluginContext ctx, RpcMsg msg) {
        SubscriptionCloseProto proto;
        try {
            proto = SubscriptionCloseProto.parseFrom(msg.getMsgData());
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        subscriptionManager.removeSubscription(ctx, proto.getSessionId(), proto.getSubscriptionId());
    }

    private static SubscriptionUpdateProto getSubscriptionUpdateProto(String sessionId, SubscriptionUpdate update) {
        SubscriptionUpdateProto.Builder builder = SubscriptionUpdateProto.newBuilder();
        builder.setSessionId(sessionId);
        builder.setSubscriptionId(update.getSubscriptionId());
        builder.setErrorCode(update.getErrorCode());
        if (update.getErrorMsg() != null) {
            builder.setErrorMsg(update.getErrorMsg());
        }
        update.getData().entrySet().forEach(
                e -> {
                    SubscriptionUpdateValueListProto.Builder dataBuilder = SubscriptionUpdateValueListProto.newBuilder();

                    dataBuilder.setKey(e.getKey());
                    e.getValue().forEach(v -> {
                        Object[] array = (Object[]) v;
                        dataBuilder.addTs((long) array[0]);
                        dataBuilder.addValue((String) array[1]);
                    });

                    builder.addData(dataBuilder.build());
                }
        );
        return builder.build();
    }

    private SubscriptionUpdate convert(SubscriptionUpdateProto proto) {
        if (proto.getErrorCode() > 0) {
            return new SubscriptionUpdate(proto.getSubscriptionId(), SubscriptionErrorCode.forCode(proto.getErrorCode()), proto.getErrorMsg());
        } else {
            Map<String, List<Object>> data = new TreeMap<>();
            proto.getDataList().forEach(v -> {
                List<Object> values = data.computeIfAbsent(v.getKey(), k -> new ArrayList<>());
                for (int i = 0; i < v.getTsCount(); i++) {
                    Object[] value = new Object[2];
                    value[0] = v.getTs(i);
                    value[1] = v.getValue(i);
                    values.add(value);
                }
            });
            return new SubscriptionUpdate(proto.getSubscriptionId(), data);
        }
    }

    public void onAttributesUpdate(PluginContext ctx, ServerAddress address, EntityId entityId, String scope, List<AttributeKvEntry> attributes) {
        ctx.sendPluginRpcMsg(new RpcMsg(address, ATTRIBUTES_UPDATE_CLAZZ, getAttributesUpdateProto(entityId, scope, attributes).toByteArray()));
    }

    public void onTimeseriesUpdate(PluginContext ctx, ServerAddress address, EntityId entityId, List<TsKvEntry> tsKvEntries) {
        ctx.sendPluginRpcMsg(new RpcMsg(address, TIMESERIES_UPDATE_CLAZZ, getTimeseriesUpdateProto(entityId, tsKvEntries).toByteArray()));
    }

    private TimeseriesUpdateProto getTimeseriesUpdateProto(EntityId entityId, List<TsKvEntry> tsKvEntries) {
        TimeseriesUpdateProto.Builder builder = TimeseriesUpdateProto.newBuilder();
        builder.setEntityId(entityId.getId().toString());
        builder.setEntityType(entityId.getEntityType().name());
        tsKvEntries.forEach(attr -> builder.addData(toKeyValueProto(attr.getTs(), attr).build()));
        return builder.build();
    }

    private AttributeUpdateProto getAttributesUpdateProto(EntityId entityId, String scope, List<AttributeKvEntry> attributes) {
        AttributeUpdateProto.Builder builder = AttributeUpdateProto.newBuilder();
        builder.setEntityId(entityId.getId().toString());
        builder.setEntityType(entityId.getEntityType().name());
        builder.setScope(scope);
        attributes.forEach(attr -> builder.addData(toKeyValueProto(attr.getLastUpdateTs(), attr).build()));
        return builder.build();
    }

    private KeyValueProto.Builder toKeyValueProto(long ts, KvEntry attr) {
        KeyValueProto.Builder dataBuilder = KeyValueProto.newBuilder();
        dataBuilder.setKey(attr.getKey());
        dataBuilder.setTs(ts);
        dataBuilder.setValueType(attr.getDataType().ordinal());
        switch (attr.getDataType()) {
            case BOOLEAN:
                dataBuilder.setBoolValue(attr.getBooleanValue().get());
                break;
            case LONG:
                dataBuilder.setLongValue(attr.getLongValue().get());
                break;
            case DOUBLE:
                dataBuilder.setDoubleValue(attr.getDoubleValue().get());
                break;
            case STRING:
                dataBuilder.setStrValue(attr.getStrValue().get());
                break;
        }
        return dataBuilder;
    }

    private AttributeKvEntry toAttribute(KeyValueProto proto) {
        return new BaseAttributeKvEntry(getKvEntry(proto), proto.getTs());
    }

    private TsKvEntry toTimeseries(KeyValueProto proto) {
        return new BasicTsKvEntry(proto.getTs(), getKvEntry(proto));
    }

    private KvEntry getKvEntry(KeyValueProto proto) {
        KvEntry entry = null;
        DataType type = DataType.values()[proto.getValueType()];
        switch (type) {
            case BOOLEAN:
                entry = new BooleanDataEntry(proto.getKey(), proto.getBoolValue());
                break;
            case LONG:
                entry = new LongDataEntry(proto.getKey(), proto.getLongValue());
                break;
            case DOUBLE:
                entry = new DoubleDataEntry(proto.getKey(), proto.getDoubleValue());
                break;
            case STRING:
                entry = new StringDataEntry(proto.getKey(), proto.getStrValue());
                break;
        }
        return entry;
    }

}
