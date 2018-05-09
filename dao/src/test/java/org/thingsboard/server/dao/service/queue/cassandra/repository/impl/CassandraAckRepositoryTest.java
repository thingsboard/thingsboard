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
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.service.DaoNoSqlTest;
import org.thingsboard.server.dao.service.queue.cassandra.MsgAck;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@DaoNoSqlTest
public class CassandraAckRepositoryTest extends AbstractServiceTest {

    @Autowired
    private CassandraAckRepository ackRepository;

    @Test
    public void acksInPartitionCouldBeFound() {
        UUID nodeId = UUID.fromString("055eee50-1883-11e8-b380-65b5d5335ba9");

        List<MsgAck> extectedAcks = Lists.newArrayList(
                new MsgAck(UUID.fromString("bebaeb60-1888-11e8-bf21-65b5d5335ba9"), nodeId, 101L, 300L),
                new MsgAck(UUID.fromString("12baeb60-1888-11e8-bf21-65b5d5335ba9"), nodeId, 101L, 300L)
        );

        List<MsgAck> actualAcks = ackRepository.findAcks(nodeId, 101L, 300L);
        assertEquals(extectedAcks, actualAcks);
    }

    @Test
    public void ackCanBeSavedAndRead() throws ExecutionException, InterruptedException {
        UUID msgId = UUIDs.timeBased();
        UUID nodeId = UUIDs.timeBased();
        MsgAck ack = new MsgAck(msgId, nodeId, 10L, 20L);
        ListenableFuture<Void> future = ackRepository.ack(ack);
        future.get();
        List<MsgAck> actualAcks = ackRepository.findAcks(nodeId, 10L, 20L);
        assertEquals(1, actualAcks.size());
        assertEquals(ack, actualAcks.get(0));
    }

    @Test
    public void expiredAcksAreNotReturned() throws ExecutionException, InterruptedException {
        ReflectionTestUtils.setField(ackRepository, "ackQueueTtl", 1);
        UUID msgId = UUIDs.timeBased();
        UUID nodeId = UUIDs.timeBased();
        MsgAck ack = new MsgAck(msgId, nodeId, 30L, 40L);
        ListenableFuture<Void> future = ackRepository.ack(ack);
        future.get();
        List<MsgAck> actualAcks = ackRepository.findAcks(nodeId, 30L, 40L);
        assertEquals(1, actualAcks.size());
        TimeUnit.SECONDS.sleep(2);
        assertTrue(ackRepository.findAcks(nodeId, 30L, 40L).isEmpty());
    }


}