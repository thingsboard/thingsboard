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

import com.datastax.driver.core.utils.UUIDs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.thingsboard.rule.engine.api.TbMsgQueueService;
import org.thingsboard.server.actors.service.ActorService;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.queue.strategy.TbMsgQueueHandlerStrategy;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public abstract class TbAbstractMsgQueueService implements TbMsgQueueService {

    @Value("${backpressure.timeout}")
    private long timeout;

    @Value("${backpressure.attempt}")
    private int attempt;

    @Autowired
    private TbMsgQueueHandlerStrategy handlerStrategy;

    @Autowired
    @Lazy
    private ActorService actorService;

    protected Map<TenantId, TbMsgQueuePack> packMap = new ConcurrentHashMap<>();

    protected final Set<TenantId> specialTenants = ConcurrentHashMap.newKeySet();

    private CountDownLatch countDownLatch;

    protected final ExecutorService executor = Executors.newFixedThreadPool(specialTenants.size() + 1);

    private final ExecutorService sendExecutor = Executors.newSingleThreadExecutor();

    protected final Map<TenantId, AtomicBoolean> ackMap = new ConcurrentHashMap<>();

    protected final TenantId collectiveTenantId = new TenantId(UUIDs.random());

    protected final AtomicBoolean isAck = new AtomicBoolean(true);

    protected TbMsgQueuePack currentPack;

    @Override
    public void ack(UUID msgId, TenantId tenantId) {
        TenantId queueTenantId = specialTenants.contains(tenantId) ? tenantId : collectiveTenantId;

        TbMsgQueuePack pack = packMap.get(queueTenantId);
        if (pack != null && pack.getMsgs().containsKey(msgId)) {
            log.info("Ack msg: [{}] tenant: [{}]", msgId, tenantId);
            pack.ackMsg(msgId);
            if (pack.getAck().get()) {
                countDownLatch.countDown();
                ackMap.get(queueTenantId).set(true);
//            isAck.set(true);
            }
        }
    }

    public void send(final TbMsgQueuePack pack) {
        //sending
        log.info("Sending msgs [{}]", pack.getMsgs());
        sendExecutor.submit(() -> actorService.onMsgFromTbMsgQueue(pack));
//        actorService.onMsgFromTbMsgQueue(pack);
        try {
            countDownLatch = new CountDownLatch(1);
            countDownLatch.await(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        printStats(pack);
        if (pack.getAck().get()) {
           ackMap.get(pack.getTenantId()).set(true);
        } else {
            TbMsgQueuePack handlePack = handlerStrategy.handleFailureMsgs(pack);
            packMap.put(handlePack.getTenantId(), handlePack);
            retry(handlePack);
        }
    }

    private void retry(TbMsgQueuePack pack) {
        if (pack.getAck().get()) {
            ackMap.get(pack.getTenantId()).set(true);
        } else if (pack.getRetryAttempt().get() < attempt) {
            pack.incrementRetryAttempt();
            pack.getMsgs().forEach((id, msg) -> msg.incrementRetryAttempt());
            send(pack);
        } else {
            pack.getAck().set(true);
            ackMap.get(pack.getTenantId()).set(true);
        }
    }

    private void printStats(TbMsgQueuePack pack) {
            log.info("INFO about pack after sending - tenantId: [{}] packId: [{}] ack:[{}] totalCount:[{}] ackCount:[{}] retryAttempt:[{}]",
                    pack.getTenantId(),
                    pack.getPackId(),
                    pack.getAck().get(),
                    pack.getTotalCount().get(),
                    pack.getAckCount().get(),
                    pack.getRetryAttempt().get());
    }

    protected void destroy() {
        executor.shutdown();
        sendExecutor.shutdown();
    }
}
