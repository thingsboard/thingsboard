/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.service.queue.ruleengine;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Data
@Slf4j
public class TbQueueConsumerTask {

    private final Object key;
    private final TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> consumer;
    private volatile Future<?> task;

    public void subscribe(Set<TopicPartitionInfo> partitions) {
        log.trace("[{}] Subscribing to partitions: {}", key, partitions);
        consumer.subscribe(partitions);
    }

    public void initiateStop() {
        log.debug("[{}] Initiating stop", key);
        consumer.stop();
    }

    public void awaitFinish() {
        log.trace("[{}] Awaiting finish", key);
        if (isRunning()) {
            try {
                task.get(60, TimeUnit.SECONDS);
                task = null;
            } catch (Exception e) {
                log.warn("[{}] Failed to await for consumer to stop", key, e);
            }
        }
        log.trace("[{}] Awaited finish", key);
    }

    public boolean isRunning() {
        return task != null && !task.isDone();
    }

}
