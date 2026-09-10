// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.job.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.JobType;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "jobType")
@JsonSubTypes({
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
