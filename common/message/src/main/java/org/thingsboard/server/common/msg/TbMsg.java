/**
 * Copyright © 2016-2019 The Thingsboard Authors
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

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.msg.gen.MsgProtos;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Created by ashvayka on 13.01.18.
 */
@Data
@AllArgsConstructor
public final class TbMsg implements Serializable {

    private final UUID id;
    private final String type;
    private final EntityId originator;
    private final TbMsgMetaData metaData;
    private final TbMsgDataType dataType;
    private final String data;
    private final TbMsgTransactionData transactionData;

    //The following fields are not persisted to DB, because they can always be recovered from the context;
    private final RuleChainId ruleChainId;
    private final RuleNodeId ruleNodeId;
    private final long clusterPartition;

    public TbMsg(UUID id, String type, EntityId originator, TbMsgMetaData metaData, String data,
                 RuleChainId ruleChainId, RuleNodeId ruleNodeId, long clusterPartition) {
        this.id = id;
        this.type = type;
        this.originator = originator;
        this.metaData = metaData;
        this.data = data;
        this.dataType = TbMsgDataType.JSON;
        this.transactionData = new TbMsgTransactionData(id, originator);
        this.ruleChainId = ruleChainId;
        this.ruleNodeId = ruleNodeId;
        this.clusterPartition = clusterPartition;
    }

    public TbMsg(UUID id, String type, EntityId originator, TbMsgMetaData metaData, TbMsgDataType dataType, String data,
                 RuleChainId ruleChainId, RuleNodeId ruleNodeId, long clusterPartition) {
        this(id, type, originator, metaData, dataType, data, new TbMsgTransactionData(id, originator), ruleChainId, ruleNodeId, clusterPartition);
    }

    public static ByteBuffer toBytes(TbMsg msg) {
        MsgProtos.TbMsgProto.Builder builder = MsgProtos.TbMsgProto.newBuilder();
        builder.setId(msg.getId().toString());
        builder.setType(msg.getType());
        builder.setEntityType(msg.getOriginator().getEntityType().name());
        builder.setEntityIdMSB(msg.getOriginator().getId().getMostSignificantBits());
        builder.setEntityIdLSB(msg.getOriginator().getId().getLeastSignificantBits());

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

        TbMsgTransactionData transactionData = msg.getTransactionData();
        if (transactionData != null) {
            MsgProtos.TbMsgTransactionDataProto.Builder transactionBuilder = MsgProtos.TbMsgTransactionDataProto.newBuilder();
            transactionBuilder.setId(transactionData.getTransactionId().toString());
            transactionBuilder.setEntityType(transactionData.getOriginatorId().getEntityType().name());
            transactionBuilder.setEntityIdMSB(transactionData.getOriginatorId().getId().getMostSignificantBits());
            transactionBuilder.setEntityIdLSB(transactionData.getOriginatorId().getId().getLeastSignificantBits());
            builder.setTransactionData(transactionBuilder.build());
        }

        builder.setDataType(msg.getDataType().ordinal());
        builder.setData(msg.getData());
        byte[] bytes = builder.build().toByteArray();
        return ByteBuffer.wrap(bytes);
    }

    public static TbMsg fromBytes(ByteBuffer buffer) {
        try {
            MsgProtos.TbMsgProto proto = MsgProtos.TbMsgProto.parseFrom(buffer.array());
            TbMsgMetaData metaData = new TbMsgMetaData(proto.getMetaData().getDataMap());
            EntityId transactionEntityId = EntityIdFactory.getByTypeAndUuid(proto.getTransactionData().getEntityType(),
                    new UUID(proto.getTransactionData().getEntityIdMSB(), proto.getTransactionData().getEntityIdLSB()));
            TbMsgTransactionData transactionData = new TbMsgTransactionData(UUID.fromString(proto.getTransactionData().getId()), transactionEntityId);
            EntityId entityId = EntityIdFactory.getByTypeAndUuid(proto.getEntityType(), new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB()));
            RuleChainId ruleChainId = new RuleChainId(new UUID(proto.getRuleChainIdMSB(), proto.getRuleChainIdLSB()));
            RuleNodeId ruleNodeId = null;
            if(proto.getRuleNodeIdMSB() != 0L && proto.getRuleNodeIdLSB() != 0L) {
                 ruleNodeId = new RuleNodeId(new UUID(proto.getRuleNodeIdMSB(), proto.getRuleNodeIdLSB()));
            }
            TbMsgDataType dataType = TbMsgDataType.values()[proto.getDataType()];
            return new TbMsg(UUID.fromString(proto.getId()), proto.getType(), entityId, metaData, dataType, proto.getData(), transactionData, ruleChainId, ruleNodeId, proto.getClusterPartition());
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalStateException("Could not parse protobuf for TbMsg", e);
        }
    }

    public TbMsg copy(UUID newId, RuleChainId ruleChainId, RuleNodeId ruleNodeId, long clusterPartition) {
        return new TbMsg(newId, type, originator, metaData.copy(), dataType, data, transactionData, ruleChainId, ruleNodeId, clusterPartition);
    }

}
