// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.service;

import org.thingsboard.server.common.data.job.task.ReportTask;
import org.thingsboard.server.common.data.report.ReportData;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.report.context.TbReportCtx;

public interface ReportService {

    ReportData generateReport(ReportTask task, TbReportCtx ctx) throws Exception;

    TbReportFormat getFormat();

}
