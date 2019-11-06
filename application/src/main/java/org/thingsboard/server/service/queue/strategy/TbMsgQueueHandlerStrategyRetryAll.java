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

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.thingsboard.server.service.queue.TbMsgQueuePack;
import org.thingsboard.server.service.queue.TbMsgQueueState;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@ConditionalOnProperty(prefix = "backpressure", value = "strategy", havingValue = "retry_all")

public class TbMsgQueueHandlerStrategyRetryAll implements TbMsgQueueHandlerStrategy {
    @Override
    public TbMsgQueuePack handleFailureMsgs(TbMsgQueuePack msgQueuePack) {
        UUID packId = UUID.randomUUID();
        TbMsgQueuePack newPack = new TbMsgQueuePack(
                packId,
                new AtomicInteger(msgQueuePack.getRetryAttempt().get()),
                new AtomicInteger(0),
                new AtomicInteger(0),
                new AtomicBoolean(false));

        msgQueuePack.getMsgs().forEach((k, v) -> {
            TbMsgQueueState msg = v;
            msg.setAck(new AtomicBoolean(false));
            newPack.addMsg(msg);
        });
        return newPack;
    }
}
