// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasOwnerId;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@Setter
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class BaseReportTemplate extends BaseData<ReportTemplateId> implements HasName, TenantEntity, HasCustomerId, HasOwnerId, ExportableEntity<ReportTemplateId>, HasVersion {

    private static final long serialVersionUID = -1756737145862011794L;

    @Schema(description = "JSON object with Tenant Id. Tenant Id of the report template can't be changed.", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;

    @Schema(description = "JSON object with Customer Id. Optional: when omitted the Report Template is owned by the tenant. When the request is made by a Customer user, the value is forced to the user's own Customer Id.")
    private CustomerId customerId;

    @NoXss
    @Length(fieldName = "name")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Report name", example = "Weekly Report")
    private String name;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Report format", allowableValues = {"PDF, CSV"})
    private TbReportFormat format;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Report template type", allowableValues = {"REPORT, SUB_REPORT"})
    private ReportTemplateType type;

    @NoXss
    @Length(fieldName = "description", max = 1024)
    @Schema(description = "Description")
    private String description;

    private ReportTemplateId externalId;

    private Long version;

    public BaseReportTemplate() {
        super();
    }

    public BaseReportTemplate(ReportTemplateId id) {
        super(id);
    }

    public BaseReportTemplate(BaseReportTemplate reportTemplate) {
        super(reportTemplate);
        this.tenantId = reportTemplate.getTenantId();
        this.customerId = reportTemplate.getCustomerId();
        this.name = reportTemplate.getName();
        this.format = reportTemplate.getFormat();
        this.type = reportTemplate.getType();
        this.description = reportTemplate.getDescription();
        this.externalId = reportTemplate.getExternalId();
        this.version = reportTemplate.getVersion();
    }

    @Schema(description = "JSON object with the report template Id. " +
            "Specify this field to update the report. " +
            "Referencing non-existing report template Id will cause error. " +
            "Omit this field to create new report template" )
    @Override
    public ReportTemplateId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the report template creation, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
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

    @Override
    @JsonIgnore
    public EntityType getEntityType() {
        return EntityType.REPORT_TEMPLATE;
    }

}
