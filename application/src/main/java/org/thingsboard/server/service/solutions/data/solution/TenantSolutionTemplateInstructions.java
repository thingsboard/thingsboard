// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data.solution;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;

@Schema
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TenantSolutionTemplateInstructions {

    @Schema(description = "Id of the main dashboard of the solution")
    private DashboardId dashboardId;
    @Schema(description = "Id of the public customer if solution has public entities")
    private CustomerId publicId;
    @Schema(description = "Is the main dashboard public")
    private boolean mainDashboardPublic;
    @Schema(description = "Markdown with solution usage instructions")
    private String details;

    public TenantSolutionTemplateInstructions(TenantSolutionTemplateInstructions instructions) {
        this.dashboardId = instructions.getDashboardId();
        this.publicId = instructions.getPublicId();
        this.mainDashboardPublic = instructions.isMainDashboardPublic();
        this.details = instructions.getDetails();
    }
}
