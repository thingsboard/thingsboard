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

//import static org.junit.jupiter.api.Assertions.*;

import com.datastax.driver.core.utils.UUIDs;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.service.DaoNoSqlTest;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@DaoNoSqlTest
public class CassandraMsgRepositoryTest extends AbstractServiceTest {

    @Autowired
    private CassandraMsgRepository msgRepository;

    @Test
    public void msgCanBeSavedAndRead() throws ExecutionException, InterruptedException {
        TbMsg msg = new TbMsg(UUIDs.timeBased(), "type", new DeviceId(UUIDs.timeBased()), null, TbMsgDataType.JSON, "0000",
                new RuleChainId(UUIDs.timeBased()), new RuleNodeId(UUIDs.timeBased()), 0L);
        UUID nodeId = UUIDs.timeBased();
        ListenableFuture<Void> future = msgRepository.save(msg, nodeId, 1L, 1L, 1L);
        future.get();
        List<TbMsg> msgs = msgRepository.findMsgs(nodeId, 1L, 1L);
        assertEquals(1, msgs.size());
    }

    @Test
    public void expiredMsgsAreNotReturned() throws ExecutionException, InterruptedException {
        ReflectionTestUtils.setField(msgRepository, "msqQueueTtl", 1);
        TbMsg msg = new TbMsg(UUIDs.timeBased(), "type", new DeviceId(UUIDs.timeBased()), null, TbMsgDataType.JSON, "0000",
                new RuleChainId(UUIDs.timeBased()), new RuleNodeId(UUIDs.timeBased()), 0L);
        UUID nodeId = UUIDs.timeBased();
        ListenableFuture<Void> future = msgRepository.save(msg, nodeId, 2L, 2L, 2L);
        future.get();
        TimeUnit.SECONDS.sleep(2);
        assertTrue(msgRepository.findMsgs(nodeId, 2L, 2L).isEmpty());
    }

    @Test
    public void protoBufConverterWorkAsExpected() throws ExecutionException, InterruptedException {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("key", "value");
        String dataStr = "someContent";
        TbMsg msg = new TbMsg(UUIDs.timeBased(), "type", new DeviceId(UUIDs.timeBased()), metaData, TbMsgDataType.JSON, dataStr,
                new RuleChainId(UUIDs.timeBased()), new RuleNodeId(UUIDs.timeBased()), 0L);
        UUID nodeId = UUIDs.timeBased();
        ListenableFuture<Void> future = msgRepository.save(msg, nodeId, 1L, 1L, 1L);
        future.get();
        List<TbMsg> msgs = msgRepository.findMsgs(nodeId, 1L, 1L);
        assertEquals(1, msgs.size());
        assertEquals(msg, msgs.get(0));
    }


}