// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.job;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.job.DummyJobConfiguration;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.task.DummyTask;
import org.thingsboard.server.common.data.job.task.DummyTaskResult;
import org.thingsboard.server.common.data.job.task.DummyTaskResult.DummyTaskFailure;
import org.thingsboard.server.common.data.job.task.Task;
import org.thingsboard.server.common.data.job.task.TaskResult;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class DummyJobProcessor implements JobProcessor {

    @Override
    public int process(Job job, Consumer<Task<?>> taskConsumer) throws Exception {
        DummyJobConfiguration configuration = job.getConfiguration();
        if (configuration.getGeneralError() != null) {
            for (int number = 1; number <= configuration.getSubmittedTasksBeforeGeneralError(); number++) {
                taskConsumer.accept(createTask(job, configuration, number, null, false));
            }
            Thread.sleep(configuration.getTaskProcessingTimeMs() * (configuration.getSubmittedTasksBeforeGeneralError() / 2)); // sleeping so that some tasks are processed
            throw new RuntimeException(configuration.getGeneralError());
        }

        int taskNumber = 1;
        for (int i = 0; i < configuration.getSuccessfulTasksCount(); i++) {
            taskConsumer.accept(createTask(job, configuration, taskNumber, null, false));
            taskNumber++;
        }
        if (configuration.getErrors() != null) {
            for (int i = 0; i < configuration.getFailedTasksCount(); i++) {
                taskConsumer.accept(createTask(job, configuration, taskNumber, configuration.getErrors(), false));
                taskNumber++;
            }
            for (int i = 0; i < configuration.getPermanentlyFailedTasksCount(); i++) {
                taskConsumer.accept(createTask(job, configuration, taskNumber, configuration.getErrors(), true));
                taskNumber++;
            }
        }
        return configuration.getSuccessfulTasksCount() + configuration.getFailedTasksCount() + configuration.getPermanentlyFailedTasksCount();
    }

    @Override
    public void reprocess(Job job, List<TaskResult> taskFailures, Consumer<Task<?>> taskConsumer) throws Exception {
        for (TaskResult taskFailure : taskFailures) {
            DummyTaskFailure failure = ((DummyTaskResult) taskFailure).getFailure();
            taskConsumer.accept(createTask(job, job.getConfiguration(), failure.getNumber(), failure.isFailAlways() ?
                    List.of(failure.getError()) : Collections.emptyList(), failure.isFailAlways()));
        }
    }

    private DummyTask createTask(Job job, DummyJobConfiguration configuration, int number, List<String> errors, boolean failAlways) {
        return DummyTask.builder()
                .tenantId(job.getTenantId())
                .jobId(job.getId())
                .key(configuration.getTasksKey())
                .retries(configuration.getRetries())
                .number(number)
                .processingTimeMs(configuration.getTaskProcessingTimeMs())
                .errors(errors)
                .failAlways(failAlways)
                .processingTimeoutMs(configuration.getTaskProcessingTimeoutMs())
                .build();
    }

    @Override
    public JobType getType() {
        return JobType.DUMMY;
    }

}
