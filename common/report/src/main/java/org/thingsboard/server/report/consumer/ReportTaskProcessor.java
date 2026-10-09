// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.consumer;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.task.ReportTask;
import org.thingsboard.server.common.data.job.task.ReportTaskResult;
import org.thingsboard.server.common.data.report.Report;
import org.thingsboard.server.queue.task.TaskProcessor;
import org.thingsboard.server.queue.util.TbReportComponent;
import org.thingsboard.server.report.service.TbReportService;

@TbReportComponent
@Component
@RequiredArgsConstructor
public class ReportTaskProcessor extends TaskProcessor<ReportTask, ReportTaskResult> {

    private final TbReportService tbReportService;

    @Value("${reports.generation_timeout_ms:120000}")
    private int timeoutMs;

    @Override
    public ReportTaskResult process(ReportTask task) throws Exception {
        Report report = tbReportService.generateReport(task);
        return ReportTaskResult.success(task, report);
    }

    @Override
    public long getProcessingTimeout(ReportTask task) {
        return timeoutMs;
    }

    @Override
    public JobType getJobType() {
        return JobType.REPORT;
    }

}
