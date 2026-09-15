// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data.solution;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.Collections;
import java.util.List;

@Schema
@Data
public class SolutionInstallResponse extends TenantSolutionTemplateInstructions {

    @Schema(description = "Indicates that template was installed successfully")
    private boolean success;
    @Schema(description = "List of entity IDs created during solution installation")
    private List<EntityId> createdEntityIds;
    @Schema(description = "What keys to delete during template uninstall")
    private List<String> tenantTelemetryKeys;
    @Schema(description = "What attributes to delete during template uninstall")
    private List<String> tenantAttributeKeys;

    public SolutionInstallResponse(TenantSolutionTemplateInstructions instructions, boolean success, List<EntityId> createdEntityIds) {
        this(instructions, success, createdEntityIds, Collections.emptyList(), Collections.emptyList());
    }

    public SolutionInstallResponse(TenantSolutionTemplateInstructions instructions, boolean success, List<EntityId> createdEntityIds,
                                   List<String> tenantTelemetryKeys,  List<String> tenantAttributeKeys) {
        super(instructions);
        this.success = success;
        this.createdEntityIds = createdEntityIds;
        this.tenantTelemetryKeys = tenantTelemetryKeys;
        this.tenantAttributeKeys = tenantAttributeKeys;
    }

    public SolutionInstallResponse() {
        super();
    }
}
