// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.scheduler;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.EntityInfo;

@Data
public class ScheduledReportInfo extends SchedulerEventInfo {

    @Schema(description = "Report template info", accessMode = Schema.AccessMode.READ_ONLY)
    private EntityInfo templateInfo;
    @Schema(description = "Customer title", accessMode = Schema.AccessMode.READ_ONLY)
    private String customerTitle;
    @Schema(description = "Report user name", accessMode = Schema.AccessMode.READ_ONLY)
    private String userName;

    public ScheduledReportInfo() {
        super();
    }

    public ScheduledReportInfo(SchedulerEventInfo schedulerEventInfo) {
        super(schedulerEventInfo);
    }

}
