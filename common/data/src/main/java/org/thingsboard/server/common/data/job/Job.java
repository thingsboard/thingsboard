// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.job;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.task.TaskResult;

import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Job extends BaseData<JobId> implements HasTenantId {

    @NotNull
    private TenantId tenantId;
    @NotNull
    private JobType type;
    @NotBlank
    private String key;
    @NotNull
    private EntityId entityId;
    private String entityName; // read-only
    @NotNull
    private JobStatus status;
    @NotNull
    @Valid
    private JobConfiguration configuration;
    @NotNull
    private JobResult result;

    public static final Set<EntityType> SUPPORTED_ENTITY_TYPES = Set.of(
            EntityType.DEVICE, EntityType.ASSET, EntityType.DEVICE_PROFILE, EntityType.ASSET_PROFILE
    );

    @Builder(toBuilder = true)
    public Job(TenantId tenantId, JobType type, String key, EntityId entityId, JobConfiguration configuration) {
        this.tenantId = tenantId;
        this.type = type;
        this.key = key;
        this.entityId = entityId;
        this.configuration = configuration;
        this.configuration.setTasksKey(UUID.randomUUID().toString());
        presetResult();
    }

    public void presetResult() {
        this.result = switch (type) {
            case DUMMY -> new DummyJobResult();
        };
    }

    @SuppressWarnings("unchecked")
    public <C extends JobConfiguration> C getConfiguration() {
        return (C) configuration;
    }

    @JsonIgnore
    public String getError() {
        if (status == JobStatus.CANCELLED) {
            return "The task was cancelled";
        }
        if (result.getGeneralError() != null) {
            return result.getGeneralError();
        }
        if (result.getFailedCount() > 0 && result.getResults() != null) {
            StringBuilder errorMessage = new StringBuilder();
            for (TaskResult taskResult : result.getResults()) {
                if (taskResult.isSuccess() || taskResult.isDiscarded()) {
                    continue;
                }
                String error = taskResult.getError();
                if (error == null) {
                    continue;
                }
                if (!errorMessage.isEmpty()) {
                    if (errorMessage.length() + 2 + error.length() > 256) {
                        errorMessage.append("...");
                        break;
                    }
                    errorMessage.append("; ");
                }
                errorMessage.append(error);
            }
            return errorMessage.toString();
        }
        return "Task failed";
    }

}
