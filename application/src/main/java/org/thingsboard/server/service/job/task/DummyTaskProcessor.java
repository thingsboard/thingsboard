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
