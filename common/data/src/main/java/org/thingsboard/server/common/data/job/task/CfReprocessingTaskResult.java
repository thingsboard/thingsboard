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
public class CfReprocessingTaskResult extends TaskResult {

    private CfReprocessingTaskFailure failure;

    @Builder
    private CfReprocessingTaskResult(boolean success, boolean discarded, CfReprocessingTaskFailure failure) {
        super(success, discarded);
        this.failure = failure;
    }

    public static CfReprocessingTaskResult success(CfReprocessingTask task) {
        return CfReprocessingTaskResult.builder()
                .success(true)
                .build();
    }

    public static CfReprocessingTaskResult failed(CfReprocessingTask task, Throwable error) {
        return CfReprocessingTaskResult.builder()
                .failure(CfReprocessingTaskFailure.builder()
                        .error(error.getMessage())
                        .entityInfo(task.getEntityInfo())
                        .build())
                .build();
    }

    public static CfReprocessingTaskResult discarded(CfReprocessingTask task) {
        return CfReprocessingTaskResult.builder()
                .discarded(true)
                .build();
    }

    @Override
    public JobType getJobType() {
        return JobType.CF_REPROCESSING;
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
    public static class CfReprocessingTaskFailure extends TaskFailure {

        private EntityInfo entityInfo;

    }

}
