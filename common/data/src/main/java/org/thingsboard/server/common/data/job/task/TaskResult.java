// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.job.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.thingsboard.server.common.data.job.JobType;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Schema(
        discriminatorProperty = "jobType",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "CF_REPROCESSING", schema = CfReprocessingTaskResult.class),
                @DiscriminatorMapping(value = "REPORT", schema = ReportTaskResult.class),
                @DiscriminatorMapping(value = "DUMMY", schema = DummyTaskResult.class)
        }
)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "jobType")
@JsonSubTypes({
        @Type(name = "CF_REPROCESSING", value = CfReprocessingTaskResult.class),
        @Type(name = "REPORT", value = ReportTaskResult.class),
        @Type(name = "DUMMY", value = DummyTaskResult.class)
})
public abstract class TaskResult {

    private String key;
    private boolean success;
    private boolean discarded;
    private long finishTs;

    protected TaskResult(boolean success, boolean discarded) {
        this.success = success;
        this.discarded = discarded;
    }

    @JsonIgnore
    public abstract JobType getJobType();

    public abstract String getError();

}
