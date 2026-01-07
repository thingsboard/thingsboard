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
