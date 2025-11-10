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

import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.exception.CalculatedFieldStateException;
import org.thingsboard.server.gen.transport.TransportProtos.ToCalculatedFieldMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.common.consumer.PartitionedQueueConsumerManager;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldState;

import java.util.Set;

public interface CalculatedFieldStateService {

    void init(PartitionedQueueConsumerManager<TbProtoQueueMsg<ToCalculatedFieldMsg>> eventConsumer);

    void persistState(CalculatedFieldEntityCtxId stateId, CalculatedFieldState state, TbCallback callback) throws CalculatedFieldStateException;

    void deleteState(CalculatedFieldEntityCtxId stateId, TbCallback callback);

    void restore(QueueKey queueKey, Set<TopicPartitionInfo> partitions);

    void delete(Set<TopicPartitionInfo> partitions);

    Set<TopicPartitionInfo> getPartitions();

    void stop();

}
