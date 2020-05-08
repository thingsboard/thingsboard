/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.msg.gen.MsgProtos;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;

import java.io.IOException;
import java.io.Serializable;
import java.util.UUID;

/**
 * Created by ashvayka on 13.01.18.
 */
@Data
@Builder
@Slf4j
public final class TbMsg implements Serializable {

    private final UUID id;
    private final String type;
    private final EntityId originator;
    private final TbMsgMetaData metaData;
    private final TbMsgDataType dataType;
    private final String data;
    private final RuleChainId ruleChainId;
    private final RuleNodeId ruleNodeId;
    //This field is not serialized because we use queues and there is no need to do it
    transient private final TbMsgCallback callback;

    public static TbMsg newMsg(String type, EntityId originator, TbMsgMetaData metaData, String data) {
        return new TbMsg(UUID.randomUUID(), type, originator, metaData.copy(), TbMsgDataType.JSON, data, null, null, TbMsgCallback.EMPTY);
    }

    public static TbMsg newMsg(String type, EntityId originator, TbMsgMetaData metaData, String data, RuleChainId ruleChainId, RuleNodeId ruleNodeId) {
        return new TbMsg(UUID.randomUUID(), type, originator, metaData.copy(), TbMsgDataType.JSON, data, ruleChainId, ruleNodeId, TbMsgCallback.EMPTY);
    }

    public static TbMsg newMsg(String type, EntityId originator, TbMsgMetaData metaData, TbMsgDataType dataType, String data) {
        return new TbMsg(UUID.randomUUID(), type, originator, metaData.copy(), dataType, data, null, null, TbMsgCallback.EMPTY);
    }

    public static TbMsg newMsg(String type, EntityId originator, TbMsgMetaData metaData, TbMsgDataType dataType, String data, RuleChainId ruleChainId, RuleNodeId ruleNodeId) {
        return new TbMsg(UUID.randomUUID(), type, originator, metaData.copy(), dataType, data, ruleChainId, ruleNodeId, TbMsgCallback.EMPTY);
    }

    public static TbMsg newMsg(String type, EntityId originator, TbMsgMetaData metaData, String data, TbMsgCallback callback) {
        return new TbMsg(UUID.randomUUID(), type, originator, metaData.copy(), TbMsgDataType.JSON, data, null, null, callback);
    }

    public static TbMsg transformMsg(TbMsg origMsg, String type, EntityId originator, TbMsgMetaData metaData, String data) {
        return new TbMsg(origMsg.getId(), type, originator, metaData.copy(), origMsg.getDataType(),
                data, origMsg.getRuleChainId(), origMsg.getRuleNodeId(), origMsg.getCallback());
    }

    public static TbMsg newMsg(TbMsg tbMsg, RuleChainId ruleChainId, RuleNodeId ruleNodeId) {
        return new TbMsg(UUID.randomUUID(), tbMsg.getType(), tbMsg.getOriginator(), tbMsg.getMetaData().copy(),
                tbMsg.getDataType(), tbMsg.getData(), ruleChainId, ruleNodeId, TbMsgCallback.EMPTY);
    }

    private TbMsg(UUID id, String type, EntityId originator, TbMsgMetaData metaData, TbMsgDataType dataType, String data,
                  RuleChainId ruleChainId, RuleNodeId ruleNodeId, TbMsgCallback callback) {
        this.id = id;
        this.type = type;
        this.originator = originator;
        this.metaData = metaData;
        this.dataType = dataType;
        this.data = data;
        this.ruleChainId = ruleChainId;
        this.ruleNodeId = ruleNodeId;
        if (callback != null) {
            this.callback = callback;
        } else {
            log.warn("[{}] Created message with empty callback: {}", originator, type);
            this.callback = TbMsgCallback.EMPTY;
        }
    }

    public static ByteString toByteString(TbMsg msg) {
        return ByteString.copyFrom(toByteArray(msg));
    }

    public static byte[] toByteArray(TbMsg msg) {
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


        builder.setDataType(msg.getDataType().ordinal());
        builder.setData(msg.getData());
        return builder.build().toByteArray();
    }

    public static TbMsg fromBytes(byte[] data, TbMsgCallback callback) {
        try {
            MsgProtos.TbMsgProto proto = MsgProtos.TbMsgProto.parseFrom(data);
            TbMsgMetaData metaData = new TbMsgMetaData(proto.getMetaData().getDataMap());
            EntityId entityId = EntityIdFactory.getByTypeAndUuid(proto.getEntityType(), new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB()));
            RuleChainId ruleChainId = null;
            RuleNodeId ruleNodeId = null;
            if (proto.getRuleChainIdMSB() != 0L && proto.getRuleChainIdLSB() != 0L) {
                ruleChainId = new RuleChainId(new UUID(proto.getRuleChainIdMSB(), proto.getRuleChainIdLSB()));
            }
            if (proto.getRuleNodeIdMSB() != 0L && proto.getRuleNodeIdLSB() != 0L) {
                ruleNodeId = new RuleNodeId(new UUID(proto.getRuleNodeIdMSB(), proto.getRuleNodeIdLSB()));
            }
            TbMsgDataType dataType = TbMsgDataType.values()[proto.getDataType()];
            return new TbMsg(UUID.fromString(proto.getId()), proto.getType(), entityId, metaData, dataType, proto.getData(), ruleChainId, ruleNodeId, callback);
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalStateException("Could not parse protobuf for TbMsg", e);
        }
    }

    public TbMsg copyWithRuleChainId(RuleChainId ruleChainId) {
        return new TbMsg(this.id, this.type, this.originator, this.metaData, this.dataType, this.data, ruleChainId, null, callback);
    }

    public TbMsg copyWithRuleNodeId(RuleChainId ruleChainId, RuleNodeId ruleNodeId) {
        return new TbMsg(this.id, this.type, this.originator, this.metaData, this.dataType, this.data, ruleChainId, ruleNodeId, callback);
    }

    public TbMsgCallback getCallback() {
        //May be null in case of deserialization;
        if (callback != null) {
            return callback;
        } else {
            return TbMsgCallback.EMPTY;
        }
    }
}
