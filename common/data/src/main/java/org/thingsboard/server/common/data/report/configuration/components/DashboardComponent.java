// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.components;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.dashboardreport.DashboardReportConfig;
import org.thingsboard.server.common.data.report.configuration.image.ImageAlignment;
import org.thingsboard.server.common.data.report.configuration.image.ImageWidthType;

@Schema
@Data
@EqualsAndHashCode
@NoArgsConstructor
public class DashboardComponent extends AbstractImageComponent {

    @NotNull
    @Schema(description = "Dashboard report configuration.")
    private DashboardReportConfig config;

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.DASHBOARD;
    }

}
