// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cf.ctx.state;

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
                } catch (Exception e) {
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
