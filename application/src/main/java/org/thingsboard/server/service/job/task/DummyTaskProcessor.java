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
package org.thingsboard.server.service.job.task;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.job.DummyTask;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.queue.task.TaskProcessor;

@Component
@RequiredArgsConstructor
public class DummyTaskProcessor extends TaskProcessor<DummyTask> {

    @Override
    public void process(DummyTask task) throws Exception {
        if (task.getProcessingTimeMs() > 0) {
            Thread.sleep(task.getProcessingTimeMs());
        }
        if (task.getErrors() != null && task.getAttempt() <= task.getErrors().size()) {
            String error = task.getErrors().get(task.getAttempt() - 1);
            throw new RuntimeException(error);
        }
    }

    @Override
    public JobType getJobType() {
        return JobType.DUMMY;
    }

}
