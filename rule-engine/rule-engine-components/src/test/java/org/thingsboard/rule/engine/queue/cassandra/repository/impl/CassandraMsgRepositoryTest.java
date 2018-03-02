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

//import static org.junit.jupiter.api.Assertions.*;

import com.datastax.driver.core.utils.UUIDs;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.rule.engine.api.TbMsg;
import org.thingsboard.rule.engine.api.TbMsgMetaData;
import org.thingsboard.server.common.data.id.DeviceId;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CassandraMsgRepositoryTest extends SimpleAbstractCassandraDaoTest {

    private CassandraMsgRepository msgRepository;

    @Before
    public void init() {
        msgRepository = new CassandraMsgRepository(cassandraUnit.session, 1);
    }

    @Test
    public void msgCanBeSavedAndRead() throws ExecutionException, InterruptedException {
        TbMsg msg = new TbMsg(UUIDs.timeBased(), "type", new DeviceId(UUIDs.timeBased()), null, new byte[4]);
        UUID nodeId = UUIDs.timeBased();
        ListenableFuture<Void> future = msgRepository.save(msg, nodeId, 1L, 1L, 1L);
        future.get();
        List<TbMsg> msgs = msgRepository.findMsgs(nodeId, 1L, 1L);
        assertEquals(1, msgs.size());
    }

    @Test
    public void expiredMsgsAreNotReturned() throws ExecutionException, InterruptedException {
        TbMsg msg = new TbMsg(UUIDs.timeBased(), "type", new DeviceId(UUIDs.timeBased()), null, new byte[4]);
        UUID nodeId = UUIDs.timeBased();
        ListenableFuture<Void> future = msgRepository.save(msg, nodeId, 2L, 2L, 2L);
        future.get();
        List<TbMsg> msgs = msgRepository.findMsgs(nodeId, 2L, 2L);
        assertEquals(1, msgs.size());
        TimeUnit.SECONDS.sleep(2);
        assertTrue(msgRepository.findMsgs(nodeId, 2L, 2L).isEmpty());
    }

    @Test
    public void protoBufConverterWorkAsExpected() throws ExecutionException, InterruptedException {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("key", "value");
        String dataStr = "someContent";
        TbMsg msg = new TbMsg(UUIDs.timeBased(), "type", new DeviceId(UUIDs.timeBased()), metaData, dataStr.getBytes());
        UUID nodeId = UUIDs.timeBased();
        ListenableFuture<Void> future = msgRepository.save(msg, nodeId, 1L, 1L, 1L);
        future.get();
        List<TbMsg> msgs = msgRepository.findMsgs(nodeId, 1L, 1L);
        assertEquals(1, msgs.size());
        assertEquals(msg, msgs.get(0));
    }


}