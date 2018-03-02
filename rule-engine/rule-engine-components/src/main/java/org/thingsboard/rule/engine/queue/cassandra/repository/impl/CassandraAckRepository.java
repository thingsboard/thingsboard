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
import org.springframework.stereotype.Component;
import org.thingsboard.rule.engine.queue.cassandra.MsgAck;
import org.thingsboard.rule.engine.queue.cassandra.repository.AckRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class CassandraAckRepository extends SimpleAbstractCassandraDao implements AckRepository {

    private final int ackQueueTtl;

    public CassandraAckRepository(Session session, int ackQueueTtl) {
        super(session);
        this.ackQueueTtl = ackQueueTtl;
    }

    @Override
    public ListenableFuture<Void> ack(MsgAck msgAck) {
        String insert = "INSERT INTO msg_ack_queue (node_id, clustered_hash, partition, msg_id) VALUES (?, ?, ?, ?) USING TTL ?";
        PreparedStatement statement = prepare(insert);
        BoundStatement boundStatement = statement.bind(msgAck.getNodeId(), msgAck.getClusteredHash(),
                msgAck.getPartition(), msgAck.getMsgId(), ackQueueTtl);
        ResultSetFuture resultSetFuture = executeAsyncWrite(boundStatement);
        return Futures.transform(resultSetFuture, (Function<ResultSet, Void>) input -> null);
    }

    @Override
    public List<MsgAck> findAcks(UUID nodeId, long clusteredHash, long partition) {
        String select = "SELECT msg_id FROM msg_ack_queue WHERE " +
                "node_id = ? AND clustered_hash = ? AND partition = ?";
        PreparedStatement statement = prepare(select);
        BoundStatement boundStatement = statement.bind(nodeId, clusteredHash, partition);
        ResultSet rows = executeRead(boundStatement);
        List<MsgAck> msgs = new ArrayList<>();
        for (Row row : rows) {
            msgs.add(new MsgAck(row.getUUID("msg_id"), nodeId, clusteredHash, partition));
        }
        return msgs;
    }

}
