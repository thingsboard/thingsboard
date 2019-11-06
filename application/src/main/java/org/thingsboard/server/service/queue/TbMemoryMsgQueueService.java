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
package org.thingsboard.server.service.queue;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.msg.TbMsg;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@ConditionalOnProperty(prefix = "backpressure", value = "type", havingValue = "memory")
public class TbMemoryMsgQueueService extends TbAbstractMsgQueueService {

    @Value("${backpressure.pack_size}")
    private int msgPackSize;

    private Queue<TbMsg> msgQueue = new ConcurrentLinkedQueue<>();

    @PostConstruct
    private void init() {
        executor.submit(() -> {
            while (true) {
                if (isAck.get() && !msgQueue.isEmpty()) {
                    int currentMsgPackSize = Math.min(msgPackSize, msgQueue.size());

                    UUID packId = UUID.randomUUID();
                    currentPack = new TbMsgQueuePack(packId, new AtomicInteger(0), new AtomicInteger(0), new AtomicInteger(0), new AtomicBoolean(false));


                    for (int i = 0; i < currentMsgPackSize; i++) {
                        TbMsg msg = msgQueue.poll();
                        TbMsgQueueState msgQueueState = new TbMsgQueueState(
                                msg.copy(msg.getId(), packId, msg.getRuleChainId(), msg.getRuleNodeId(), msg.getClusterPartition()),
                                new AtomicInteger(0),
                                new AtomicBoolean(false));
                        currentPack.addMsg(msgQueueState);
                    }
                    send(currentPack);
                }
            }
        });
    }

    @Override
    public void add(TbMsg tbMsg) {
        if (tbMsg != null) {
            msgQueue.add(tbMsg);
        }
    }

    @PreDestroy
    private void destroy() {
        executor.shutdown();
    }
}
