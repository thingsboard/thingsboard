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
import org.thingsboard.rule.engine.queue.cassandra.repository.ProcessedPartitionRepository;

import java.util.Optional;
import java.util.UUID;

@Component
public class CassandraProcessedPartitionRepository extends SimpleAbstractCassandraDao implements ProcessedPartitionRepository {

    private final int repositoryTtl;

    public CassandraProcessedPartitionRepository(Session session, int repositoryTtl) {
        super(session);
        this.repositoryTtl = repositoryTtl;
    }

    @Override
    public ListenableFuture<Void> partitionProcessed(UUID nodeId, long clusteredHash, long partition) {
        String insert = "INSERT INTO processed_msg_partitions (node_id, clustered_hash, partition) VALUES (?, ?, ?) USING TTL ?";
        PreparedStatement prepared = prepare(insert);
        BoundStatement boundStatement = prepared.bind(nodeId, clusteredHash, partition, repositoryTtl);
        ResultSetFuture resultSetFuture = executeAsyncWrite(boundStatement);
        return Futures.transform(resultSetFuture, (Function<ResultSet, Void>) input -> null);
    }

    @Override
    public Optional<Long> findLastProcessedPartition(UUID nodeId, long clusteredHash) {
        String select = "SELECT partition FROM processed_msg_partitions WHERE " +
                "node_id = ? AND clustered_hash = ?";
        PreparedStatement prepared = prepare(select);
        BoundStatement boundStatement = prepared.bind(nodeId, clusteredHash);
        Row row = executeRead(boundStatement).one();
        if (row == null) {
            return Optional.empty();
        }

        return Optional.of(row.getLong("partition"));
    }
}
