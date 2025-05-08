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

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class DummyTaskResult extends TaskResult {

    private static final DummyTaskResult SUCCESS = DummyTaskResult.builder().success(true).build();
    private static final DummyTaskResult DISCARDED = DummyTaskResult.builder().discarded(true).build();

    private DummyTaskFailure failure;

    public static DummyTaskResult success() {
        return SUCCESS;
    }

    public static DummyTaskResult failed(DummyTask task, Throwable error) {
        DummyTaskResult result = new DummyTaskResult();
        result.setFailure(DummyTaskFailure.builder()
                .error(error.getMessage())
                .number(task.getNumber())
                .failAlways(task.isFailAlways())
                .build());
        return result;
    }

    public static DummyTaskResult discarded() {
        return DISCARDED;
    }

    @Override
    public JobType getJobType() {
        return JobType.DUMMY;
    }

    @Data
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    @SuperBuilder
    public static class DummyTaskFailure extends TaskFailure {

        private int number;
        private boolean failAlways;

    }

}
