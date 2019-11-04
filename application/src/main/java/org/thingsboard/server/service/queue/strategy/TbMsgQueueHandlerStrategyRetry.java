/**
 * Copyright © 2016-2019 The Thingsboard Authors
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
package org.thingsboard.server.service.queue.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgPack;
import org.thingsboard.server.service.queue.TbAbstractMsgQueueService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "backpressure", value = "strategy", havingValue = "retry")
public class TbMsgQueueHandlerStrategyRetry implements TbMsgQueueHandlerStrategy {
    @Autowired
    private TbAbstractMsgQueueService msgQueueService;

    @Value("${backpressure.attempt}")
    private int attempt;

    @Override
    public void handleFailureMsgs(Map<UUID, TbMsg> msgMap) {
        if (msgQueueService.currentAttempt.get() < attempt) {
            msgQueueService.currentAttempt.incrementAndGet();
            List<TbMsg> msgs = new ArrayList<>(msgMap.size());
            msgMap.forEach((k, v) -> msgs.add(v));
            TbMsgPack pack = new TbMsgPack(UUID.randomUUID(), msgs);
            msgQueueService.send(pack);
        } else {
            msgMap.clear();
        }
    }
}
