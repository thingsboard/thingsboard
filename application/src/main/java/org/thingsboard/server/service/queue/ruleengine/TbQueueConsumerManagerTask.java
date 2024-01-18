/**
 * Copyright © 2016-2024 The Thingsboard Authors
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

import lombok.Getter;
import lombok.ToString;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;

import java.util.Set;

@Getter
@ToString
public class TbQueueConsumerManagerTask {

    private final QueueEvent event;
    private Queue queue;
    private Set<TopicPartitionInfo> partitions;

    public TbQueueConsumerManagerTask(QueueEvent event) {
        this.event = event;
    }

    public TbQueueConsumerManagerTask(QueueEvent event, Queue queue) {
        this.event = event;
        this.queue = queue;
    }

    public TbQueueConsumerManagerTask(QueueEvent event, Set<TopicPartitionInfo> partitions) {
        this.event = event;
        this.partitions = partitions;
    }

}
