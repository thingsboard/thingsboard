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
package org.thingsboard.server.service.queue.memory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.service.queue.TbAbstractMsgQueueService;
import org.thingsboard.server.service.queue.TbMsgQueuePack;
import org.thingsboard.server.service.queue.TbMsgQueueState;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@ConditionalOnProperty(prefix = "backpressure", value = "type", havingValue = "memory")
public class TbMemoryMsgQueueService extends TbAbstractMsgQueueService {

    @Value("${backpressure.pack_size}")
    private int msgPackSize;

    private final Map<TenantId, Queue<TbMsgQueueState>> msgQueueMap = new ConcurrentHashMap<>();

//    private Queue<TbMsgQueueState> queue = new ConcurrentLinkedQueue<>();

//    @PostConstruct
//    private void init() {
//        executor.submit(() -> {
//            while (true) {
//                if (isAck.get() && !queue.isEmpty()) {
//                    isAck.set(false);
//                    int currentMsgPackSize = Math.min(msgPackSize, queue.size());
//
//                    UUID packId = UUID.randomUUID();
//                    currentPack = new TbMsgQueuePack(packId, new AtomicInteger(0), new AtomicInteger(0), new AtomicInteger(0), new AtomicBoolean(false), collectiveTenantId);
//
//                    for (int i = 0; i < currentMsgPackSize; i++) {
//                        TbMsgQueueState msgQueueState = queue.poll();
//                        currentPack.addMsg(msgQueueState);
//                    }
//                    send(currentPack);
//                }
//            }
//        });
//    }

    @Override
    public void add(TbMsg msg, TenantId tenantId) {
        log.info("Add new message: [{}] for tenant: [{}]", msg, tenantId.getId());
        TenantId queueTenantId = specialTenants.contains(tenantId) ? tenantId : collectiveTenantId;
        Queue<TbMsgQueueState> msgQueue = msgQueueMap.get(queueTenantId);

        if (msgQueue == null) {
            msgQueue = new ConcurrentLinkedQueue<>();
            msgQueueMap.put(queueTenantId, msgQueue);
            ackMap.put(queueTenantId, new AtomicBoolean(true));
            subscribeForNewTenant(queueTenantId);
        }

        TbMsgQueueState msgQueueState = new TbMsgQueueState(
                msg.copy(msg.getId(), msg.getTbMsgPackId(), msg.getRuleChainId(), msg.getRuleNodeId(), msg.getClusterPartition()),
                tenantId,
                new AtomicInteger(0),
                new AtomicBoolean(false));
        msgQueue.add(msgQueueState);
    }

    private void subscribeForNewTenant(TenantId tenantId) {
        executor.submit(() -> {
            Queue<TbMsgQueueState> queue = msgQueueMap.get(tenantId);
            AtomicBoolean ack = ackMap.get(tenantId);
            while (true) {
                if (ack.get() && !queue.isEmpty()) {
                    ack.set(false);
                    int currentMsgPackSize = Math.min(msgPackSize, msgQueueMap.get(tenantId).size());

                    UUID packId = UUID.randomUUID();
                    TbMsgQueuePack pack = new TbMsgQueuePack(
                            packId,
                            new AtomicInteger(0),
                            new AtomicInteger(0),
                            new AtomicInteger(0),
                            new AtomicBoolean(false),
                            tenantId);

                    for (int i = 0; i < currentMsgPackSize; i++) {
                        TbMsgQueueState msgQueueState = queue.poll();

                        pack.addMsg(msgQueueState);
                        packMap.put(tenantId, pack);
                    }
                    send(pack);
                }
            }
        });
    }

    @PreDestroy
    @Override
    protected void destroy() {
        super.destroy();
    }
}
