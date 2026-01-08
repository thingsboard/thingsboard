/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.queue.processing;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;

@Slf4j
public abstract class SequentialByEntityIdTbRuleEngineSubmitStrategy extends AbstractTbRuleEngineSubmitStrategy {

    private volatile BiConsumer<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> msgConsumer;
    private volatile ConcurrentMap<UUID, EntityId> msgToEntityIdMap = new ConcurrentHashMap<>();
    private volatile ConcurrentMap<EntityId, Queue<IdMsgPair<TransportProtos.ToRuleEngineMsg>>> entityIdToListMap = new ConcurrentHashMap<>();

    public SequentialByEntityIdTbRuleEngineSubmitStrategy(String queueName) {
        super(queueName);
    }

    @Override
    public void init(List<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> msgs) {
        super.init(msgs);
        initMaps();
    }

    @Override
    public void submitAttempt(BiConsumer<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> msgConsumer) {
        this.msgConsumer = msgConsumer;
        entityIdToListMap.forEach((entityId, queue) -> {
            IdMsgPair<TransportProtos.ToRuleEngineMsg> msg = queue.peek();
            if (msg != null) {
                msgConsumer.accept(msg.uuid, msg.msg);
            }
        });
    }

    @Override
    public void update(ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> reprocessMap) {
        super.update(reprocessMap);
        initMaps();
    }

    @Override
    protected void doOnSuccess(UUID id) {
        EntityId entityId = msgToEntityIdMap.get(id);
        if (entityId != null) {
            Queue<IdMsgPair<TransportProtos.ToRuleEngineMsg>> queue = entityIdToListMap.get(entityId);
            if (queue != null) {
                IdMsgPair<TransportProtos.ToRuleEngineMsg> next = null;
                synchronized (queue) {
                    IdMsgPair<TransportProtos.ToRuleEngineMsg> expected = queue.peek();
                    if (expected != null && expected.uuid.equals(id)) {
                        queue.poll();
                        next = queue.peek();
                    }
                }
                if (next != null) {
                    msgConsumer.accept(next.uuid, next.msg);
                }
            }
        }
    }

    private void initMaps() {
        msgToEntityIdMap.clear();
        entityIdToListMap.clear();
        for (IdMsgPair<TransportProtos.ToRuleEngineMsg> pair : orderedMsgList) {
            EntityId entityId = getEntityId(pair.msg.getValue());
            if (entityId != null) {
                msgToEntityIdMap.put(pair.uuid, entityId);
                entityIdToListMap.computeIfAbsent(entityId, id -> new LinkedList<>()).add(pair);
            }
        }
    }

    protected abstract EntityId getEntityId(TransportProtos.ToRuleEngineMsg msg);

}
