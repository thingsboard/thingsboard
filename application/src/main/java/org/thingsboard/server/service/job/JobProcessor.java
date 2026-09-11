// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.job;

import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.task.Task;
import org.thingsboard.server.common.data.job.task.TaskResult;

import java.util.List;
import java.util.function.Consumer;

public interface JobProcessor {

    int process(Job job, Consumer<Task<?>> taskConsumer) throws Exception;

    void reprocess(Job job, List<TaskResult> taskFailures, Consumer<Task<?>> taskConsumer) throws Exception;

    default void onJobFinished(Job job) {}

    JobType getType();

}
