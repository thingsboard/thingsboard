// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.job.task;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.report.Report;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString(callSuper = true)
public class ReportTaskResult extends TaskResult {

    private Report report;
    private String error;

    @Builder
    private ReportTaskResult(boolean success, boolean discarded, Report report, String error) {
        super(success, discarded);
        this.report = report;
        this.error = error;
    }

    public static ReportTaskResult success(ReportTask task, Report report) {
        return ReportTaskResult.builder()
                .success(true)
                .report(report)
                .build();
    }

    public static ReportTaskResult failed(ReportTask task, Throwable error) {
        return ReportTaskResult.builder()
                .error(error.getMessage())
                .build();
    }

    public static ReportTaskResult discarded(ReportTask task) {
        return ReportTaskResult.builder()
                .discarded(true)
                .build();
    }

    @Override
    public JobType getJobType() {
        return JobType.REPORT;
    }

    @Override
    public String getError() {
        return error;
    }

}
