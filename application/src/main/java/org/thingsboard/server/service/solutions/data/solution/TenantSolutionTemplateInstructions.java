/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
