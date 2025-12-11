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
package org.thingsboard.server.service.cf.ctx.state;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldStateProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToCalculatedFieldMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.common.consumer.PartitionedQueueConsumerManager;
import org.thingsboard.server.queue.common.state.DefaultQueueStateService;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.service.cf.AbstractCalculatedFieldStateService;
import org.thingsboard.server.service.cf.CfRocksDb;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnExpression("'${queue.type:null}'=='in-memory'")
public class RocksDBCalculatedFieldStateService extends AbstractCalculatedFieldStateService {

    private final CfRocksDb cfRocksDb;

    @Override
    public void init(PartitionedQueueConsumerManager<TbProtoQueueMsg<ToCalculatedFieldMsg>> eventConsumer) {
        super.stateService = new DefaultQueueStateService<>(eventConsumer);
    }

    @Override
    protected void doPersist(CalculatedFieldEntityCtxId stateId, CalculatedFieldStateProto stateMsgProto, TbCallback callback) {
        cfRocksDb.put(stateId.toKey(), stateMsgProto.toByteArray());
        callback.onSuccess();
    }

    @Override
    protected void doRemove(CalculatedFieldEntityCtxId stateId, TbCallback callback) {
        cfRocksDb.delete(stateId.toKey());
        callback.onSuccess();
    }

    @Override
    public void restore(QueueKey queueKey, Set<TopicPartitionInfo> partitions) {
        if (stateService.getPartitions().isEmpty()) {
            cfRocksDb.forEach((key, value) -> {
                CalculatedFieldStateProto stateMsg;
                try {
                    stateMsg = CalculatedFieldStateProto.parseFrom(value);
                } catch (InvalidProtocolBufferException e) {
                    log.error("Failed to parse CalculatedFieldStateProto for key {}", key, e);
                    return;
                }
                processRestoredState(stateMsg, new TbCallback() {
                    @Override
                    public void onSuccess() {}

                    @Override
                    public void onFailure(Throwable t) {
                        log.error("Failed to process CF state message: {}", stateMsg, t);
                    }
                });
            });
        }
        super.restore(queueKey, partitions);
    }

}
