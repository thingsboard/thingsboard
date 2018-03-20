/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.msg.gen.MsgProtos;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Created by ashvayka on 13.01.18.
 */
@Data
public final class TbMsg implements Serializable, Cloneable {

    private final UUID id;
    private final String type;
    private final EntityId originator;
    private final TbMsgMetaData metaData;

    private final byte[] data;

    @Override
    public TbMsg clone() {
        return fromBytes(toBytes(this));
    }

    public static ByteBuffer toBytes(TbMsg msg) {
        MsgProtos.TbMsgProto.Builder builder = MsgProtos.TbMsgProto.newBuilder();
        builder.setId(msg.getId().toString());
        builder.setType(msg.getType());
        if (msg.getOriginator() != null) {
            builder.setEntityType(msg.getOriginator().getEntityType().name());
            builder.setEntityId(msg.getOriginator().getId().toString());
        }

        if (msg.getMetaData() != null) {
            MsgProtos.TbMsgProto.TbMsgMetaDataProto.Builder metadataBuilder = MsgProtos.TbMsgProto.TbMsgMetaDataProto.newBuilder();
            metadataBuilder.putAllData(msg.getMetaData().getData());
            builder.addMetaData(metadataBuilder.build());
        }

        builder.setData(ByteString.copyFrom(msg.getData()));
        byte[] bytes = builder.build().toByteArray();
        return ByteBuffer.wrap(bytes);
    }

    public static TbMsg fromBytes(ByteBuffer buffer) {
        try {
            MsgProtos.TbMsgProto proto = MsgProtos.TbMsgProto.parseFrom(buffer.array());
            TbMsgMetaData metaData = new TbMsgMetaData();
            if (proto.getMetaDataCount() > 0) {
                metaData.setData(proto.getMetaData(0).getDataMap());
            }

            EntityId entityId = null;
            if (proto.getEntityId() != null) {
                entityId = EntityIdFactory.getByTypeAndId(proto.getEntityType(), proto.getEntityId());
            }

            return new TbMsg(UUID.fromString(proto.getId()), proto.getType(), entityId, metaData, proto.getData().toByteArray());
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalStateException("Could not parse protobuf for TbMsg", e);
        }
    }

}
