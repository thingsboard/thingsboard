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
import lombok.experimental.SuperBuilder;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.JobType;

@Data
@Schema(
        discriminatorProperty = "jobType",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "CF_REPROCESSING", schema = CfReprocessingTask.class),
                @DiscriminatorMapping(value = "REPORT", schema = ReportTask.class),
                @DiscriminatorMapping(value = "DUMMY", schema = DummyTask.class)
        }
)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "jobType")
@JsonSubTypes({
        @Type(name = "CF_REPROCESSING", value = CfReprocessingTask.class),
        @Type(name = "REPORT", value = ReportTask.class),
        @Type(name = "DUMMY", value = DummyTask.class)
})
@SuperBuilder
@AllArgsConstructor
public abstract class Task<R extends TaskResult> {

    private TenantId tenantId;
    private JobId jobId;
    private String key;
    private int retries;

    public Task() {
    }

    private int attempt;

    public abstract R toFailed(Throwable error);

    public abstract R toDiscarded();

    @JsonIgnore
    public abstract EntityId getEntityId();

    @JsonIgnore
    public abstract JobType getJobType();

}
