/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.service.cf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.calculatedField.CalculatedFieldStateRestoreMsg;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.exception.CalculatedFieldStateException;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldStateProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToCalculatedFieldMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.common.state.QueueStateService;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldState;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static org.thingsboard.server.utils.CalculatedFieldUtils.fromProto;
import static org.thingsboard.server.utils.CalculatedFieldUtils.toProto;

public abstract class AbstractCalculatedFieldStateService implements CalculatedFieldStateService {

    @Autowired
    @Lazy
    private ActorSystemContext actorSystemContext;

    protected QueueStateService<TbProtoQueueMsg<ToCalculatedFieldMsg>, TbProtoQueueMsg<CalculatedFieldStateProto>> stateService;

    @Override
    public final void persistState(CalculatedFieldEntityCtxId stateId, CalculatedFieldState state, TbCallback callback) {
        if (state.isSizeExceedsLimit()) {
            throw new CalculatedFieldStateException("State size exceeds the maximum allowed limit. The state will not be persisted to RocksDB.");
        }
        doPersist(stateId, toProto(stateId, state), callback);
    }

    protected abstract void doPersist(CalculatedFieldEntityCtxId stateId, CalculatedFieldStateProto stateMsgProto, TbCallback callback);

    @Override
    public final void removeState(CalculatedFieldEntityCtxId stateId, TbCallback callback) {
        doRemove(stateId, callback);
    }

    protected abstract void doRemove(CalculatedFieldEntityCtxId stateId, TbCallback callback);

    protected void processRestoredState(CalculatedFieldStateProto stateMsg, TbCallback callback) {
        var id = fromProto(stateMsg.getId());
        var state = fromProto(stateMsg);
        processRestoredState(id, state, callback);
    }

    protected void processRestoredState(CalculatedFieldEntityCtxId id, CalculatedFieldState state, TbCallback callback) {
        actorSystemContext.tell(new CalculatedFieldStateRestoreMsg(id, state, callback));
    }

    @Override
    public void restore(QueueKey queueKey, Set<TopicPartitionInfo> partitions) {
        stateService.update(queueKey, partitions, null);
    }

    @Override
    public void delete(Set<TopicPartitionInfo> partitions) {
        stateService.delete(partitions);
    }

    @Override
    public Set<TopicPartitionInfo> getPartitions() {
        return stateService.getPartitions().values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
    }

    @Override
    public void stop() {
        stateService.stop();
    }

}
