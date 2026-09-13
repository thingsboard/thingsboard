// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.housekeeper;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTaskType;

import java.util.Set;

@Component
@Getter
public class HousekeeperConfig {

    @Value("${queue.core.housekeeper.disabled-task-types:}")
    private Set<HousekeeperTaskType> disabledTaskTypes;
    @Value("${queue.core.housekeeper.task-processing-timeout-ms:120000}")
    private int taskProcessingTimeout;
    @Value("${queue.core.housekeeper.poll-interval-ms:500}")
    private int pollInterval;
    @Value("${queue.core.housekeeper.task-reprocessing-delay-ms:3000}")
    private int taskReprocessingDelay;
    @Value("${queue.core.housekeeper.max-reprocessing-attempts:10}")
    private int maxReprocessingAttempts;

}
