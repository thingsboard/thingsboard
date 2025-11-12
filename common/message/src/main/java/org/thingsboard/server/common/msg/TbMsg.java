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
package org.thingsboard.server.common.msg;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.gen.MsgProtos;
import org.thingsboard.server.common.msg.gen.MsgProtos.TbMsgProto;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Data
@Slf4j
public final class TbMsg implements Serializable {

    public static final String EMPTY_JSON_OBJECT = "{}";
    public static final String EMPTY_JSON_ARRAY = "[]";
    public static final String EMPTY_STRING = "";

    private final String queueName;
    private final UUID id;
    private final long ts;
    private final String type;
    private final TbMsgType internalType;
    private final EntityId originator;
    private final CustomerId customerId;
    private final TbMsgMetaData metaData;
    private final TbMsgDataType dataType;
    private final String data;
    private final RuleChainId ruleChainId;
    private final RuleNodeId ruleNodeId;

    private final UUID correlationId;
    private final Integer partition;

    private final List<CalculatedFieldId> previousCalculatedFieldIds;

    @Getter(value = AccessLevel.NONE)
    @JsonIgnore
    //This field is not serialized because we use queues and there is no need to do it
    private final TbMsgProcessingCtx ctx;

    //This field is not serialized because we use queues and there is no need to do it
    @JsonIgnore
    transient private final TbMsgCallback callback;

    public static TbMsgBuilder newMsg() {
        return new TbMsgBuilder();
    }

    public TbMsgBuilder transform() {
        return new TbMsgTransformer(this);
    }

    public TbMsgBuilder copy() {
        return new TbMsgBuilder(this);
    }

    public TbMsg transform(String queueName) {
        return transform()
                .queueName(queueName)
                .resetRuleNodeId()
                .build();
    }

    // used for enqueueForTellNext
    public static TbMsg newMsg(TbMsg tbMsg, String queueName, RuleChainId ruleChainId, RuleNodeId ruleNodeId) {
        return tbMsg.transform()
                .id(UUID.randomUUID())
                .queueName(queueName)
                .metaData(tbMsg.getMetaData())
                .ruleChainId(ruleChainId)
                .ruleNodeId(ruleNodeId)
                .callback(TbMsgCallback.EMPTY)
                .build();
    }

    public TbMsg copyWithNewCtx() {
        return copy()
                .ctx(ctx.copy())
                .callback(TbMsgCallback.EMPTY)
                .build();
    }

    private TbMsg(String queueName, UUID id, long ts, TbMsgType internalType, String type, EntityId originator, CustomerId customerId, TbMsgMetaData metaData, TbMsgDataType dataType, String data,
                  RuleChainId ruleChainId, RuleNodeId ruleNodeId, UUID correlationId, Integer partition, List<CalculatedFieldId> previousCalculatedFieldIds, TbMsgProcessingCtx ctx, TbMsgCallback callback) {
        this.id = id != null ? id : UUID.randomUUID();
        this.queueName = queueName;
        if (ts > 0) {
            this.ts = ts;
        } else {
            this.ts = System.currentTimeMillis();
        }
        this.internalType = internalType != null ? internalType : getInternalType(type);
        this.type = type != null ? type : this.internalType.name();
        this.originator = originator;
        if (customerId == null || customerId.isNullUid()) {
            if (originator != null && originator.getEntityType() == EntityType.CUSTOMER) {
                this.customerId = new CustomerId(originator.getId());
            } else {
                this.customerId = null;
            }
        } else {
            this.customerId = customerId;
        }
        this.metaData = metaData;
        this.dataType = dataType != null ? dataType : TbMsgDataType.JSON;
        this.data = data;
        this.ruleChainId = ruleChainId;
        this.ruleNodeId = ruleNodeId;
        this.correlationId = correlationId;
        this.partition = partition;
        this.previousCalculatedFieldIds = previousCalculatedFieldIds != null
                ? new CopyOnWriteArrayList<>(previousCalculatedFieldIds)
                : new CopyOnWriteArrayList<>();
        this.ctx = ctx != null ? ctx : new TbMsgProcessingCtx();
        this.callback = Objects.requireNonNullElse(callback, TbMsgCallback.EMPTY);
    }

