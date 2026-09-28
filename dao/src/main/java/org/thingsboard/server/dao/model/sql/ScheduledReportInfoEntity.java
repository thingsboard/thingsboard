// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.scheduler.ScheduledReportInfo;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.SCHEDULER_REPORT_EVENT_CUSTOMER_TTTLE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SCHEDULER_REPORT_EVENT_TEMPLATE_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SCHEDULER_REPORT_EVENT_TEMPLATE_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SCHEDULER_REPORT_EVENT_USER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SCHEDULER_REPORT_EVENT_USER_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SCHEDULER_REPORT_EVENT_VIEW_NAME;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = SCHEDULER_REPORT_EVENT_VIEW_NAME)
public final class ScheduledReportInfoEntity extends AbstractSchedulerEventInfoEntity<ScheduledReportInfo> {

    @Column(name = SCHEDULER_REPORT_EVENT_TEMPLATE_ID_PROPERTY)
    private UUID reportTemplateId;
    @Column(name = SCHEDULER_REPORT_EVENT_TEMPLATE_NAME_PROPERTY)
    private String reportTemplateName;
    @Column(name = SCHEDULER_REPORT_EVENT_CUSTOMER_TTTLE_PROPERTY)
    private String customerTitle;
    @Column(name = SCHEDULER_REPORT_EVENT_USER_ID_PROPERTY)
    private UUID userId;
    @Column(name = SCHEDULER_REPORT_EVENT_USER_NAME_PROPERTY)
    private String userName;

    public ScheduledReportInfoEntity() {
        super();
    }

    @Override
    public ScheduledReportInfo toData() {
        ScheduledReportInfo schedulerReportEventInfo = new ScheduledReportInfo(super.toSchedulerEventInfo());
        schedulerReportEventInfo.setTemplateInfo(new EntityInfo(reportTemplateId, EntityType.REPORT_TEMPLATE.name(), reportTemplateName));
        schedulerReportEventInfo.setCustomerTitle(customerTitle);
        schedulerReportEventInfo.setUserName(userName);
        return schedulerReportEventInfo;
    }

}
