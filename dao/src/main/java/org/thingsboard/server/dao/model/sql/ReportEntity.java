// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.report.Report;

import static org.thingsboard.server.dao.model.ModelConstants.REPORT_TABLE_NAME;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = REPORT_TABLE_NAME)
public class ReportEntity extends AbstractReportEntity<Report> {

    public ReportEntity() {
        super();
    }

    public ReportEntity(Report report) {
        super(report);
    }

    @Override
    public Report toData() {
        return super.toReport();
    }

}
