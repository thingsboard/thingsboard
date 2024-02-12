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

import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.QueueKey;

import java.util.List;

public class TbRuleEngineInternalQueueConsumerManager extends TbRuleEngineQueueConsumerManager {

    public TbRuleEngineInternalQueueConsumerManager(TbRuleEngineConsumerContext ctx, QueueKey queueKey) {
        super(ctx, queueKey);
    }

    @Override
    protected void processMsgs(List<TbProtoQueueMsg<ToRuleEngineMsg>> msgs, TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineMsg>> consumer, Queue queue) throws InterruptedException {
        // TODO:  stats.log(result, decision.isCommit());
        //        ctx.isPrometheusStatsEnabled() ?
        //                new TbMsgPackCallback(id, tenantId, packCtx, stats.getTimer(tenantId, SUCCESSFUL_STATUS), stats.getTimer(tenantId, FAILED_STATUS)) :
        //                new TbMsgPackCallback(id, tenantId, packCtx)
    }

    @Override
    public void printStats(long ts) {
        stats.printStats();
        stats.reset();
    }

}
