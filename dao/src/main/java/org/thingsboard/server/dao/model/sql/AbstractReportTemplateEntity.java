// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.report.BaseReportTemplate;
import org.thingsboard.server.common.data.report.ReportTemplateType;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.REPORT_TEMPLATE_FORMAT_PROPERTY;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
public abstract class AbstractReportTemplateEntity<T extends BaseReportTemplate> extends BaseVersionedEntity<T> {

    @Column(name = ModelConstants.REPORT_TEMPLATE_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.REPORT_TEMPLATE_CUSTOMER_ID_PROPERTY)
    private UUID customerId;

    @Column(name = ModelConstants.REPORT_TEMPLATE_NAME_PROPERTY)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = REPORT_TEMPLATE_FORMAT_PROPERTY)
    private TbReportFormat format;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.REPORT_TEMPLATE_TYPE_PROPERTY)
    private ReportTemplateType type;

    @Column(name = ModelConstants.REPORT_TEMPLATE_DESCRIPTION_PROPERTY)
    private String description;

    @Column(name = ModelConstants.EXTERNAL_ID_PROPERTY)
    private UUID externalId;

    public AbstractReportTemplateEntity() {
        super();
    }

    public AbstractReportTemplateEntity(T reportTemplate) {
        super(reportTemplate);
        if (reportTemplate.getTenantId() != null) {
            this.tenantId = reportTemplate.getTenantId().getId();
        }
        if (reportTemplate.getCustomerId() != null) {
            this.customerId = reportTemplate.getCustomerId().getId();
        }
        this.name = reportTemplate.getName();
        this.format = reportTemplate.getFormat();
        this.type = reportTemplate.getType();
        this.description = reportTemplate.getDescription();
        if (reportTemplate.getExternalId() != null) {
            this.externalId = reportTemplate.getExternalId().getId();
        }
    }

    public AbstractReportTemplateEntity(AbstractReportTemplateEntity reportTemplateEntity) {
        super(reportTemplateEntity);
        this.tenantId = reportTemplateEntity.getTenantId();
        this.customerId = reportTemplateEntity.getCustomerId();
        this.name = reportTemplateEntity.getName();
        this.format = reportTemplateEntity.getFormat();
        this.type = reportTemplateEntity.getType();
        this.description = reportTemplateEntity.getDescription();
        this.externalId = reportTemplateEntity.getExternalId();
    }

    protected BaseReportTemplate toBaseReportTemplate() {
        BaseReportTemplate reportTemplate = new BaseReportTemplate(new ReportTemplateId(id));
        reportTemplate.setCreatedTime(getCreatedTime());
        if (tenantId != null) {
            reportTemplate.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (customerId != null) {
            reportTemplate.setCustomerId(new CustomerId(customerId));
        }
        reportTemplate.setName(name);
        reportTemplate.setFormat(format);
        reportTemplate.setType(type);
        reportTemplate.setDescription(description);
        if (externalId != null) {
            reportTemplate.setExternalId(new ReportTemplateId(externalId));
        }
        return reportTemplate;
    }

}
