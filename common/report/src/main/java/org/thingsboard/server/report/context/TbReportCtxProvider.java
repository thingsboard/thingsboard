// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.context;

import org.thingsboard.server.common.data.job.task.ReportTask;

public interface TbReportCtxProvider {

    TbReportCtx newContext(ReportTask task);

}
