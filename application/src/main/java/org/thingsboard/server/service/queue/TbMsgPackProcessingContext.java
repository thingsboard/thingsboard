/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import lombok.Getter;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.service.queue.processing.TbRuleEngineSubmitStrategy;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TbMsgPackProcessingContext {

    private final TbRuleEngineSubmitStrategy submitStrategy;

    private final AtomicInteger pendingCount;
    private final CountDownLatch processingTimeoutLatch = new CountDownLatch(1);
    @Getter
    private final ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> pendingMap;
    @Getter
    private final ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> successMap = new ConcurrentHashMap<>();
    @Getter
    private final ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> failedMap = new ConcurrentHashMap<>();
    @Getter
    private final ConcurrentMap<TenantId, RuleEngineException> exceptionsMap = new ConcurrentHashMap<>();

    public TbMsgPackProcessingContext(TbRuleEngineSubmitStrategy submitStrategy) {
        this.submitStrategy = submitStrategy;
        this.pendingMap = submitStrategy.getPendingMap();
        this.pendingCount = new AtomicInteger(pendingMap.size());
    }

    public boolean await(long packProcessingTimeout, TimeUnit milliseconds) throws InterruptedException {
        return processingTimeoutLatch.await(packProcessingTimeout, milliseconds);
    }

    public void onSuccess(UUID id) {
        TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg> msg;
        boolean empty = false;
        msg = pendingMap.remove(id);
        if (msg != null) {
            empty = pendingCount.decrementAndGet() == 0;
            successMap.put(id, msg);
            submitStrategy.onSuccess(id);
        }
        if (empty) {
            processingTimeoutLatch.countDown();
        }
    }

    public void onFailure(TenantId tenantId, UUID id, RuleEngineException e) {
        TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg> msg;
        boolean empty = false;
        msg = pendingMap.remove(id);
        if (msg != null) {
            empty = pendingCount.decrementAndGet() == 0;
            failedMap.put(id, msg);
            exceptionsMap.putIfAbsent(tenantId, e);
        }
        if (empty) {
            processingTimeoutLatch.countDown();
        }
    }
}
