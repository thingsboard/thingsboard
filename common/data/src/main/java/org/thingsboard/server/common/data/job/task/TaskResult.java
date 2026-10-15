// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.job.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.job.JobType;

@Data
@NoArgsConstructor
@Schema(
        discriminatorProperty = "jobType",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "DUMMY", schema = DummyTaskResult.class)
        }
)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "jobType")
@JsonSubTypes({
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
