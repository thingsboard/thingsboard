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
package org.thingsboard.server.common.data.job.task;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.task.DummyTask.DummyTaskFailure;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class DummyTaskResult extends TaskResult {

    private static final DummyTaskResult SUCCESS = new DummyTaskResult(true);

    private DummyTaskFailure failure;

    public DummyTaskResult(boolean success) {
        super(success);
    }

    public static DummyTaskResult success() {
        return SUCCESS;
    }

    @Override
    public JobType getJobType() {
        return JobType.DUMMY;
    }

}
