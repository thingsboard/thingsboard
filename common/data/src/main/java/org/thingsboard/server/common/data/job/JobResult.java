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
