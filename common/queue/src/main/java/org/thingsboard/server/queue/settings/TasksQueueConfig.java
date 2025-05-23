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
package org.thingsboard.server.queue.settings;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class TasksQueueConfig {

    @Value("${queue.tasks.poll_interval:500}")
    private int pollInterval;

    @Value("${queue.tasks.partitioning_strategy:tenant}")
    private String partitioningStrategy;

    @Value("${queue.tasks.stats.topic:jobs.stats}")
    private String statsTopic;

    @Value("${queue.tasks.stats.poll_interval:500}")
    private int statsPollInterval;

    @Value("${queue.tasks.stats.processing_interval:1000}")
    private int statsProcessingInterval;

}
