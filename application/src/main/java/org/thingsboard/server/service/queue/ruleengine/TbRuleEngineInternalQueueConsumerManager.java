/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.queue.ruleengine;

import com.google.protobuf.ProtocolStringList;
import lombok.Builder;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.QueueToRuleEngineMsg;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.QueueKey;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

public class TbRuleEngineInternalQueueConsumerManager extends TbRuleEngineQueueConsumerManager {

    @Builder(builderClassName = "TbRuleEngineInternalQueueConsumerManagerBuilder", builderMethodName = "create")
    public TbRuleEngineInternalQueueConsumerManager(TbRuleEngineConsumerContext ctx,
                                                    QueueKey queueKey,
                                                    ExecutorService consumerExecutor,
                                                    ScheduledExecutorService scheduler,
                                                    ExecutorService taskExecutor) {
        super(ctx, queueKey, consumerExecutor, scheduler, taskExecutor);
    }

    public static class TbRuleEngineInternalQueueConsumerManagerBuilder extends TbRuleEngineQueueConsumerManager.TbRuleEngineQueueConsumerManagerBuilder {
        @Override
        public TbRuleEngineInternalQueueConsumerManager build() {
            return new TbRuleEngineInternalQueueConsumerManager(ctx, queueKey, consumerExecutor, scheduler, taskExecutor);
        }
    }

    @Override
    protected void processMsgs(List<TbProtoQueueMsg<ToRuleEngineMsg>> msgs, TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineMsg>> consumer, Queue queue) throws InterruptedException {
        // TODO:  stats.log(result, decision.isCommit());

        for (TbProtoQueueMsg<ToRuleEngineMsg> msg : msgs) {
            forwardToRuleEngineActor(msg.getValue());
        }
        consumer.commit();
    }

    private void forwardToRuleEngineActor(ToRuleEngineMsg toRuleEngineMsg) {
        TbMsg tbMsg = TbMsg.fromBytes(toRuleEngineMsg.getQueueName(), toRuleEngineMsg.getTbMsg().toByteArray(), TbMsgCallback.EMPTY);
        QueueToRuleEngineMsg msg;
        ProtocolStringList relationTypesList = toRuleEngineMsg.getRelationTypesList();
        Set<String> relationTypes;
        if (relationTypesList.size() == 1) {
            relationTypes = Collections.singleton(relationTypesList.get(0));
        } else {
            relationTypes = new HashSet<>(relationTypesList);
        }
        msg = new QueueToRuleEngineMsg(new TenantId(new UUID(toRuleEngineMsg.getTenantIdMSB(), toRuleEngineMsg.getTenantIdLSB())), tbMsg, relationTypes, toRuleEngineMsg.getFailureMessage());
        ctx.getActorContext().tell(msg);
    }

    @Override
    public void printStats(long ts) {
//        stats.printStats();
//        stats.reset();
    }

}
