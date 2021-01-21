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

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

@Slf4j
public class SequentialTbRuleEngineSubmitStrategy extends AbstractTbRuleEngineSubmitStrategy {

    private final AtomicInteger msgIdx = new AtomicInteger(0);
    private volatile BiConsumer<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> msgConsumer;
    private volatile UUID expectedMsgId;

    public SequentialTbRuleEngineSubmitStrategy(String queueName) {
        super(queueName);
    }

    @Override
    public void submitAttempt(BiConsumer<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> msgConsumer) {
        this.msgConsumer = msgConsumer;
        msgIdx.set(0);
        submitNext();
    }

    @Override
    public void update(ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> reprocessMap) {
        super.update(reprocessMap);
    }

    @Override
    protected void doOnSuccess(UUID id) {
        if (expectedMsgId.equals(id)) {
            msgIdx.incrementAndGet();
            submitNext();
        }
    }

    private void submitNext() {
        int listSize = orderedMsgList.size();
        int idx = msgIdx.get();
        if (idx < listSize) {
            IdMsgPair pair = orderedMsgList.get(idx);
            expectedMsgId = pair.uuid;
            if (log.isDebugEnabled()) {
                log.debug("[{}] submitting [{}] message to rule engine", queueName, pair.msg);
            }
            msgConsumer.accept(pair.uuid, pair.msg);
        }
    }

}