    public static TbMsgProto toProto(TbMsg msg) {
        TbMsgProto.Builder builder = TbMsgProto.newBuilder();
        builder.setId(msg.getId().toString());
        builder.setTs(msg.getTs());
        builder.setType(msg.getType());
        builder.setEntityType(msg.getOriginator().getEntityType().name());
        builder.setEntityIdMSB(msg.getOriginator().getId().getMostSignificantBits());
        builder.setEntityIdLSB(msg.getOriginator().getId().getLeastSignificantBits());

        if (msg.getCustomerId() != null) {
            builder.setCustomerIdMSB(msg.getCustomerId().getId().getMostSignificantBits());
            builder.setCustomerIdLSB(msg.getCustomerId().getId().getLeastSignificantBits());
        }

        if (msg.getRuleChainId() != null) {
            builder.setRuleChainIdMSB(msg.getRuleChainId().getId().getMostSignificantBits());
            builder.setRuleChainIdLSB(msg.getRuleChainId().getId().getLeastSignificantBits());
        }

        if (msg.getRuleNodeId() != null) {
            builder.setRuleNodeIdMSB(msg.getRuleNodeId().getId().getMostSignificantBits());
            builder.setRuleNodeIdLSB(msg.getRuleNodeId().getId().getLeastSignificantBits());
        }

        if (msg.getMetaData() != null) {
            builder.setMetaData(MsgProtos.TbMsgMetaDataProto.newBuilder().putAllData(msg.getMetaData().getData()).build());
        }

        builder.setDataType(msg.getDataType().ordinal());
        builder.setData(msg.getData());

        if (msg.getCorrelationId() != null) {
            builder.setCorrelationIdMSB(msg.getCorrelationId().getMostSignificantBits());
            builder.setCorrelationIdLSB(msg.getCorrelationId().getLeastSignificantBits());
        }
        if (msg.getPartition() != null) {
            builder.setPartition(msg.getPartition());
        }

        if (msg.getPreviousCalculatedFieldIds() != null) {
            for (CalculatedFieldId calculatedFieldId : msg.getPreviousCalculatedFieldIds()) {
                MsgProtos.CalculatedFieldIdProto calculatedFieldIdProto = MsgProtos.CalculatedFieldIdProto.newBuilder()
                        .setCalculatedFieldIdMSB(calculatedFieldId.getId().getMostSignificantBits())
                        .setCalculatedFieldIdLSB(calculatedFieldId.getId().getLeastSignificantBits())
                        .build();
                builder.addCalculatedFields(calculatedFieldIdProto);
            }
        }

        builder.setCtx(msg.ctx.toProto());
        return builder.build();
    }

    @Deprecated(forRemoval = true, since = "4.1") // to be removed in 4.2
    public static TbMsg fromProto(String queueName, TbMsgProto proto, ByteString data, TbMsgCallback callback) {
        try {
            if (!data.isEmpty()) {
                proto = TbMsgProto.parseFrom(data);
            }
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalStateException("Could not parse protobuf for TbMsg", e);
        }
        return fromProto(queueName, proto, callback);
    }

    public static TbMsg fromProto(String queueName, TbMsgProto proto, TbMsgCallback callback) {
        TbMsgMetaData metaData = new TbMsgMetaData(proto.getMetaData().getDataMap());
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(proto.getEntityType(), new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB()));
        CustomerId customerId = null;
        RuleChainId ruleChainId = null;
        RuleNodeId ruleNodeId = null;
        UUID correlationId = null;
        Integer partition = null;
        List<CalculatedFieldId> calculatedFieldIds = new CopyOnWriteArrayList<>();
        if (proto.getCustomerIdMSB() != 0L && proto.getCustomerIdLSB() != 0L) {
            customerId = new CustomerId(new UUID(proto.getCustomerIdMSB(), proto.getCustomerIdLSB()));
        }
        if (proto.getRuleChainIdMSB() != 0L && proto.getRuleChainIdLSB() != 0L) {
            ruleChainId = new RuleChainId(new UUID(proto.getRuleChainIdMSB(), proto.getRuleChainIdLSB()));
        }
        if (proto.getRuleNodeIdMSB() != 0L && proto.getRuleNodeIdLSB() != 0L) {
            ruleNodeId = new RuleNodeId(new UUID(proto.getRuleNodeIdMSB(), proto.getRuleNodeIdLSB()));
        }
        if (proto.getCorrelationIdMSB() != 0L && proto.getCorrelationIdLSB() != 0L) {
            correlationId = new UUID(proto.getCorrelationIdMSB(), proto.getCorrelationIdLSB());
            partition = proto.getPartition();
        }

