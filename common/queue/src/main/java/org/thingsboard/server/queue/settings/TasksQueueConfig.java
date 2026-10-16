// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
