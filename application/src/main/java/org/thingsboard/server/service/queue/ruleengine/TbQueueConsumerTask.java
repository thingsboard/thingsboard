/**
 * Copyright © 2016-2023 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.queue.ruleengine;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.QueueKey;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Data
@Slf4j
public class TbQueueConsumerTask {

    private final QueueKey key;
    private final Object id;
    private final TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> consumer;
    private volatile Future<?> task;

    public void stop() {
        this.consumer.stop();
    }

    public boolean stopAndAwait() {
        this.consumer.stop();
        return await();
    }

    public boolean await() {
        if (task != null) {
            //TODO: maybe task.cancel() to interrupt the consumer?
            try {
                this.task.get(3, TimeUnit.MINUTES);
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                log.warn("[{}][{}] Failed to await for consumer to stop", key, id, e);
                return false;
            }
        }
        return true;
    }

    public void subscribe(Set<TopicPartitionInfo> partitions) {
        this.consumer.subscribe(partitions);
    }


    public void unsubscribe() {
        this.consumer.unsubscribe();
    }
}
