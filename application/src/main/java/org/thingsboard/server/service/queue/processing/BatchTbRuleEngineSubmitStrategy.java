/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.service.queue.processing;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

@Slf4j
public class BatchTbRuleEngineSubmitStrategy extends AbstractTbRuleEngineSubmitStrategy {

    private final int batchSize;
    private final AtomicInteger packIdx = new AtomicInteger(0);
    private final Map<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> pendingPack = new LinkedHashMap<>();
    private volatile BiConsumer<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> msgConsumer;

    public BatchTbRuleEngineSubmitStrategy(String queueName, int batchSize) {
        super(queueName);
        this.batchSize = batchSize;
    }

    @Override
    public void submitAttempt(BiConsumer<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> msgConsumer) {
        this.msgConsumer = msgConsumer;
        submitNext();
    }

    @Override
    public void update(ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> reprocessMap) {
        super.update(reprocessMap);
        packIdx.set(0);
    }

    @Override
    protected void doOnSuccess(UUID id) {
        boolean endOfPendingPack;
        synchronized (pendingPack) {
            TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg> msg = pendingPack.remove(id);
            endOfPendingPack = msg != null && pendingPack.isEmpty();
        }
        if (endOfPendingPack) {
            packIdx.incrementAndGet();
            submitNext();
        }
    }

    private void submitNext() {
        int listSize = orderedMsgList.size();
        int startIdx = Math.min(packIdx.get() * batchSize, listSize);
        int endIdx = Math.min(startIdx + batchSize, listSize);
        Map<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> tmpPack;
        synchronized (pendingPack) {
            pendingPack.clear();
            for (int i = startIdx; i < endIdx; i++) {
                IdMsgPair pair = orderedMsgList.get(i);
                pendingPack.put(pair.uuid, pair.msg);
            }
            tmpPack = new LinkedHashMap<>(pendingPack);
        }
        int submitSize = pendingPack.size();
        if (log.isDebugEnabled() && submitSize > 0) {
            log.debug("[{}] submitting [{}] messages to rule engine", queueName, submitSize);
        }
        tmpPack.forEach(msgConsumer);
    }

}
