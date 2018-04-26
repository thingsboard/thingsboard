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

import com.datastax.driver.core.utils.UUIDs;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.dao.queue.MsgQueue;
import org.thingsboard.server.dao.service.queue.cassandra.repository.AckRepository;
import org.thingsboard.server.dao.service.queue.cassandra.repository.MsgRepository;
import org.thingsboard.server.dao.util.NoSqlDao;

import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@NoSqlDao
public class CassandraMsgQueue implements MsgQueue {

    @Autowired
    private MsgRepository msgRepository;
    @Autowired
    private AckRepository ackRepository;
    @Autowired
    private UnprocessedMsgFilter unprocessedMsgFilter;
    @Autowired
    private QueuePartitioner queuePartitioner;

    @Override
    public ListenableFuture<Void> put(TbMsg msg, UUID nodeId, long clusterPartition) {
        long msgTime = getMsgTime(msg);
        long tsPartition = queuePartitioner.getPartition(msgTime);
        return msgRepository.save(msg, nodeId, clusterPartition, tsPartition, msgTime);
    }

    @Override
    public ListenableFuture<Void> ack(TbMsg msg, UUID nodeId, long clusterPartition) {
        long tsPartition = queuePartitioner.getPartition(getMsgTime(msg));
        MsgAck ack = new MsgAck(msg.getId(), nodeId, clusterPartition, tsPartition);
        return ackRepository.ack(ack);
    }

    @Override
    public Iterable<TbMsg> findUnprocessed(UUID nodeId, long clusterPartition) {
        List<TbMsg> unprocessedMsgs = Lists.newArrayList();
        for (Long tsPartition : queuePartitioner.findUnprocessedPartitions(nodeId, clusterPartition)) {
            List<TbMsg> msgs = msgRepository.findMsgs(nodeId, clusterPartition, tsPartition);
            List<MsgAck> acks = ackRepository.findAcks(nodeId, clusterPartition, tsPartition);
            unprocessedMsgs.addAll(unprocessedMsgFilter.filter(msgs, acks));
        }
        return unprocessedMsgs;
    }

    private long getMsgTime(TbMsg msg) {
        return UUIDs.unixTimestamp(msg.getId());
    }

}
