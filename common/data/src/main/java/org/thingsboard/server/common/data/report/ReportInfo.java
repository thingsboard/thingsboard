// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.EntityInfo;

@Data
public class ReportInfo extends Report {

    @Schema
    private EntityInfo templateInfo;
    @Schema
    private String customerTitle;
    @Schema
    private String userName;

    public ReportInfo() {
        super();
    }

    public ReportInfo(Report report) {
        super(report);
    }

}
