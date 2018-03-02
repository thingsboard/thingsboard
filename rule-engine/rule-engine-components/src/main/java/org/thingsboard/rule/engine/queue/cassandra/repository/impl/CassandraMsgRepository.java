/**
 * Copyright © 2016-2017 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.rule.engine.queue.cassandra.repository.impl;

import com.datastax.driver.core.*;
import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.springframework.stereotype.Component;
import org.thingsboard.rule.engine.api.TbMsg;
import org.thingsboard.rule.engine.api.TbMsgMetaData;
import org.thingsboard.rule.engine.queue.cassandra.repository.MsgRepository;
import org.thingsboard.rule.engine.queue.cassandra.repository.gen.MsgQueueProtos;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class CassandraMsgRepository extends SimpleAbstractCassandraDao implements MsgRepository {

    private final int msqQueueTtl;


    public CassandraMsgRepository(Session session, int msqQueueTtl) {
        super(session);
        this.msqQueueTtl = msqQueueTtl;
    }

    @Override
    public ListenableFuture<Void> save(TbMsg msg, UUID nodeId, long clusteredHash, long partition, long msgTs) {
        String insert = "INSERT INTO msg_queue (node_id, clustered_hash, partition, ts, msg) VALUES (?, ?, ?, ?, ?) USING TTL ?";
        PreparedStatement statement = prepare(insert);
        BoundStatement boundStatement = statement.bind(nodeId, clusteredHash, partition, msgTs, toBytes(msg), msqQueueTtl);
        ResultSetFuture resultSetFuture = executeAsyncWrite(boundStatement);
        return Futures.transform(resultSetFuture, (Function<ResultSet, Void>) input -> null);
    }

    @Override
    public List<TbMsg> findMsgs(UUID nodeId, long clusteredHash, long partition) {
        String select = "SELECT node_id, clustered_hash, partition, ts, msg FROM msg_queue WHERE " +
                "node_id = ? AND clustered_hash = ? AND partition = ?";
        PreparedStatement statement = prepare(select);
        BoundStatement boundStatement = statement.bind(nodeId, clusteredHash, partition);
        ResultSet rows = executeRead(boundStatement);
        List<TbMsg> msgs = new ArrayList<>();
        for (Row row : rows) {
            msgs.add(fromBytes(row.getBytes("msg")));
        }
        return msgs;
    }

    private ByteBuffer toBytes(TbMsg msg) {
        MsgQueueProtos.TbMsgProto.Builder builder = MsgQueueProtos.TbMsgProto.newBuilder();
        builder.setId(msg.getId().toString());
        builder.setType(msg.getType());
        if (msg.getOriginator() != null) {
            builder.setEntityType(msg.getOriginator().getEntityType().name());
            builder.setEntityId(msg.getOriginator().getId().toString());
        }

        if (msg.getMetaData() != null) {
            MsgQueueProtos.TbMsgProto.TbMsgMetaDataProto.Builder metadataBuilder = MsgQueueProtos.TbMsgProto.TbMsgMetaDataProto.newBuilder();
            metadataBuilder.putAllData(msg.getMetaData().getData());
            builder.addMetaData(metadataBuilder.build());
        }

        builder.setData(ByteString.copyFrom(msg.getData()));
        byte[] bytes = builder.build().toByteArray();
        return ByteBuffer.wrap(bytes);
    }

    private TbMsg fromBytes(ByteBuffer buffer) {
        try {
            MsgQueueProtos.TbMsgProto proto = MsgQueueProtos.TbMsgProto.parseFrom(buffer.array());
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
