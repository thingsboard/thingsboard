// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
            IdMsgPair<TransportProtos.ToRuleEngineMsg> pair = orderedMsgList.get(idx);
            expectedMsgId = pair.uuid;
            if (log.isDebugEnabled()) {
                log.debug("[{}] submitting [{}] message to rule engine", queueName, pair.msg);
            }
            msgConsumer.accept(pair.uuid, pair.msg);
        }
    }

}
