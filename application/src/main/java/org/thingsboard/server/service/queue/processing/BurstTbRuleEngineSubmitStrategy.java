// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.queue.processing;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import java.util.UUID;
import java.util.function.BiConsumer;

@Slf4j
public class BurstTbRuleEngineSubmitStrategy extends AbstractTbRuleEngineSubmitStrategy {

    public BurstTbRuleEngineSubmitStrategy(String queueName) {
        super(queueName);
    }

    @Override
    public void submitAttempt(BiConsumer<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> msgConsumer) {
        if (log.isDebugEnabled()) {
            log.debug("[{}] submitting [{}] messages to rule engine", queueName, orderedMsgList.size());
        }
        orderedMsgList.forEach(pair -> msgConsumer.accept(pair.uuid, pair.msg));
    }

    @Override
    protected void doOnSuccess(UUID id) {

    }
}
