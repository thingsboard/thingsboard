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

import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class TbMsgQueuePack {
    private final UUID packId;
    private final Map<UUID, TbMsgQueueState> msgs = new ConcurrentHashMap<>();
    private final Set<UUID> ackMsgIds = ConcurrentHashMap.newKeySet();

    private final AtomicInteger retryAttempt;
    private final AtomicInteger totalCount;
    private final AtomicInteger ackCount;
    private final AtomicBoolean ack;

    public TbMsgQueuePack(UUID packId, AtomicInteger retryAttempt, AtomicInteger totalCount, AtomicInteger ackCount, AtomicBoolean ack) {
        this.packId = packId;
        this.retryAttempt = retryAttempt;
        this.totalCount = totalCount;
        this.ackCount = ackCount;
        this.ack = ack;
    }

    public void addMsg(TbMsgQueueState msg) {
        msgs.put(msg.getMsg().getId(), msg);
        totalCount.set(msgs.size());
    }

    public void ackMsg(UUID msgId) {
        ackMsgIds.add(msgId);
        msgs.get(msgId).ack();
        ackCount.set(ackMsgIds.size());
        ack.set(totalCount.get() == ackCount.get());
    }

    public void incrementRetryAttempt() {
        retryAttempt.incrementAndGet();
    }
}
