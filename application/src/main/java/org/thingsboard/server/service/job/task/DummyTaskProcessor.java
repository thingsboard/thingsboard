// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.job.task;

import org.apache.commons.lang3.tuple.Pair;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.task.DummyTask;
import org.thingsboard.server.common.data.job.task.DummyTaskResult;
import org.thingsboard.server.common.data.job.task.Task;
import org.thingsboard.server.queue.task.TaskProcessor;

import java.util.Map;
import java.util.concurrent.Future;

public class DummyTaskProcessor extends TaskProcessor<DummyTask, DummyTaskResult> {

    @Override
    public DummyTaskResult process(DummyTask task) throws Exception {
        if (task.getProcessingTimeMs() > 0) {
            Thread.sleep(task.getProcessingTimeMs());
        }
        if (task.isFailAlways()) {
            throw new RuntimeException(task.getErrors().get(0));
        }
        if (task.getErrors() != null && task.getAttempt() <= task.getErrors().size()) {
            String error = task.getErrors().get(task.getAttempt() - 1);
            throw new RuntimeException(error);
        }
        return DummyTaskResult.success(task);
    }

    @Override
    public long getProcessingTimeout(DummyTask task) {
        return task.getProcessingTimeoutMs() > 0 ? task.getProcessingTimeoutMs() : 2000;
    }

    public Map<Object, Pair<Task<DummyTaskResult>, Future<DummyTaskResult>>> getCurrentTasks() {
        return currentTasks;
    }

    @Override
    public JobType getJobType() {
        return JobType.DUMMY;
    }

}
