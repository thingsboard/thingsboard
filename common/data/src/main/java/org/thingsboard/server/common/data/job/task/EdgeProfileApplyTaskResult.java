// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.job.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.job.JobType;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class EdgeProfileApplyTaskResult extends TaskResult {

    private EdgeProfileApplyTaskFailure failure;

    @Builder
    private EdgeProfileApplyTaskResult(boolean success, boolean discarded, EdgeProfileApplyTaskFailure failure) {
        super(success, discarded);
        this.failure = failure;
    }

    public static EdgeProfileApplyTaskResult success(EdgeProfileApplyTask task) {
        return EdgeProfileApplyTaskResult.builder()
                .success(true)
                .build();
    }

    public static EdgeProfileApplyTaskResult failed(EdgeProfileApplyTask task, Throwable error) {
        return EdgeProfileApplyTaskResult.builder()
                .failure(EdgeProfileApplyTaskFailure.builder()
                        .error(error.getMessage())
                        .entityInfo(task.getEntityInfo())
                        .build())
                .build();
    }

    public static EdgeProfileApplyTaskResult discarded(EdgeProfileApplyTask task) {
        return EdgeProfileApplyTaskResult.builder()
                .discarded(true)
                .build();
    }

    @Override
    public JobType getJobType() {
        return JobType.EDGE_PROFILE_APPLY;
    }

    @JsonIgnore
    @Override
    public String getError() {
        if (failure == null) {
            return null;
        }
        String error = failure.getError();
        if (failure.getEntityInfo() != null) {
            error = failure.getEntityInfo().getId() + ": " + error;
        }
        return error;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @NoArgsConstructor
    @SuperBuilder
    public static class EdgeProfileApplyTaskFailure extends TaskFailure {

        private EntityInfo entityInfo;

    }

}
