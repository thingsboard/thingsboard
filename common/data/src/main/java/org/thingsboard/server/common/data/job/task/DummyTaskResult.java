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
package org.thingsboard.server.common.data.job.task;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.thingsboard.server.common.data.job.JobType;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString(callSuper = true)
public class DummyTaskResult extends TaskResult {

    private DummyTaskFailure failure;

    @Builder
    private DummyTaskResult(boolean success, boolean discarded, DummyTaskFailure failure) {
        super(success, discarded);
        this.failure = failure;
    }

    public static DummyTaskResult success(DummyTask task) {
        return DummyTaskResult.builder()
                .success(true)
                .build();
    }

    public static DummyTaskResult failed(DummyTask task, Throwable error) {
        return DummyTaskResult.builder()
                .failure(DummyTaskFailure.builder()
                        .error(error.getMessage())
                        .number(task.getNumber())
                        .failAlways(task.isFailAlways())
                        .build())
                .build();
    }

    public static DummyTaskResult discarded(DummyTask task) {
        return DummyTaskResult.builder()
                .discarded(true)
                .build();
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
