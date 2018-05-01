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

import com.datastax.driver.core.utils.UUIDs;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.service.DaoNoSqlTest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@DaoNoSqlTest
public class CassandraProcessedPartitionRepositoryTest extends AbstractServiceTest {

    @Autowired
    private CassandraProcessedPartitionRepository partitionRepository;

    @Test
    public void lastProcessedPartitionCouldBeFound() {
        UUID nodeId = UUID.fromString("055eee50-1883-11e8-b380-65b5d5335ba9");
        Optional<Long> lastProcessedPartition = partitionRepository.findLastProcessedPartition(nodeId, 101L);
        assertTrue(lastProcessedPartition.isPresent());
        assertEquals((Long) 777L, lastProcessedPartition.get());
    }

    @Test
    public void highestProcessedPartitionReturned() throws ExecutionException, InterruptedException {
        UUID nodeId = UUIDs.timeBased();
        ListenableFuture<Void> future1 = partitionRepository.partitionProcessed(nodeId, 303L, 100L);
        ListenableFuture<Void> future2 = partitionRepository.partitionProcessed(nodeId, 303L, 200L);
        ListenableFuture<Void> future3 = partitionRepository.partitionProcessed(nodeId, 303L, 10L);
        ListenableFuture<List<Void>> allFutures = Futures.allAsList(future1, future2, future3);
        allFutures.get();
        Optional<Long> actual = partitionRepository.findLastProcessedPartition(nodeId, 303L);
        assertTrue(actual.isPresent());
        assertEquals((Long) 200L, actual.get());
    }

    @Test
    public void expiredPartitionsAreNotReturned() throws ExecutionException, InterruptedException {
        ReflectionTestUtils.setField(partitionRepository, "partitionsTtl", 1);
        UUID nodeId = UUIDs.timeBased();
        ListenableFuture<Void> future = partitionRepository.partitionProcessed(nodeId, 404L, 10L);
        future.get();
        Optional<Long> actual = partitionRepository.findLastProcessedPartition(nodeId, 404L);
        assertEquals((Long) 10L, actual.get());
        TimeUnit.SECONDS.sleep(2);
        assertFalse(partitionRepository.findLastProcessedPartition(nodeId, 404L).isPresent());
    }

    @Test
    public void ifNoPartitionsWereProcessedEmptyResultReturned() {
        UUID nodeId = UUIDs.timeBased();
        Optional<Long> actual = partitionRepository.findLastProcessedPartition(nodeId, 505L);
        assertFalse(actual.isPresent());
    }

}