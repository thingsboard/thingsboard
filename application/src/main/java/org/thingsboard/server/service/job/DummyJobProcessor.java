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
package org.thingsboard.server.service.job;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.job.DummyJobConfiguration;
import org.thingsboard.server.common.data.job.DummyTask;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.Task;

import java.util.List;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class DummyJobProcessor extends JobProcessor {

    @Override
    public int process(Job job, Consumer<Task> taskConsumer) {
        DummyJobConfiguration configuration = job.getConfiguration();
        for (int number = 1; number <= configuration.getSuccessfulTasksCount(); number++) {
            taskConsumer.accept(createTask(job, configuration, number, null));
        }
        if (configuration.getErrors() != null) {
            for (int number = 1; number <= configuration.getFailedTasksCount(); number++) {
                taskConsumer.accept(createTask(job, configuration, number, configuration.getErrors()));
            }
        }
        return configuration.getSuccessfulTasksCount() + configuration.getFailedTasksCount();
    }

    private Task createTask(Job job, DummyJobConfiguration configuration, int number, List<String> errors) {
        return DummyTask.builder()
                .tenantId(job.getTenantId())
                .jobId(job.getId())
                .key("Task " + number)
                .retries(configuration.getRetries())
                .number(number)
                .processingTimeMs(configuration.getTaskProcessingTimeMs())
                .errors(errors)
                .build();
    }

    @Override
    public JobType getType() {
        return JobType.DUMMY;
    }

}
