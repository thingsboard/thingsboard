// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.job;

import io.swagger.v3.oas.annotations.media.Schema;
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
import org.thingsboard.server.common.data.HasOwnerId;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.job.task.TaskResult;
import org.thingsboard.server.common.data.notification.NotificationRequest;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Job extends BaseData<JobId> implements TenantEntity, HasOwnerId {

    @NotNull
    private TenantId tenantId;
    private CustomerId customerId;
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
            EntityType.DEVICE, EntityType.ASSET, EntityType.DEVICE_PROFILE, EntityType.ASSET_PROFILE,
            EntityType.REPORT_TEMPLATE
    );

    @Builder(toBuilder = true)
    public Job(TenantId tenantId, CustomerId customerId, JobType type, String key, EntityId entityId, JobConfiguration configuration) {
        this.tenantId = tenantId;
        this.customerId = customerId != null && !customerId.isNullUid() ? customerId : null;
        this.type = type;
        this.key = key;
        this.entityId = entityId;
        this.configuration = configuration;
        this.configuration.setTasksKey(UUID.randomUUID().toString());
        presetResult();
    }

    public void presetResult() {
        this.result = switch (type) {
            case CF_REPROCESSING -> new CfReprocessingJobResult();
            case REPORT -> new ReportJobResult();
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

    public static ReportJobBuilder newReportJob() {
        return new ReportJobBuilder();
    }

    @Schema(description = "JSON object with Customer or Tenant Id", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public EntityId getOwnerId() {
        return customerId != null ? customerId : tenantId;
    }

    @Override
    public void setOwnerId(EntityId entityId) {
        if (EntityType.CUSTOMER.equals(entityId.getEntityType())) {
            this.customerId = new CustomerId(entityId.getId());
        } else {
            this.customerId = null;
        }
    }

    public static class ReportJobBuilder {

        private TenantId tenantId;
        private CustomerId customerId;
        private EntityId entityId;
        private final ReportJobConfiguration configuration = new ReportJobConfiguration();

        public ReportJobBuilder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public ReportJobBuilder customerId(CustomerId customerId) {
            this.customerId = customerId;
            return this;
        }

        public ReportJobBuilder reportTemplateId(ReportTemplateId reportTemplateId) {
            this.entityId = reportTemplateId;
            this.configuration.setReportTemplateId(reportTemplateId);
            return this;
        }

        public ReportJobBuilder userId(UserId userId) {
            this.configuration.setUserId(userId);
            return this;
        }

        public ReportJobBuilder timezone(String timezone) {
            this.configuration.setTimezone(timezone);
            return this;
        }

        public ReportJobBuilder makePublic(boolean makePublic) {
            this.configuration.setMakePublic(makePublic);
            return this;
        }

        public ReportJobBuilder originator(EntityId originator) {
            this.configuration.setOriginator(originator);
            return this;
        }

        public ReportJobBuilder targets(List<UUID> targets) {
            this.configuration.setTargets(targets);
            return this;
        }

        public ReportJobBuilder notificationTemplateId(NotificationTemplateId notificationTemplateId) {
            this.configuration.setNotificationTemplateId(notificationTemplateId);
            return this;
        }

        public ReportJobBuilder notificationRequests(List<NotificationRequest> notificationRequests) {
            this.configuration.setNotificationRequests(notificationRequests);
            return this;
        }

        public Job build() {
            String key = UUID.randomUUID().toString(); // we can submit multiple report jobs at once regardless of the configuration
            return new Job(tenantId, customerId, JobType.REPORT, key, entityId, configuration);
        }

    }

    @Override
    public EntityType getEntityType() {
        return EntityType.JOB;
    }

}
