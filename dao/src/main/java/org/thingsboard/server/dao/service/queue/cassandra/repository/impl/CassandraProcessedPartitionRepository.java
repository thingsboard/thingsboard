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
import org.thingsboard.server.dao.nosql.CassandraAbstractDao;
import org.thingsboard.server.dao.service.queue.cassandra.repository.ProcessedPartitionRepository;
import org.thingsboard.server.dao.util.NoSqlDao;

import java.util.Optional;
import java.util.UUID;

@Component
@NoSqlDao
public class CassandraProcessedPartitionRepository extends CassandraAbstractDao implements ProcessedPartitionRepository {

    @Value("${cassandra.queue.partitions.ttl}")
    private int partitionsTtl;

    @Override
    public ListenableFuture<Void> partitionProcessed(UUID nodeId, long clusterPartition, long tsPartition) {
        String insert = "INSERT INTO processed_msg_partitions (node_id, cluster_partition, ts_partition) VALUES (?, ?, ?) USING TTL ?";
        PreparedStatement prepared = prepare(insert);
        BoundStatement boundStatement = prepared.bind(nodeId, clusterPartition, tsPartition, partitionsTtl);
        ResultSetFuture resultSetFuture = executeAsyncWrite(boundStatement);
        return Futures.transform(resultSetFuture, (Function<ResultSet, Void>) input -> null);
    }

    @Override
    public Optional<Long> findLastProcessedPartition(UUID nodeId, long clusteredHash) {
        String select = "SELECT ts_partition FROM processed_msg_partitions WHERE " +
                "node_id = ? AND cluster_partition = ?";
        PreparedStatement prepared = prepare(select);
        BoundStatement boundStatement = prepared.bind(nodeId, clusteredHash);
        Row row = executeRead(boundStatement).one();
        if (row == null) {
            return Optional.empty();
        }

        return Optional.of(row.getLong("ts_partition"));
    }
}
