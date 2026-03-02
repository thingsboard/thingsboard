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
package org.thingsboard.server.queue.memory;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueMsg;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class InMemoryTbQueueConsumer<T extends TbQueueMsg> implements TbQueueConsumer<T> {
    private final InMemoryStorage storage;
    private volatile Set<TopicPartitionInfo> partitions;
    private volatile boolean stopped;
    private volatile boolean subscribed;

    public InMemoryTbQueueConsumer(InMemoryStorage storage, String topic) {
        this.storage = storage;
        this.topic = topic;
        stopped = false;
    }

    private final String topic;

    @Override
    public String getTopic() {
        return topic;
    }

    @Override
    public void subscribe() {
        partitions = Collections.singleton(new TopicPartitionInfo(topic, null, null, true));
        subscribed = true;
    }

    @Override
    public void subscribe(Set<TopicPartitionInfo> partitions) {
        this.partitions = partitions;
        subscribed = true;
    }

    @Override
    public void stop() {
        stopped = true;
    }

    @Override
    public void unsubscribe() {
        stopped = true;
        subscribed = false;
    }

    @Override
    public List<T> poll(long durationInMillis) {
        if (subscribed) {
            @SuppressWarnings("unchecked")
            List<T> messages = partitions
                    .stream()
                    .map(tpi -> {
                        try {
                            return storage.get(tpi.getFullTopicName());
                        } catch (InterruptedException e) {
                            if (!stopped) {
                                log.error("Queue was interrupted.", e);
                            }
                            return Collections.emptyList();
                        }
                    })
                    .flatMap(List::stream)
                    .map(msg -> (T) msg).collect(Collectors.toList());
            if (messages.size() > 0) {
                return messages;
            }
            try {
                Thread.sleep(durationInMillis);
            } catch (InterruptedException e) {
                if (!stopped) {
                    log.error("Failed to sleep.", e);
                }
            }
        }
        return Collections.emptyList();
    }

    @Override
    public void commit() {
    }

    @Override
    public boolean isStopped() {
        return stopped;
    }

    @Override
    public Set<TopicPartitionInfo> getPartitions() {
        return partitions;
    }

    @Override
    public List<String> getFullTopicNames() {
        return partitions.stream().map(TopicPartitionInfo::getFullTopicName).collect(Collectors.toList());
    }

}
