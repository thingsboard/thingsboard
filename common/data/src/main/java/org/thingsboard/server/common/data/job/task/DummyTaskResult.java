// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
