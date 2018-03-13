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
package org.thingsboard.server.dao.service.queue.cassandra.repository.impl;

import com.datastax.driver.core.*;
import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.dao.nosql.CassandraAbstractDao;
import org.thingsboard.server.dao.service.queue.cassandra.repository.MsgRepository;
import org.thingsboard.server.dao.util.NoSqlDao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@NoSqlDao
public class CassandraMsgRepository extends CassandraAbstractDao implements MsgRepository {

    @Value("${cassandra.queue.msg.ttl}")
    private int msqQueueTtl;

    @Override
    public ListenableFuture<Void> save(TbMsg msg, UUID nodeId, long clusterPartition, long tsPartition, long msgTs) {
        String insert = "INSERT INTO msg_queue (node_id, cluster_partition, ts_partition, ts, msg) VALUES (?, ?, ?, ?, ?) USING TTL ?";
        PreparedStatement statement = prepare(insert);
        BoundStatement boundStatement = statement.bind(nodeId, clusterPartition, tsPartition, msgTs, TbMsg.toBytes(msg), msqQueueTtl);
        ResultSetFuture resultSetFuture = executeAsyncWrite(boundStatement);
        return Futures.transform(resultSetFuture, (Function<ResultSet, Void>) input -> null);
    }

    @Override
    public List<TbMsg> findMsgs(UUID nodeId, long clusterPartition, long tsPartition) {
        String select = "SELECT node_id, cluster_partition, ts_partition, ts, msg FROM msg_queue WHERE " +
                "node_id = ? AND cluster_partition = ? AND ts_partition = ?";
        PreparedStatement statement = prepare(select);
        BoundStatement boundStatement = statement.bind(nodeId, clusterPartition, tsPartition);
        ResultSet rows = executeRead(boundStatement);
        List<TbMsg> msgs = new ArrayList<>();
        for (Row row : rows) {
            msgs.add(TbMsg.fromBytes(row.getBytes("msg")));
        }
        return msgs;
    }

}
