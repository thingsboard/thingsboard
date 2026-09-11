// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.job;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.job.task.TaskResult;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "jobType")
@JsonSubTypes({
        @Type(name = "DUMMY", value = DummyJobResult.class)
})
@Data
@NoArgsConstructor
public abstract class JobResult implements Serializable {

    private int successfulCount;
    private int failedCount;
    private int discardedCount;
    private Integer totalCount = null; // set when all tasks are submitted
    private List<TaskResult> results = new ArrayList<>();
    private String generalError;

    private long startTs;
    private long finishTs;
    private long cancellationTs;

    @JsonIgnore
    public int getCompletedCount() {
        return successfulCount + failedCount + discardedCount;
    }

    public void processTaskResult(TaskResult taskResult) {
        if (taskResult.isSuccess()) {
            if (totalCount == null || successfulCount < totalCount) {
                successfulCount++;
            }
        } else if (taskResult.isDiscarded()) {
            if (totalCount == null || discardedCount < totalCount) {
                discardedCount++;
            }
        } else {
            if (totalCount == null || failedCount < totalCount) {
                failedCount++;
            }
            if (results.size() < 100) { // preserving only first 100 errors, not reprocessing if there are more failures
                results.add(taskResult);
            }
        }
    }

    @JsonIgnore
    public abstract JobType getJobType();

}
