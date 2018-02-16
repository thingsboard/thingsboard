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

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.rule.engine.api.MsqQueue;
import org.thingsboard.rule.engine.api.TbMsg;
import org.thingsboard.rule.engine.queue.cassandra.repository.AckRepository;
import org.thingsboard.rule.engine.queue.cassandra.repository.MsgRepository;
import org.thingsboard.rule.engine.queue.cassandra.repository.ProcessedPartitionRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CassandraMsqQueue implements MsqQueue {

    @Autowired
    private MsgRepository msgRepository;

    @Autowired
    private AckRepository ackRepository;

    @Autowired
    private AckBuilder ackBuilder;

    @Autowired
    private UnprocessedMsgFilter unprocessedMsgFilter;

    @Autowired
    private ProcessedPartitionRepository processedPartitionRepository;

    @Override
    public ListenableFuture<Void> put(TbMsg msg, UUID nodeId, long clusteredHash) {
        return msgRepository.save(msg, nodeId, clusteredHash, getPartition(msg));
    }

    @Override
    public ListenableFuture<Void> ack(TbMsg msg, UUID nodeId, long clusteredHash) {
        MsgAck ack = ackBuilder.build(msg, nodeId, clusteredHash);
        return ackRepository.ack(ack);
    }

    @Override
    public Iterable<TbMsg> findUnprocessed(UUID nodeId, long clusteredHash) {
        List<TbMsg> unprocessedMsgs = Lists.newArrayList();
        for (Long partition : findUnprocessedPartitions(nodeId, clusteredHash)) {
            Iterable<TbMsg> msgs = msgRepository.findMsgs(nodeId, clusteredHash, partition);
            Iterable<MsgAck> acks = ackRepository.findAcks(nodeId, clusteredHash, partition);
            unprocessedMsgs.addAll(unprocessedMsgFilter.filter(msgs, acks));
        }
        return unprocessedMsgs;
    }

    private List<Long> findUnprocessedPartitions(UUID nodeId, long clusteredHash) {
        Optional<Long> lastPartition = processedPartitionRepository.findLastProcessedPartition(nodeId, clusteredHash);
        return Collections.emptyList();
    }

    private long getPartition(TbMsg msg) {
        return Long.MIN_VALUE;
    }

}