        for (MsgProtos.CalculatedFieldIdProto cfIdProto : proto.getCalculatedFieldsList()) {
            CalculatedFieldId calculatedFieldId = new CalculatedFieldId(new UUID(
                    cfIdProto.getCalculatedFieldIdMSB(),
                    cfIdProto.getCalculatedFieldIdLSB()
            ));
            calculatedFieldIds.add(calculatedFieldId);
        }
        TbMsgProcessingCtx ctx = TbMsgProcessingCtx.fromProto(proto.getCtx());
        TbMsgDataType dataType = TbMsgDataType.values()[proto.getDataType()];
        return new TbMsg(queueName, UUID.fromString(proto.getId()), proto.getTs(), null, proto.getType(), entityId, customerId,
                metaData, dataType, proto.getData(), ruleChainId, ruleNodeId, correlationId, partition, calculatedFieldIds, ctx, callback);
    }

    public int getAndIncrementRuleNodeCounter() {
        return ctx.getAndIncrementRuleNodeCounter();
    }

    public TbMsgCallback getCallback() {
        // May be null in case of deserialization;
        return Objects.requireNonNullElse(callback, TbMsgCallback.EMPTY);
    }

    public void pushToStack(RuleChainId ruleChainId, RuleNodeId ruleNodeId) {
        ctx.push(ruleChainId, ruleNodeId);
    }

    public TbMsgProcessingStackItem popFormStack() {
        return ctx.pop();
    }

    /**
     * Checks if the message is still valid for processing. May be invalid if the message pack is timed-out or canceled.
     *
     * @return 'true' if message is valid for processing, 'false' otherwise.
     */
    public boolean isValid() {
        return getCallback().isMsgValid();
    }

    public long getMetaDataTs() {
        String tsStr = metaData.getValue("ts");
        if (!StringUtils.isEmpty(tsStr)) {
            try {
                return Long.parseLong(tsStr);
            } catch (NumberFormatException ignored) {
            }
        }
        return ts;
    }

    private TbMsgType getInternalType(String type) {
        if (type != null) {
            try {
                return TbMsgType.valueOf(type);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return TbMsgType.NA;
    }

    public boolean isTypeOf(TbMsgType tbMsgType) {
        return internalType.equals(tbMsgType);
    }

    public boolean isTypeOneOf(TbMsgType... types) {
        for (TbMsgType type : types) {
            if (isTypeOf(type)) {
                return true;
            }
        }
        return false;
    }

    public static class TbMsgTransformer extends TbMsgBuilder {

        TbMsgTransformer(TbMsg tbMsg) {
            super(tbMsg);
        }

        /*
         * metadata is only copied if specified explicitly during transform
         * */
        @Override
        public TbMsgTransformer metaData(TbMsgMetaData metaData) {
            this.metaData = metaData.copy();
            return this;
        }

        /*
         * setting ruleNodeId to null when updating ruleChainId
         * */
        @Override
        public TbMsgBuilder ruleChainId(RuleChainId ruleChainId) {
            this.ruleChainId = ruleChainId;
            this.ruleNodeId = null;
            return this;
        }

        @Override
        public TbMsg build() {
            /*
             * always copying ctx when transforming
             * */
            if (this.ctx != null) {
                this.ctx = this.ctx.copy();
            }
            return super.build();
        }

    }

    public static class TbMsgBuilder {

        protected String queueName;
        protected UUID id;
        protected long ts;
        protected String type;
        protected TbMsgType internalType;
        protected EntityId originator;
        protected CustomerId customerId;
        protected TbMsgMetaData metaData;
        protected TbMsgDataType dataType;
        protected String data;
        protected RuleChainId ruleChainId;
        protected RuleNodeId ruleNodeId;
        protected UUID correlationId;
        protected Integer partition;
        protected List<CalculatedFieldId> previousCalculatedFieldIds;
        protected TbMsgProcessingCtx ctx;
        protected TbMsgCallback callback;

        TbMsgBuilder() {}

        TbMsgBuilder(TbMsg tbMsg) {
            this.queueName = tbMsg.queueName;
            this.id = tbMsg.id;
            this.ts = tbMsg.ts;
            this.type = tbMsg.type;
            this.internalType = tbMsg.internalType;
            this.originator = tbMsg.originator;
            this.customerId = tbMsg.customerId;
            this.metaData = tbMsg.metaData;
            this.dataType = tbMsg.dataType;
            this.data = tbMsg.data;
            this.ruleChainId = tbMsg.ruleChainId;
            this.ruleNodeId = tbMsg.ruleNodeId;
            this.correlationId = tbMsg.correlationId;
            this.partition = tbMsg.partition;
            this.previousCalculatedFieldIds = tbMsg.previousCalculatedFieldIds;
            this.ctx = tbMsg.ctx;
            this.callback = tbMsg.callback;
        }

        public TbMsgBuilder queueName(String queueName) {
            this.queueName = queueName;
            return this;
        }

        public TbMsgBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public TbMsgBuilder ts(long ts) {
            this.ts = ts;
            return this;
        }

        /**
         * <p><strong>Deprecated:</strong> This should only be used when you need to specify a custom message type that doesn't exist in the {@link TbMsgType} enum.
         * Prefer using {@link #type(TbMsgType)} instead.
         */
        @Deprecated
        public TbMsgBuilder type(String type) {
            this.type = type;
            this.internalType = null;
            return this;
        }

        public TbMsgBuilder type(TbMsgType internalType) {
            this.internalType = internalType;
            this.type = internalType.name();
            return this;
        }

        public TbMsgBuilder originator(EntityId originator) {
            this.originator = originator;
            return this;
        }

        public TbMsgBuilder customerId(CustomerId customerId) {
            this.customerId = customerId;
            return this;
        }

        public TbMsgBuilder metaData(TbMsgMetaData metaData) {
            this.metaData = metaData;
            return this;
        }

        public TbMsgBuilder copyMetaData(TbMsgMetaData metaData) {
            this.metaData = metaData.copy();
            return this;
        }

        public TbMsgBuilder dataType(TbMsgDataType dataType) {
            this.dataType = dataType;
            return this;
        }

        public TbMsgBuilder data(String data) {
            this.data = data;
            return this;
        }

        public TbMsgBuilder ruleChainId(RuleChainId ruleChainId) {
            this.ruleChainId = ruleChainId;
            return this;
        }

        public TbMsgBuilder ruleNodeId(RuleNodeId ruleNodeId) {
            this.ruleNodeId = ruleNodeId;
            return this;
        }

        public TbMsgBuilder resetRuleNodeId() {
            return ruleNodeId(null);
        }

        public TbMsgBuilder correlationId(UUID correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public TbMsgBuilder partition(Integer partition) {
            this.partition = partition;
            return this;
        }

        public TbMsgBuilder previousCalculatedFieldIds(List<CalculatedFieldId> previousCalculatedFieldIds) {
            this.previousCalculatedFieldIds = new CopyOnWriteArrayList<>(previousCalculatedFieldIds);
            return this;
        }

        public TbMsgBuilder ctx(TbMsgProcessingCtx ctx) {
            this.ctx = ctx;
            return this;
        }

        public TbMsgBuilder callback(TbMsgCallback callback) {
            this.callback = callback;
            return this;
        }

        public TbMsg build() {
            return new TbMsg(queueName, id, ts, internalType, type, originator, customerId, metaData, dataType, data, ruleChainId, ruleNodeId, correlationId, partition, previousCalculatedFieldIds, ctx, callback);
        }

        public String toString() {
            return "TbMsg.TbMsgBuilder(queueName=" + this.queueName + ", id=" + this.id + ", ts=" + this.ts +
                    ", type=" + this.type + ", internalType=" + this.internalType + ", originator=" + this.originator +
                    ", customerId=" + this.customerId + ", metaData=" + this.metaData + ", dataType=" + this.dataType +
                    ", data=" + this.data + ", ruleChainId=" + this.ruleChainId + ", ruleNodeId=" + this.ruleNodeId +
                    ", correlationId=" + this.correlationId + ", partition=" + this.partition + ", previousCalculatedFields=" + this.previousCalculatedFieldIds +
                    ", ctx=" + this.ctx + ", callback=" + this.callback + ")";
        }

    }

}
