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
package org.thingsboard.server.common.msg;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.gen.MsgProtos;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Created by ashvayka on 13.01.18.
 */
@Data
@Slf4j
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
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

    @Getter(value = AccessLevel.NONE)
    @JsonIgnore
    //This field is not serialized because we use queues and there is no need to do it
    private final TbMsgProcessingCtx ctx;

    //This field is not serialized because we use queues and there is no need to do it
    @JsonIgnore
    transient private final TbMsgCallback callback;

    public int getAndIncrementRuleNodeCounter() {
        return ctx.getAndIncrementRuleNodeCounter();
    }

    @Deprecated(since = "3.6.0", forRemoval = true)
    public static TbMsg newMsg(String queueName, String type, EntityId originator, TbMsgMetaData metaData, String data, RuleChainId ruleChainId, RuleNodeId ruleNodeId) {
        return newMsg(queueName, type, originator, null, metaData, data, ruleChainId, ruleNodeId);
    }

    /**
     * Creates a new TbMsg instance with the specified parameters.
     *
     * <p><strong>Deprecated:</strong> This method is deprecated since version 3.6.0 and should only be used when you need to
     * specify a custom message type that doesn't exist in the {@link TbMsgType} enum. For standard message types,
     * it is recommended to use the {@link #newMsg(String, TbMsgType, EntityId, CustomerId, TbMsgMetaData, String, RuleChainId, RuleNodeId)}
     * method instead.</p>
     *
     * @param queueName   the name of the queue where the message will be sent
     * @param type        the type of the message
     * @param originator  the originator of the message
     * @param customerId  the ID of the customer associated with the message
     * @param metaData    the metadata of the message
     * @param data        the data of the message
     * @param ruleChainId the ID of the rule chain associated with the message
     * @param ruleNodeId  the ID of the rule node associated with the message
     * @return new TbMsg instance
     */
    @Deprecated(since = "3.6.0")
    public static TbMsg newMsg(String queueName, String type, EntityId originator, CustomerId customerId, TbMsgMetaData metaData, String data, RuleChainId ruleChainId, RuleNodeId ruleNodeId) {
        return new TbMsg(queueName, UUID.randomUUID(), System.currentTimeMillis(), null, type, originator, customerId,
                metaData.copy(), TbMsgDataType.JSON, data, ruleChainId, ruleNodeId, null, TbMsgCallback.EMPTY);
    }

    @Deprecated(since = "3.6.0", forRemoval = true)
    public static TbMsg newMsg(String type, EntityId originator, TbMsgMetaData metaData, String data) {
        return newMsg(type, originator, null, metaData, data);
    }

    @Deprecated(since = "3.6.0", forRemoval = true)
    public static TbMsg newMsg(String type, EntityId originator, CustomerId customerId, TbMsgMetaData metaData, String data) {
        return new TbMsg(null, UUID.randomUUID(), System.currentTimeMillis(), null, type, originator, customerId,
                metaData.copy(), TbMsgDataType.JSON, data, null, null, null, TbMsgCallback.EMPTY);
    }

    public static TbMsg newMsg(String queueName, TbMsgType type, EntityId originator, TbMsgMetaData metaData, String data, RuleChainId ruleChainId, RuleNodeId ruleNodeId) {
        return newMsg(queueName, type, originator, null, metaData, data, ruleChainId, ruleNodeId);
    }

    public static TbMsg newMsg(String queueName, TbMsgType type, EntityId originator, CustomerId customerId, TbMsgMetaData metaData, String data, RuleChainId ruleChainId, RuleNodeId ruleNodeId) {
        return new TbMsg(queueName, UUID.randomUUID(), System.currentTimeMillis(), type, originator, customerId,
                metaData.copy(), TbMsgDataType.JSON, data, ruleChainId, ruleNodeId, null, TbMsgCallback.EMPTY);
    }

    public static TbMsg newMsg(TbMsgType type, EntityId originator, TbMsgMetaData metaData, String data) {
        return newMsg(type, originator, null, metaData, data);
    }

    public static TbMsg newMsg(TbMsgType type, EntityId originator, CustomerId customerId, TbMsgMetaData metaData, String data) {
        return new TbMsg(null, UUID.randomUUID(), System.currentTimeMillis(), type, originator, customerId,
                metaData.copy(), TbMsgDataType.JSON, data, null, null, null, TbMsgCallback.EMPTY);
    }

    public static TbMsg newMsg(TbMsgType type, EntityId originator, TbMsgMetaData metaData, String data, long ts) {
        return new TbMsg(null, UUID.randomUUID(), ts, type, originator, null,
                metaData.copy(), TbMsgDataType.JSON, data, null, null, null, TbMsgCallback.EMPTY);
    }

    // REALLY NEW MSG

    /**
     * Creates a new TbMsg instance with the specified parameters.
     *
     * <p><strong>Deprecated:</strong> This method is deprecated since version 3.6.0 and should only be used when you need to
     * specify a custom message type that doesn't exist in the {@link TbMsgType} enum. For standard message types,
     * it is recommended to use the {@link #newMsg(String, TbMsgType, EntityId, TbMsgMetaData, String)}
     * method instead.</p>
     *
     * @param queueName   the name of the queue where the message will be sent
     * @param type        the type of the message
     * @param originator  the originator of the message
     * @param metaData    the metadata of the message
     * @param data        the data of the message
     * @return new TbMsg instance
     */
    @Deprecated(since = "3.6.0")
    public static TbMsg newMsg(String queueName, String type, EntityId originator, TbMsgMetaData metaData, String data) {
        return newMsg(queueName, type, originator, null, metaData, data);
    }

    /**
     * Creates a new TbMsg instance with the specified parameters.
     *
     * <p><strong>Deprecated:</strong> This method is deprecated since version 3.6.0 and should only be used when you need to
     * specify a custom message type that doesn't exist in the {@link TbMsgType} enum. For standard message types,
     * it is recommended to use the {@link #newMsg(String, TbMsgType, EntityId, CustomerId, TbMsgMetaData, String)}
     * method instead.</p>
     *
     * @param queueName   the name of the queue where the message will be sent
     * @param type        the type of the message
     * @param originator  the originator of the message
     * @param customerId  the ID of the customer associated with the message
     * @param metaData    the metadata of the message
     * @param data        the data of the message
     * @return new TbMsg instance
     */
    @Deprecated(since = "3.6.0")
    public static TbMsg newMsg(String queueName, String type, EntityId originator, CustomerId customerId, TbMsgMetaData metaData, String data) {
        return new TbMsg(queueName, UUID.randomUUID(), System.currentTimeMillis(), null, type, originator, customerId,
                metaData.copy(), TbMsgDataType.JSON, data, null, null, null, TbMsgCallback.EMPTY);
    }

    @Deprecated(since = "3.6.0", forRemoval = true)
    public static TbMsg newMsg(String type, EntityId originator, CustomerId customerId, TbMsgMetaData metaData, TbMsgDataType dataType, String data) {
        return new TbMsg(null, UUID.randomUUID(), System.currentTimeMillis(), null, type, originator, customerId,
                metaData.copy(), dataType, data, null, null, null, TbMsgCallback.EMPTY);
    }

    /**
     * Creates a new TbMsg instance with the specified parameters.
     *
     * <p><strong>Deprecated:</strong> This method is deprecated since version 3.6.0 and should only be used when you need to
     * specify a custom message type that doesn't exist in the {@link TbMsgType} enum. For standard message types,
     * it is recommended to use the {@link #newMsg(TbMsgType, EntityId, TbMsgMetaData, TbMsgDataType, String)}
     * method instead.</p>
     *
     * @param type        the type of the message
     * @param originator  the originator of the message
     * @param metaData    the metadata of the message
     * @param dataType    the dataType of the message
     * @param data        the data of the message
     * @return new TbMsg instance
     */
    @Deprecated(since = "3.6.0")
    public static TbMsg newMsg(String type, EntityId originator, TbMsgMetaData metaData, TbMsgDataType dataType, String data) {
        return newMsg(type, originator, null, metaData, dataType, data);
    }

    public static TbMsg newMsg(String queueName, TbMsgType type, EntityId originator, TbMsgMetaData metaData, String data) {
        return newMsg(queueName, type, originator, null, metaData, data);
    }

    public static TbMsg newMsg(String queueName, TbMsgType type, EntityId originator, CustomerId customerId, TbMsgMetaData metaData, String data) {
        return new TbMsg(queueName, UUID.randomUUID(), System.currentTimeMillis(), type, originator, customerId,
                metaData.copy(), TbMsgDataType.JSON, data, null, null, null, TbMsgCallback.EMPTY);
    }

    public static TbMsg newMsg(TbMsgType type, EntityId originator, CustomerId customerId, TbMsgMetaData metaData, TbMsgDataType dataType, String data) {
        return new TbMsg(null, UUID.randomUUID(), System.currentTimeMillis(), type, originator, customerId,
                metaData.copy(), dataType, data, null, null, null, TbMsgCallback.EMPTY);
    }

    public static TbMsg newMsg(TbMsgType type, EntityId originator, TbMsgMetaData metaData, TbMsgDataType dataType, String data) {
        return newMsg(type, originator, null, metaData, dataType, data);
    }

    // For Tests only

    @Deprecated(since = "3.6.0", forRemoval = true)
    public static TbMsg newMsg(String type, EntityId originator, TbMsgMetaData metaData, TbMsgDataType dataType, String data, RuleChainId ruleChainId, RuleNodeId ruleNodeId) {
        return new TbMsg(null, UUID.randomUUID(), System.currentTimeMillis(), null, type, originator, null,
                metaData.copy(), dataType, data, ruleChainId, ruleNodeId, null, TbMsgCallback.EMPTY);
    }

    @Deprecated(since = "3.6.0", forRemoval = true)
    public static TbMsg newMsg(String type, EntityId originator, TbMsgMetaData metaData, String data, TbMsgCallback callback) {
        return new TbMsg(null, UUID.randomUUID(), System.currentTimeMillis(), null, type, originator, null,
                metaData.copy(), TbMsgDataType.JSON, data, null, null, null, callback);
    }

    /**
     * Transforms an existing TbMsg instance by changing its message type, originator, metadata, and data.
     *
     * <p><strong>Deprecated:</strong> This method is deprecated since version 3.6.0 and should only be used when you need to
     * specify a custom message type that doesn't exist in the {@link TbMsgType} enum. For standard message types,
     * it is recommended to use the {@link #transformMsg(TbMsg, TbMsgType, EntityId, TbMsgMetaData, String)}
     * method instead.</p>
     *
     *
     * @param tbMsg      the TbMsg instance to transform
     * @param type       the new message type
     * @param originator the new originator
     * @param metaData   the new metadata
     * @param data       the new data
     * @return the transformed TbMsg instance
     */
    @Deprecated(since = "3.6.0")
    public static TbMsg transformMsg(TbMsg tbMsg, String type, EntityId originator, TbMsgMetaData metaData, String data) {
        return new TbMsg(tbMsg.queueName, tbMsg.id, tbMsg.ts, null, type, originator, tbMsg.customerId, metaData.copy(), tbMsg.dataType,
                data, tbMsg.ruleChainId, tbMsg.ruleNodeId, tbMsg.correlationId, tbMsg.partition, tbMsg.ctx.copy(), tbMsg.callback);
    }

    public static TbMsg newMsg(TbMsgType type, EntityId originator, TbMsgMetaData metaData, TbMsgDataType dataType, String data, RuleChainId ruleChainId, RuleNodeId ruleNodeId) {
        return new TbMsg(null, UUID.randomUUID(), System.currentTimeMillis(), type, originator, null,
                metaData.copy(), dataType, data, ruleChainId, ruleNodeId, null, TbMsgCallback.EMPTY);
    }

    public static TbMsg newMsg(TbMsgType type, EntityId originator, TbMsgMetaData metaData, String data, TbMsgCallback callback) {
        return new TbMsg(null, UUID.randomUUID(), System.currentTimeMillis(), type, originator, null,
                metaData.copy(), TbMsgDataType.JSON, data, null, null, null, callback);
    }

    public static TbMsg transformMsg(TbMsg tbMsg, TbMsgType type, EntityId originator, TbMsgMetaData metaData, String data) {
        return new TbMsg(tbMsg.queueName, tbMsg.id, tbMsg.ts, type, type.name(), originator, tbMsg.customerId, metaData.copy(), tbMsg.dataType,
                data, tbMsg.ruleChainId, tbMsg.ruleNodeId, tbMsg.correlationId, tbMsg.partition, tbMsg.ctx.copy(), tbMsg.callback);
    }

    public static TbMsg transformMsgOriginator(TbMsg tbMsg, EntityId originatorId) {
        return new TbMsg(tbMsg.queueName, tbMsg.id, tbMsg.ts, tbMsg.internalType, tbMsg.type, originatorId, tbMsg.getCustomerId(), tbMsg.metaData, tbMsg.dataType,
                tbMsg.data, tbMsg.ruleChainId, tbMsg.ruleNodeId, tbMsg.correlationId, tbMsg.partition, tbMsg.ctx.copy(), tbMsg.getCallback());
    }

    public static TbMsg transformMsgData(TbMsg tbMsg, String data) {
        return new TbMsg(tbMsg.queueName, tbMsg.id, tbMsg.ts, tbMsg.internalType, tbMsg.type, tbMsg.originator, tbMsg.customerId, tbMsg.metaData, tbMsg.dataType,
                data, tbMsg.ruleChainId, tbMsg.ruleNodeId, tbMsg.correlationId, tbMsg.partition, tbMsg.ctx.copy(), tbMsg.getCallback());
    }

    public static TbMsg transformMsgMetadata(TbMsg tbMsg, TbMsgMetaData metadata) {
        return new TbMsg(tbMsg.queueName, tbMsg.id, tbMsg.ts, tbMsg.internalType, tbMsg.type, tbMsg.originator, tbMsg.customerId, metadata.copy(), tbMsg.dataType,
                tbMsg.data, tbMsg.ruleChainId, tbMsg.ruleNodeId, tbMsg.correlationId, tbMsg.partition, tbMsg.ctx.copy(), tbMsg.getCallback());
    }

    public static TbMsg transformMsg(TbMsg tbMsg, TbMsgMetaData metadata, String data) {
        return new TbMsg(tbMsg.queueName, tbMsg.id, tbMsg.ts, tbMsg.internalType, tbMsg.type, tbMsg.originator, tbMsg.customerId, metadata, tbMsg.dataType,
                data, tbMsg.ruleChainId, tbMsg.ruleNodeId, tbMsg.correlationId, tbMsg.partition, tbMsg.ctx.copy(), tbMsg.getCallback());
    }

    public static TbMsg transformMsgCustomerId(TbMsg tbMsg, CustomerId customerId) {
        return new TbMsg(tbMsg.queueName, tbMsg.id, tbMsg.ts, tbMsg.internalType, tbMsg.type, tbMsg.originator, customerId, tbMsg.metaData, tbMsg.dataType,
                tbMsg.data, tbMsg.ruleChainId, tbMsg.ruleNodeId, tbMsg.correlationId, tbMsg.partition, tbMsg.ctx.copy(), tbMsg.getCallback());
    }

    public static TbMsg transformMsgRuleChainId(TbMsg tbMsg, RuleChainId ruleChainId) {
        return new TbMsg(tbMsg.queueName, tbMsg.id, tbMsg.ts, tbMsg.internalType, tbMsg.type, tbMsg.originator, tbMsg.customerId, tbMsg.metaData, tbMsg.dataType,
                tbMsg.data, ruleChainId, null, tbMsg.correlationId, tbMsg.partition, tbMsg.ctx.copy(), tbMsg.getCallback());
    }

    public static TbMsg transformMsgQueueName(TbMsg tbMsg, String queueName) {
        return new TbMsg(queueName, tbMsg.id, tbMsg.ts, tbMsg.internalType, tbMsg.type, tbMsg.originator, tbMsg.customerId, tbMsg.metaData, tbMsg.dataType,
                tbMsg.data, tbMsg.getRuleChainId(), null, tbMsg.correlationId, tbMsg.partition, tbMsg.ctx.copy(), tbMsg.getCallback());
    }

    public static TbMsg transformMsg(TbMsg tbMsg, RuleChainId ruleChainId, String queueName) {
        return new TbMsg(queueName, tbMsg.id, tbMsg.ts, tbMsg.internalType, tbMsg.type, tbMsg.originator, tbMsg.customerId, tbMsg.metaData, tbMsg.dataType,
                tbMsg.data, ruleChainId, null, tbMsg.correlationId, tbMsg.partition, tbMsg.ctx.copy(), tbMsg.getCallback());
    }

    //used for enqueueForTellNext
    public static TbMsg newMsg(TbMsg tbMsg, String queueName, RuleChainId ruleChainId, RuleNodeId ruleNodeId) {
        return new TbMsg(queueName, UUID.randomUUID(), tbMsg.getTs(), tbMsg.getInternalType(), tbMsg.getType(), tbMsg.getOriginator(), tbMsg.customerId, tbMsg.getMetaData().copy(),
                tbMsg.getDataType(), tbMsg.getData(), ruleChainId, ruleNodeId, tbMsg.correlationId, tbMsg.partition, tbMsg.ctx.copy(), TbMsgCallback.EMPTY);
    }

    private TbMsg(String queueName, UUID id, long ts, TbMsgType internalType, EntityId originator, CustomerId customerId, TbMsgMetaData metaData, TbMsgDataType dataType, String data,
                  RuleChainId ruleChainId, RuleNodeId ruleNodeId, TbMsgProcessingCtx ctx, TbMsgCallback callback) {
        this(queueName, id, ts, internalType, internalType.name(), originator, customerId, metaData, dataType, data, ruleChainId, ruleNodeId, ctx, callback);
    }

    private TbMsg(String queueName, UUID id, long ts, TbMsgType internalType, String type, EntityId originator, CustomerId customerId, TbMsgMetaData metaData, TbMsgDataType dataType, String data,
                  RuleChainId ruleChainId, RuleNodeId ruleNodeId, TbMsgProcessingCtx ctx, TbMsgCallback callback) {
        this(queueName, id, ts, internalType, type, originator, customerId, metaData, dataType, data, ruleChainId, ruleNodeId, null, null, ctx, callback);
    }

    private TbMsg(String queueName, UUID id, long ts, TbMsgType internalType, String type, EntityId originator, CustomerId customerId, TbMsgMetaData metaData, TbMsgDataType dataType, String data,
                  RuleChainId ruleChainId, RuleNodeId ruleNodeId, UUID correlationId, Integer partition, TbMsgProcessingCtx ctx, TbMsgCallback callback) {
        this.id = id;
        this.queueName = queueName;
        if (ts > 0) {
            this.ts = ts;
        } else {
            this.ts = System.currentTimeMillis();
        }
        this.type = type;
        this.internalType = internalType != null ? internalType : getInternalType(type);
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
        this.dataType = dataType;
        this.data = data;
        this.ruleChainId = ruleChainId;
        this.ruleNodeId = ruleNodeId;
        this.correlationId = correlationId;
        this.partition = partition;
        this.ctx = ctx != null ? ctx : new TbMsgProcessingCtx();
        this.callback = Objects.requireNonNullElse(callback, TbMsgCallback.EMPTY);
    }

    public static ByteString toByteString(TbMsg msg) {
        return ByteString.copyFrom(toByteArray(msg));
    }

    public static byte[] toByteArray(TbMsg msg) {
        MsgProtos.TbMsgProto.Builder builder = MsgProtos.TbMsgProto.newBuilder();
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

        builder.setCtx(msg.ctx.toProto());
        return builder.build().toByteArray();
    }

    public static TbMsg fromBytes(String queueName, byte[] data, TbMsgCallback callback) {
        try {
            MsgProtos.TbMsgProto proto = MsgProtos.TbMsgProto.parseFrom(data);
            TbMsgMetaData metaData = new TbMsgMetaData(proto.getMetaData().getDataMap());
            EntityId entityId = EntityIdFactory.getByTypeAndUuid(proto.getEntityType(), new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB()));
            CustomerId customerId = null;
            RuleChainId ruleChainId = null;
            RuleNodeId ruleNodeId = null;
            UUID correlationId = null;
            Integer partition = null;
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

            TbMsgProcessingCtx ctx;
            if (proto.hasCtx()) {
                ctx = TbMsgProcessingCtx.fromProto(proto.getCtx());
            } else {
                // Backward compatibility with unprocessed messages fetched from queue after update.
                ctx = new TbMsgProcessingCtx(proto.getRuleNodeExecCounter());
            }

            TbMsgDataType dataType = TbMsgDataType.values()[proto.getDataType()];
            return new TbMsg(queueName, UUID.fromString(proto.getId()), proto.getTs(), null, proto.getType(), entityId, customerId,
                    metaData, dataType, proto.getData(), ruleChainId, ruleNodeId, correlationId, partition, ctx, callback);
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalStateException("Could not parse protobuf for TbMsg", e);
        }
    }

    public TbMsg copyWithRuleChainId(RuleChainId ruleChainId) {
        return copyWithRuleChainId(ruleChainId, this.id);
    }

    public TbMsg copyWithRuleChainId(RuleChainId ruleChainId, UUID msgId) {
        return new TbMsg(this.queueName, msgId, this.ts, this.internalType, this.type, this.originator, this.customerId,
                this.metaData, this.dataType, this.data, ruleChainId, null, this.correlationId, this.partition, this.ctx, callback);
    }

    public TbMsg copyWithRuleNodeId(RuleChainId ruleChainId, RuleNodeId ruleNodeId, UUID msgId) {
        return new TbMsg(this.queueName, msgId, this.ts, this.internalType, this.type, this.originator, this.customerId,
                this.metaData, this.dataType, this.data, ruleChainId, ruleNodeId, this.correlationId, this.partition, this.ctx, callback);
    }

    public TbMsg copyWithNewCtx() {
        return new TbMsg(this.queueName, this.id, this.ts, this.internalType, this.type, this.originator, this.customerId,
                this.metaData, this.dataType, this.data, ruleChainId, ruleNodeId, this.correlationId, this.partition, this.ctx.copy(), TbMsgCallback.EMPTY);
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
        try {
            return TbMsgType.valueOf(type);
        } catch (IllegalArgumentException e) {
            return TbMsgType.NA;
        }
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

}
