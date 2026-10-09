// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasOwnerId;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.ReportId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;

@Setter
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Report extends BaseData<ReportId> implements HasName, TenantEntity, HasCustomerId, HasOwnerId {

    @NotNull
    private TenantId tenantId;
    private CustomerId customerId;
    @NotNull
    private ReportTemplateId templateId;
    @NotNull
    private TbReportFormat format;
    @NotBlank
    private String name;
    @NotNull
    private UserId userId;
    private String publicKey;
    private boolean isPublic;

    public Report() {
    }

    public Report(ReportId reportId) {
        super(reportId);
    }

    public Report(Report report) {
        super(report);
        this.tenantId = report.getTenantId();
        this.customerId = report.getCustomerId();
        this.templateId = report.getTemplateId();
        this.name = report.getName();
        this.format = report.getFormat();
        this.userId = report.getUserId();
        this.publicKey = report.getPublicKey();
        this.isPublic = report.isPublic();
    }

    @Override
    @JsonIgnore
    public EntityType getEntityType() {
        return EntityType.REPORT;
    }

    @Schema(description = "JSON object with Customer or Tenant Id", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public EntityId getOwnerId() {
        return customerId != null && !customerId.isNullUid() ? customerId : tenantId;
    }

    @Override
    public void setOwnerId(EntityId entityId) {
        if (EntityType.CUSTOMER.equals(entityId.getEntityType())) {
            this.customerId = new CustomerId(entityId.getId());
        } else {
            this.customerId = new CustomerId(CustomerId.NULL_UUID);
        }
    }
}
