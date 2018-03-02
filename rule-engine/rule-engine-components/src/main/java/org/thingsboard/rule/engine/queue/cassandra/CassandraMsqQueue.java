/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.rule.engine.queue.cassandra;

import com.datastax.driver.core.utils.UUIDs;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.rule.engine.api.MsqQueue;
import org.thingsboard.rule.engine.api.TbMsg;
import org.thingsboard.rule.engine.queue.cassandra.repository.AckRepository;
import org.thingsboard.rule.engine.queue.cassandra.repository.MsgRepository;
import org.thingsboard.server.common.data.UUIDConverter;

import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class CassandraMsqQueue implements MsqQueue {

    private final MsgRepository msgRepository;
    private final AckRepository ackRepository;
    private final UnprocessedMsgFilter unprocessedMsgFilter;
    private final QueuePartitioner queuePartitioner;

    public CassandraMsqQueue(MsgRepository msgRepository, AckRepository ackRepository,
                             UnprocessedMsgFilter unprocessedMsgFilter, QueuePartitioner queuePartitioner) {
        this.msgRepository = msgRepository;
        this.ackRepository = ackRepository;
        this.unprocessedMsgFilter = unprocessedMsgFilter;
        this.queuePartitioner = queuePartitioner;
    }


    @Override
    public ListenableFuture<Void> put(TbMsg msg, UUID nodeId, long clusteredHash) {
        long msgTime = getMsgTime(msg);
        long partition = queuePartitioner.getPartition(msgTime);
        return msgRepository.save(msg, nodeId, clusteredHash, partition, msgTime);
    }

    @Override
    public ListenableFuture<Void> ack(TbMsg msg, UUID nodeId, long clusteredHash) {
        long partition = queuePartitioner.getPartition(getMsgTime(msg));
        MsgAck ack = new MsgAck(msg.getId(), nodeId, clusteredHash, partition);
        return ackRepository.ack(ack);
    }

    @Override
    public Iterable<TbMsg> findUnprocessed(UUID nodeId, long clusteredHash) {
        List<TbMsg> unprocessedMsgs = Lists.newArrayList();
        for (Long partition : queuePartitioner.findUnprocessedPartitions(nodeId, clusteredHash)) {
            List<TbMsg> msgs = msgRepository.findMsgs(nodeId, clusteredHash, partition);
            List<MsgAck> acks = ackRepository.findAcks(nodeId, clusteredHash, partition);
            unprocessedMsgs.addAll(unprocessedMsgFilter.filter(msgs, acks));
        }
        return unprocessedMsgs;
    }

    private long getMsgTime(TbMsg msg) {
        return UUIDs.unixTimestamp(msg.getId());
    }

}
