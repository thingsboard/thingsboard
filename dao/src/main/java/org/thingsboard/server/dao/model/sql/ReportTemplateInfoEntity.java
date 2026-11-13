// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Immutable;
import org.thingsboard.server.common.data.report.ReportTemplateInfo;
import org.thingsboard.server.dao.model.ModelConstants;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Immutable
@Table(name = ModelConstants.REPORT_TEMPLATE_INFO_VIEW_TABLE_NAME)
public class ReportTemplateInfoEntity extends AbstractReportTemplateEntity<ReportTemplateInfo> {

    @Column(name = ModelConstants.OWNER_NAME_COLUMN)
    private String ownerName;

    public ReportTemplateInfoEntity() {
        super();
    }

    @Override
    public ReportTemplateInfo toData() {
        return new ReportTemplateInfo(super.toBaseReportTemplate(), ownerName);
    }
}
