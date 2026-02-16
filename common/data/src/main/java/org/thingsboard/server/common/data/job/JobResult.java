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
package org.thingsboard.server.common.data.job;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.job.task.TaskResult;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Schema(
        description = "Job execution result",
        discriminatorProperty = "jobType",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "DUMMY", schema = DummyJobResult.class)
        }
)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "jobType")
@JsonSubTypes({
        @Type(name = "DUMMY", value = DummyJobResult.class)
})
@Data
@NoArgsConstructor
public abstract class JobResult implements Serializable {

    @Schema(description = "Count of successfully completed tasks")
    private int successfulCount;
    @Schema(description = "Count of failed tasks")
    private int failedCount;
    @Schema(description = "Count of discarded tasks")
    private int discardedCount;
    @Schema(description = "Total number of tasks, set when all tasks are submitted", nullable = true)
    private Integer totalCount = null;
    @ArraySchema(schema = @Schema(ref = "#/components/schemas/TaskResult"))
    private List<TaskResult> results = new ArrayList<>();
    @Schema(description = "General error message if the job failed")
    private String generalError;

    @Schema(description = "Timestamp of the job start, in milliseconds")
    private long startTs;
    @Schema(description = "Timestamp of the job finish, in milliseconds")
    private long finishTs;
    @Schema(description = "Timestamp of the job cancellation, in milliseconds")
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

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonIgnore
    public abstract JobType getJobType();

}
