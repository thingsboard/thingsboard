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
package org.thingsboard.server.dao.service.queue.cassandra;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.thingsboard.server.dao.service.queue.cassandra.repository.ProcessedPartitionRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class QueuePartitionerTest {

    private QueuePartitioner queuePartitioner;

    @Mock
    private ProcessedPartitionRepository partitionRepo;

    private Instant startInstant;
    private Instant endInstant;

    @Before
    public void init() {
        queuePartitioner = new QueuePartitioner("MINUTES", partitionRepo);
        startInstant = Instant.now();
        endInstant = startInstant.plus(2, ChronoUnit.MINUTES);
        queuePartitioner.setClock(Clock.fixed(endInstant, ZoneOffset.UTC));
    }

    @Test
    public void partitionCalculated() {
        long time = 1519390191425L;
        long partition = queuePartitioner.getPartition(time);
        assertEquals(1519390140000L, partition);
    }

    @Test
    public void unprocessedPartitionsReturned() {
        UUID nodeId = UUID.randomUUID();
        long clusteredHash = 101L;
        when(partitionRepo.findLastProcessedPartition(nodeId, clusteredHash)).thenReturn(Optional.of(startInstant.toEpochMilli()));
        List<Long> actual = queuePartitioner.findUnprocessedPartitions(nodeId, clusteredHash);
        assertEquals(3, actual.size());
    }

    @Test
    public void defaultShiftUsedIfNoPartitionWasProcessed() {
        UUID nodeId = UUID.randomUUID();
        long clusteredHash = 101L;
        when(partitionRepo.findLastProcessedPartition(nodeId, clusteredHash)).thenReturn(Optional.empty());
        List<Long> actual = queuePartitioner.findUnprocessedPartitions(nodeId, clusteredHash);
        assertEquals(10083, actual.size());
    }

}