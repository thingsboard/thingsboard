// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Immutable;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.report.ReportInfo;
import org.thingsboard.server.dao.model.ModelConstants;

import static org.thingsboard.server.dao.model.ModelConstants.SCHEDULER_REPORT_EVENT_CUSTOMER_TTTLE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SCHEDULER_REPORT_EVENT_TEMPLATE_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SCHEDULER_REPORT_EVENT_USER_NAME_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Immutable
@Table(name = ModelConstants.REPORT_INFO_VIEW_NAME)
public class ReportInfoEntity extends AbstractReportEntity<ReportInfo> {

    @Column(name = SCHEDULER_REPORT_EVENT_TEMPLATE_NAME_PROPERTY)
    private String reportTemplateName;
    @Column(name = SCHEDULER_REPORT_EVENT_CUSTOMER_TTTLE_PROPERTY)
    private String customerTitle;
    @Column(name = SCHEDULER_REPORT_EVENT_USER_NAME_PROPERTY)
    private String userName;

    public ReportInfoEntity() {
        super();
    }

    @Override
    public ReportInfo toData() {
        ReportInfo reportInfo = new ReportInfo(super.toReport());
        if (getTemplateId() != null) {
            reportInfo.setTemplateInfo(new EntityInfo(getTemplateId(), EntityType.REPORT_TEMPLATE.name(), reportTemplateName));
        }
        reportInfo.setCustomerTitle(customerTitle);
        reportInfo.setUserName(userName);
        return reportInfo;
    }

}
