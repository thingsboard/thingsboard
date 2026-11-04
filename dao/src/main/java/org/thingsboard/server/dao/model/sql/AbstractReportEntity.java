// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.ReportId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.report.Report;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.dao.model.BaseSqlEntity;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.REPORT_CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.REPORT_IS_PUBLIC_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.REPORT_PUBLIC_KEY_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.REPORT_FORMAT_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.REPORT_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.REPORT_TEMPLATE_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.REPORT_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.REPORT_USER_ID_PROPERTY;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
public abstract class AbstractReportEntity<T extends Report> extends BaseSqlEntity<T> {

    @Column(name = REPORT_TENANT_ID_PROPERTY, columnDefinition = "uuid", nullable = false)
    private UUID tenantId;

    @Column(name = REPORT_CUSTOMER_ID_PROPERTY, columnDefinition = "uuid")
    private UUID customerId;

    @Column(name = REPORT_TEMPLATE_ID_PROPERTY)
    private UUID templateId;

    @Column(name = REPORT_FORMAT_PROPERTY, nullable = false)
    private TbReportFormat format;

    @Column(name = REPORT_NAME_PROPERTY, nullable = false)
    private String name;

    @Column(name = REPORT_USER_ID_PROPERTY, nullable = false)
    private UUID userId;

    @Column(name = REPORT_PUBLIC_KEY_PROPERTY)
    private String publicKey;

    @Column(name = REPORT_IS_PUBLIC_PROPERTY)
    private boolean isPublic;

    public AbstractReportEntity() {
        super();
    }

    public AbstractReportEntity(T report) {
        super(report);
        this.tenantId = report.getTenantId().getId();
        if (report.getCustomerId() != null) {
            this.customerId = report.getCustomerId().getId();
        }
        if (report.getTemplateId() != null) {
            this.templateId = report.getTemplateId().getId();
        }
        this.format = report.getFormat();
        this.name = report.getName();
        this.userId = report.getUserId().getId();
        this.publicKey = report.getPublicKey();
        this.isPublic = report.isPublic();
    }

    public AbstractReportEntity(AbstractReportEntity reportEntity) {
        super(reportEntity);
        this.tenantId = reportEntity.getTenantId();
        this.customerId = reportEntity.getCustomerId();
        this.templateId = reportEntity.getTemplateId();
        this.format = reportEntity.getFormat();
        this.name = reportEntity.getName();
        this.userId = reportEntity.getUserId();
        this.publicKey = reportEntity.getPublicKey();
        this.isPublic = reportEntity.isPublic();
    }

    protected Report toReport() {
        Report report = new Report(new ReportId(id));
        report.setCreatedTime(getCreatedTime());
        if (tenantId != null) {
            report.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (customerId != null) {
            report.setCustomerId(new CustomerId(customerId));
        }
        if (templateId != null) {
            report.setTemplateId(new ReportTemplateId(templateId));
        }
        report.setFormat(format);
        report.setName(name);
        if (userId != null) {
            report.setUserId(new UserId(userId));
        }
        report.setPublicKey(publicKey);
        report.setPublic(isPublic);
        return report;
    }

}
